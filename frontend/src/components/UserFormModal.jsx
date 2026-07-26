import { useState } from 'react'
import { Modal, Form, Button, Alert } from 'react-bootstrap'
import { createUser } from '../services/iam/userService'

const ROLES = ['GUEST', 'FRONTDESK', 'HOUSEKEEPING', 'FBMANAGER', 'REVENUEMANAGER', 'ADMIN']

export default function UserFormModal({ show, onClose, onSaved }) {
  const [form, setForm] = useState({ name: '', email: '', phone: '', role: 'GUEST', password: '' })
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }))

  const submit = async (e) => {
    e.preventDefault()
    setSaving(true); setError('')
    try {
      await createUser(form)
      setForm({ name: '', email: '', phone: '', role: 'GUEST', password: '' })
      onSaved()
    } catch (err) { setError(err.message) }
    finally { setSaving(false) }
  }

  return (
    <Modal show={show} onHide={onClose}>
      <Modal.Header closeButton><Modal.Title>Add User</Modal.Title></Modal.Header>
      <Form onSubmit={submit}>
        <Modal.Body>
          {error && <Alert variant="danger" className="py-2">{error}</Alert>}
          <Form.Group className="mb-3"><Form.Label>Name</Form.Label><Form.Control required value={form.name} onChange={e => set('name', e.target.value)} /></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Email</Form.Label><Form.Control type="email" required value={form.email} onChange={e => set('email', e.target.value)} /></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Phone</Form.Label><Form.Control value={form.phone} onChange={e => set('phone', e.target.value)} /></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Role</Form.Label><Form.Select value={form.role} onChange={e => set('role', e.target.value)}>{ROLES.map(r => <option key={r}>{r}</option>)}</Form.Select></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Password</Form.Label><Form.Control type="password" required value={form.password} onChange={e => set('password', e.target.value)} /></Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button type="submit" disabled={saving} style={{ background: '#1e3a5f', borderColor: '#1e3a5f' }}>{saving ? 'Saving…' : 'Create User'}</Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}