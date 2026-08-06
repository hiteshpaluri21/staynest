import { useEffect, useState } from 'react'
import { Modal, Form, Button, Alert } from 'react-bootstrap'
import { createRatePlan, updateRatePlan } from '../services/ric/ratePlanService'
import { getRoomTypes } from '../services/ric/roomTypeService'

const NAMES = ['RACK', 'CORPORATE', 'SEASONAL', 'PROMO']

const EMPTY = { roomTypeId: '', name: 'RACK', pricePerNight: 2500, validFrom: '', validTo: '', mealPlanIncluded: false }

/**
 * Creates a rate plan, or edits an existing one when `plan` is supplied.
 *
 * The backend rejects a second ACTIVE plan of the same name covering any of the same days
 * for a room type, so a clash comes back as a plain message and is shown inline here.
 */
export default function RatePlanFormModal({ show, plan, onClose, onSaved }) {
  const isEdit = plan != null
  const [types, setTypes] = useState([])
  const [form, setForm] = useState(EMPTY)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }))

  useEffect(() => {
    if (!show) return
    setError('')
    getRoomTypes().then(setTypes).catch(() => { })
    // Load the plan being edited into the form, or start from a blank one.
    setForm(isEdit
      ? {
        roomTypeId: String(plan.roomTypeId ?? ''),
        name: plan.name ?? 'RACK',
        pricePerNight: plan.pricePerNight ?? 0,
        validFrom: plan.validFrom ?? '',
        validTo: plan.validTo ?? '',
        mealPlanIncluded: !!plan.mealPlanIncluded,
      }
      : EMPTY)
  }, [show, plan])

  const submit = async (e) => {
    e.preventDefault(); setSaving(true); setError('')
    const payload = {
      ...form,
      roomTypeId: Number(form.roomTypeId),
      pricePerNight: Number(form.pricePerNight),
    }
    try {
      if (isEdit) await updateRatePlan(plan.ratePlanId, payload)
      else await createRatePlan(payload)
      onSaved()
    } catch (err) { setError(err.message) } finally { setSaving(false) }
  }

  return (
    <Modal show={show} onHide={onClose}>
      <Modal.Header closeButton>
        <Modal.Title>{isEdit ? `Edit Rate Plan #${plan.ratePlanId}` : 'Add Rate Plan'}</Modal.Title>
      </Modal.Header>
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
          <Form.Group className="mb-3">
            <Form.Label>Valid To</Form.Label>
            {/* Cannot end before it starts; the backend enforces the same rule. */}
            <Form.Control type="date" required min={form.validFrom || undefined} value={form.validTo} onChange={e => set('validTo', e.target.value)} />
          </Form.Group>
          <Form.Check type="switch" label="Meal Plan Included" checked={form.mealPlanIncluded} onChange={e => set('mealPlanIncluded', e.target.checked)} />
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button type="submit" disabled={saving}>
            {saving ? 'Saving…' : isEdit ? 'Save Changes' : 'Create'}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}
