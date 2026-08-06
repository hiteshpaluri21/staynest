import { useState } from 'react'
import { Modal, Form, Button, Alert, Badge } from 'react-bootstrap'
import { createReservation } from '../services/rbm/reservationService'
import { useAuth } from '../context/AuthContext'

export default function BookingConfirmModal({ data, onClose }) {
  const { guestId, user } = useAuth()
  const [ratePlanId, setRatePlanId] = useState(data.ratePlans?.[0]?.ratePlanId || '')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [done, setDone] = useState(false)

  const nights = Math.max(1, Math.ceil((new Date(data.checkOut) - new Date(data.checkIn)) / 86400000))
  const rate = data.ratePlans?.find(p => p.ratePlanId === Number(ratePlanId))
  const total = rate ? rate.pricePerNight * nights : (data.type?.baseRate || 0) * nights

  const submit = async (e) => {
    e.preventDefault(); setSaving(true); setError('')
    if (!data.type?.roomTypeId) {
      setError('Room type is unavailable — please retry your search.'); setSaving(false); return
    }
    const partySize = Number(data.adults) + Number(data.children || 0)
    if (data.type.maxOccupancy && partySize > data.type.maxOccupancy) {
      setError(`This room type sleeps up to ${data.type.maxOccupancy} guest(s), but ${partySize} were selected.`)
      setSaving(false); return
    }
    if (!guestId) {
      setError('Your guest profile could not be loaded — please sign in again.'); setSaving(false); return
    }
    // A booking has to be priced against a real rate plan. This used to be sent as null and
    // reservation-service silently substituted plan 1, pricing the stay off an unrelated plan.
    if (!Number(ratePlanId)) {
      setError('No rate plan is available for these dates, so this room cannot be booked yet.')
      setSaving(false); return
    }
    try {
      await createReservation({
        // Must be the reservation-service guestId, not the IAM userId. Sending userId filed the
        // booking under a different, auto-created profile, so it never showed in My Reservations.
        guestId,
        roomTypeId: data.type.roomTypeId,
        ratePlanId: Number(ratePlanId),
        checkInDate: data.checkIn,
        checkOutDate: data.checkOut,
        nights,
        adults: Number(data.adults),
        children: Number(data.children),
        totalAmount: total,
        bookingChannel: 'DIRECT',
      })
      setDone(true)
      // /my-reservations is guest-only now, so send staff to the shared reservations list
      // instead of bouncing them onto the unauthorized page.
      const dest = user?.role === 'GUEST' ? '/my-reservations' : '/reservations'
      setTimeout(() => { onClose(); window.location.href = dest }, 1200)
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
            <p><Badge bg="primary">{data.type?.name}</Badge> {data.type?.bedConfiguration}</p>
            <p className="small text-muted">Check-in: {data.checkIn} · Check-out: {data.checkOut} · {nights} night(s)</p>
            <p className="small text-muted">{data.adults} adult(s), {data.children || 0} child(ren) · sleeps up to {data.type?.maxOccupancy}</p>
            <p className="small text-muted">Your room number is assigned when you check in.</p>
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
            <Button type="submit" disabled={saving} className="btn-accent">{saving ? 'Booking…' : 'Confirm Booking'}</Button>
          </Modal.Footer>
        </Form>
      )}
    </Modal>
  )
}