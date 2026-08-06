import { useEffect, useState } from 'react'
import { Card, Button, Badge } from 'react-bootstrap'
import { getMyGuestProfile } from '../../services/rbm/guestService'
import { useAuth } from '../../context/AuthContext'
import Loader from '../../components/Loader'
import GuestProfileFormModal from '../../components/GuestProfileFormModal'
import { loyaltyBadge } from '../../utils/badges'
import { missingProfileFields } from '../../utils/validation'

export default function GuestProfilePage() {
  const { user, refreshGuest } = useAuth()
  const [guest, setGuest] = useState(null)
  const [loading, setLoading] = useState(true)
  const [showModal, setShowModal] = useState(false)

  const load = async () => {
    if (!user) {
      setLoading(false)
      return
    }
    setLoading(true)
    try {
      // Resolve by JWT rather than treating user.userId as the guest PK — that lookup missed
      // and made the page claim no profile was attached.
      const data = await getMyGuestProfile()
      setGuest(data)
    } catch {
      setGuest(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [user])

  const missing = missingProfileFields(guest)

  if (loading) return <Loader />

  return (
    <div>
      <h4 className="mb-3">My Profile</h4>
      {guest && missing.length > 0 && (
        <div className="alert alert-warning py-2" style={{ maxWidth: 700 }}>
          Please add your {missing.map(f => f.label.toLowerCase()).join(', ')} — these are required before you can reserve a room.
        </div>
      )}
      <Card className="shadow-sm mb-4" style={{ maxWidth: 700 }}>
        <Card.Body>
          <div className="d-flex justify-content-between align-items-start">
            <div>
              <h5 className="mb-1">
                {user?.name || 'User'}
                {/* {guest?.loyaltyTier ? (
                  <Badge bg={loyaltyBadge(guest.loyaltyTier)} className="ms-2">{guest.loyaltyTier}</Badge>
                ) : (
                  <Badge bg="secondary" className="ms-2">{user?.role || 'STAFF'}</Badge>
                )} */}
              </h5>
              <p className="text-muted small mb-0">{user?.email || guest?.email}</p>
            </div>
            {guest && (
              <Button variant="outline-primary" size="sm" onClick={() => setShowModal(true)}>Edit details</Button>
            )}
          </div>
          <div className="row mt-4">
            <div className="col-md-6 mb-2"><strong>Guest ID:</strong> {guest ? `${guest.guestId}` : '—'}</div>
            <div className="col-md-6 mb-2"><strong>Role:</strong> {user?.role}</div>
            {guest ? (
              <>
                <div className="col-md-6 mb-2"><strong>Phone:</strong> {guest.phone || '—'}</div>
                <div className="col-md-6 mb-2"><strong>Nationality:</strong> {guest.nationality || 'Indian'}</div>
                <div className="col-md-6 mb-2"><strong>ID Type:</strong> {guest.idDocumentType || '—'}</div>
                <div className="col-md-6 mb-2"><strong>ID Number:</strong> {guest.idNumber || '—'}</div>
              </>
            ) : (
              <div className="col-12 mt-2 text-muted small">
                No guest profile attached to this account.
              </div>
            )}
          </div>
        </Card.Body>
      </Card>
      {guest && (
        <GuestProfileFormModal show={showModal} guest={guest} onClose={() => setShowModal(false)} onSaved={() => { setShowModal(false); load(); refreshGuest() }} />
      )}
    </div>
  )
}