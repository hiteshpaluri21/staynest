import { useEffect, useState } from 'react'
import { Modal, Form, Button, Alert } from 'react-bootstrap'
import { getAvailableRooms } from '../services/ric/roomService'
import { checkIn } from '../services/fds/stayService'

export default function RoomAssignModal({ reservation, onClose, onDone }) {
  const [rooms, setRooms] = useState([])
  const [roomId, setRoomId] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    getAvailableRooms(reservation.checkInDate, reservation.checkOutDate)
      .then(r => setRooms(r.filter(x => x.roomTypeId === reservation.roomTypeId)))
      .catch(() => {})
  }, [reservation])

  const submit = async (e) => {
    e.preventDefault(); setSaving(true); setError('')
    try {
      await checkIn({ reservationId: reservation.reservationId, roomId: Number(roomId) })
      onDone()
    } catch (err) { setError(err.message) } finally { setSaving(false) }
  }

  return (
    <Modal show onHide={onClose}>
      <Modal.Header closeButton><Modal.Title>Assign Room — Res #{reservation.reservationId}</Modal.Title></Modal.Header>
      <Form onSubmit={submit}>
        <Modal.Body>
          {error && <Alert variant="danger" className="py-2">{error}</Alert>}
          <p className="small text-muted">Guest: {reservation.guestName} · Type {reservation.roomTypeId}</p>
          <Form.Group><Form.Label>Available Rooms</Form.Label>
            <Form.Select required value={roomId} onChange={e => setRoomId(e.target.value)}>
              <option value="">— Select Room —</option>
              {rooms.map(r => <option key={r.roomId} value={r.roomId}>#{r.roomNumber} (Floor {r.floor})</option>)}
            </Form.Select>
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button type="submit" disabled={saving || !roomId} style={{ background: '#16a34a', borderColor: '#16a34a' }}>{saving ? 'Checking in…' : 'Confirm Check-In'}</Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}