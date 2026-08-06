import { useEffect, useState } from 'react'
import { Card, Row, Col, Form, Button, Badge, InputGroup } from 'react-bootstrap'
import { getAvailableRooms } from '../../services/ric/roomService'
import { getRoomTypes } from '../../services/ric/roomTypeService'
import { getRatePlans } from '../../services/ric/ratePlanService'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'
import BookingConfirmModal from '../../components/BookingConfirmModal'
import CompleteProfileModal from '../../components/CompleteProfileModal'
import { useAuth } from '../../context/AuthContext'
import { isProfileComplete } from '../../utils/validation'

const formatDate = (date) => {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const addDays = (dateStr, days) => {
  const d = new Date(dateStr + 'T00:00:00')
  d.setDate(d.getDate() + days)
  return formatDate(d)
}

export default function BookingSearchPage() {
  const todayStr = formatDate(new Date())
  const tomorrowStr = addDays(todayStr, 1)
  const [form, setForm] = useState({ checkIn: todayStr, checkOut: tomorrowStr, adults: 2, children: 0 })
  const [rooms, setRooms] = useState([])
  const [types, setTypes] = useState([])
  const [plans, setPlans] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [searched, setSearched] = useState(false)
  const [selectedType, setSelectedType] = useState(null)
  // Room type the guest picked while their profile was still incomplete; booking resumes with
  // it as soon as they finish filling in the missing details.
  const [pendingType, setPendingType] = useState(null)
  const { user, guest } = useAuth()
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }))

  const handleCheckInChange = (e) => {
    const newIn = e.target.value
    const minOut = addDays(newIn, 1)
    setForm(f => ({
      ...f,
      checkIn: newIn,
      checkOut: f.checkOut <= newIn ? minOut : f.checkOut
    }))
  }

  useEffect(() => {
    Promise.all([getRoomTypes(), getRatePlans()]).then(([t, p]) => { setTypes(t); setPlans(p) }).catch(() => {})
  }, [])

  const search = async (e) => {
    e.preventDefault()
    setLoading(true); setError(''); setSearched(true)
    try { setRooms(await getAvailableRooms(form.checkIn, form.checkOut)) }
    catch (err) { setError(err.message) } finally { setLoading(false) }
  }

  const typeMap = Object.fromEntries(types.map(t => [t.roomTypeId, t]))

  // Guests book a room type, not a specific room — the actual room is assigned at check-in.
  // The availability endpoint returns individual rooms, so collapse them into one entry per
  // room type carrying how many of that type are still free for the selected dates.
  const availableTypes = Object.values(
    rooms.reduce((acc, r) => {
      if (!acc[r.roomTypeId]) acc[r.roomTypeId] = { roomTypeId: r.roomTypeId, type: typeMap[r.roomTypeId], count: 0 }
      acc[r.roomTypeId].count += 1
      return acc
    }, {})
  ).filter(g => g.type && g.count > 0)

  // A room type can only be booked if the whole party fits within its max occupancy.
  const partySize = Math.max(1, Number(form.adults) || 0) + Math.max(0, Number(form.children) || 0)

  // Guests must have phone + ID details on file before reserving. Staff booking on a guest's
  // behalf are not gated here.
  const requestBooking = (selection) => {
    if (user?.role === 'GUEST' && !isProfileComplete(guest)) {
      setPendingType(selection)
      return
    }
    setSelectedType(selection)
  }

  // Only rate plans that are ACTIVE and valid for the whole selected stay
  // (validFrom on/before check-in, validTo on/after the last night) should appear.
  const plansByType = (id) => {
    const lastNight = addDays(form.checkOut, -1)
    return plans.filter(p =>
      p.roomTypeId === id &&
      (!p.status || p.status === 'ACTIVE') &&
      (!p.validFrom || p.validFrom <= form.checkIn) &&
      (!p.validTo || p.validTo >= lastNight)
    )
  }

  return (
    <div>
      <h4 className="mb-3">Search Availability</h4>
      <Card className="shadow-sm mb-4">
        <Card.Body>
          <Form onSubmit={search}>
            {/* Five across only from lg up. At md the date inputs were ~180px and the
                number inputs ~120px, which is too narrow to read. Below that they pair
                up two per row, and the button takes the full width. */}
            <Row className="g-2">
              <Col xs={6} lg={3}><Form.Group><Form.Label>Check-In</Form.Label><Form.Control type="date" min={todayStr} required value={form.checkIn} onChange={handleCheckInChange} /></Form.Group></Col>
              <Col xs={6} lg={3}><Form.Group><Form.Label>Check-Out</Form.Label><Form.Control type="date" min={addDays(form.checkIn, 1)} required value={form.checkOut} onChange={e => set('checkOut', e.target.value)} /></Form.Group></Col>
              <Col xs={6} sm={3} lg={2}><Form.Group><Form.Label>Adults</Form.Label><Form.Control type="number" min="1" value={form.adults} onChange={e => set('adults', e.target.value)} /></Form.Group></Col>
              <Col xs={6} sm={3} lg={2}><Form.Group><Form.Label>Children</Form.Label><Form.Control type="number" min="0" value={form.children} onChange={e => set('children', e.target.value)} /></Form.Group></Col>
              <Col xs={12} sm={6} lg={2} className="d-flex align-items-end"><Button type="submit" className="w-100">Search</Button></Col>
            </Row>
          </Form>
        </Card.Body>
      </Card>

      {loading ? <Loader /> : error ? <div className="alert alert-danger">{error}</div> :
        searched && availableTypes.length === 0 ? <EmptyState message="No room types available for selected dates" /> :
        <Row>
          {availableTypes.map(({ roomTypeId, type: t, count }) => {
            const fits = !t.maxOccupancy || partySize <= t.maxOccupancy
            // Without a rate plan valid for these dates there is no price to book at, and
            // reservation-service now rejects the booking rather than guessing a plan.
            const hasRate = plansByType(roomTypeId).length > 0
            return (
              <Col md={6} lg={4} key={roomTypeId} className="mb-3">
                <Card className="h-100 shadow-sm">
                  <Card.Body>
                    <div className="d-flex justify-content-between">
                      <h5><Badge bg="primary">{t.name}</Badge></h5>
                      <Badge bg={count > 3 ? 'success' : 'warning'}>{count} available</Badge>
                    </div>
                    <div><strong>Base Rate:</strong> ₹{t.baseRate}/night</div>
                    <div><strong>Max Occupancy:</strong> {t.maxOccupancy}</div>
                    <div className="small text-muted mb-3">{t.bedConfiguration}</div>
                    <Button
                      size="sm"
                      className={fits && hasRate ? 'btn-accent' : undefined}
                      disabled={!fits || !hasRate}
                      onClick={() => requestBooking({ type: t, availableCount: count })}
                    >
                      Book Now
                    </Button>
                    {!fits && (
                      <div className="small text-danger mt-2">
                        Sleeps up to {t.maxOccupancy} guest(s) — you selected {partySize}.
                      </div>
                    )}
                    {fits && !hasRate && (
                      <div className="small text-warning mt-2">
                        No rate plan covers these dates yet, so this room can't be booked.
                      </div>
                    )}
                  </Card.Body>
                </Card>
              </Col>
            )
          })}
        </Row>
      }

      <CompleteProfileModal
        show={Boolean(pendingType)}
        onClose={() => setPendingType(null)}
        onCompleted={() => { setSelectedType(pendingType); setPendingType(null) }}
      />

      {selectedType && (
        <BookingConfirmModal
          data={{ ...form, ...selectedType, ratePlans: plansByType(selectedType.type?.roomTypeId) }}
          onClose={() => setSelectedType(null)}
        />
      )}
    </div>
  )
}