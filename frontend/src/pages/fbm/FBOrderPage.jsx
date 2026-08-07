import { useEffect, useMemo, useState } from 'react'
import { Table, Button, Badge, Form, Row, Col, Card, Alert } from 'react-bootstrap'
import { getOrders, placeOrder, updateOrderStatus, cancelOrder } from '../../services/fbm/orderService'
import { getMenuItems } from '../../services/fbm/menuService'
import { getStays } from '../../services/fds/stayService'
import { getRooms } from '../../services/ric/roomService'
import { getGuestById } from '../../services/rbm/guestService'
import { useAuth } from '../../context/AuthContext'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'
import ConfirmModal from '../../components/ConfirmModal'
import { statusBadge } from '../../utils/badges'

const NEXT = { PLACED: 'PREPARING', PREPARING: 'SERVED', SERVED: 'BILLED' }
const MENU_CATEGORIES = ['BREAKFAST', 'MAINCOURSE', 'BEVERAGE', 'DESSERT']

export default function FBOrderPage() {
  const { user } = useAuth()
  const [orders, setOrders] = useState([])
  const [menu, setMenu] = useState([])
  // Only stays that are still open can be charged, so the picker is built from those.
  const [activeStays, setActiveStays] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [form, setForm] = useState({ stayId: '', orderType: 'INROOMDINING', cart: {} })
  const [menuCat, setMenuCat] = useState('BREAKFAST')
  const [submitErr, setSubmitErr] = useState('')
  const [actionErr, setActionErr] = useState('')
  // The order awaiting cancel confirmation, or null when the modal is closed.
  const [pendingCancel, setPendingCancel] = useState(null)

  const load = async () => {
    setLoading(true); setError('')
    try {
      const [os, ms, stays, rooms] = await Promise.all([
        getOrders(),
        getMenuItems({ available: true }),
        getStays().catch(() => []),
        getRooms().catch(() => []),
      ])
      setOrders(os); setMenu(ms)

      const open = (stays || []).filter(s => s.status === 'ACTIVE')
      const roomNumbers = Object.fromEntries((rooms || []).map(r => [r.roomId, r.roomNumber]))
      // Guest names make the picker readable; a failed lookup just falls back to the id.
      const named = await Promise.all(open.map(async s => {
        const guest = await getGuestById(s.guestId).catch(() => null)
        return {
          ...s,
          guestName: guest?.name ?? `Guest #${s.guestId}`,
          roomLabel: roomNumbers[s.assignedRoomId] != null
            ? `Room ${roomNumbers[s.assignedRoomId]}`
            : `Room ${s.assignedRoomId}`,
        }
      }))
      setActiveStays(named)
    } catch (e) { setError(e.message) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  const setQty = (id, qty) => setForm(f => {
    const cart = { ...f.cart }
    if (!qty || qty <= 0) delete cart[id]; else cart[id] = Number(qty)
    return { ...f, cart }
  })

  const cartItems = Object.entries(form.cart).map(([id, qty]) => ({ itemId: Number(id), qty }))
  const total = cartItems.reduce((sum, ci) => {
    const m = menu.find(x => x.menuItemId === ci.itemId)
    return sum + (m ? Number(m.price) * ci.qty : 0)
  }, 0)

  const stayLabels = useMemo(
    () => Object.fromEntries(activeStays.map(s => [s.stayId, `${s.guestName} — ${s.roomLabel}`])),
    [activeStays]
  )

  const submit = async (e) => {
    e.preventDefault(); setSubmitErr('')
    if (!form.stayId) { setSubmitErr('Select the stay this order is for'); return }
    if (cartItems.length === 0) { setSubmitErr('Select at least one item'); return }
    try {
      await placeOrder({
        stayId: Number(form.stayId),
        orderType: form.orderType,
        itemsJson: JSON.stringify(cartItems),
        placedBy: user?.userId,
      })
      setForm({ stayId: '', orderType: 'INROOMDINING', cart: {} })
      load()
    } catch (err) { setSubmitErr(err.message) }
  }

  // Action failures land in an inline alert above the order table rather than a native dialog.
  const advance = async (id, status) => {
    setActionErr('')
    try { await updateOrderStatus(id, status); load() } catch (e) { setActionErr(e.message) }
  }

  // Confirmed in-app via ConfirmModal, which surfaces its own errors and reloads on success.
  const confirmCancel = async () => {
    await cancelOrder(pendingCancel.orderId)
    await load()
  }

  return (
    <div>
      <h4 className="mb-3">F&B Orders</h4>
      <Row>
        <Col md={5}>
          <Card className="shadow-sm mb-3">
            <Card.Header><strong>New Order</strong></Card.Header>
            <Card.Body>
              {submitErr && <Alert variant="danger" className="py-2">{submitErr}</Alert>}
              {!loading && activeStays.length === 0 && (
                <Alert variant="info" className="py-2">
                  No guests are checked in, so there is no stay to charge an order to.
                </Alert>
              )}
              <Form onSubmit={submit}>
                <Form.Group className="mb-2">
                  <Form.Label>Guest / Stay</Form.Label>
                  <Form.Select required value={form.stayId} onChange={e => setForm(f => ({ ...f, stayId: e.target.value }))}>
                    <option value="">Select a checked-in guest…</option>
                    {activeStays.map(s => (
                      <option key={s.stayId} value={s.stayId}>
                        {s.guestName} — {s.roomLabel} (Stay {s.stayId})
                      </option>
                    ))}
                  </Form.Select>
                </Form.Group>
                {/* No dine-in here — eating at an outlet is booked on the Dining Reservations page. */}
                <Form.Group className="mb-3"><Form.Label>Order Type</Form.Label>
                  <Form.Select value={form.orderType} onChange={e => setForm(f => ({ ...f, orderType: e.target.value }))}>
                    <option value="INROOMDINING">In-Room Dining</option>
                    <option value="TAKEAWAY">Takeaway</option>
                  </Form.Select>
                </Form.Group>
                <Form.Group className="mb-2">
                  <Form.Label>Category</Form.Label>
                  <Form.Select value={menuCat} onChange={e => setMenuCat(e.target.value)}>
                    {MENU_CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                  </Form.Select>
                </Form.Group>
                <div style={{ maxHeight: 240, overflowY: 'auto' }}>
                  {(() => {
                    const shown = menu.filter(m => m.category === menuCat)
                    if (shown.length === 0) return <p className="text-muted small mb-0">No items in this category.</p>
                    return shown.map(m => (
                      <div key={m.menuItemId} className="d-flex justify-content-between align-items-center py-1 border-bottom">
                        <div><strong>{m.name}</strong> <span className="text-muted small">₹{m.price}</span></div>
                        <Form.Control type="number" min="0" style={{ width: 70 }} value={form.cart[m.menuItemId] || ''} onChange={e => setQty(m.menuItemId, e.target.value)} />
                      </div>
                    ))
                  })()}
                </div>
                <div className="d-flex justify-content-between mt-2">
                  <strong>Total</strong><strong className="text-primary">₹{total}</strong>
                </div>
                <Button type="submit" className="w-100 mt-2" disabled={activeStays.length === 0}>Place Order</Button>
              </Form>
            </Card.Body>
          </Card>
        </Col>

        <Col md={7}>
          <Card className="shadow-sm">
            <Card.Header><strong>Active Orders</strong></Card.Header>
            <Card.Body>
              {actionErr && <Alert variant="danger" dismissible onClose={() => setActionErr('')} className="py-2">{actionErr}</Alert>}
              {loading ? <Loader /> : orders.length === 0 ? <EmptyState /> :
                <Table hover responsive size="sm">
                  <thead><tr><th>ID</th><th>Guest</th><th>Type</th><th>Items</th><th>Total</th><th>Status</th><th>Actions</th></tr></thead>
                  <tbody>
                    {orders.map(o => (
                      <tr key={o.orderId}>
                        <td>{o.orderId}</td>
                        <td className="small">{stayLabels[o.stayId] ?? `Stay ${o.stayId}`}</td>
                        <td><Badge bg="info">{o.orderType}</Badge></td>
                        <td className="small">
                          {o.items && o.items.length
                            ? o.items.map(i => `${i.name} ×${i.qty}`).join(', ')
                            : o.itemsJson}
                        </td>
                        <td>₹{o.totalAmount}</td>
                        <td><Badge bg={statusBadge(o.status)}>{o.status}</Badge></td>
                        <td>
                          {NEXT[o.status] && <Button size="sm" variant="outline-primary" className="me-1" onClick={() => advance(o.orderId, NEXT[o.status])}> {NEXT[o.status]}</Button>}
                          {o.status === 'PLACED' && <Button size="sm" variant="outline-danger" onClick={() => { setActionErr(''); setPendingCancel(o) }}>Cancel</Button>}
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

      <ConfirmModal
        show={pendingCancel != null}
        title="Cancel order"
        body={pendingCancel && (
          <p className="mb-0">
            Cancel order #{pendingCancel.orderId} for{' '}
            <strong>{stayLabels[pendingCancel.stayId] ?? `Stay #${pendingCancel.stayId}`}</strong>?
            {' '}The ₹{pendingCancel.totalAmount} charge will be taken back off the guest's bill.
          </p>
        )}
        confirmLabel="Cancel order"
        onClose={() => setPendingCancel(null)}
        onConfirm={confirmCancel}
      />
    </div>
  )
}
