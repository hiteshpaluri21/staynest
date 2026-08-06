import { useEffect, useMemo, useState } from 'react'
import { Card, Row, Col, Table, Badge, Form, Button, Alert } from 'react-bootstrap'
import { getStays, getFolioItems } from '../../services/fds/stayService'
import { getRooms } from '../../services/ric/roomService'
import { getMenuItems } from '../../services/fbm/menuService'
import { getOrders, placeOrder, cancelOrder } from '../../services/fbm/orderService'
import { useAuth } from '../../context/AuthContext'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'
import RequestServiceModal from '../../components/RequestServiceModal'
import ConfirmModal from '../../components/ConfirmModal'
import { statusBadge } from '../../utils/badges'

const MENU_CATEGORIES = ['BREAKFAST', 'MAINCOURSE', 'BEVERAGE', 'DESSERT']

export default function MyStayPage() {
  const { user, guestId } = useAuth()
  const [stay, setStay] = useState(null)
  const [folio, setFolio] = useState([])
  const [orders, setOrders] = useState([])
  const [menu, setMenu] = useState([])
  const [roomNumber, setRoomNumber] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [menuCat, setMenuCat] = useState('BREAKFAST')
  const [cart, setCart] = useState({})
  const [orderType, setOrderType] = useState('INROOMDINING')
  const [submitErr, setSubmitErr] = useState('')
  const [submitOk, setSubmitOk] = useState('')
  const [showAddCharge, setShowAddCharge] = useState(false)
  // The order awaiting cancel confirmation, or null when the modal is closed.
  const [pendingCancel, setPendingCancel] = useState(null)

  const load = async () => {
    setLoading(true); setError('')
    try {
      // Stays are filed under the reservation-service guestId, not user.userId — passing the
      // latter returned an empty list, so a checked-in guest was told they had no active stay.
      const stays = await getStays({ guestId }).catch(() => [])
      const active = (stays || []).find(s => s.status === 'ACTIVE') || null
      setStay(active)
      if (active) {
        const [f, os, ms, rooms] = await Promise.all([
          getFolioItems(active.stayId).catch(() => []),
          getOrders({ stayId: active.stayId }).catch(() => []),
          getMenuItems({ available: true }).catch(() => []),
          getRooms().catch(() => []),
        ])
        setFolio(f || [])
        setOrders(os || [])
        setMenu(ms || [])
        const room = (rooms || []).find(r => r.roomId === active.assignedRoomId)
        setRoomNumber(room?.roomNumber ?? null)
      }
    } catch (e) { setError(e.message) } finally { setLoading(false) }
  }
  // Wait for guestId to resolve, otherwise the first load queries with undefined.
  useEffect(() => { if (user && guestId) load() }, [user, guestId])

  const setQty = (id, qty) => setCart(c => {
    const next = { ...c }
    if (!qty || qty <= 0) delete next[id]; else next[id] = Number(qty)
    return next
  })

  const cartItems = Object.entries(cart).map(([id, qty]) => ({ itemId: Number(id), qty }))
  const cartTotal = useMemo(() => cartItems.reduce((sum, ci) => {
    const m = menu.find(x => x.menuItemId === ci.itemId)
    return sum + (m ? Number(m.price) * ci.qty : 0)
  }, 0), [cart, menu])

  /**
   * Placing an order posts an FBCHARGE straight to the folio, so the stay itself (which carries the
   * Current Bill) and the folio table have to be refetched — not just the order list. Deliberately
   * avoids load() so the success alert isn't torn down by the page-level loading state.
   */
  const refreshBill = async () => {
    const [stays, f, os] = await Promise.all([
      getStays({ guestId }).catch(() => []),
      getFolioItems(stay.stayId).catch(() => []),
      getOrders({ stayId: stay.stayId }).catch(() => []),
    ])
    const current = (stays || []).find(s => s.stayId === stay.stayId)
    if (current) setStay(current)
    setFolio(f || [])
    setOrders(os || [])
  }

  const submitOrder = async (e) => {
    e.preventDefault(); setSubmitErr(''); setSubmitOk('')
    if (cartItems.length === 0) { setSubmitErr('Select at least one item'); return }
    try {
      await placeOrder({
        stayId: stay.stayId,
        orderType,
        itemsJson: JSON.stringify(cartItems),
        placedBy: user?.userId,
      })
      setCart({}); setSubmitOk('Order placed and added to your bill. You can track it below.')
      await refreshBill()
    } catch (err) { setSubmitErr(err.message) }
  }

  // Cancelling reverses the charge off the folio, so the bill has to be refetched here too. Errors
  // propagate to ConfirmModal, which shows them inline and keeps itself open for a retry.
  const confirmCancel = async () => {
    const orderId = pendingCancel.orderId
    setSubmitErr(''); setSubmitOk('')
    await cancelOrder(orderId)
    setSubmitOk(`Order #${orderId} cancelled and removed from your bill.`)
    await refreshBill()
  }

  if (loading) return <Loader />
  if (error) return <div className="alert alert-danger">{error}</div>

  if (!stay) {
    return (
      <div>
        <h4 className="mb-3">My Stay</h4>
        <Alert variant="info">You have no active stay right now. Once you're checked in, your bill and in-room dining will appear here.</Alert>
      </div>
    )
  }

  return (
    <div>
      <h4 className="mb-3">My Stay</h4>
      <Row>
        <Col md={5}>
          <Card className="shadow-sm mb-3">
            <Card.Header className="bg-white"><strong>Stay Summary</strong></Card.Header>
            <Card.Body>
              <div className="mb-2"><strong>Reservation:</strong> #{stay.reservationId}</div>
              <div className="mb-2"><strong>Room:</strong> {roomNumber != null ? `Room ${roomNumber}` : `Room #${stay.assignedRoomId}`}</div>
              <div className="mb-2"><strong>Status:</strong> <Badge bg={statusBadge(stay.status)}>{stay.status}</Badge></div>
              <hr />
              <div className="d-flex justify-content-between">
                <strong>Current Bill</strong><strong className="text-primary">₹{stay.folioBalance}</strong>
              </div>
            </Card.Body>
          </Card>

          <Card className="shadow-sm">
            <Card.Header className="bg-white d-flex justify-content-between align-items-center">
              <strong>My Bill (Folio)</strong>
              <Button size="sm" variant="outline-primary" disabled={stay.status !== 'ACTIVE'} onClick={() => setShowAddCharge(true)}>+ Request Service</Button>
            </Card.Header>
            <Card.Body>
              {folio.length === 0 ? <p className="text-muted mb-0">No charges yet.</p> :
                <Table size="sm" className="mb-0">
                  <thead><tr><th>Type</th><th>Description</th><th className="text-end">Amount</th></tr></thead>
                  <tbody>
                    {folio.map(it => (
                      <tr key={it.folioItemId}>
                        <td><Badge bg="secondary">{it.chargeType}</Badge></td>
                        <td>{it.description}</td>
                        <td className="text-end">₹{it.amount}</td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              }
            </Card.Body>
          </Card>
        </Col>

        <Col md={7}>
          <Card className="shadow-sm mb-3">
            <Card.Header className="bg-white"><strong>Order Food & Beverage</strong></Card.Header>
            <Card.Body>
              {submitErr && <Alert variant="danger" className="py-2">{submitErr}</Alert>}
              {submitOk && <Alert variant="success" className="py-2">{submitOk}</Alert>}
              <Form onSubmit={submitOrder}>
                <Row className="mb-2">
                  <Col md={6}>
                    <Form.Label className="small">Order Type</Form.Label>
                    {/* No dine-in here — to eat at an outlet, book a table under Dining Reservations. */}
                    <Form.Select value={orderType} onChange={e => setOrderType(e.target.value)}>
                      <option value="INROOMDINING">In-Room Dining</option>
                      <option value="TAKEAWAY">Takeaway</option>
                    </Form.Select>
                  </Col>
                  <Col md={6}>
                    <Form.Label className="small">Category</Form.Label>
                    <Form.Select value={menuCat} onChange={e => setMenuCat(e.target.value)}>
                      {MENU_CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                    </Form.Select>
                  </Col>
                </Row>
                <div style={{ maxHeight: 240, overflowY: 'auto' }}>
                  {(() => {
                    const shown = menu.filter(m => m.category === menuCat)
                    if (shown.length === 0) return <p className="text-muted small mb-0">No items in this category.</p>
                    return shown.map(m => (
                      <div key={m.menuItemId} className="d-flex justify-content-between align-items-center py-1 border-bottom">
                        <div><strong>{m.name}</strong> <span className="text-muted small">₹{m.price}</span></div>
                        <Form.Control type="number" min="0" style={{ width: 70 }} value={cart[m.menuItemId] || ''} onChange={e => setQty(m.menuItemId, e.target.value)} />
                      </div>
                    ))
                  })()}
                </div>
                <div className="d-flex justify-content-between mt-2">
                  <strong>Total</strong><strong className="text-primary">₹{cartTotal}</strong>
                </div>
                <Button type="submit" className="w-100 mt-2" style={{ background: '#1e3a5f', borderColor: '#1e3a5f' }}>Place Order</Button>
              </Form>
            </Card.Body>
          </Card>

          <Card className="shadow-sm">
            <Card.Header className="bg-white"><strong>My Orders</strong></Card.Header>
            <Card.Body>
              {orders.length === 0 ? <EmptyState message="No orders yet" /> :
                <Table hover size="sm" className="mb-0">
                  <thead><tr><th>ID</th><th>Type</th><th>Items</th><th>Total</th><th>Status</th><th>Action</th></tr></thead>
                  <tbody>
                    {orders.map(o => (
                      <tr key={o.orderId}>
                        <td>#{o.orderId}</td>
                        <td><Badge bg="info">{o.orderType}</Badge></td>
                        <td className="small">
                          {o.items && o.items.length ? o.items.map(i => `${i.name} ×${i.qty}`).join(', ') : o.itemsJson}
                        </td>
                        <td>₹{o.totalAmount}</td>
                        <td><Badge bg={statusBadge(o.status)}>{o.status}</Badge></td>
                        <td>
                          {/* Only while the kitchen hasn't started, and only before check-out — the
                              folio won't accept a reversal against a closed stay. */}
                          {o.status === 'PLACED' && stay.status === 'ACTIVE'
                            ? <Button size="sm" variant="outline-danger" onClick={() => setPendingCancel(o)}>Cancel</Button>
                            : <span className="text-muted small">—</span>}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              }
            </Card.Body>
          </Card>
        </Col>
      </Row>

      <RequestServiceModal
        show={showAddCharge}
        stayId={stay.stayId}
        onClose={() => setShowAddCharge(false)}
        onSaved={() => { setShowAddCharge(false); load() }}
      />

      <ConfirmModal
        show={pendingCancel != null}
        title="Cancel order"
        body={pendingCancel && (
          <p className="mb-0">
            Cancel order #{pendingCancel.orderId}? The ₹{pendingCancel.totalAmount} charge will be
            taken off your bill.
          </p>
        )}
        confirmLabel="Cancel order"
        onClose={() => setPendingCancel(null)}
        onConfirm={confirmCancel}
      />
    </div>
  )
}
