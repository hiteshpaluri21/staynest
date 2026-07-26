import { useEffect, useState } from 'react'
import { Card, Button, Badge } from 'react-bootstrap'
import { getGuestById } from '../../services/rbm/guestService'
import { useAuth } from '../../context/AuthContext'
import Loader from '../../components/Loader'
import GuestProfileFormModal from '../../components/GuestProfileFormModal'
import { loyaltyBadge } from '../../utils/badges'

export default function GuestProfilePage() {
  const { user } = useAuth()
  const [guest, setGuest] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showModal, setShowModal] = useState(false)

  const load = async () => {
    if (!user?.userId) return
    setLoading(true); setError('')
    try { setGuest(await getGuestById(user.userId)) }
    catch (e) { setError(e.message) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [user])

  if (loading) return <Loader />
  if (error) return <div className="alert alert-danger">{error}</div>
  if (!guest) return null

  return (
    <div>
      <h4 className="mb-3">My Profile</h4>
      <Card className="shadow-sm" style={{ maxWidth: 700 }}>
        <Card.Body>
          <div className="d-flex justify-content-between align-items-start">
            <div>
              <h5 className="mb-1">
                {guest.name} <Badge bg={loyaltyBadge(guest.loyaltyTier)}>{guest.loyaltyTier}</Badge>
              </h5>
              <p className="text-muted small mb-0">{guest.email}</p>
            </div>
            <Button variant="outline-primary" size="sm" onClick={() => setShowModal(true)}>Edit Profile</Button>
          </div>
          <div className="row mt-4">
            <div className="col-md-6 mb-2"><strong>Phone:</strong> {guest.phone || '—'}</div>
            <div className="col-md-6 mb-2"><strong>Nationality:</strong> {guest.nationality || '—'}</div>
            <div className="col-md-6 mb-2"><strong>ID Type:</strong> {guest.idDocumentType || '—'}</div>
            <div className="col-md-6 mb-2"><strong>ID Number:</strong> {guest.idNumber || '—'}</div>
            <div className="col-12"><strong>Preferences:</strong> <span className="text-muted">{guest.preferencesJson || '—'}</span></div>
          </div>
        </Card.Body>
      </Card>
      <GuestProfileFormModal show={showModal} guest={guest} onClose={() => setShowModal(false)} onSaved={() => { setShowModal(false); load() }} />
    </div>
  )
}