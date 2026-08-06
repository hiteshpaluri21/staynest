import { useEffect, useState } from 'react'
import { Card, Table, Button, Badge, Form, Alert } from 'react-bootstrap'
import { getByUser, markAsRead, markAllAsRead } from '../../services/nal/notificationService'
import { useAuth } from '../../context/AuthContext'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'
import { statusBadge } from '../../utils/badges'

export default function NotificationsPage() {
  const { user } = useAuth()
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [catFilter, setCatFilter] = useState('')
  // Failures from mark-as-read; `error` is not reused, as it replaces the whole list.
  const [actionErr, setActionErr] = useState('')

  const load = async () => {
    if (!user?.userId) return
    setLoading(true); setError('')
    try { setItems(await getByUser(user.userId)) }
    catch (e) { setError(e.message) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [user])

  const markOne = async (id) => { setActionErr(''); try { await markAsRead(id); load() } catch (e) { setActionErr(e.message) } }
  const markAll = async () => { setActionErr(''); try { await markAllAsRead(user.userId); load() } catch (e) { setActionErr(e.message) } }

  const filtered = catFilter ? items.filter(n => n.category === catFilter) : items
  const unread = items.filter(n => n.status === 'UNREAD').length

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h4 className="mb-0">Notifications {unread > 0 && <Badge bg="danger">{unread} unread</Badge>}</h4>
        <Button variant="outline-primary" size="sm" onClick={markAll} disabled={unread === 0}>Mark all as read</Button>
      </div>
      <Form.Select style={{ maxWidth: 240 }} className="mb-3" value={catFilter} onChange={e => setCatFilter(e.target.value)}>
        <option value="">All Categories</option>
        {['RESERVATION', 'FRONTDESK', 'HOUSEKEEPING', 'FB', 'REVENUE'].map(c => <option key={c}>{c}</option>)}
      </Form.Select>
      <Card className="shadow-sm">
        <Card.Body>
          {actionErr && <Alert variant="danger" dismissible onClose={() => setActionErr('')} className="py-2">{actionErr}</Alert>}
          {loading ? <Loader /> : error ? <div className="alert alert-danger">{error}</div> :
            filtered.length === 0 ? <EmptyState message="No notifications" /> :
            <Table hover responsive>
              <thead><tr><th>Category</th><th>Message</th><th>Status</th><th>Created</th><th>Action</th></tr></thead>
              <tbody>
                {filtered.map(n => (
                  <tr key={n.notificationId} className={n.status === 'UNREAD' ? 'fw-semibold' : ''}>
                    <td><Badge bg="info">{n.category}</Badge></td>
                    <td>{n.message}</td>
                    <td><Badge bg={statusBadge(n.status)}>{n.status}</Badge></td>
                    <td className="small">{n.createdDate}</td>
                    <td>{n.status === 'UNREAD' && <Button size="sm" variant="outline-success" onClick={() => markOne(n.notificationId)}>Mark read</Button>}</td>
                  </tr>
                ))}
              </tbody>
            </Table>
          }
        </Card.Body>
      </Card>
    </div>
  )
}