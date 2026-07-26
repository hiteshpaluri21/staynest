import { useState } from 'react'
import { Modal, Form, Button, Alert } from 'react-bootstrap'
import { postFolioItem } from '../services/fds/stayService'
import { useAuth } from '../context/AuthContext'

const TYPES = ['ROOMRENT', 'FBCHARGE', 'LAUNDRY', 'SPA', 'TAX', 'DISCOUNT']

export default function AddChargeModal({ show, stayId, onClose, onSaved }) {
  const { user } = useAuth()
  const [form, setForm] = useState({ chargeType: 'FBCHARGE', description: '', amount: 0 })
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }))

  const submit = async (e) => {
    e.preventDefault(); setSaving(true); setError('')
    try {
      await postFolioItem(stayId, { ...form, amount: Number(form.amount), postedBy: user?.userId || 1 })
      onSaved()
    } catch (err) { setError(err.message) } finally { setSaving(false) }
  }

  return (
    <Modal show={show} onHide={onClose}>
      <Modal.Header closeButton><Modal.Title>Add Folio Charge</Modal.Title></Modal.Header>
      <Form onSubmit={submit}>
        <Modal.Body>
          {error && <Alert variant="danger" className="py-2">{error}</Alert>}
          <Form.Group className="mb-3"><Form.Label>Charge Type</Form.Label><Form.Select value={form.chargeType} onChange={e => set('chargeType', e.target.value)}>{TYPES.map(t => <option key={t}>{t}</option>)}</Form.Select></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Description</Form.Label><Form.Control required value={form.description} onChange={e => set('description', e.target.value)} /></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Amount (₹)</Form.Label><Form.Control type="number" step="0.01" min="0" required value={form.amount} onChange={e => set('amount', e.target.value)} /></Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button type="submit" disabled={saving} style={{ background: '#1e3a5f', borderColor: '#1e3a5f' }}>{saving ? 'Posting…' : 'Post Charge'}</Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}