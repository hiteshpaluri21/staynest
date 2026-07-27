import { useEffect, useState } from 'react'
import { Card, Row, Col, Form, Button, Badge, InputGroup } from 'react-bootstrap'
import { getAvailableRooms } from '../../services/ric/roomService'
import { getRoomTypes } from '../../services/ric/roomTypeService'
import { getRatePlans } from '../../services/ric/ratePlanService'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'
import BookingConfirmModal from '../../components/BookingConfirmModal'

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
  const [selectedRoom, setSelectedRoom] = useState(null)
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
  const plansByType = (id) => plans.filter(p => p.roomTypeId === id)

  return (
    <div>
      <h4 className="mb-3">Search Availability</h4>
      <Card className="shadow-sm mb-4">
        <Card.Body>
          <Form onSubmit={search}>
            <Row>
              <Col md={3}><Form.Group><Form.Label>Check-In</Form.Label><Form.Control type="date" min={todayStr} required value={form.checkIn} onChange={handleCheckInChange} /></Form.Group></Col>
              <Col md={3}><Form.Group><Form.Label>Check-Out</Form.Label><Form.Control type="date" min={addDays(form.checkIn, 1)} required value={form.checkOut} onChange={e => set('checkOut', e.target.value)} /></Form.Group></Col>
              <Col md={2}><Form.Group><Form.Label>Adults</Form.Label><Form.Control type="number" min="1" value={form.adults} onChange={e => set('adults', e.target.value)} /></Form.Group></Col>
              <Col md={2}><Form.Group><Form.Label>Children</Form.Label><Form.Control type="number" min="0" value={form.children} onChange={e => set('children', e.target.value)} /></Form.Group></Col>
              <Col md={2} className="d-flex align-items-end"><Button type="submit" className="w-100" style={{ background: '#1e3a5f', borderColor: '#1e3a5f' }}>Search</Button></Col>
            </Row>
          </Form>
        </Card.Body>
      </Card>

      {loading ? <Loader /> : error ? <div className="alert alert-danger">{error}</div> :
        searched && rooms.length === 0 ? <EmptyState message="No rooms available for selected dates" /> :
        <Row>
          {rooms.map(r => {
            const t = typeMap[r.roomTypeId]
            return (
              <Col md={6} lg={4} key={r.roomId} className="mb-3">
                <Card className="h-100 shadow-sm">
                  <Card.Body>
                    <div className="d-flex justify-content-between">
                      <h5><Badge bg="primary">{t?.name || 'Room'}</Badge></h5>
                      <Badge className={`badge-room-${r.status}`}>{r.status}</Badge>
                    </div>
                    <p className="text-muted small">Room #{r.roomNumber} · Floor {r.floor}</p>
                    {t && <>
                      <div><strong>Base Rate:</strong> ₹{t.baseRate}/night</div>
                      <div><strong>Max Occupancy:</strong> {t.maxOccupancy}</div>
                      <div className="small text-muted mb-3">{t.bedConfiguration}</div>
                    </>}
                    <Button size="sm" style={{ background: '#f59e0b', borderColor: '#f59e0b' }} onClick={() => setSelectedRoom({ room: r, type: t })}>
                      Book Now
                    </Button>
                  </Card.Body>
                </Card>
              </Col>
            )
          })}
        </Row>
      }

      {selectedRoom && (
        <BookingConfirmModal
          data={{ ...form, ...selectedRoom, ratePlans: plansByType(selectedRoom.type?.roomTypeId) }}
          onClose={() => setSelectedRoom(null)}
        />
      )}
    </div>
  )
}