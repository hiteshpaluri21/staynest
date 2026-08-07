import { useEffect, useMemo, useState } from 'react'
import { Card, Row, Col, Table, Badge, Form, Button, Alert, Nav } from 'react-bootstrap'
import { getStays, getFolioItems } from '../../services/fds/stayService'
import { getRoomNumbers } from '../../services/ric/roomService'
import { getMenuItems } from '../../services/fbm/menuService'
import { getOrders, placeOrder, cancelOrder } from '../../services/fbm/orderService'
import { useAuth } from '../../context/AuthContext'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'
import RequestServiceModal from '../../components/RequestServiceModal'
import ConfirmModal from '../../components/ConfirmModal'
import { statusBadge } from '../../utils/badges'

const MENU_CATEGORIES = ['BREAKFAST', 'MAINCOURSE', 'BEVERAGE', 'DESSERT']

// Mirrors the FoodType enum in fb-service. Items predating it are left unmarked.
const FOOD_TYPES = {
  VEG: { label: 'Veg', variant: 'success' },
  NONVEG: { label: 'Non-veg', variant: 'danger' },
  EGG: { label: 'Egg', variant: 'warning' },
}

/**
 * A guest's open stays, with the bill and in-room dining for whichever one they are looking at.
 *
 * A guest can be checked in more than once at the same time — two rooms for a family, or a
 * room booked for a colleague on the same profile. This used to pick the first ACTIVE stay and
 * silently ignore the rest, so the other rooms' bills were unreachable. Every open stay is now
 * listed, and the tabs choose which one the panels below act on.
 */
export default function MyStayPage() {
  const { user, guestId } = useAuth()
  const [stays, setStays] = useState([])
  const [selectedStayId, setSelectedStayId] = useState(null)
  const [folio, setFolio] = useState([])
  const [orders, setOrders] = useState([])
  const [menu, setMenu] = useState([])
  const [roomNumbers, setRoomNumbers] = useState({})
  const [loading, setLoading] = useState(true)
  const [detailLoading, setDetailLoading] = useState(false)
  const [error, setError] = useState('')
  const [menuCat, setMenuCat] = useState('BREAKFAST')
  const [cart, setCart] = useState({})
  const [orderType, setOrderType] = useState('INROOMDINING')
  const [submitErr, setSubmitErr] = useState('')
  const [submitOk, setSubmitOk] = useState('')
  const [showAddCharge, setShowAddCharge] = useState(false)
  // The order awaiting cancel confirmation, or null when the modal is closed.
  const [pendingCancel, setPendingCancel] = useState(null)
  // Bumped to re-run the folio/orders fetch for the selected stay after an order changes.
  const [detailNonce, setDetailNonce] = useState(0)

  /** Only the open stays, newest arrival first. */
  const activeOnly = (all) => (all || [])
    .filter(s => s.status === 'ACTIVE')
    .sort((a, b) => String(b.actualCheckIn || '').localeCompare(String(a.actualCheckIn || '')))

  const loadStays = async () => {
    setLoading(true); setError('')
    try {
      // Stays are filed under the reservation-service guestId, not user.userId — passing the
      // latter returned an empty list, so a checked-in guest was told they had no active stay.
      const active = activeOnly(await getStays({ guestId }).catch(() => []))
      setStays(active)
      // Keep the tab the guest is on if it is still open, otherwise fall back to the first.
      setSelectedStayId(prev =>
        active.some(s => s.stayId === prev) ? prev : (active[0]?.stayId ?? null))

      const [items, numbers] = await Promise.all([
        getMenuItems({ available: true }).catch(() => []),
        getRoomNumbers(active.map(s => s.assignedRoomId)),
      ])
      setMenu(items || [])
      setRoomNumbers(numbers)
    } catch (e) { setError(e.message) } finally { setLoading(false) }
  }
  // Wait for guestId to resolve, otherwise the first load queries with undefined.
  useEffect(() => { if (user && guestId) loadStays() }, [user, guestId])

  // The bill and orders belong to one stay, so they are refetched whenever the tab changes.
  useEffect(() => {
    if (selectedStayId == null) { setFolio([]); setOrders([]); return }
    let cancelled = false
    setDetailLoading(true)
    Promise.all([
      getFolioItems(selectedStayId).catch(() => []),
      getOrders({ stayId: selectedStayId }).catch(() => []),
    ]).then(([f, os]) => {
      if (cancelled) return
      setFolio(f || [])
      setOrders(os || [])
    }).finally(() => { if (!cancelled) setDetailLoading(false) })
    return () => { cancelled = true }
  }, [selectedStayId, detailNonce])

  const stay = stays.find(s => s.stayId === selectedStayId) || null
  const roomLabel = (s) => (roomNumbers[s.assignedRoomId] != null
    ? `Room ${roomNumbers[s.assignedRoomId]}`
    : 'Room not available')

  // Switching stays must not carry a half-built order across to the other room's bill.
  const selectStay = (stayId) => {
    if (stayId === selectedStayId) return
    setSelectedStayId(stayId)
    setCart({}); setSubmitErr(''); setSubmitOk('')
  }

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

  const combinedBalance = stays.reduce((sum, s) => sum + Number(s.folioBalance || 0), 0)

  /**
   * Placing an order posts an FBCHARGE straight to the folio, so the stays (which carry the
   * Current Bill) and the selected stay's folio have to be refetched — not just the order list.
   * Deliberately avoids loadStays() so the success alert isn't torn down by the page-level
   * loading state.
   */
  const refreshBill = async () => {
    setStays(activeOnly(await getStays({ guestId }).catch(() => [])))
    setDetailNonce(n => n + 1)
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
      setCart({})
      setSubmitOk(`Order placed and added to the bill for ${roomLabel(stay)}. You can track it below.`)
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

  if (stays.length === 0) {
    return (
      <div>
        <h4 className="mb-3">My Stay</h4>
        <Alert variant="info">You have no active stay right now. Once you're checked in, your bill and in-room dining will appear here.</Alert>
      </div>
    )
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3">
        <h4 className="mb-0">My Stay{stays.length > 1 ? 's' : ''}</h4>
        {stays.length > 1 && (
          <span className="text-muted small">
            {stays.length} rooms checked in · combined bill{' '}
            <strong className="text-primary">₹{combinedBalance}</strong>
          </span>
        )}
      </div>

      {/* One tab per open stay. Hidden for a single stay, where there is nothing to choose. */}
      {stays.length > 1 && (
        <Card className="shadow-sm mb-3">
          <Card.Body className="py-2">
            <Nav variant="pills" className="flex-wrap gap-1">
              {stays.map(s => (
                <Nav.Item key={s.stayId}>
                  <Nav.Link
                    active={s.stayId === selectedStayId}
                    onClick={() => selectStay(s.stayId)}
                    className="py-1"
                  >
                    {roomLabel(s)}
                    <span className="ms-2 small opacity-75">₹{s.folioBalance}</span>
                  </Nav.Link>
                </Nav.Item>
              ))}
            </Nav>
          </Card.Body>
        </Card>
      )}

      {!stay ? <EmptyState message="Select one of your stays above" /> : (
        <Row>
          <Col md={5}>
            <Card className="shadow-sm mb-3">
              <Card.Header><strong>Stay Summary</strong></Card.Header>
              <Card.Body>
                <div className="mb-2"><strong>Reservation:</strong> {stay.reservationId}</div>
                <div className="mb-2"><strong>Room:</strong> {roomLabel(stay)}</div>
                <div className="mb-2"><strong>Checked in:</strong> {stay.actualCheckIn ? String(stay.actualCheckIn).replace('T', ' ').slice(0, 16) : '—'}</div>
                <div className="mb-2"><strong>Status:</strong> <Badge bg={statusBadge(stay.status)}>{stay.status}</Badge></div>
                <hr />
                <div className="d-flex justify-content-between">
                  <strong>Current Bill</strong><strong className="text-primary">₹{stay.folioBalance}</strong>
                </div>
              </Card.Body>
            </Card>

            <Card className="shadow-sm">
              <Card.Header className="d-flex justify-content-between align-items-center">
                <strong>Bill for {roomLabel(stay)}</strong>
                <Button size="sm" variant="outline-primary" disabled={stay.status !== 'ACTIVE'} onClick={() => setShowAddCharge(true)}>+ Request Service</Button>
              </Card.Header>
              <Card.Body>
                {detailLoading ? <Loader /> :
                  folio.length === 0 ? <p className="text-muted mb-0">No charges yet.</p> :
                    <Table responsive size="sm" className="mb-0">
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
              <Card.Header><strong>Order Food & Beverage</strong></Card.Header>
              <Card.Body>
                {submitErr && <Alert variant="danger" className="py-2">{submitErr}</Alert>}
                {submitOk && <Alert variant="success" className="py-2">{submitOk}</Alert>}
                {stays.length > 1 && (
                  <p className="text-muted small">
                    This order goes to <strong>{roomLabel(stay)}</strong> and onto that room's bill.
                  </p>
                )}
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
                          <div>
                            <strong>{m.name}</strong> <span className="text-muted small">₹{m.price}</span>
                            {FOOD_TYPES[m.foodType] && (
                              <Badge bg={FOOD_TYPES[m.foodType].variant} className="ms-2">{FOOD_TYPES[m.foodType].label}</Badge>
                            )}
                          </div>
                          <Form.Control type="number" min="0" style={{ width: 70 }} value={cart[m.menuItemId] || ''} onChange={e => setQty(m.menuItemId, e.target.value)} />
                        </div>
                      ))
                    })()}
                  </div>
                  <div className="d-flex justify-content-between mt-2">
                    <strong>Total</strong><strong className="text-primary">₹{cartTotal}</strong>
                  </div>
                  <Button type="submit" className="w-100 mt-2">Place Order</Button>
                </Form>
              </Card.Body>
            </Card>

            <Card className="shadow-sm">
              <Card.Header><strong>Orders for {roomLabel(stay)}</strong></Card.Header>
              <Card.Body>
                {detailLoading ? <Loader /> :
                  orders.length === 0 ? <EmptyState message="No orders yet" /> :
                    <Table hover responsive size="sm" className="mb-0">
                      <thead><tr><th>ID</th><th>Type</th><th>Items</th><th>Total</th><th>Status</th><th>Action</th></tr></thead>
                      <tbody>
                        {orders.map(o => (
                          <tr key={o.orderId}>
                            <td>{o.orderId}</td>
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
      )}

      {stay && (
        <RequestServiceModal
          show={showAddCharge}
          stayId={stay.stayId}
          onClose={() => setShowAddCharge(false)}
          onSaved={() => { setShowAddCharge(false); refreshBill() }}
        />
      )}

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
