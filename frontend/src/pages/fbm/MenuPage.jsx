import { useEffect, useState } from 'react'
import { Card, Row, Col, Button, Badge, Form } from 'react-bootstrap'
import { getMenuItems, updateAvailability } from '../../services/fbm/menuService'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'
import MenuItemFormModal from '../../components/MenuItemFormModal'

const CATEGORIES = ['BREAKFAST', 'MAINCOURSE', 'BEVERAGE', 'DESSERT']

export default function MenuPage() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [catFilter, setCatFilter] = useState('')

  const load = async () => {
    setLoading(true); setError('')
    try { setItems(await getMenuItems(catFilter ? { category: catFilter } : {})) }
    catch (e) { setError(e.message) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [catFilter])

  const toggle = async (item) => {
    try { await updateAvailability(item.menuItemId, !item.isAvailable); load() }
    catch (e) { alert(e.message) }
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h4 className="mb-0">Menu Items</h4>
        <Button style={{ background: '#1e3a5f', borderColor: '#1e3a5f' }} onClick={() => setShowModal(true)}>+ Add Menu Item</Button>
      </div>
      <Form.Select style={{ maxWidth: 240 }} className="mb-3" value={catFilter} onChange={e => setCatFilter(e.target.value)}>
        <option value="">All Categories</option>
        {CATEGORIES.map(c => <option key={c}>{c}</option>)}
      </Form.Select>
      {loading ? <Loader /> : error ? <div className="alert alert-danger">{error}</div> :
        items.length === 0 ? <EmptyState message="No menu items" /> :
        <Row>
          {items.map(m => (
            <Col md={4} lg={3} key={m.menuItemId} className="mb-3">
              <Card className="h-100 shadow-sm">
                <Card.Body>
                  <div className="d-flex justify-content-between">
                    <Badge bg="info">{m.category}</Badge>
                    <Badge bg={m.isAvailable ? 'success' : 'danger'}>{m.isAvailable ? 'Available' : 'Unavailable'}</Badge>
                  </div>
                  <h6 className="mt-2 mb-1">{m.name}</h6>
                  <div className="fw-bold text-primary mb-1">₹{m.price}</div>
                  {m.dietaryTags && <div className="small text-muted mb-2">{m.dietaryTags}</div>}
                  <Button size="sm" variant="outline-secondary" onClick={() => toggle(m)}>
                    Toggle Availability
                  </Button>
                </Card.Body>
              </Card>
            </Col>
          ))}
        </Row>
      }
      <MenuItemFormModal show={showModal} onClose={() => setShowModal(false)} onSaved={() => { setShowModal(false); load() }} />
    </div>
  )
}