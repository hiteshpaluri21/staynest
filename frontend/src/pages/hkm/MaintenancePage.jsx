import { useEffect, useMemo, useState } from 'react'
import { Table, Button, Badge, Form, Modal, Alert } from 'react-bootstrap'
import { getRequests, reportIssue, updateRequestStatus } from '../../services/hkm/maintenanceService'
import { getStays } from '../../services/fds/stayService'
import { getRooms } from '../../services/ric/roomService'
import { useAuth } from '../../context/AuthContext'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'
import { statusBadge } from '../../utils/badges'

const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']
const STATUSES = ['OPEN', 'INPROGRESS', 'RESOLVED', 'DEFERRED']

export default function MaintenancePage() {
  const { user, guestId } = useAuth()
  const isGuest = user?.role === 'GUEST'
  const [items, setItems] = useState([])
  const [rooms, setRooms] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [show, setShow] = useState(false)
  const [form, setForm] = useState({ roomId: '', issueDescription: '', priority: 'MEDIUM' })
  const [saveErr, setSaveErr] = useState('')
  // Failures from row actions; `error` is not reused, as it replaces the whole table.
  const [actionErr, setActionErr] = useState('')
  // For guests: the room they are currently checked in to (null until resolved / when not checked in).
  const [activeRoomId, setActiveRoomId] = useState(null)

  const load = async () => {
    setLoading(true); setError('')
    try {
      // Rooms let us show a human room number instead of a raw id (best-effort; ignore if not permitted).
      const roomList = await getRooms().catch(() => [])
      setRooms(roomList || [])
      if (isGuest) {
        // A guest sees only their own requests, and can raise a new one for their active stay's room.
        // reportedBy is an IAM userId, but stays are keyed by the reservation-service guestId —
        // two different ids, and mixing them up hid the guest's active stay.
        const [mine, stays] = await Promise.all([
          getRequests({ reportedBy: user?.userId }).catch(() => []),
          getStays({ guestId }).catch(() => []),
        ])
        const active = (stays || []).find(s => s.status === 'ACTIVE')
        setActiveRoomId(active?.assignedRoomId ?? null)
        setItems(mine || [])
      } else {
        setItems(await getRequests())
      }
    } catch (e) { setError(e.message) } finally { setLoading(false) }
  }
  // For guests, hold off until guestId resolves so the stay lookup isn't run with undefined.
  useEffect(() => { if (!isGuest || guestId) load() }, [user, guestId, isGuest])

  const roomNumberMap = useMemo(
    () => Object.fromEntries((rooms || []).map(r => [r.roomId, r.roomNumber])),
    [rooms]
  )
  const roomLabel = (roomId) => {
    const num = roomNumberMap[roomId]
    return num != null ? `Room ${num}` : `Room ${roomId}`
  }

  const changeStatus = async (id, status) => {
    setActionErr('')
    try { await updateRequestStatus(id, status); load() } catch (e) { setActionErr(e.message) }
  }

  const openReport = () => {
    setSaveErr('')
    // Pre-fill the guest's checked-in room; staff pick from the dropdown.
    setForm({ roomId: isGuest && activeRoomId != null ? String(activeRoomId) : '', issueDescription: '', priority: 'MEDIUM' })
    setShow(true)
  }

  const submit = async (e) => {
    e.preventDefault(); setSaveErr('')
    // Guests may only report for the room of their active stay.
    const roomId = isGuest ? activeRoomId : Number(form.roomId)
    if (roomId == null || Number.isNaN(roomId) || roomId === 0) { setSaveErr('A valid room is required'); return }
    try {
      await reportIssue({ ...form, roomId: Number(roomId), reportedBy: user?.userId })
      setShow(false); setForm({ roomId: '', issueDescription: '', priority: 'MEDIUM' }); load()
    } catch (err) { setSaveErr(err.message) }
  }

  const canReport = !isGuest || activeRoomId != null

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h4 className="mb-0">{isGuest ? 'My Maintenance Requests' : 'Maintenance Requests'}</h4>
        {canReport && (
          <Button onClick={openReport}>+ Report Issue</Button>
        )}
      </div>

      {isGuest && !canReport && !loading && (
        <Alert variant="info">You need to be checked in to raise a maintenance request.</Alert>
      )}

      {actionErr && <Alert variant="danger" dismissible onClose={() => setActionErr('')} className="py-2">{actionErr}</Alert>}
      {loading ? <Loader /> : error ? <div className="alert alert-danger">{error}</div> :
        items.length === 0 ? <EmptyState message={isGuest ? 'You have no maintenance requests' : 'No maintenance requests'} /> :
        <Table hover responsive className="align-middle">
          <thead><tr><th>ID</th><th>Room</th><th>Issue</th><th>Priority</th><th>Raised</th><th>Status</th>{!isGuest && <th style={{ width: 170 }}>Action</th>}</tr></thead>
          <tbody>
            {items.map(m => (
              <tr key={m.requestId}>
                <td>{m.requestId}</td>
                <td>{roomLabel(m.roomId)}</td>
                <td>{m.issueDescription}</td>
                <td><Badge bg={m.priority === 'URGENT' ? 'danger' : m.priority === 'HIGH' ? 'warning' : 'secondary'}>{m.priority}</Badge></td>
                <td className="small">{m.raisedDate}</td>
                <td><Badge bg={statusBadge(m.status)}>{m.status}</Badge></td>
                {!isGuest && (
                  <td>
                    <Form.Select
                      size="sm"
                      value={m.status}
                      onChange={e => changeStatus(m.requestId, e.target.value)}
                    >
                      {STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
                    </Form.Select>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </Table>
      }

      <Modal show={show} onHide={() => setShow(false)}>
        <Modal.Header closeButton><Modal.Title>Report Maintenance Issue</Modal.Title></Modal.Header>
        <Form onSubmit={submit}>
          <Modal.Body>
            {saveErr && <Alert variant="danger" className="py-2">{saveErr}</Alert>}
            <Form.Group className="mb-3">
              <Form.Label>Room</Form.Label>
              {isGuest ? (
                <Form.Control value={activeRoomId != null ? roomLabel(activeRoomId) : ''} disabled />
              ) : (
                <Form.Select required value={form.roomId} onChange={e => setForm(f => ({ ...f, roomId: e.target.value }))}>
                  <option value="">Select a room…</option>
                  {rooms.map(r => (
                    <option key={r.roomId} value={r.roomId}>
                      Room {r.roomNumber ?? r.roomId}{r.status ? ` — ${r.status}` : ''}
                    </option>
                  ))}
                </Form.Select>
              )}
              {isGuest && <Form.Text className="text-muted">Your checked-in room.</Form.Text>}
            </Form.Group>
            <Form.Group className="mb-3"><Form.Label>Issue Description</Form.Label><Form.Control as="textarea" rows={3} required value={form.issueDescription} onChange={e => setForm(f => ({ ...f, issueDescription: e.target.value }))} /></Form.Group>
            <Form.Group><Form.Label>Priority</Form.Label><Form.Select value={form.priority} onChange={e => setForm(f => ({ ...f, priority: e.target.value }))}>{PRIORITIES.map(p => <option key={p}>{p}</option>)}</Form.Select></Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShow(false)}>Cancel</Button>
            <Button type="submit">Report</Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </div>
  )
}
