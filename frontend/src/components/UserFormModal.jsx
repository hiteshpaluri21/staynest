import { useState } from 'react'
import { Modal, Form, Button, Alert } from 'react-bootstrap'
import { createUser } from '../services/iam/userService'
import { validateEmail, validatePhone, normalizePhone, isClean } from '../utils/validation'

const ROLES = ['GUEST', 'FRONTDESK', 'HOUSEKEEPING', 'FBMANAGER', 'ADMIN']

const BLANK = { name: '', email: '', phone: '', role: 'GUEST', password: '' }

export default function UserFormModal({ show, onClose, onSaved }) {
  const [form, setForm] = useState(BLANK)
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [saving, setSaving] = useState(false)

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }))

  const validateAll = (values) => ({
    email: validateEmail(values.email),
    phone: validatePhone(values.phone),
    password: values.password.length < 6 ? 'Password must be at least 6 characters' : '',
  })

  const submit = async (e) => {
    e.preventDefault()
    const errors = validateAll(form)
    setFieldErrors(errors)
    if (!isClean(errors)) return

    setSaving(true); setError('')
    try {
      await createUser({ ...form, name: form.name.trim(), email: form.email.trim(), phone: normalizePhone(form.phone) })
      setForm(BLANK)
      setFieldErrors({})
      onSaved()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal show={show} onHide={onClose}>
      <Modal.Header closeButton><Modal.Title>Add User</Modal.Title></Modal.Header>
      <Form onSubmit={submit}>
        <Modal.Body>
          {error && <Alert variant="danger" className="py-2">{error}</Alert>}
          <Form.Group className="mb-3"><Form.Label>Name</Form.Label><Form.Control required value={form.name} onChange={e => set('name', e.target.value)} /></Form.Group>
          <Form.Group className="mb-3">
            <Form.Label>Email</Form.Label>
            <Form.Control type="email" required isInvalid={!!fieldErrors.email} value={form.email} onChange={e => set('email', e.target.value)} />
            <Form.Control.Feedback type="invalid">{fieldErrors.email}</Form.Control.Feedback>
          </Form.Group>
          <Form.Group className="mb-3">
            <Form.Label>Phone</Form.Label>
            <Form.Control type="tel" required isInvalid={!!fieldErrors.phone} value={form.phone} onChange={e => set('phone', e.target.value)} placeholder="9876543210" />
            <Form.Control.Feedback type="invalid">{fieldErrors.phone}</Form.Control.Feedback>
          </Form.Group>
          <Form.Group className="mb-3"><Form.Label>Role</Form.Label><Form.Select value={form.role} onChange={e => set('role', e.target.value)}>{ROLES.map(r => <option key={r}>{r}</option>)}</Form.Select></Form.Group>
          <Form.Group className="mb-3">
            <Form.Label>Password</Form.Label>
            <Form.Control type="password" required isInvalid={!!fieldErrors.password} value={form.password} onChange={e => set('password', e.target.value)} />
            <Form.Control.Feedback type="invalid">{fieldErrors.password}</Form.Control.Feedback>
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button type="submit" disabled={saving}>{saving ? 'Saving…' : 'Create User'}</Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}