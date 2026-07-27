import { useState } from 'react'
import { Modal, Form, Button, Alert } from 'react-bootstrap'
import { createRoomType } from '../services/ric/roomTypeService'

const NAMES = ['STANDARD', 'DELUXE', 'SUITE', 'VILLA']

export default function RoomTypeFormModal({ show, onClose, onSaved }) {
  const [form, setForm] = useState({ name: 'STANDARD', bedConfiguration: '1 King Bed', maxOccupancy: 2, baseRate: 2000, amenitiesList: 'WiFi, AC' })
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }))

  const submit = async (e) => {
    e.preventDefault(); 
    setSaving(true); 
    setError('')
    
    try { 
      await createRoomType({ ...form, maxOccupancy: Number(form.maxOccupancy), baseRate: Number(form.baseRate) }); 
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
      <Modal.Header closeButton><Modal.Title>Add Room Type</Modal.Title></Modal.Header>
      
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
          <Button type="submit" disabled={saving} style={{ background: '#1e3a5f', borderColor: '#1e3a5f' }}>{saving ? 'Saving…' : 'Create'}</Button>
        </Modal.Footer>

      </Form>
    </Modal>
  )
}