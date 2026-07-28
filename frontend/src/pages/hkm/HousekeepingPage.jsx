import { useEffect, useState } from 'react'
import { Row, Col, Card, Button, Badge, Modal, Form, Alert } from 'react-bootstrap'
import { getTasks, updateTaskStatus, createTask } from '../../services/hkm/taskService'
import { getRooms } from '../../services/ric/roomService'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'

const COLUMNS = [
  { key: 'PENDING', label: 'Pending', color: '#0ea5e9' },
  { key: 'INPROGRESS', label: 'In Progress', color: '#f59e0b' },
  { key: 'DONE', label: 'Done', color: '#16a34a' },
]

const TASK_TYPES = ['CHECKOUT', 'STAYOVERSERVICE', 'TURNDOWN', 'DEEPCLEAN']

export default function HousekeepingPage() {
  const [tasks, setTasks] = useState([])
  const [rooms, setRooms] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [show, setShow] = useState(false)
  const [form, setForm] = useState({ roomId: '', taskType: 'STAYOVERSERVICE' })
  const [saveErr, setSaveErr] = useState('')

  const load = async () => {
    setLoading(true); setError('')
    try {
      const [ts, rs] = await Promise.all([getTasks(), getRooms().catch(() => [])])
      setTasks(ts); setRooms(rs || [])
    } catch (e) { setError(e.message) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  const move = async (task, status) => {
    try { await updateTaskStatus(task.taskId, status); load() }
    catch (e) { alert(e.message) }
  }

  const openCreate = () => { setSaveErr(''); setForm({ roomId: '', taskType: 'STAYOVERSERVICE' }); setShow(true) }

  const submit = async (e) => {
    e.preventDefault(); setSaveErr('')
    if (!form.roomId) { setSaveErr('Please select a room'); return }
    try {
      await createTask({ roomId: Number(form.roomId), taskType: form.taskType })
      setShow(false); load()
    } catch (err) { setSaveErr(err.message) }
  }

  const grouped = (status) => tasks.filter(t => t.status === status)

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h4 className="mb-0">Housekeeping Task Board</h4>
        <Button variant="outline-success" size="sm" onClick={openCreate}>+ Quick Task</Button>
      </div>
      {loading ? <Loader /> : error ? <div className="alert alert-danger">{error}</div> :
        <Row>
          {COLUMNS.map(col => (
            <Col md={4} key={col.key}>
              <div className="kanban-col">
                <h6 className="mb-3"><Badge style={{ background: col.color }}>{col.label}</Badge> <span className="text-muted small">({grouped(col.key).length})</span></h6>
                {grouped(col.key).length === 0 ? <p className="text-muted small">No tasks</p> :
                  grouped(col.key).map(t => (
                    <div key={t.taskId} className="task-card">
                      <div className="d-flex justify-content-between">
                        <strong>Task #{t.taskId}</strong>
                        <Badge bg="secondary">{t.taskType}</Badge>
                      </div>
                      <p className="small mb-1 text-muted">Room #{t.roomId}</p>
                      {t.assignedToId && <p className="small mb-2">Assigned to: #{t.assignedToId}</p>}
                      <div className="d-flex gap-1">
                        {col.key === 'PENDING' && <Button size="sm" variant="warning" onClick={() => move(t, 'INPROGRESS')}>Start</Button>}
                        {col.key === 'INPROGRESS' && <Button size="sm" variant="success" onClick={() => move(t, 'DONE')}>Mark Done</Button>}
                        {col.key !== 'DONE' && <Button size="sm" variant="outline-secondary" onClick={() => move(t, 'SKIPPED')}>Skip</Button>}
                      </div>
                    </div>
                  ))
                }
              </div>
            </Col>
          ))}
        </Row>
      }

      <Modal show={show} onHide={() => setShow(false)}>
        <Modal.Header closeButton><Modal.Title>New Housekeeping Task</Modal.Title></Modal.Header>
        <Form onSubmit={submit}>
          <Modal.Body>
            {saveErr && <Alert variant="danger" className="py-2">{saveErr}</Alert>}
            <Form.Group className="mb-3">
              <Form.Label>Room</Form.Label>
              <Form.Select required value={form.roomId} onChange={e => setForm(f => ({ ...f, roomId: e.target.value }))}>
                <option value="">Select a room…</option>
                {rooms.map(r => (
                  <option key={r.roomId} value={r.roomId}>
                    Room #{r.roomNumber ?? r.roomId}{r.status ? ` — ${r.status}` : ''}
                  </option>
                ))}
              </Form.Select>
            </Form.Group>
            <Form.Group>
              <Form.Label>Task Type</Form.Label>
              <Form.Select value={form.taskType} onChange={e => setForm(f => ({ ...f, taskType: e.target.value }))}>
                {TASK_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
              </Form.Select>
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShow(false)}>Cancel</Button>
            <Button type="submit" style={{ background: '#1e3a5f', borderColor: '#1e3a5f' }}>Create Task</Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </div>
  )
}