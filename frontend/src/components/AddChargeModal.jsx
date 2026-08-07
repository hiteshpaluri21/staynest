import { useEffect, useState } from 'react'
import { Modal, Form, Button, Alert } from 'react-bootstrap'
import { postFolioItem, updateFolioItem } from '../services/fds/stayService'
import { useAuth } from '../context/AuthContext'

const TYPES = ['ROOMRENT', 'FBCHARGE', 'LAUNDRY', 'SPA', 'TAX', 'DISCOUNT']

// `item` (optional) puts the modal in edit mode, pre-filled from the selected folio item.
export default function AddChargeModal({ show, stayId, item, onClose, onSaved }) {
  const { user } = useAuth()
  const isEdit = !!item
  const [form, setForm] = useState({ chargeType: 'FBCHARGE', description: '', amount: 0 })
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }))

  // Re-seed the form whenever the modal opens (fresh for add, pre-filled for edit).
  useEffect(() => {
    if (!show) return
    setError('')
    setForm(item
      ? { chargeType: item.chargeType, description: item.description || '', amount: item.amount }
      : { chargeType: 'FBCHARGE', description: '', amount: 0 })
  }, [show, item])

  const submit = async (e) => {
    e.preventDefault(); setSaving(true); setError('')
    try {
      // null, never a fallback id — defaulting to user 1 credited the charge to whoever that
      // happens to be. frontdesk-service records "unknown" for a null poster.
      const payload = { ...form, amount: Number(form.amount), postedBy: user?.userId ?? null }
      if (isEdit) await updateFolioItem(stayId, item.folioItemId, payload)
      else await postFolioItem(stayId, payload)
      onSaved()
    } catch (err) { setError(err.message) } finally { setSaving(false) }
  }

  return (
    <Modal show={show} onHide={onClose}>
      <Modal.Header closeButton><Modal.Title>{isEdit ? 'Edit Folio Charge' : 'Add Folio Charge'}</Modal.Title></Modal.Header>
      <Form onSubmit={submit}>
        <Modal.Body>
          {error && <Alert variant="danger" className="py-2">{error}</Alert>}
          <Form.Group className="mb-3"><Form.Label>Charge Type</Form.Label><Form.Select value={form.chargeType} onChange={e => set('chargeType', e.target.value)}>{TYPES.map(t => <option key={t}>{t}</option>)}</Form.Select></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Description</Form.Label><Form.Control required value={form.description} onChange={e => set('description', e.target.value)} /></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Amount (₹)</Form.Label><Form.Control type="number" step="0.01" min="0" required value={form.amount} onChange={e => set('amount', e.target.value)} /></Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button type="submit" disabled={saving}>{saving ? 'Saving…' : (isEdit ? 'Save Changes' : 'Post Charge')}</Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}