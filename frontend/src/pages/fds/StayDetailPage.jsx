import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Card, Table, Button, Badge, Row, Col } from 'react-bootstrap'
import { getStayById, getFolioItems, checkOut } from '../../services/fds/stayService'
import Loader from '../../components/Loader'
import AddChargeModal from '../../components/AddChargeModal'
import { statusBadge } from '../../utils/badges'

export default function StayDetailPage() {
  const { stayId } = useParams()
  const navigate = useNavigate()
  const [stay, setStay] = useState(null)
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showAdd, setShowAdd] = useState(false)

  const load = async () => {
    setLoading(true); setError('')
    try {
      const [s, it] = await Promise.all([getStayById(stayId), getFolioItems(stayId)])
      setStay(s); setItems(it)
    } catch (e) { setError(e.message) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [stayId])

  const doCheckout = async () => {
    if (!window.confirm(`Checkout? Total folio: ₹${stay.folioBalance}`)) return
    try { await checkOut(stayId); navigate('/front-desk') }
    catch (e) { alert(e.message) }
  }

  if (loading) return <Loader />
  if (error) return <div className="alert alert-danger">{error}</div>
  if (!stay) return null

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h4 className="mb-0">Stay Folio #{stay.stayId} <Badge bg={statusBadge(stay.status)}>{stay.status}</Badge></h4>
        <div>
          <Button variant="outline-primary" className="me-2" disabled={stay.status !== 'ACTIVE'} onClick={() => setShowAdd(true)}>+ Add Charge</Button>
          <Button variant="danger" disabled={stay.status !== 'ACTIVE'} onClick={doCheckout}>Checkout</Button>
        </div>
      </div>
      <Row>
        <Col md={4}>
          <Card className="shadow-sm mb-3">
            <Card.Body>
              <div className="mb-2"><strong>Reservation:</strong> #{stay.reservationId}</div>
              <div className="mb-2"><strong>Guest ID:</strong> {stay.guestId}</div>
              <div className="mb-2"><strong>Room:</strong> {stay.assignedRoomId}</div>
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
            <Card.Header className="bg-white"><strong>Folio Items</strong></Card.Header>
            <Card.Body>
              {items.length === 0 ? <p className="text-muted">No charges posted yet.</p> :
                <Table hover size="sm">
                  <thead><tr><th>Type</th><th>Description</th><th>Amount</th><th>Posted</th></tr></thead>
                  <tbody>
                    {items.map(it => (
                      <tr key={it.folioItemId}>
                        <td><Badge bg="secondary">{it.chargeType}</Badge></td>
                        <td>{it.description}</td>
                        <td>₹{it.amount}</td>
                        <td className="small">{it.postedDate}</td>
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
    </div>
  )
}