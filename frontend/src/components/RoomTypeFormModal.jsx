import { useEffect, useState } from 'react'
import { Modal, Form, Button, Alert } from 'react-bootstrap'
import { createRoomType, updateRoomType } from '../services/ric/roomTypeService'

const NAMES = ['STANDARD', 'DELUXE', 'SUITE', 'VILLA']

const BLANK = { name: 'STANDARD', bedConfiguration: '1 King Bed', maxOccupancy: 2, baseRate: 2000, amenitiesList: 'WiFi, AC' }

export default function RoomTypeFormModal({ show, onClose, onSaved, roomType }) {
  const isEdit = Boolean(roomType)
  const [form, setForm] = useState(BLANK)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }))

  // Reload the fields whenever the modal is (re)opened so an edit shows the selected
  // room type and a create always starts from a clean form.
  useEffect(() => {
    if (!show) return
    setError('')
    setForm(roomType ? {
      name: roomType.name,
      bedConfiguration: roomType.bedConfiguration || '',
      maxOccupancy: roomType.maxOccupancy,
      baseRate: roomType.baseRate,
      amenitiesList: roomType.amenitiesList || '',
    } : BLANK)
  }, [show, roomType])

  const submit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('')

    try {
      const payload = { ...form, maxOccupancy: Number(form.maxOccupancy), baseRate: Number(form.baseRate) }
      if (isEdit) await updateRoomType(roomType.roomTypeId, payload)
      else await createRoomType(payload)
      onSaved()
    }
    catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal show={show} onHide={onClose}>
      <Modal.Header closeButton><Modal.Title>{isEdit ? 'Edit Room Type' : 'Add Room Type'}</Modal.Title></Modal.Header>
      
      <Form onSubmit={submit}>

        <Modal.Body>
          {error && <Alert variant="danger" className="py-2">{error}</Alert>}
          <Form.Group className="mb-3"><Form.Label>Name</Form.Label><Form.Select value={form.name} onChange={e => set('name', e.target.value)}>{NAMES.map(n => <option key={n}>{n}</option>)}</Form.Select></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Bed Configuration</Form.Label><Form.Control required value={form.bedConfiguration} onChange={e => set('bedConfiguration', e.target.value)} placeholder="e.g., 1 King + 1 Sofa" /></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Max Occupancy</Form.Label><Form.Control type="number" min="1" value={form.maxOccupancy} onChange={e => set('maxOccupancy', e.target.value)} /></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Base Rate (₹)</Form.Label><Form.Control type="number" min="0" value={form.baseRate} onChange={e => set('baseRate', e.target.value)} /></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Amenities (comma-separated)</Form.Label><Form.Control value={form.amenitiesList} onChange={e => set('amenitiesList', e.target.value)} placeholder="WiFi, AC, Mini-bar" /></Form.Group>
        </Modal.Body>

        <Modal.Footer>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button type="submit" disabled={saving}>{saving ? 'Saving…' : isEdit ? 'Save Changes' : 'Create'}</Button>
        </Modal.Footer>

      </Form>
    </Modal>
  )
}