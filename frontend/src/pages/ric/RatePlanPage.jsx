import { useEffect, useState } from 'react'
import { Table, Form, Button, Badge } from 'react-bootstrap'
import { getRatePlans, updateRatePlanStatus } from '../../services/ric/ratePlanService'
import { getRoomTypes } from '../../services/ric/roomTypeService'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'
import RatePlanFormModal from '../../components/RatePlanFormModal'
import { statusBadge } from '../../utils/badges'
import { useAuth } from '../../context/AuthContext'

export default function RatePlanPage() {
  const { user } = useAuth()
  const canManage = user?.role !== 'GUEST'
  const [plans, setPlans] = useState([])
  const [types, setTypes] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [filterType, setFilterType] = useState('')

  const load = async () => {
    setLoading(true); setError('')
    try {
      const [ps, ts] = await Promise.all([getRatePlans(), getRoomTypes()])
      setPlans(ps); setTypes(ts)
    } catch (e) { setError(e.message) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  // The backend only filters rate plans when both roomTypeId AND date are supplied, so filter here.
  const shownPlans = plans.filter(p => !filterType || String(p.roomTypeId) === String(filterType))

  const toggle = async (p) => {
    const next = p.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    try { await updateRatePlanStatus(p.ratePlanId, next); load() } catch (e) { alert(e.message) }
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h4 className="mb-0">Rate Plans</h4>
        {canManage && <Button style={{ background: '#1e3a5f', borderColor: '#1e3a5f' }} onClick={() => setShowModal(true)}>+ Add Rate Plan</Button>}
      </div>
      <Form.Select style={{ maxWidth: 240 }} className="mb-3" value={filterType} onChange={e => setFilterType(e.target.value)}>
        <option value="">All Room Types</option>
        {types.map(t => <option key={t.roomTypeId} value={t.roomTypeId}>{t.name}</option>)}
      </Form.Select>
      {loading ? <Loader /> : error ? <div className="alert alert-danger">{error}</div> :
        shownPlans.length === 0 ? <EmptyState /> :
        <Table hover responsive>
          <thead><tr><th>ID</th><th>Room Type</th><th>Name</th><th>Price/Night</th><th>Valid</th><th>Meal Plan</th><th>Status</th>{canManage && <th>Action</th>}</tr></thead>
          <tbody>
            {shownPlans.map(p => (
              <tr key={p.ratePlanId}>
                <td>{p.ratePlanId}</td>
                <td>{p.roomTypeName}</td>
                <td><Badge bg="info">{p.name}</Badge></td>
                <td>₹{p.pricePerNight}</td>
                <td className="small">{p.validFrom} → {p.validTo}</td>
                <td>{p.mealPlanIncluded ? '✓' : '—'}</td>
                <td><Badge bg={statusBadge(p.status)}>{p.status}</Badge></td>
                {canManage && <td><Button size="sm" variant="outline-secondary" onClick={() => toggle(p)}>{p.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}</Button></td>}
              </tr>
            ))}
          </tbody>
        </Table>
      }
      <RatePlanFormModal show={showModal} onClose={() => setShowModal(false)} onSaved={() => { setShowModal(false); load() }} />
    </div>
  )
}