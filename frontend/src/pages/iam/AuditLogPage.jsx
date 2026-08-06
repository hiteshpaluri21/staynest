import { useEffect, useMemo, useState } from 'react'
import { Card, Table, Badge, Form, Button, Row, Col } from 'react-bootstrap'
import { FaSyncAlt, FaUndo } from 'react-icons/fa'
import { getAuditLogs, getAuditLogsByAction, getAuditLogsByUser, getAuditLogsByRange } from '../../services/iam/auditLogService'
import { getUsers } from '../../services/iam/userService'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'
import { actionBadge, roleBadge } from '../../utils/badges'

const PAGE_SIZE = 20

// The actions and entity types the AuditRecorder in each service writes. Kept in step
// with the record(...) calls in the service layer — a value missing here is simply not
// offered as a filter, the trail itself still shows it.
const ACTIONS = [
  'LOGIN', 'CREATE', 'CREATE_USER', 'UPDATE', 'UPDATE_STATUS', 'UPDATE_PRICE', 'UPDATE_AVAILABILITY',
  'UPDATE_LOYALTY', 'DELETE', 'SOFT_DELETE', 'CANCEL', 'CHECKIN', 'CHECKOUT', 'ASSIGN',
  'RESOLVE', 'POST_CHARGE', 'BLACKLIST', 'MARK_READ', 'MARK_ALL_READ',
]
const ENTITY_TYPES = [
  'User', 'ROOM', 'ROOMTYPE', 'RATEPLAN', 'RESERVATION', 'GUESTPROFILE', 'STAY', 'FOLIOITEM',
  'HOUSEKEEPINGTASK', 'MAINTENANCEREQUEST', 'MENUITEM', 'FBORDER', 'DININGRESERVATION', 'NOTIFICATION',
]

const EMPTY_FILTERS = { userId: '', action: '', entityType: '', start: '', end: '' }

/** Renders 2026-08-07T14:03:22.4 as 07 Aug 2026, 14:03. */
const formatStamp = (ts) => {
  if (!ts) return '—'
  const d = new Date(ts)
  if (Number.isNaN(d.getTime())) return ts
  return d.toLocaleString(undefined, {
    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

export default function AuditLogPage() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [users, setUsers] = useState([])
  const [page, setPage] = useState(0)
  // Server paging only applies to the unfiltered listing; the filtered endpoints
  // return a plain list, so `totalPages` comes from the rows we hold instead.
  const [serverPages, setServerPages] = useState(null)

  // `applied` is what the table shows; `draft` is what the form holds until Apply.
  const [draft, setDraft] = useState(EMPTY_FILTERS)
  const [applied, setApplied] = useState(EMPTY_FILTERS)

  useEffect(() => {
    // Names for the user ids on each entry. A failure here is not worth blocking
    // the trail for — the table falls back to showing the raw id.
    getUsers().then(setUsers).catch(() => setUsers([]))
  }, [])

  const userById = useMemo(
    () => Object.fromEntries(users.map(u => [String(u.userId), u])),
    [users],
  )

  useEffect(() => {
    let cancelled = false
    const load = async () => {
      setLoading(true); setError('')
      try {
        /*
         * Only one filter can be pushed to the server, as each endpoint takes a
         * single criterion. The narrowest one wins and the rest are applied below.
         */
        let list
        let pages = null
        if (applied.start && applied.end) {
          list = await getAuditLogsByRange(`${applied.start}T00:00:00`, `${applied.end}T23:59:59`)
        } else if (applied.userId) {
          list = await getAuditLogsByUser(applied.userId)
        } else if (applied.action) {
          list = await getAuditLogsByAction(applied.action)
        } else {
          const p = await getAuditLogs({ page, size: PAGE_SIZE })
          list = p?.content || []
          pages = p?.totalPages ?? 1
        }
        if (cancelled) return
        setRows(list || [])
        setServerPages(pages)
      } catch (e) {
        if (!cancelled) { setError(e.message); setRows([]) }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => { cancelled = true }
  }, [applied, page])

  // Whatever the server could not narrow, filtered here.
  const filtered = useMemo(() => rows.filter(r =>
    (!applied.userId || String(r.userId) === String(applied.userId)) &&
    (!applied.action || r.action === applied.action) &&
    (!applied.entityType || r.entityType === applied.entityType)
  ), [rows, applied])

  const clientPaged = serverPages === null
  const totalPages = clientPaged ? Math.max(1, Math.ceil(filtered.length / PAGE_SIZE)) : serverPages
  const visible = clientPaged ? filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE) : filtered

  const apply = () => { setPage(0); setApplied(draft) }
  const reset = () => { setPage(0); setDraft(EMPTY_FILTERS); setApplied(EMPTY_FILTERS) }
  const refresh = () => { setApplied({ ...applied }) }

  // A start without an end (or the reverse) cannot be sent to the range endpoint.
  const halfRange = Boolean(draft.start) !== Boolean(draft.end)

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h4 className="mb-0">Audit Logs</h4>
        <Button variant="outline-primary" size="sm" onClick={refresh}>
          <FaSyncAlt className="me-2" /> Refresh
        </Button>
      </div>

      <Card className="shadow-sm mb-3">
        <Card.Body>
          <Row className="g-2 align-items-end">
            <Col md={3}>
              <Form.Label className="small mb-1">User</Form.Label>
              <Form.Select value={draft.userId} onChange={e => setDraft({ ...draft, userId: e.target.value })}>
                <option value="">All users</option>
                {users.map(u => <option key={u.userId} value={u.userId}>{u.name} (#{u.userId})</option>)}
              </Form.Select>
            </Col>
            <Col md={3}>
              <Form.Label className="small mb-1">Action</Form.Label>
              <Form.Select value={draft.action} onChange={e => setDraft({ ...draft, action: e.target.value })}>
                <option value="">All actions</option>
                {ACTIONS.map(a => <option key={a}>{a}</option>)}
              </Form.Select>
            </Col>
            <Col md={3}>
              <Form.Label className="small mb-1">Entity</Form.Label>
              <Form.Select value={draft.entityType} onChange={e => setDraft({ ...draft, entityType: e.target.value })}>
                <option value="">All entities</option>
                {ENTITY_TYPES.map(t => <option key={t}>{t}</option>)}
              </Form.Select>
            </Col>
            <Col md={3} className="d-flex gap-2">
              <Button className="flex-grow-1" onClick={apply} disabled={halfRange}>Apply</Button>
              <Button variant="outline-secondary" onClick={reset} title="Clear filters"><FaUndo /></Button>
            </Col>
            <Col md={3}>
              <Form.Label className="small mb-1">From</Form.Label>
              <Form.Control type="date" value={draft.start} onChange={e => setDraft({ ...draft, start: e.target.value })} />
            </Col>
            <Col md={3}>
              <Form.Label className="small mb-1">To</Form.Label>
              <Form.Control type="date" value={draft.end} onChange={e => setDraft({ ...draft, end: e.target.value })} />
            </Col>
            <Col md={6}>
              {halfRange && <div className="small text-danger">Pick both a From and a To date, or leave both empty.</div>}
            </Col>
          </Row>
        </Card.Body>
      </Card>

      <Card className="shadow-sm">
        <Card.Body>
          {loading ? <Loader /> :
            error ? <div className="alert alert-danger">{error}</div> :
              visible.length === 0 ? <EmptyState message="No audit entries match these filters" /> :
                <>
                  <Table hover responsive className="align-middle">
                    <thead>
                      <tr>
                        <th>ID</th><th>When</th><th>User</th><th>Role</th><th>Action</th><th>Entity</th><th>Entity ID</th>
                      </tr>
                    </thead>
                    <tbody>
                      {visible.map(a => {
                        const u = userById[String(a.userId)]
                        return (
                          <tr key={a.auditId}>
                            <td className="small text-muted">{a.auditId}</td>
                            <td className="small">{formatStamp(a.timestamp)}</td>
                            <td>{u ? u.name : (a.userId != null ? `#${a.userId}` : 'System')}</td>
                            <td>{u ? <Badge bg={roleBadge(u.role)}>{u.role}</Badge> : '—'}</td>
                            <td><Badge bg={actionBadge(a.action)}>{a.action}</Badge></td>
                            <td>{a.entityType}</td>
                            <td className="small text-muted">{a.entityId ?? '—'}</td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </Table>

                  <div className="d-flex justify-content-between align-items-center">
                    <span className="small text-muted">Page {page + 1} of {totalPages}</span>
                    <div className="d-flex gap-2">
                      <Button size="sm" variant="outline-secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button>
                      <Button size="sm" variant="outline-secondary" disabled={page + 1 >= totalPages} onClick={() => setPage(page + 1)}>Next</Button>
                    </div>
                  </div>
                </>
          }
        </Card.Body>
      </Card>
    </div>
  )
}
