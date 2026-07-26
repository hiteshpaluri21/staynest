import { useEffect, useState } from 'react'
import { Card, Row, Col, Button, Badge } from 'react-bootstrap'
import { FaPlus, FaEdit } from 'react-icons/fa'
import { getRoomTypes, updateRoomTypeStatus } from '../../services/ric/roomTypeService'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'
import RoomTypeFormModal from '../../components/RoomTypeFormModal'
import { statusBadge } from '../../utils/badges'

export default function RoomTypePage() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showModal, setShowModal] = useState(false)

  const load = async () => {
    setLoading(true); setError('')
    try { setItems(await getRoomTypes()) } catch (e) { setError(e.message) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  const toggle = async (rt) => {
    const next = rt.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    try { await updateRoomTypeStatus(rt.roomTypeId, next); load() } catch (e) { alert(e.message) }
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h4 className="mb-0">Room Types</h4>
        <Button style={{ background: '#1e3a5f', borderColor: '#1e3a5f' }} onClick={() => setShowModal(true)}>
          <FaPlus className="me-2" /> Add Room Type
        </Button>
      </div>
      {loading ? <Loader /> : error ? <div className="alert alert-danger">{error}</div> :
        items.length === 0 ? <EmptyState /> :
        <Row>
          {items.map(rt => (
            <Col md={6} key={rt.roomTypeId} className="mb-3">
              <Card className="h-100 shadow-sm">
                <Card.Body>
                  <div className="d-flex justify-content-between">
                    <h5><Badge bg="primary">{rt.name}</Badge></h5>
                    <Badge bg={statusBadge(rt.status)}>{rt.status}</Badge>
                  </div>
                  <p className="text-muted small mb-2">{rt.bedConfiguration}</p>
                  <div className="mb-1"><strong>Max Occupancy:</strong> {rt.maxOccupancy}</div>
                  <div className="mb-1"><strong>Base Rate:</strong> ₹{rt.baseRate}</div>
                  <div className="mb-3"><strong>Amenities:</strong> {rt.amenitiesList || '—'}</div>
                  <Button size="sm" variant="outline-secondary" onClick={() => toggle(rt)}>
                    <FaEdit className="me-1" /> {rt.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                  </Button>
                </Card.Body>
              </Card>
            </Col>
          ))}
        </Row>
      }
      <RoomTypeFormModal show={showModal} onClose={() => setShowModal(false)} onSaved={() => { setShowModal(false); load() }} />
    </div>
  )
}