import { useEffect, useState } from 'react'
import { Card, Row, Col, Button, Badge, Table } from 'react-bootstrap'
import { getUpcoming } from '../../services/rbm/reservationService'
import { getStays } from '../../services/fds/stayService'
import { getRooms } from '../../services/ric/roomService'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'
import RoomAssignModal from '../../components/RoomAssignModal'
import { Link } from 'react-router-dom'
import { statusBadge } from '../../utils/badges'

export default function FrontDeskPage() {
  const [arrivals, setArrivals] = useState([])
  const [stays, setStays] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selected, setSelected] = useState(null)

  const [allStays, setAllStays] = useState([])
  // Stays carry only assignedRoomId (a PK), so load the rooms to show the real room number.
  const [roomNumbers, setRoomNumbers] = useState({})

  const load = async () => {
    setLoading(true); setError('')
    try {
      const [up, st, fullSt, rooms] = await Promise.all([
        getUpcoming().catch(() => []),
        getStays({ status: 'ACTIVE' }).catch(() => []),
        getStays().catch(() => []),
        getRooms().catch(() => []),
      ])
      setArrivals(up); setStays(st); setAllStays(fullSt)
      setRoomNumbers(Object.fromEntries((rooms || []).map(r => [r.roomId, r.roomNumber])))
    } catch (e) { setError(e.message) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  const roomLabel = (roomId) => roomNumbers[roomId] ?? (roomId != null ? `id ${roomId}` : '—')

  const activeResIds = new Set(stays.map(s => s.reservationId))
  const checkedOutResIds = new Set(allStays.filter(s => s.status === 'CHECKEDOUT').map(s => s.reservationId))
  const filteredArrivals = arrivals.filter(r => r.status !== 'CHECKEDOUT' && !checkedOutResIds.has(r.reservationId))

  return (
    <div>
      <h4 className="mb-4">Front Desk Console</h4>
      {loading ? <Loader /> : error ? <div className="alert alert-danger">{error}</div> :
        <Row>
          <Col md={7}>
            <Card className="shadow-sm mb-4">
              <Card.Header><strong>Today's Arrivals</strong></Card.Header>
              <Card.Body>
                {filteredArrivals.length === 0 ? <EmptyState message="No upcoming arrivals" /> :
                  <Table hover responsive>
                    <thead><tr><th>Res ID</th><th>Guest</th><th>Check-In</th><th>Nights</th><th>Status / Action</th></tr></thead>
                    <tbody>
                      {filteredArrivals.map(r => {
                        const isCheckedIn = activeResIds.has(r.reservationId) || r.status === 'CHECKEDIN'
                        return (
                          <tr key={r.reservationId}>
                            <td>{r.reservationId}</td>
                            <td>{r.guestName}</td>
                            <td>{r.checkInDate}</td>
                            <td>{r.nights}</td>
                            <td>
                              {isCheckedIn ? (
                                <Badge bg="success">CHECKED IN</Badge>
                              ) : r.status === 'CONFIRMED' ? (
                                <Button size="sm" variant="success" onClick={() => setSelected(r)}>Check-In</Button>
                              ) : (
                                <Badge bg={statusBadge(r.status)}>{r.status}</Badge>
                              )}
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </Table>
                }
              </Card.Body>
            </Card>
          </Col>
          <Col md={5}>
            <Card className="shadow-sm">
              <Card.Header><strong>Active Stays</strong></Card.Header>
              <Card.Body>
                {stays.length === 0 ? <EmptyState message="No active stays" /> :
                  <Table hover responsive size="sm">
                    <thead><tr><th>Stay</th><th>Room</th><th>Balance</th><th></th></tr></thead>
                    <tbody>
                      {stays.map(s => (
                        <tr key={s.stayId}>
                          <td>{s.stayId}</td>
                          <td>{roomLabel(s.assignedRoomId)}</td>
                          <td>₹{s.folioBalance}</td>
                          <td><Button as={Link} to={`/stays/${s.stayId}`} size="sm" variant="outline-primary">Folio</Button></td>
                        </tr>
                      ))}
                    </tbody>
                  </Table>
                }
              </Card.Body>
            </Card>
          </Col>
        </Row>
      }
      {selected && <RoomAssignModal reservation={selected} onClose={() => setSelected(null)} onDone={() => { setSelected(null); load() }} />}
    </div>
  )
}