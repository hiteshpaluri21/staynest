import { useState } from 'react'
import { Modal, Form, Button, Alert } from 'react-bootstrap'
import { api } from '../services/api'

export default function GuestProfileFormModal({ show, guest, onClose, onSaved }) {
  const [form, setForm] = useState({ ...guest })
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }))

  const submit = async (e) => {
    e.preventDefault(); setSaving(true); setError('')
    try {
      // Reservation-service doesn't expose a PUT; if you add one, swap this. For now, we use the loyalty endpoint to demonstrate.
      // For full edit support add PATCH /api/guests/{id} on backend.
      await api.patch(`/api/guests/${guest.guestId}/loyalty`, null, { params: { tier: form.loyaltyTier } })
      onSaved()
    } catch (err) { setError(err.message) } finally { setSaving(false) }
  }

  return (
    <Modal show={show} onHide={onClose}>
      <Modal.Header closeButton><Modal.Title>Edit Profile</Modal.Title></Modal.Header>
      <Form onSubmit={submit}>
        <Modal.Body>
          {error && <Alert variant="danger" className="py-2">{error}</Alert>}
          <Form.Group className="mb-3"><Form.Label>Name</Form.Label><Form.Control value={form.name || ''} onChange={e => set('name', e.target.value)} /></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Phone</Form.Label><Form.Control value={form.phone || ''} onChange={e => set('phone', e.target.value)} /></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Nationality</Form.Label><Form.Control value={form.nationality || ''} onChange={e => set('nationality', e.target.value)} /></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Preferences</Form.Label><Form.Control as="textarea" rows={2} value={form.preferencesJson || ''} onChange={e => set('preferencesJson', e.target.value)} /></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Loyalty Tier</Form.Label>
            <Form.Select value={form.loyaltyTier || 'NONE'} onChange={e => set('loyaltyTier', e.target.value)}>
              {['NONE', 'SILVER', 'GOLD', 'PLATINUM'].map(t => <option key={t}>{t}</option>)}
            </Form.Select>
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button type="submit" disabled={saving} style={{ background: '#1e3a5f', borderColor: '#1e3a5f' }}>{saving ? 'Saving…' : 'Save'}</Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}