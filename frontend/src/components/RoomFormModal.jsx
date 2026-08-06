import { useEffect, useState } from 'react'
import { Modal, Form, Button, Alert } from 'react-bootstrap'
import { createRoom } from '../services/ric/roomService'
import { getRoomTypes } from '../services/ric/roomTypeService'

export default function RoomFormModal({ show, onClose, onSaved }) {
  const [types, setTypes] = useState([])
  const [form, setForm] = useState({ roomNumber: '', floor: 1, roomTypeId: '' })
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }))

  useEffect(() => {
    if (show) getRoomTypes().then(setTypes).catch(() => {})
  }, [show])

  const submit = async (e) => {
    e.preventDefault(); 
    setSaving(true); 
    setError('')
    try { 
      await createRoom({ 
        ...form, 
        floor: Number(form.floor), 
        roomTypeId: Number(form.roomTypeId) 
      }) 
      onSaved() 
    } catch (err) { 
      setError(err.message) 
    } finally { 
      setSaving(false) 
    }
  }

  return (
    <Modal show={show} onHide={onClose}>
      <Modal.Header closeButton><Modal.Title>Add Room</Modal.Title></Modal.Header>
      <Form onSubmit={submit}>
        <Modal.Body>
          {error && <Alert variant="danger" className="py-2">{error}</Alert>}
          <Form.Group className="mb-3"><Form.Label>Room Number</Form.Label><Form.Control required value={form.roomNumber} onChange={e => set('roomNumber', e.target.value)} /></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Floor</Form.Label><Form.Control type="number" min="1" required value={form.floor} onChange={e => set('floor', e.target.value)} /></Form.Group>
          <Form.Group className="mb-3"><Form.Label>Room Type</Form.Label>
            <Form.Select required value={form.roomTypeId} onChange={e => set('roomTypeId', e.target.value)}>
              <option value="">— Select —</option>
              {types.map(t => <option key={t.roomTypeId} value={t.roomTypeId}>
                                {t.name} — {t.amenitiesList || 'no amenities'} (₹{t.baseRate})
                              </option>)}
            </Form.Select>
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={saving}>
            {saving ? 'Saving…' : 'Create'}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}