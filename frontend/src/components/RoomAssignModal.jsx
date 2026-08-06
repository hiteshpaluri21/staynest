import { useEffect, useState } from 'react'
import { Modal, Form, Button, Alert } from 'react-bootstrap'
import { getRooms } from '../services/ric/roomService'
import { checkIn } from '../services/fds/stayService'

export default function RoomAssignModal({ reservation, onClose, onDone }) {
  const [rooms, setRooms] = useState([])
  const [roomId, setRoomId] = useState('')
  const [error, setError] = useState('')
  const [loadError, setLoadError] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    let cancelled = false
    setLoading(true); setLoadError(''); setRoomId('')

    // Allocation asks "which physical rooms of this type are free right now", so filter on room
    // STATUS. The date-based /api/rooms/available endpoint answers a different question — how
    // many of this type can still be sold — and it subtracts THIS reservation from its own
    // type's inventory. For types with only one or two rooms that left the dropdown empty, so
    // deluxe/villa guests could not be checked in at all.
    getRooms({ roomTypeId: reservation.roomTypeId })
      .then(list => {
        if (cancelled) return
        setRooms((list || []).filter(r => r.status === 'AVAILABLE'))
      })
      .catch(err => { if (!cancelled) setLoadError(err.message) })
      .finally(() => { if (!cancelled) setLoading(false) })

    return () => { cancelled = true }
  }, [reservation])

  const submit = async (e) => {
    e.preventDefault(); setSaving(true); setError('')
    try {
      await checkIn({ reservationId: reservation.reservationId, roomId: Number(roomId) })
      onDone()
    } catch (err) { setError(err.message) } finally { setSaving(false) }
  }

  const noneFree = !loading && !loadError && rooms.length === 0

  return (
    <Modal show onHide={onClose}>
      <Modal.Header closeButton><Modal.Title>Assign Room — Res {reservation.reservationId}</Modal.Title></Modal.Header>
      <Form onSubmit={submit}>
        <Modal.Body>
          {error && <Alert variant="danger" className="py-2">{error}</Alert>}
          {loadError && <Alert variant="danger" className="py-2">Could not load rooms: {loadError}</Alert>}
          {noneFree && (
            <Alert variant="warning" className="py-2">
              No rooms of this type are free right now — they are all occupied, being cleaned, or blocked.
              Free one up in Rooms, then check in.
            </Alert>
          )}
          <p className="small text-muted">
            Guest: {reservation.guestName} · {reservation.roomTypeName || `Type ${reservation.roomTypeId}`}
          </p>
          <Form.Group><Form.Label>Available Rooms</Form.Label>
            <Form.Select required value={roomId} disabled={loading || noneFree} onChange={e => setRoomId(e.target.value)}>
              <option value="">{loading ? 'Loading rooms…' : '— Select Room —'}</option>
              {rooms.map(r => <option key={r.roomId} value={r.roomId}>Room {r.roomNumber} (Floor {r.floor})</option>)}
            </Form.Select>
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button type="submit" variant="success" disabled={saving || !roomId}>{saving ? 'Checking in…' : 'Confirm Check-In'}</Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}
