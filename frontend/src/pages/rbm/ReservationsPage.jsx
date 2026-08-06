import { useEffect, useMemo, useState } from 'react'
import { Table, Button, Badge, Form, InputGroup, Row, Col, Card } from 'react-bootstrap'
import { getReservations, cancelReservation } from '../../services/rbm/reservationService'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'
import ConfirmModal from '../../components/ConfirmModal'
import { statusBadge } from '../../utils/badges'

const STATUSES = ['ALL', 'CONFIRMED', 'CHECKEDIN', 'CHECKEDOUT', 'CANCELLED', 'NOSHOW']

export default function ReservationsPage() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [status, setStatus] = useState('ALL')
  const [query, setQuery] = useState('')
  // The reservation awaiting cancel confirmation, or null when the modal is closed.
  const [pendingCancel, setPendingCancel] = useState(null)

  const load = async () => {
    setLoading(true); setError('')
    try {
      const list = await getReservations(status === 'ALL' ? {} : { status })
      setItems(list)
    } catch (e) { setError(e.message) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [status])

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return items
    return items.filter(r =>
      String(r.reservationId).includes(q) ||
      String(r.guestId ?? '').includes(q) ||
      (r.guestName || '').toLowerCase().includes(q)
    )
  }, [items, query])

  // Confirmed in-app via ConfirmModal, which shows any failure inline and reloads on success.
  const confirmCancel = async () => {
    await cancelReservation(pendingCancel.reservationId)
    await load()
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h4 className="mb-0">Reservations</h4>
        <Badge bg="secondary">{filtered.length} total</Badge>
      </div>

      <Card className="shadow-sm mb-3">
        <Card.Body>
          <Row className="g-2 align-items-end">
            <Col md={4}>
              <Form.Label className="small text-muted mb-1">Status</Form.Label>
              <Form.Select value={status} onChange={e => setStatus(e.target.value)}>
                {STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
              </Form.Select>
            </Col>
            <Col md={8}>
              <Form.Label className="small text-muted mb-1">Search</Form.Label>
              <InputGroup>
                <Form.Control
                  placeholder="Booking ID, guest ID or guest name"
                  value={query}
                  onChange={e => setQuery(e.target.value)}
                />
                {query && <Button variant="outline-secondary" onClick={() => setQuery('')}>Clear</Button>}
              </InputGroup>
            </Col>
          </Row>
        </Card.Body>
      </Card>

      {loading ? <Loader /> : error ? <div className="alert alert-danger">{error}</div> :
        filtered.length === 0 ? <EmptyState message="No reservations found" /> :
        <Table hover responsive className="align-middle">
          <thead>
            <tr>
              <th>Booking ID</th>
              <th>Guest</th>
              <th>Check-In</th>
              <th>Check-Out</th>
              <th>Nights</th>
              <th>Guests</th>
              <th>Channel</th>
              <th>Total</th>
              <th>Status</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map(r => (
              <tr key={r.reservationId}>
                <td><strong>{r.reservationId}</strong></td>
                <td>{r.guestName || <span className="text-muted small">Guest #{r.guestId}</span>}</td>
                <td>{r.checkInDate}</td>
                <td>{r.checkOutDate}</td>
                <td>{r.nights}</td>
                <td>{r.adults}A {r.children}C</td>
                <td><Badge bg="secondary">{r.bookingChannel}</Badge></td>
                <td>₹{r.totalAmount}</td>
                <td><Badge bg={statusBadge(r.status)}>{r.status}</Badge></td>
                <td>
                  {r.status === 'CONFIRMED' && (
                    <Button size="sm" variant="outline-danger" onClick={() => setPendingCancel(r)}>Cancel</Button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      }

      <ConfirmModal
        show={pendingCancel != null}
        title="Cancel reservation"
        body={pendingCancel && (
          <p className="mb-0">
            Cancel booking <strong>{pendingCancel.reservationId}</strong> for{' '}
            {pendingCancel.guestName || `Guest #${pendingCancel.guestId}`} ({pendingCancel.checkInDate} →{' '}
            {pendingCancel.checkOutDate})? This cannot be undone.
          </p>
        )}
        confirmLabel="Cancel reservation"
        onClose={() => setPendingCancel(null)}
        onConfirm={confirmCancel}
      />
    </div>
  )
}
