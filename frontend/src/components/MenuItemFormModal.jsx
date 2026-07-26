import { useState } from 'react'
import { Modal, Form, Button, Alert } from 'react-bootstrap'
import { createMenuItem } from '../services/fbm/menuService'

const CATS = ['BREAKFAST', 'MAINCOURSE', 'BEVERAGE', 'DESSERT']

export default function MenuItemFormModal({ show, onClose, onSaved }) {
  const [form, setForm] = useState({ name: '', category: 'BREAKFAST', price: 100, dietaryTags: '' })
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }))

  const submit = async (e) => {
    e.preventDefault(); setSaving(true); setError('')
    try { await createMenuItem({ ...form, price: Number(form.price) }); onSaved() }
    catch (err) { setError(err.message) } finally { setSaving(false) }
  }

  return (
    <Modal show={show} onHide={onClose}>
      <Modal.Header closeButton><Modal.Title>Add Menu Item</Modal.Title></Modal.Header>
      <Form onSubmit={submit}>
        <Modal.Body>
          {error && <Alert variant="danger" className="py-2">{error}</Alert>}
          <Form.Group className="mb-3"><Form.Label>Name</Form.Label><Form.Control required value={form.name} onChange={e => set('name', e.target.value)} /></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Category</Form.Label><Form.Select value={form.category} onChange={e => set('category', e.target.value)}>{CATS.map(c => <option key={c}>{c}</option>)}</Form.Select></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Price (₹)</Form.Label><Form.Control type="number" min="0" required value={form.price} onChange={e => set('price', e.target.value)} /></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Dietary Tags</Form.Label><Form.Control value={form.dietaryTags} onChange={e => set('dietaryTags', e.target.value)} placeholder="e.g., Vegetarian, Vegan" /></Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button type="submit" disabled={saving} style={{ background: '#1e3a5f', borderColor: '#1e3a5f' }}>{saving ? 'Saving…' : 'Create'}</Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}