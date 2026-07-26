import { useEffect, useState } from 'react'
import { Row, Col, Card, Button, Badge } from 'react-bootstrap'
import { getTasks, updateTaskStatus, createTask } from '../../services/hkm/taskService'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'

const COLUMNS = [
  { key: 'PENDING', label: 'Pending', color: '#0ea5e9' },
  { key: 'INPROGRESS', label: 'In Progress', color: '#f59e0b' },
  { key: 'DONE', label: 'Done', color: '#16a34a' },
]

export default function HousekeepingPage() {
  const [tasks, setTasks] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = async () => {
    setLoading(true); setError('')
    try { setTasks(await getTasks()) } catch (e) { setError(e.message) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  const move = async (task, status) => {
    try { await updateTaskStatus(task.taskId, status); load() }
    catch (e) { alert(e.message) }
  }

  const grouped = (status) => tasks.filter(t => t.status === status)

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h4 className="mb-0">Housekeeping Task Board</h4>
        <Button variant="outline-success" size="sm" onClick={() => {
          const roomId = Number(prompt('Room ID?'))
          if (!roomId) return
          createTask({ roomId, taskType: 'STAYOVERSERVICE' }).then(load).catch(e => alert(e.message))
        }}>+ Quick Task</Button>
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
    </div>
  )
}