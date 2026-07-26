import { useEffect, useState } from 'react'
import { Table, Button, Badge } from 'react-bootstrap'
import { getReservations, cancelReservation } from '../../services/rbm/reservationService'
import { useAuth } from '../../context/AuthContext'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'
import { statusBadge } from '../../utils/badges'

export default function MyReservationsPage() {
  const { user } = useAuth()
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = async () => {
    setLoading(true); setError('')
    try { setItems(await getReservations({ guestId: user?.userId })) }
    catch (e) { setError(e.message) } finally { setLoading(false) }
  }
  useEffect(() => { if (user) load() }, [user])

  const cancel = async (id) => {
    if (!window.confirm('Cancel this reservation?')) return
    try { await cancelReservation(id); load() }
    catch (e) { alert(e.message) }
  }

  return (
    <div>
      <h4 className="mb-3">My Reservations</h4>
      {loading ? <Loader /> : error ? <div className="alert alert-danger">{error}</div> :
        items.length === 0 ? <EmptyState message="You have no reservations yet" /> :
        <Table hover responsive>
          <thead><tr><th>ID</th><th>RoomTypeId</th><th>Check-In</th><th>Check-Out</th><th>Nights</th><th>Guests</th><th>Total</th><th>Status</th><th>Action</th></tr></thead>
          <tbody>
            {items.map(r => (
              <tr key={r.reservationId}>
                <td>{r.reservationId}</td>
                <td>{r.roomTypeId}</td>
                <td>{r.checkInDate}</td>
                <td>{r.checkOutDate}</td>
                <td>{r.nights}</td>
                <td>{r.adults}A {r.children}C</td>
                <td>₹{r.totalAmount}</td>
                <td><Badge bg={statusBadge(r.status)}>{r.status}</Badge></td>
                <td>
                  {r.status === 'CONFIRMED' && (
                    <Button size="sm" variant="outline-danger" onClick={() => cancel(r.reservationId)}>Cancel</Button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      }
    </div>
  )
}