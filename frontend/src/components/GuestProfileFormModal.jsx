import { useState, useEffect } from 'react'
import { Modal, Form, Button, Alert } from 'react-bootstrap'
import { updateGuest } from '../services/rbm/guestService'

export default function GuestProfileFormModal({ show, guest, onClose, onSaved }) {
  const [form, setForm] = useState({ ...guest })
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }))

  // Reset the form to the latest guest data each time the modal opens so cancelled edits don't persist.
  useEffect(() => { if (show) { setForm({ ...guest }); setError('') } }, [show, guest])

  const submit = async (e) => {
    e.preventDefault(); setSaving(true); setError('')
    try {
      await updateGuest(guest.guestId, {
        name: form.name,
        phone: form.phone,
        nationality: form.nationality,
        idDocumentType: form.idDocumentType,
        idNumber: form.idNumber,
        preferencesJson: form.preferencesJson,
      })
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
          <Form.Group className="mb-3"><Form.Label>ID Document Type</Form.Label><Form.Control value={form.idDocumentType || ''} onChange={e => set('idDocumentType', e.target.value)} /></Form.Group>
          <Form.Group className="mb-3"><Form.Label>ID Number</Form.Label><Form.Control value={form.idNumber || ''} onChange={e => set('idNumber', e.target.value)} /></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Preferences</Form.Label><Form.Control as="textarea" rows={2} value={form.preferencesJson || ''} onChange={e => set('preferencesJson', e.target.value)} /></Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button type="submit" disabled={saving} style={{ background: '#1e3a5f', borderColor: '#1e3a5f' }}>{saving ? 'Saving…' : 'Save'}</Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}