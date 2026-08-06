import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Card, Table, Button, Badge, Row, Col } from 'react-bootstrap'
import { getStayById, getFolioItems } from '../../services/fds/stayService'
import { getRooms } from '../../services/ric/roomService'
import Loader from '../../components/Loader'
import AddChargeModal from '../../components/AddChargeModal'
import CheckoutModal from '../../components/CheckoutModal'
import { statusBadge } from '../../utils/badges'

export default function StayDetailPage() {
  const { stayId } = useParams()
  const navigate = useNavigate()
  const [stay, setStay] = useState(null)
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showAdd, setShowAdd] = useState(false)
  const [showCheckout, setShowCheckout] = useState(false)
  const [editItem, setEditItem] = useState(null)
  // The stay only stores assignedRoomId, so resolve it to the guest-facing room number.
  const [roomNumbers, setRoomNumbers] = useState({})

  const load = async () => {
    setLoading(true); setError('')
    try {
      // Load the stay and its folio independently: a folio-items failure should not
      // blank out the stay details, and vice-versa.
      const [s, it, rooms] = await Promise.all([
        getStayById(stayId),
        getFolioItems(stayId).catch(() => []),
        getRooms().catch(() => []),
      ])
      setStay(s); setItems(it || [])
      setRoomNumbers(Object.fromEntries((rooms || []).map(r => [r.roomId, r.roomNumber])))
    } catch (e) { setError(e.message) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [stayId])

  if (loading) return <Loader />
  if (error) return <div className="alert alert-danger">{error}</div>
  if (!stay) return null

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h4 className="mb-0">Stay Folio #{stay.stayId} <Badge bg={statusBadge(stay.status)}>{stay.status}</Badge></h4>
        <div>
          <Button variant="outline-primary" className="me-2" disabled={stay.status !== 'ACTIVE'} onClick={() => setShowAdd(true)}>+ Add Charge</Button>
          <Button variant="danger" disabled={stay.status !== 'ACTIVE'} onClick={() => setShowCheckout(true)}>Checkout</Button>
        </div>
      </div>
      <Row>
        <Col md={4}>
          <Card className="shadow-sm mb-3">
            <Card.Body>
              <div className="mb-2"><strong>Reservation:</strong> {stay.reservationId}</div>
              <div className="mb-2"><strong>Guest ID:</strong> {stay.guestId}</div>
              <div className="mb-2"><strong>Room:</strong> {roomNumbers[stay.assignedRoomId] ?? (stay.assignedRoomId != null ? `id ${stay.assignedRoomId}` : '—')}</div>
              <div className="mb-2"><strong>Check-In:</strong> {stay.actualCheckIn}</div>
              {stay.actualCheckOut && <div className="mb-2"><strong>Check-Out:</strong> {stay.actualCheckOut}</div>}
              <hr />
              <div className="d-flex justify-content-between">
                <strong>Folio Balance</strong><strong className="text-primary">₹{stay.folioBalance}</strong>
              </div>
            </Card.Body>
          </Card>
        </Col>
        <Col md={8}>
          <Card className="shadow-sm">
            <Card.Header><strong>Folio Items</strong></Card.Header>
            <Card.Body>
              {items.length === 0 ? <p className="text-muted">No charges posted yet.</p> :
                <Table hover responsive size="sm">
                  <thead><tr><th>Type</th><th>Description</th><th>Amount</th><th>Posted</th>{stay.status === 'ACTIVE' && <th></th>}</tr></thead>
                  <tbody>
                    {items.map(it => (
                      <tr key={it.folioItemId}>
                        <td><Badge bg="secondary">{it.chargeType}</Badge></td>
                        <td>{it.description}</td>
                        <td>₹{it.amount}</td>
                        <td className="small">{it.postedDate}</td>
                        {stay.status === 'ACTIVE' && (
                          <td className="text-end">
                            <Button size="sm" variant="outline-secondary" onClick={() => setEditItem(it)}>Edit</Button>
                          </td>
                        )}
                      </tr>
                    ))}
                  </tbody>
                </Table>
              }
            </Card.Body>
          </Card>
        </Col>
      </Row>
      <AddChargeModal show={showAdd} stayId={stay.stayId} onClose={() => setShowAdd(false)} onSaved={() => { setShowAdd(false); load() }} />
      <AddChargeModal show={!!editItem} stayId={stay.stayId} item={editItem} onClose={() => setEditItem(null)} onSaved={() => { setEditItem(null); load() }} />
      <CheckoutModal
        show={showCheckout}
        stay={stay}
        roomLabel={roomNumbers[stay.assignedRoomId] != null ? `Room ${roomNumbers[stay.assignedRoomId]}` : null}
        folio={items}
        onClose={() => setShowCheckout(false)}
        onDone={() => { setShowCheckout(false); navigate('/front-desk') }}
      />
    </div>
  )
}