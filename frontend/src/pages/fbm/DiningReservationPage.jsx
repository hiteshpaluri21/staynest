import { useEffect, useState } from 'react'
import { Table, Button, Badge, Form, Modal, Alert, Row, Col } from 'react-bootstrap'
import { getDiningReservations, createDiningReservation, updateDiningStatus, cancelDiningReservation } from '../../services/fbm/diningService'
import { getStays } from '../../services/fds/stayService'
import { getGuestById } from '../../services/rbm/guestService'
import { useAuth } from '../../context/AuthContext'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'
import ConfirmModal from '../../components/ConfirmModal'
import { statusBadge } from '../../utils/badges'

const OUTLETS = ['The Garden Bistro', 'Sky Lounge', 'Poolside Bar', 'Rooftop Grill']

// Matches DEFAULT_SITTING in DiningReservationServiceImpl, so the form shows the same end
// time the server would pick for itself.
const SITTING_MINUTES = 90

/** "19:00" + 90 → "20:30". Clamped at the end of the day, as a booking is filed against one date. */
const addMinutes = (time, minutes) => {
  const [h, m] = String(time).split(':').map(Number)
  if (Number.isNaN(h) || Number.isNaN(m)) return ''
  const total = h * 60 + m + minutes
  if (total >= 24 * 60) return '23:59'
  return `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`
}

/** Times come back as "20:30:00"; the seconds add nothing for a table booking. */
const shortTime = (time) => (time ? String(time).slice(0, 5) : '—')

const EMPTY_FORM = { restaurantOutlet: OUTLETS[0], date: '', time: '19:00', endTime: addMinutes('19:00', SITTING_MINUTES), covers: 2 }

/**
 * Dining reservations follow the same split as front desk / housekeeping: the guest raises the
 * request from their own account, and F&B staff only work the queue (seat, then complete). Staff
 * therefore get no booking form — a table is never reserved on a guest's behalf here.
 */
export default function DiningReservationPage() {
  const { user, guestId } = useAuth()
  const isGuest = user?.role === 'GUEST'
  const [items, setItems] = useState([])
  const [guestNames, setGuestNames] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [show, setShow] = useState(false)
  const [form, setForm] = useState(EMPTY_FORM)
  const [saveErr, setSaveErr] = useState('')
  const [dateFilter, setDateFilter] = useState('')
  const [actionErr, setActionErr] = useState('')
  // The reservation awaiting cancel confirmation, or null when the modal is closed.
  const [pendingCancel, setPendingCancel] = useState(null)
  // The guest's open stay, if any — attached to the booking so F&B can tie it back to the room.
  const [activeStayId, setActiveStayId] = useState(null)

  const load = async () => {
    setLoading(true); setError('')
    try {
      if (isGuest) {
        const [mine, stays] = await Promise.all([
          getDiningReservations({ guestId }),
          getStays({ guestId }).catch(() => []),
        ])
        setItems(mine || [])
        setActiveStayId((stays || []).find(s => s.status === 'ACTIVE')?.stayId ?? null)
      } else {
        const all = await getDiningReservations(dateFilter ? { date: dateFilter } : {})
        setItems(all || [])
        // Show who the booking is for rather than a raw id; a failed lookup falls back to the id.
        const ids = [...new Set((all || []).map(d => d.guestId))]
        const names = await Promise.all(ids.map(async id => [id, (await getGuestById(id).catch(() => null))?.name ?? null]))
        setGuestNames(Object.fromEntries(names.filter(([, n]) => n)))
      }
    }
    catch (e) { setError(e.message) } finally { setLoading(false) }
  }
  // For guests, hold off until guestId resolves so reservations aren't queried with undefined.
  useEffect(() => { if (!isGuest || guestId) load() }, [dateFilter, isGuest, guestId])

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }))

  // Moving the start drags the end along, keeping the sitting the same length. The guest can
  // still shorten or extend it afterwards.
  const setStart = (time) => setForm(f => ({ ...f, time, endTime: addMinutes(time, SITTING_MINUTES) }))

  // The server rejects this too; catching it here saves a round trip and reads better.
  const endBeforeStart = Boolean(form.time && form.endTime && form.endTime <= form.time)

  const submit = async (e) => {
    e.preventDefault(); setSaveErr('')
    if (endBeforeStart) { setSaveErr('The end time must be after the start time.'); return }
    try {
      await createDiningReservation({
        ...form,
        guestId,
        stayId: activeStayId,
        covers: Number(form.covers),
      })
      setShow(false)
      setForm(EMPTY_FORM)
      load()
    } catch (err) { setSaveErr(err.message) }
  }

  // Action failures land in an inline alert above the table rather than a native dialog. `error` is
  // not reused for these — it replaces the whole table, which would hide the row just acted on.
  const setStatus = async (id, status) => {
    setActionErr('')
    try { await updateDiningStatus(id, status); load() } catch (e) { setActionErr(e.message) }
  }

  // Confirmed in-app via ConfirmModal; it surfaces its own errors and reloads on success.
  const confirmCancel = async () => {
    await cancelDiningReservation(pendingCancel.diningResId)
    await load()
  }

  const guestLabel = (id) => guestNames[id] ?? `Guest ${id}`

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h4 className="mb-0">{isGuest ? 'My Dining Reservations' : 'Dining Reservations'}</h4>
        {isGuest && (
          <Button onClick={() => { setSaveErr(''); setShow(true) }}>+ New Reservation</Button>
        )}
      </div>
      {!isGuest && (
        <Form.Control type="date" style={{ maxWidth: 200 }} className="mb-3" value={dateFilter} onChange={e => setDateFilter(e.target.value)} />
      )}
      {actionErr && <Alert variant="danger" dismissible onClose={() => setActionErr('')} className="py-2">{actionErr}</Alert>}
      {loading ? <Loader /> : error ? <div className="alert alert-danger">{error}</div> :
        items.length === 0 ? <EmptyState message={isGuest ? 'You have no dining reservations' : 'No dining reservations'} /> :
        <Table hover responsive className="align-middle">
          <thead><tr><th>ID</th>{!isGuest && <th>Guest</th>}<th>Outlet</th><th>Date</th><th>From</th><th>Until</th><th>Covers</th><th>Status</th><th>Action</th></tr></thead>
          <tbody>
            {items.map(d => (
              <tr key={d.diningResId}>
                <td>{d.diningResId}</td>
                {!isGuest && <td>{guestLabel(d.guestId)}</td>}
                <td>{d.restaurantOutlet}</td>
                <td>{d.date}</td>
                <td>{shortTime(d.time)}</td>
                {/* Bookings made before end times existed have none — those show a dash. */}
                <td>{shortTime(d.endTime)}</td>
                <td>{d.covers}</td>
                <td><Badge bg={statusBadge(d.status)}>{d.status}</Badge></td>
                <td>
                  {/* Seating and completing are staff actions; cancelling is open to the guest who
                      booked the table, and only while it is still CONFIRMED. */}
                  {!isGuest && d.status === 'CONFIRMED' && <Button size="sm" variant="outline-success" className="me-1" onClick={() => setStatus(d.diningResId, 'SEATED')}>Seat</Button>}
                  {!isGuest && d.status === 'SEATED' && <Button size="sm" variant="outline-primary" onClick={() => setStatus(d.diningResId, 'COMPLETED')}>Complete</Button>}
                  {d.status === 'CONFIRMED' && <Button size="sm" variant="outline-danger" onClick={() => { setActionErr(''); setPendingCancel(d) }}>Cancel</Button>}
                  {d.status !== 'CONFIRMED' && d.status !== 'SEATED' && <span className="text-muted small">—</span>}
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      }

      <ConfirmModal
        show={pendingCancel != null}
        title="Cancel dining reservation"
        body={pendingCancel && (
          <p className="mb-0">
            Cancel the table for {pendingCancel.covers} at <strong>{pendingCancel.restaurantOutlet}</strong>
            {' '}on {pendingCancel.date}, {shortTime(pendingCancel.time)}–{shortTime(pendingCancel.endTime)}?
            {' '}This cannot be undone.
          </p>
        )}
        confirmLabel="Cancel reservation"
        onClose={() => setPendingCancel(null)}
        onConfirm={confirmCancel}
      />

      <Modal show={show} onHide={() => setShow(false)}>
        <Modal.Header closeButton><Modal.Title>New Dining Reservation</Modal.Title></Modal.Header>
        <Form onSubmit={submit}>
          <Modal.Body>
            {saveErr && <Alert variant="danger" className="py-2">{saveErr}</Alert>}
            <Form.Group className="mb-3"><Form.Label>Outlet</Form.Label><Form.Select value={form.restaurantOutlet} onChange={e => set('restaurantOutlet', e.target.value)}>{OUTLETS.map(o => <option key={o}>{o}</option>)}</Form.Select></Form.Group>
            <Row>
              <Col xs={6} md={3}><Form.Group className="mb-3"><Form.Label>Date</Form.Label><Form.Control type="date" required min={new Date().toISOString().split('T')[0]} value={form.date} onChange={e => set('date', e.target.value)} /></Form.Group></Col>
              <Col xs={6} md={3}><Form.Group className="mb-3"><Form.Label>Entry Time</Form.Label><Form.Control type="time" required value={form.time} onChange={e => setStart(e.target.value)} /></Form.Group></Col>
              <Col xs={6} md={3}>
                <Form.Group className="mb-3">
                  <Form.Label>Exit Time</Form.Label>
                  <Form.Control type="time" required value={form.endTime} isInvalid={endBeforeStart} onChange={e => set('endTime', e.target.value)} />
                </Form.Group>
              </Col>
              <Col xs={6} md={3}><Form.Group className="mb-3"><Form.Label>Covers</Form.Label><Form.Control type="number" min="1" required value={form.covers} onChange={e => set('covers', e.target.value)} /></Form.Group></Col>
            </Row>
            {endBeforeStart && <p className="small text-danger mb-2">The end time must be after the start time.</p>}
            <p className="text-muted small mb-1">
              The outlet is yours for this whole window, so nobody else can book it while you
              have it. Pick a different time if the one you want is already taken.
            </p>
            <p className="text-muted small mb-0">
              {activeStayId != null
                ? `This booking will be linked to your current stay (${activeStayId}).`
                : 'You are not checked in, so this booking will not be linked to a stay.'}
            </p>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShow(false)}>Cancel</Button>
            <Button type="submit">Reserve</Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </div>
  )
}
