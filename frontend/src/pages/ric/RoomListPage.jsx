import { useEffect, useState } from 'react'
import { Table, Form, Button, Badge } from 'react-bootstrap'
import { getRooms, updateRoomStatus } from '../../services/ric/roomService'
import { getRoomTypes } from '../../services/ric/roomTypeService'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'
import RoomFormModal from '../../components/RoomFormModal'

const STATUSES = ['AVAILABLE', 'OCCUPIED', 'CLEANING', 'MAINTENANCE', 'BLOCKED']

export default function RoomListPage() {
  const [rooms, setRooms] = useState([])
  const [types, setTypes] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [filter, setFilter] = useState({ status: '', roomTypeId: '' })

  const load = async () => {
    setLoading(true); setError('')
    try {
      // Backend honors only one filter param at a time, so fetch by status and filter type client-side.
      const [rs, ts] = await Promise.all([getRooms(filter.status ? { status: filter.status } : {}), getRoomTypes()])
      setRooms(rs)
      setTypes(ts)
    } catch (e) { setError(e.message) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [filter.status])

  const shownRooms = rooms.filter(r => !filter.roomTypeId || String(r.roomTypeId) === String(filter.roomTypeId))

  const changeStatus = async (room, status) => {
    try { await updateRoomStatus(room.roomId, status); load() } catch (e) { alert(e.message) }
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h4 className="mb-0">Rooms</h4>
        <Button style={{ background: '#1e3a5f', borderColor: '#1e3a5f' }} onClick={() => setShowModal(true)}>+ Add Room</Button>
      </div>
      <div className="d-flex gap-2 mb-3">
        <Form.Select style={{ maxWidth: 200 }} value={filter.status} onChange={e => setFilter(f => ({ ...f, status: e.target.value }))}>
          <option value="">All Statuses</option>
          {STATUSES.map(s => <option key={s}>{s}</option>)}
        </Form.Select>
        <Form.Select style={{ maxWidth: 200 }} value={filter.roomTypeId} onChange={e => setFilter(f => ({ ...f, roomTypeId: e.target.value }))}>
          <option value="">All Types</option>
          {types.map(t => <option key={t.roomTypeId} value={t.roomTypeId}>{t.name} — {t.amenitiesList || 'no amenities'}</option>)}
        </Form.Select>
      </div>
      {loading ? <Loader /> : error ? <div className="alert alert-danger">{error}</div> :
        shownRooms.length === 0 ? <EmptyState /> :
          <Table hover responsive>
            <thead>
              <tr>
                {/* <th>ID</th> */}
                <th>Room No</th>
                <th>Floor</th>
                <th>Type</th>
                <th>Status</th>
                <th>Change Status</th>
              </tr>
            </thead>
            <tbody>
              {shownRooms.map(r => (
                <tr key={r.roomId}>
                  {/* <td>{r.roomId}</td> */}
                  <td><strong>{r.roomNumber}</strong></td>
                  <td>{r.floor}</td>
                  <td>{r.roomTypeName}</td>
                  <td><Badge className={`badge-room-${r.status}`}>{r.status}</Badge></td>
                  <td>
                    <Form.Select size="sm" style={{ maxWidth: 180 }} value={r.status} onChange={e => changeStatus(r, e.target.value)}>
                      {STATUSES.map(s => <option key={s}>{s}</option>)}
                    </Form.Select>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
      }
      <RoomFormModal 
        show={showModal} 
        onClose={() => setShowModal(false)} 
        onSaved={() => { setShowModal(false) 
        load() }} />
    </div>
  )
}