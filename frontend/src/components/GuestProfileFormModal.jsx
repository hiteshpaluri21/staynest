import { useState, useEffect } from 'react'
import { Modal, Form, Button, Alert } from 'react-bootstrap'
import { updateGuest } from '../services/rbm/guestService'
import { validatePhone, validateName, normalizePhone, isClean } from '../utils/validation'
import { ID_DOCUMENT_TYPES, idTypeLabel } from '../utils/idTypes'

export default function GuestProfileFormModal({ show, guest, onClose, onSaved }) {
  const [form, setForm] = useState({ ...guest })
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [saving, setSaving] = useState(false)
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }))

  // Reset the form to the latest guest data each time the modal opens so cancelled edits don't persist.
  useEffect(() => { if (show) { setForm({ ...guest }); setError(''); setFieldErrors({}) } }, [show, guest])

  const submit = async (e) => {
    e.preventDefault()
    const errors = { name: validateName(form.name), phone: validatePhone(form.phone) }
    setFieldErrors(errors)
    if (!isClean(errors)) return

    setSaving(true); setError('')
    try {
      await updateGuest(guest.guestId, {
        name: form.name.trim(),
        phone: normalizePhone(form.phone),
        nationality: form.nationality,
        idDocumentType: form.idDocumentType,
        idNumber: form.idNumber,
        // preferencesJson: form.preferencesJson,
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
          <Form.Group className="mb-3">
            <Form.Label>Name</Form.Label>
            <Form.Control required isInvalid={!!fieldErrors.name} value={form.name || ''} onChange={e => set('name', e.target.value)} />
            <Form.Control.Feedback type="invalid">{fieldErrors.name}</Form.Control.Feedback>
          </Form.Group>
          <Form.Group className="mb-3">
            <Form.Label>Phone</Form.Label>
            <Form.Control type="tel" required isInvalid={!!fieldErrors.phone} value={form.phone || ''} onChange={e => set('phone', e.target.value)} placeholder="9876543210" />
            <Form.Control.Feedback type="invalid">{fieldErrors.phone}</Form.Control.Feedback>
          </Form.Group>
          <Form.Group className="mb-3"><Form.Label>Nationality</Form.Label><Form.Control value={form.nationality || ''} onChange={e => set('nationality', e.target.value)} /></Form.Group>
          <Form.Group className="mb-3">
            <Form.Label>ID Document Type</Form.Label>
            <Form.Select value={form.idDocumentType || ''} onChange={e => set('idDocumentType', e.target.value)}>
              <option value="">— Select ID type —</option>
              {/* Profiles saved before this was a dropdown may hold free text; keep that value
                  selectable so opening and saving the form can't silently discard it. */}
              {form.idDocumentType && !ID_DOCUMENT_TYPES.includes(form.idDocumentType) && (
                <option value={form.idDocumentType}>{form.idDocumentType}</option>
              )}
              {ID_DOCUMENT_TYPES.map(t => <option key={t} value={t}>{idTypeLabel(t)}</option>)}
            </Form.Select>
          </Form.Group>
          <Form.Group className="mb-3"><Form.Label>ID Number</Form.Label><Form.Control value={form.idNumber || ''} onChange={e => set('idNumber', e.target.value)} /></Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save'}</Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}