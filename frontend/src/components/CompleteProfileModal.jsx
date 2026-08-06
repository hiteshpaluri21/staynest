import { useEffect, useState } from 'react'
import { Modal, Form, Button, Alert } from 'react-bootstrap'
import { updateGuest } from '../services/rbm/guestService'
import { useAuth } from '../context/AuthContext'
import { validatePhone, normalizePhone, isClean, missingProfileFields } from '../utils/validation'

const ID_TYPES = ['PASSPORT', 'AADHAAR', 'DRIVING_LICENSE', 'NATIONAL_ID', 'VOTER_ID']

/**
 * Shown when a guest tries to book with an incomplete profile. On save it refreshes the
 * cached profile and calls onCompleted() so the caller can continue straight into booking —
 * the guest never loses their search.
 */
export default function CompleteProfileModal({ show, onClose, onCompleted }) {
  const { guest, refreshGuest } = useAuth()
  const [form, setForm] = useState({ phone: '', nationality: '', idDocumentType: 'PASSPORT', idNumber: '' })
  const [fieldErrors, setFieldErrors] = useState({})
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }))

  useEffect(() => {
    if (!show) return
    setError(''); setFieldErrors({})
    setForm({
      phone: guest?.phone || '',
      nationality: guest?.nationality || '',
      idDocumentType: guest?.idDocumentType || 'PASSPORT',
      idNumber: guest?.idNumber || '',
    })
  }, [show, guest])

  const missing = missingProfileFields(guest).map(f => f.label).join(', ')

  const submit = async (e) => {
    e.preventDefault()
    const errors = {
      phone: validatePhone(form.phone),
      idNumber: form.idNumber.trim() ? '' : 'ID number is required',
      idDocumentType: form.idDocumentType ? '' : 'ID document type is required',
    }
    setFieldErrors(errors)
    if (!isClean(errors)) return

    setSaving(true); setError('')
    try {
      // name is required by the backend and is not editable here, so pass the stored one through.
      await updateGuest(guest.guestId, {
        name: guest.name,
        phone: normalizePhone(form.phone),
        nationality: form.nationality.trim() || null,
        idDocumentType: form.idDocumentType,
        idNumber: form.idNumber.trim(),
        preferencesJson: guest.preferencesJson ?? null,
      })
      await refreshGuest()
      onCompleted()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal show={show} onHide={onClose}>
      <Modal.Header closeButton><Modal.Title>Complete Your Profile</Modal.Title></Modal.Header>
      <Form onSubmit={submit}>
        <Modal.Body>
          {error && <Alert variant="danger" className="py-2">{error}</Alert>}
          <p className="small text-muted">
            We need a few details before you can reserve a room{missing ? ` — missing: ${missing}` : ''}.
          </p>

          <Form.Group className="mb-3">
            <Form.Label>Phone Number</Form.Label>
            <Form.Control type="tel" required isInvalid={!!fieldErrors.phone} value={form.phone}
              onChange={e => set('phone', e.target.value)} placeholder="9876543210" />
            <Form.Control.Feedback type="invalid">{fieldErrors.phone}</Form.Control.Feedback>
          </Form.Group>

          <Form.Group className="mb-3">
            <Form.Label>ID Document Type</Form.Label>
            <Form.Select value={form.idDocumentType} onChange={e => set('idDocumentType', e.target.value)}>
              {ID_TYPES.map(t => <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>)}
            </Form.Select>
          </Form.Group>

          <Form.Group className="mb-3">
            <Form.Label>ID Number</Form.Label>
            <Form.Control required isInvalid={!!fieldErrors.idNumber} value={form.idNumber}
              onChange={e => set('idNumber', e.target.value)} />
            <Form.Control.Feedback type="invalid">{fieldErrors.idNumber}</Form.Control.Feedback>
          </Form.Group>

          <Form.Group className="mb-3">
            <Form.Label>Nationality <span className="text-muted small">(optional)</span></Form.Label>
            <Form.Control value={form.nationality} onChange={e => set('nationality', e.target.value)} />
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button type="submit" disabled={saving}>
            {saving ? 'Saving…' : 'Save & Continue'}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}
