import { useEffect, useState } from 'react'
import { Modal, Button, Alert, Form, Table, Badge } from 'react-bootstrap'
import { checkOut } from '../services/fds/stayService'
import { getUsersByRole } from '../services/iam/userService'

/**
 * Replaces the browser confirm()/alert() pair that used to drive check-out — those render as
 * "localhost says…" and can't show the folio or take an input.
 *
 * Check-out also raises the post-checkout cleaning task, so the assignee is chosen here: a task
 * must never reach the board unassigned.
 */
export default function CheckoutModal({ show, stay, roomLabel, folio = [], onClose, onDone }) {
  const [staff, setStaff] = useState([])
  const [staffId, setStaffId] = useState('')
  const [error, setError] = useState('')
  const [loadingStaff, setLoadingStaff] = useState(false)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!show) return
    setError(''); setStaffId(''); setLoadingStaff(true)
    getUsersByRole('HOUSEKEEPING')
      .then(list => setStaff(list || []))
      .catch(() => setStaff([]))
      .finally(() => setLoadingStaff(false))
  }, [show])

  const noStaff = !loadingStaff && staff.length === 0

  const submit = async (e) => {
    e.preventDefault(); setSaving(true); setError('')
    try {
      await checkOut(stay.stayId, staffId ? Number(staffId) : undefined)
      onDone()
    } catch (err) { setError(err.message) } finally { setSaving(false) }
  }

  if (!stay) return null

  return (
    <Modal show={show} onHide={onClose}>
      <Modal.Header closeButton><Modal.Title>Check Out — Stay {stay.stayId}</Modal.Title></Modal.Header>
      <Form onSubmit={submit}>
        <Modal.Body>
          {error && <Alert variant="danger" className="py-2">{error}</Alert>}
          <p className="small text-muted mb-2">
            {roomLabel ? `${roomLabel} · ` : ''}Reservation {stay.reservationId}
          </p>

          {folio.length > 0 && (
            <Table responsive size="sm" className="mb-2">
              <tbody>
                {folio.map(it => (
                  <tr key={it.folioItemId}>
                    <td><Badge bg="secondary">{it.chargeType}</Badge></td>
                    <td className="small">{it.description}</td>
                    <td className="text-end">₹{it.amount}</td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
          <div className="d-flex justify-content-between border-top pt-2 mb-3">
            <strong>Folio total</strong>
            <strong className="text-primary">₹{stay.folioBalance}</strong>
          </div>

          <Form.Group>
            <Form.Label>Assign cleaning to</Form.Label>
            <Form.Select
              required={!noStaff}
              disabled={loadingStaff || noStaff}
              value={staffId}
              onChange={e => setStaffId(e.target.value)}
            >
              <option value="">{loadingStaff ? 'Loading staff…' : '— Select housekeeping staff —'}</option>
              {staff.map(s => <option key={s.userId} value={s.userId}>{s.name}</option>)}
            </Form.Select>
            {noStaff ? (
              <Alert variant="warning" className="py-2 mt-2 mb-0 small">
                No housekeeping staff are available, so no cleaning task will be raised. Check out now
                and add the task from the Housekeeping board once staff exist.
              </Alert>
            ) : (
              <Form.Text className="text-muted">
                A CHECKOUT cleaning task for this room goes straight to them.
              </Form.Text>
            )}
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button type="submit" variant="danger" disabled={saving || (!noStaff && !staffId)}>
            {saving ? 'Checking out…' : 'Confirm Check-Out'}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  )
}
