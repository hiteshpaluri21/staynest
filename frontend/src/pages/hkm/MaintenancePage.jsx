import { useEffect, useState } from 'react'
import { Table, Button, Badge, Form, Modal, Alert } from 'react-bootstrap'
import { getRequests, reportIssue, resolveRequest } from '../../services/hkm/maintenanceService'
import { useAuth } from '../../context/AuthContext'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'
import { statusBadge } from '../../utils/badges'

const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']

export default function MaintenancePage() {
  const { user } = useAuth()
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [show, setShow] = useState(false)
  const [form, setForm] = useState({ roomId: '', issueDescription: '', priority: 'MEDIUM' })
  const [saveErr, setSaveErr] = useState('')

  const load = async () => {
    setLoading(true); setError('')
    try { setItems(await getRequests()) } catch (e) { setError(e.message) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  const resolve = async (id) => {
    if (!window.confirm('Mark as resolved?')) return
    try { await resolveRequest(id); load() } catch (e) { alert(e.message) }
  }

  const submit = async (e) => {
    e.preventDefault(); setSaveErr('')
    try {
      await reportIssue({ ...form, roomId: Number(form.roomId), reportedBy: user?.userId })
      setShow(false); setForm({ roomId: '', issueDescription: '', priority: 'MEDIUM' }); load()
    } catch (err) { setSaveErr(err.message) }
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h4 className="mb-0">Maintenance Requests</h4>
        <Button style={{ background: '#1e3a5f', borderColor: '#1e3a5f' }} onClick={() => setShow(true)}>+ Report Issue</Button>
      </div>
      {loading ? <Loader /> : error ? <div className="alert alert-danger">{error}</div> :
        items.length === 0 ? <EmptyState message="No maintenance requests" /> :
        <Table hover responsive>
          <thead><tr><th>ID</th><th>Room</th><th>Issue</th><th>Priority</th><th>Raised</th><th>Status</th><th>Action</th></tr></thead>
          <tbody>
            {items.map(m => (
              <tr key={m.requestId}>
                <td>{m.requestId}</td>
                <td>#{m.roomId}</td>
                <td>{m.issueDescription}</td>
                <td><Badge bg={m.priority === 'URGENT' ? 'danger' : m.priority === 'HIGH' ? 'warning' : 'secondary'}>{m.priority}</Badge></td>
                <td className="small">{m.raisedDate}</td>
                <td><Badge bg={statusBadge(m.status)}>{m.status}</Badge></td>
                <td>
                  {m.status !== 'RESOLVED' && <Button size="sm" variant="outline-success" onClick={() => resolve(m.requestId)}>Resolve</Button>}
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      }

      <Modal show={show} onHide={() => setShow(false)}>
        <Modal.Header closeButton><Modal.Title>Report Maintenance Issue</Modal.Title></Modal.Header>
        <Form onSubmit={submit}>
          <Modal.Body>
            {saveErr && <Alert variant="danger" className="py-2">{saveErr}</Alert>}
            <Form.Group className="mb-3"><Form.Label>Room ID</Form.Label><Form.Control type="number" required value={form.roomId} onChange={e => setForm(f => ({ ...f, roomId: e.target.value }))} /></Form.Group>
            <Form.Group className="mb-3"><Form.Label>Issue Description</Form.Label><Form.Control as="textarea" rows={3} required value={form.issueDescription} onChange={e => setForm(f => ({ ...f, issueDescription: e.target.value }))} /></Form.Group>
            <Form.Group><Form.Label>Priority</Form.Label><Form.Select value={form.priority} onChange={e => setForm(f => ({ ...f, priority: e.target.value }))}>{PRIORITIES.map(p => <option key={p}>{p}</option>)}</Form.Select></Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShow(false)}>Cancel</Button>
            <Button type="submit" style={{ background: '#1e3a5f', borderColor: '#1e3a5f' }}>Report</Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </div>
  )
}