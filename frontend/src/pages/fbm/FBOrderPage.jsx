import { useEffect, useState } from 'react'
import { Table, Button, Badge, Form, Row, Col, Card, Alert } from 'react-bootstrap'
import { getOrders, placeOrder, updateOrderStatus, cancelOrder } from '../../services/fbm/orderService'
import { getMenuItems } from '../../services/fbm/menuService'
import { useAuth } from '../../context/AuthContext'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'
import { statusBadge } from '../../utils/badges'

const NEXT = { PLACED: 'PREPARING', PREPARING: 'SERVED', SERVED: 'BILLED' }
const MENU_CATEGORIES = ['BREAKFAST', 'MAINCOURSE', 'BEVERAGE', 'DESSERT']

export default function FBOrderPage() {
  const { user } = useAuth()
  const [orders, setOrders] = useState([])
  const [menu, setMenu] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [form, setForm] = useState({ stayId: '', tableNumber: '', orderType: 'DINEIN', cart: {} })
  const [menuCat, setMenuCat] = useState('BREAKFAST')
  const [submitErr, setSubmitErr] = useState('')

  const load = async () => {
    setLoading(true); setError('')
    try {
      const [os, ms] = await Promise.all([getOrders(), getMenuItems({ available: true })])
      setOrders(os); setMenu(ms)
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

  const submit = async (e) => {
    e.preventDefault(); setSubmitErr('')
    if (cartItems.length === 0) { setSubmitErr('Select at least one item'); return }
    try {
      await placeOrder({
        stayId: Number(form.stayId),
        tableNumber: form.tableNumber,
        orderType: form.orderType,
        itemsJson: JSON.stringify(cartItems),
        placedBy: user?.userId,
      })
      setForm({ stayId: '', tableNumber: '', orderType: 'DINEIN', cart: {} })
      load()
    } catch (err) { setSubmitErr(err.message) }
  }

  const advance = async (id, status) => { try { await updateOrderStatus(id, status); load() } catch (e) { alert(e.message) } }
  const cancel = async (id) => { try { await cancelOrder(id); load() } catch (e) { alert(e.message) } }

  return (
    <div>
      <h4 className="mb-3">F&B Orders</h4>
      <Row>
        <Col md={5}>
          <Card className="shadow-sm mb-3">
            <Card.Header className="bg-white"><strong>New Order</strong></Card.Header>
            <Card.Body>
              {submitErr && <Alert variant="danger" className="py-2">{submitErr}</Alert>}
              <Form onSubmit={submit}>
                <Row>
                  <Col md={6}><Form.Group className="mb-2"><Form.Label className="small">Stay ID</Form.Label><Form.Control type="number" required value={form.stayId} onChange={e => setForm(f => ({ ...f, stayId: e.target.value }))} /></Form.Group></Col>
                  <Col md={6}><Form.Group className="mb-2"><Form.Label>Table No</Form.Label><Form.Control value={form.tableNumber} onChange={e => setForm(f => ({ ...f, tableNumber: e.target.value }))} /></Form.Group></Col>
                </Row>
                <Form.Group className="mb-3"><Form.Label>Order Type</Form.Label>
                  <Form.Select value={form.orderType} onChange={e => setForm(f => ({ ...f, orderType: e.target.value }))}>
                    <option value="DINEIN">Dine-In</option>
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
                <Button type="submit" className="w-100 mt-2" style={{ background: '#1e3a5f', borderColor: '#1e3a5f' }}>Place Order</Button>
              </Form>
            </Card.Body>
          </Card>
        </Col>
        <Col md={7}>
          <Card className="shadow-sm">
            <Card.Header className="bg-white"><strong>Active Orders</strong></Card.Header>
            <Card.Body>
              {loading ? <Loader /> : orders.length === 0 ? <EmptyState /> :
                <Table hover size="sm">
                  <thead><tr><th>ID</th><th>Type</th><th>Items</th><th>Total</th><th>Status</th><th>Actions</th></tr></thead>
                  <tbody>
                    {orders.map(o => (
                      <tr key={o.orderId}>
                        <td>#{o.orderId}</td>
                        <td><Badge bg="info">{o.orderType}</Badge></td>
                        <td className="small">
                          {o.items && o.items.length
                            ? o.items.map(i => `${i.name} ×${i.qty}`).join(', ')
                            : o.itemsJson}
                        </td>
                        <td>₹{o.totalAmount}</td>
                        <td><Badge bg={statusBadge(o.status)}>{o.status}</Badge></td>
                        <td>
                          {NEXT[o.status] && <Button size="sm" variant="outline-primary" className="me-1" onClick={() => advance(o.orderId, NEXT[o.status])}>→ {NEXT[o.status]}</Button>}
                          {o.status === 'PLACED' && <Button size="sm" variant="outline-danger" onClick={() => cancel(o.orderId)}>Cancel</Button>}
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
    </div>
  )
}