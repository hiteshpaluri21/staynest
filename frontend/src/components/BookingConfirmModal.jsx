import { useState } from 'react'
import { Modal, Form, Button, Alert, Badge } from 'react-bootstrap'
import { createReservation } from '../services/rbm/reservationService'
import { useAuth } from '../context/AuthContext'

export default function BookingConfirmModal({ data, onClose }) {
  const { user } = useAuth()
  const [ratePlanId, setRatePlanId] = useState(data.ratePlans?.[0]?.ratePlanId || '')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [done, setDone] = useState(false)

  const nights = Math.max(1, Math.ceil((new Date(data.checkOut) - new Date(data.checkIn)) / 86400000))
  const rate = data.ratePlans?.find(p => p.ratePlanId === Number(ratePlanId))
  const total = rate ? rate.pricePerNight * nights : (data.type?.baseRate || 0) * nights

  const submit = async (e) => {
    e.preventDefault(); setSaving(true); setError('')
    try {
      await createReservation({
        guestId: user?.userId || 1,
        roomTypeId: data.type.roomTypeId,
        ratePlanId: Number(ratePlanId) || null,
        checkInDate: data.checkIn,
        checkOutDate: data.checkOut,
        nights,
        adults: Number(data.adults),
        children: Number(data.children),
        totalAmount: total,
        bookingChannel: 'DIRECT',
      })
      setDone(true)
      setTimeout(() => { onClose(); window.location.href = '/my-reservations' }, 1200)
    } catch (err) { setError(err.message) } finally { setSaving(false) }
  }

  return (
    <Modal show onHide={onClose}>
      <Modal.Header closeButton><Modal.Title>Confirm Booking</Modal.Title></Modal.Header>
      {done ? (
        <Modal.Body><div className="alert alert-success mb-0">Reservation confirmed! Redirecting…</div></Modal.Body>
      ) : (
        <Form onSubmit={submit}>
          <Modal.Body>
            {error && <Alert variant="danger" className="py-2">{error}</Alert>}
            <p><Badge bg="primary">{data.type?.name}</Badge> Room #{data.room?.roomNumber} · Floor {data.room?.floor}</p>
            <p className="small text-muted">Check-in: {data.checkIn} · Check-out: {data.checkOut} · {nights} night(s)</p>
            <Form.Group className="mb-3">
              <Form.Label>Rate Plan</Form.Label>
              <Form.Select value={ratePlanId} onChange={e => setRatePlanId(e.target.value)}>
                {data.ratePlans?.map(p => <option key={p.ratePlanId} value={p.ratePlanId}>{p.name} — ₹{p.pricePerNight}/night {p.mealPlanIncluded ? '(incl. meals)' : ''}</option>)}
                {(!data.ratePlans || data.ratePlans.length === 0) && <option value="">— Use Base Rate —</option>}
              </Form.Select>
            </Form.Group>
            <div className="d-flex justify-content-between mt-3">
              <strong>Total Amount</strong><strong>₹{total}</strong>
            </div>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={onClose}>Cancel</Button>
            <Button type="submit" disabled={saving} style={{ background: '#f59e0b', borderColor: '#f59e0b' }}>{saving ? 'Booking…' : 'Confirm Booking'}</Button>
          </Modal.Footer>
        </Form>
      )}
    </Modal>
  )
}