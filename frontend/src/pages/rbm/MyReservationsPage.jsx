import { useEffect, useState } from 'react'
import { Table, Button, Badge } from 'react-bootstrap'
import { getReservations, cancelReservation } from '../../services/rbm/reservationService'
import { getStays } from '../../services/fds/stayService'
import { getRoomNumbers } from '../../services/ric/roomService'
import { useAuth } from '../../context/AuthContext'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'
import ConfirmModal from '../../components/ConfirmModal'
import { statusBadge } from '../../utils/badges'

export default function MyReservationsPage() {
  const { user, guestId } = useAuth()
  const [items, setItems] = useState([])
  const [stays, setStays] = useState([])
  // Stays only carry assignedRoomId (a PK), so resolve it to the room number guests recognise;
  // getRoomNumbers falls back to a per-room lookup rather than printing the raw id.
  const [roomNumbers, setRoomNumbers] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  // The reservation awaiting cancel confirmation, or null when the modal is closed.
  const [pendingCancel, setPendingCancel] = useState(null)

  const isStaff = user?.role === 'ADMIN' || user?.role === 'FRONTDESK'

  const load = async () => {
    setLoading(true); setError('')
    try {
      // Filter on the reservation-service guestId, never user.userId — they are different keys,
      // and querying by userId returned an empty list even though the booking existed.
      const [resList, stayList] = await Promise.all([
        getReservations(isStaff ? {} : { guestId }),
        getStays(isStaff ? {} : { guestId }).catch(() => []),
      ])
      setItems(resList)
      setStays(stayList)
      setRoomNumbers(await getRoomNumbers((stayList || []).map(s => s.assignedRoomId)))
    } catch (e) { setError(e.message) } finally { setLoading(false) }
  }

  // Wait for the guest profile to resolve before querying, otherwise the first load would run
  // with guestId undefined and fetch every reservation in the system.
  useEffect(() => {
    if (!user) return
    if (isStaff || guestId) load()
  }, [user, guestId, isStaff])

  const stayMap = Object.fromEntries(stays.map(s => [s.reservationId, s]))

  // Confirmed in-app via ConfirmModal, which shows any failure inline and reloads on success.
  const confirmCancel = async () => {
    await cancelReservation(pendingCancel.reservationId)
    await load()
  }

  return (
    <div>
      <h4 className="mb-3">My Reservations</h4>
      {loading ? <Loader /> : error ? <div className="alert alert-danger">{error}</div> :
        items.length === 0 ? <EmptyState message="You have no reservations yet" /> :
        <Table hover responsive className="align-middle">
          <thead>
            <tr>
              <th>Booking ID</th>
              <th>Stay Info</th>
              <th>Check-In</th>
              <th>Check-Out</th>
              <th>Nights</th>
              <th>Guests</th>
              <th>Total</th>
              <th>Status</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {items.map(r => {
              const stay = stayMap[r.reservationId]
              return (
                <tr key={r.reservationId}>
                  <td><strong>{r.reservationId}</strong></td>
                  <td>
                    {stay ? (
                      <div>
                        <Badge bg="dark" className="me-1">Stay {stay.stayId}</Badge>
                        {/* Room number, not the id — the id means nothing to a guest. It only
                            appears if room-service could not be reached at all. */}
                        <Badge bg="secondary">
                          {roomNumbers[stay.assignedRoomId] != null
                            ? `Room ${roomNumbers[stay.assignedRoomId]}`
                            : 'Room not available'}
                        </Badge>
                      </div>
                    ) : (
                      <span className="text-muted small">Not Checked In</span>
                    )}
                  </td>
                  <td>{r.checkInDate}</td>
                  <td>{r.checkOutDate}</td>
                  <td>{r.nights}</td>
                  <td>{r.adults}A {r.children}C</td>
                  <td>₹{r.totalAmount}</td>
                  <td><Badge bg={statusBadge(r.status)}>{r.status}</Badge></td>
                  <td>
                    {r.status === 'CONFIRMED' && (
                      <Button size="sm" variant="outline-danger" onClick={() => setPendingCancel(r)}>Cancel</Button>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </Table>
      }

      <ConfirmModal
        show={pendingCancel != null}
        title="Cancel reservation"
        body={pendingCancel && (
          <p className="mb-0">
            Cancel booking <strong>{pendingCancel.reservationId}</strong> for{' '}
            {pendingCancel.checkInDate} → {pendingCancel.checkOutDate}? This cannot be undone.
          </p>
        )}
        confirmLabel="Cancel reservation"
        onClose={() => setPendingCancel(null)}
        onConfirm={confirmCancel}
      />
    </div>
  )
}