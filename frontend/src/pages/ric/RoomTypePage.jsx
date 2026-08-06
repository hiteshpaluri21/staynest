import { useEffect, useState } from 'react'
import { Card, Row, Col, Button, Badge } from 'react-bootstrap'
import { FaPlus, FaEdit } from 'react-icons/fa'
import { getRoomTypes } from '../../services/ric/roomTypeService'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'
import RoomTypeFormModal from '../../components/RoomTypeFormModal'
import { statusBadge } from '../../utils/badges'

export default function RoomTypePage() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState(null)

  const load = async () => {
    setLoading(true); setError('')
    try { setItems(await getRoomTypes()) } catch (e) { setError(e.message) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  const openCreate = () => { setEditing(null); setShowModal(true) }
  const openEdit = (rt) => { setEditing(rt); setShowModal(true) }
  const closeModal = () => { setShowModal(false); setEditing(null) }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h4 className="mb-0">Room Types</h4>
        <Button onClick={openCreate}>
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
                  <Button size="sm" variant="outline-secondary" onClick={() => openEdit(rt)}>
                    <FaEdit className="me-1" /> Edit
                  </Button>
                </Card.Body>
              </Card>
            </Col>
          ))}
        </Row>
      }
      <RoomTypeFormModal show={showModal} roomType={editing} onClose={closeModal} onSaved={() => { closeModal(); load() }} />
    </div>
  )
}