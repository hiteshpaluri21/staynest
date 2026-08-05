import { useEffect, useMemo, useState } from 'react'
import { Table, Button, Badge, Form, InputGroup, Row, Col, Card } from 'react-bootstrap'
import { Link } from 'react-router-dom'
import { getStays } from '../../services/fds/stayService'
import { getReservations } from '../../services/rbm/reservationService'
import { getRooms } from '../../services/ric/roomService'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'
import { statusBadge } from '../../utils/badges'

const STATUSES = ['ALL', 'ACTIVE', 'CHECKEDOUT']

const fmt = (dt) => dt ? new Date(dt).toLocaleString() : '—'

export default function StayRecordsPage() {
  const [items, setItems] = useState([])
  const [guestNames, setGuestNames] = useState({}) // guestId -> name
  const [roomNumbers, setRoomNumbers] = useState({}) // roomId -> roomNumber
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [status, setStatus] = useState('ALL')
  const [query, setQuery] = useState('')

  const load = async () => {
    setLoading(true); setError('')
    try {
      const [list, reservations, rooms] = await Promise.all([
        getStays(status === 'ALL' ? {} : { status }),
        getReservations({}).catch(() => []),
        getRooms().catch(() => []),
      ])
      setItems(list)
      setGuestNames(Object.fromEntries((reservations || [])
        .filter(r => r.guestId != null && r.guestName)
        .map(r => [r.guestId, r.guestName])))
      setRoomNumbers(Object.fromEntries((rooms || []).map(r => [r.roomId, r.roomNumber])))
    } catch (e) { setError(e.message) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [status])

  const guestLabel = (guestId) => guestNames[guestId] || `Guest #${guestId}`
  const roomLabel = (roomId) => roomNumbers[roomId] != null ? `Room ${roomNumbers[roomId]}` : `Room #${roomId}`

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return items
    return items.filter(s =>
      String(s.stayId).includes(q) ||
      String(s.reservationId ?? '').includes(q) ||
      String(s.guestId ?? '').includes(q) ||
      (guestNames[s.guestId] || '').toLowerCase().includes(q) ||
      String(roomNumbers[s.assignedRoomId] ?? s.assignedRoomId ?? '').toLowerCase().includes(q)
    )
  }, [items, query, guestNames, roomNumbers])

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h4 className="mb-0">Stay Records</h4>
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
                  placeholder="Stay ID, booking ID, guest ID or room"
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
        filtered.length === 0 ? <EmptyState message="No stay records found" /> :
        <Table hover responsive className="align-middle">
          <thead>
            <tr>
              <th>Stay ID</th>
              <th>Booking</th>
              <th>Guest</th>
              <th>Room</th>
              <th>Checked In</th>
              <th>Checked Out</th>
              <th>Balance</th>
              <th>Status</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map(s => (
              <tr key={s.stayId}>
                <td><strong>{s.stayId}</strong></td>
                <td>{s.reservationId}</td>
                <td>{guestLabel(s.guestId)}</td>
                <td><Badge bg="secondary">{roomLabel(s.assignedRoomId)}</Badge></td>
                <td>{fmt(s.actualCheckIn)}</td>
                <td>{fmt(s.actualCheckOut)}</td>
                <td>₹{s.folioBalance}</td>
                <td><Badge bg={statusBadge(s.status)}>{s.status}</Badge></td>
                <td>
                  <Button as={Link} to={`/stays/${s.stayId}`} size="sm" variant="outline-primary">Folio</Button>
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      }
    </div>
  )
}
