import { useEffect, useState } from 'react'
import { Modal, Form, Button, Alert } from 'react-bootstrap'
import { createRatePlan } from '../services/ric/ratePlanService'
import { getRoomTypes } from '../services/ric/roomTypeService'

const NAMES = ['RACK', 'CORPORATE', 'SEASONAL', 'PROMO']

export default function RatePlanFormModal({ show, onClose, onSaved }) {
  const [types, setTypes] = useState([])
  const [form, setForm] = useState({ roomTypeId: '', name: 'RACK', pricePerNight: 2500, validFrom: '', validTo: '', mealPlanIncluded: false })
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }))

  useEffect(() => { if (show) getRoomTypes().then(setTypes).catch(() => {}) }, [show])

  const submit = async (e) => {
    e.preventDefault(); setSaving(true); setError('')
    try {
      await createRatePlan({
        ...form,
        roomTypeId: Number(form.roomTypeId),
        pricePerNight: Number(form.pricePerNight),
      })
      onSaved()
    } catch (err) { setError(err.message) } finally { setSaving(false) }
  }

  return (
    <Modal show={show} onHide={onClose}>
      <Modal.Header closeButton><Modal.Title>Add Rate Plan</Modal.Title></Modal.Header>
      <Form onSubmit={submit}>
        <Modal.Body>
          {error && <Alert variant="danger" className="py-2">{error}</Alert>}
          <Form.Group className="mb-3"><Form.Label>Room Type</Form.Label>
            <Form.Select required value={form.roomTypeId} onChange={e => set('roomTypeId', e.target.value)}>
              <option value="">— Select —</option>
              {types.map(t => <option key={t.roomTypeId} value={t.roomTypeId}>
                                {t.name} — {t.amenitiesList || 'no amenities'} (₹{t.baseRate})
                              </option>)}
            </Form.Select>
          </Form.Group>
          <Form.Group className="mb-3"><Form.Label>Plan Name</Form.Label><Form.Select value={form.name} onChange={e => set('name', e.target.value)}>{NAMES.map(n => <option key={n}>{n}</option>)}</Form.Select></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Price per Night (₹)</Form.Label><Form.Control type="number" min="0" required value={form.pricePerNight} onChange={e => set('pricePerNight', e.target.value)} /></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Valid From</Form.Label><Form.Control type="date" required value={form.validFrom} onChange={e => set('validFrom', e.target.value)} /></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Valid To</Form.Label><Form.Control type="date" required value={form.validTo} onChange={e => set('validTo', e.target.value)} /></Form.Group>
          <Form.Check type="switch" label="Meal Plan Included" checked={form.mealPlanIncluded} onChange={e => set('mealPlanIncluded', e.target.checked)} />
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button type="submit" disabled={saving}>{saving ? 'Saving…' : 'Create'}</Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}