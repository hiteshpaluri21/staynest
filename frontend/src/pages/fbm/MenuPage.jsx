import { useEffect, useState } from 'react'
import { Card, Row, Col, Button, Badge, Form, Alert } from 'react-bootstrap'
import { getMenuItems, updateAvailability, deleteMenuItem } from '../../services/fbm/menuService'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'
import MenuItemFormModal from '../../components/MenuItemFormModal'
import ConfirmModal from '../../components/ConfirmModal'
import { useAuth } from '../../context/AuthContext'

const CATEGORIES = ['BREAKFAST', 'MAINCOURSE', 'BEVERAGE', 'DESSERT']

// The conventional Indian menu marker: a green dot for veg, red for non-veg. Items created
// before the food type existed have none, and are left unmarked rather than assumed veg.
const FOOD_TYPES = {
  VEG: { label: 'Veg', variant: 'success' },
  NONVEG: { label: 'Non-veg', variant: 'danger' },
  EGG: { label: 'Egg', variant: 'warning' },
}

export default function MenuPage() {
  const { user } = useAuth()
  const canManage = user?.role !== 'GUEST'
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editItem, setEditItem] = useState(null)
  const [catFilter, setCatFilter] = useState('')
  // Failures from row actions; `error` is not reused, as it replaces the whole grid.
  const [actionErr, setActionErr] = useState('')
  // The item awaiting delete confirmation, or null when the modal is closed.
  const [pendingDelete, setPendingDelete] = useState(null)

  const load = async () => {
    setLoading(true); setError('')
    try { setItems(await getMenuItems(catFilter ? { category: catFilter } : {})) }
    catch (e) { setError(e.message) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [catFilter])

  const toggle = async (item) => {
    setActionErr('')
    try { await updateAvailability(item.menuItemId, !item.isAvailable); load() }
    catch (e) { setActionErr(e.message) }
  }

  // Errors propagate to ConfirmModal, which shows them inline and stays open — that matters
  // here, as the server refuses the delete while the dish is on an unfinished order.
  const confirmDelete = async () => {
    await deleteMenuItem(pendingDelete.menuItemId)
    await load()
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h4 className="mb-0">Menu Items</h4>
        {canManage && <Button onClick={() => setShowModal(true)}>+ Add Menu Item</Button>}
      </div>
      <Form.Select style={{ maxWidth: 240 }} className="mb-3" value={catFilter} onChange={e => setCatFilter(e.target.value)}>
        <option value="">All Categories</option>
        {CATEGORIES.map(c => <option key={c}>{c}</option>)}
      </Form.Select>
      {actionErr && <Alert variant="danger" dismissible onClose={() => setActionErr('')} className="py-2">{actionErr}</Alert>}
      {loading ? <Loader /> : error ? <div className="alert alert-danger">{error}</div> :
        items.length === 0 ? <EmptyState message="No menu items" /> :
        <Row>
          {items.map(m => (
            <Col sm={6} md={4} lg={3} key={m.menuItemId} className="mb-3">
              <Card className="h-100 shadow-sm">
                <Card.Body>
                  <div className="d-flex justify-content-between">
                    <Badge bg="info">{m.category}</Badge>
                    <Badge bg={m.isAvailable ? 'success' : 'danger'}>{m.isAvailable ? 'Available' : 'Unavailable'}</Badge>
                  </div>
                  <h6 className="mt-2 mb-1">{m.name}</h6>
                  <div className="d-flex align-items-center gap-2 mb-1">
                    <span className="fw-bold text-primary">₹{m.price}</span>
                    {FOOD_TYPES[m.foodType] && (
                      <Badge bg={FOOD_TYPES[m.foodType].variant}>{FOOD_TYPES[m.foodType].label}</Badge>
                    )}
                  </div>
                  {m.dietaryTags && <div className="small text-muted mb-2">{m.dietaryTags}</div>}
                  {canManage && (
                    <div className="d-flex flex-wrap gap-1">
                      <Button size="sm" variant="outline-primary" onClick={() => setEditItem(m)}>Edit</Button>
                      <Button size="sm" variant="outline-secondary" onClick={() => toggle(m)}>
                        {m.isAvailable ? 'Mark Unavailable' : 'Mark Available'}
                      </Button>
                      <Button size="sm" variant="outline-danger" onClick={() => { setActionErr(''); setPendingDelete(m) }}>
                        Delete
                      </Button>
                    </div>
                  )}
                </Card.Body>
              </Card>
            </Col>
          ))}
        </Row>
      }
      <MenuItemFormModal show={showModal} onClose={() => setShowModal(false)} onSaved={() => { setShowModal(false); load() }} />
      <MenuItemFormModal show={!!editItem} item={editItem} onClose={() => setEditItem(null)} onSaved={() => { setEditItem(null); load() }} />

      <ConfirmModal
        show={pendingDelete != null}
        title="Delete menu item"
        body={pendingDelete && (
          <p className="mb-0">
            Delete <strong>{pendingDelete.name}</strong> from the menu for good? To take it off
            the ordering screens but keep it on record, mark it unavailable instead.
          </p>
        )}
        confirmLabel="Delete item"
        onClose={() => setPendingDelete(null)}
        onConfirm={confirmDelete}
      />
    </div>
  )
}