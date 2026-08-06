import { useEffect, useState } from 'react'
import { Table, Button, Badge, Form, InputGroup, Card, Alert } from 'react-bootstrap'
import { FaSearch, FaUserPlus } from 'react-icons/fa'
import { getUsers, createUser, updateUserStatus } from '../../services/iam/userService'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'
import UserFormModal from '../../components/UserFormModal'
import { roleBadge, statusBadge } from '../../utils/badges'
import { useAuth } from '../../context/AuthContext'

export default function UserListPage() {
  const { user: currentUser } = useAuth()
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [query, setQuery] = useState('')
  const [showModal, setShowModal] = useState(false)
  // Failures from row actions; `error` is not reused, as it replaces the whole table.
  const [actionErr, setActionErr] = useState('')

  const load = async () => {
    setLoading(true);
    setError('')
    try { setUsers(await getUsers()) }
    catch (e) { setError(e.message) }
    finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  const toggleStatus = async (u) => {
    const next = u.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    setActionErr('')
    if (next === 'INACTIVE' && u.userId === currentUser?.userId) {
      setActionErr('You cannot deactivate your own account.')
      return
    }
    try { await updateUserStatus(u.userId, next); load() }
    catch (e) { setActionErr(e.message) }
  }

  const filtered = users.filter(u =>
    !query ||
    u.name?.toLowerCase().includes(query.toLowerCase()) ||
    u.email?.toLowerCase().includes(query.toLowerCase())
  )

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h4 className="mb-0">User Management</h4>
        <Button onClick={() => setShowModal(true)}>
          <FaUserPlus className="me-2" /> Add User
        </Button>
      </div>

      <Card className="shadow-sm">
        <Card.Body>
          <InputGroup className="mb-3" style={{ maxWidth: 360 }}>
            <InputGroup.Text><FaSearch /></InputGroup.Text>
            <Form.Control placeholder="Search by name or email" value={query} onChange={e => setQuery(e.target.value)} />
          </InputGroup>

          {actionErr && <Alert variant="danger" dismissible onClose={() => setActionErr('')} className="py-2">{actionErr}</Alert>}
          {loading ? <Loader /> :
            error ? <div className="alert alert-danger">{error}</div> :
              filtered.length === 0 ? <EmptyState /> :
                <Table hover responsive>
                  <thead>
                    <tr>
                      <th>ID</th><th>Name</th><th>Role</th><th>Email</th><th>Phone</th><th>Status</th><th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filtered.map(u => (
                      <tr key={u.userId}>
                        <td>{u.userId}</td>
                        <td>{u.name}</td>
                        <td><Badge bg={roleBadge(u.role)}>{u.role}</Badge></td>
                        <td>{u.email}</td>
                        <td>{u.phone || '—'}</td>
                        <td><Badge bg={statusBadge(u.status)}>{u.status}</Badge></td>
                        <td>
                          <Button
                            size="sm"
                            variant={u.status === 'ACTIVE' ? 'outline-danger' : 'outline-success'}
                            onClick={() => toggleStatus(u)}
                            disabled={u.status === 'ACTIVE' && u.userId === currentUser?.userId}
                            title={u.status === 'ACTIVE' && u.userId === currentUser?.userId ? 'You cannot deactivate your own account' : ''}
                          >
                            {u.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
          }
        </Card.Body>
      </Card>

      <UserFormModal show={showModal} onClose={() => setShowModal(false)} onSaved={() => { setShowModal(false); load() }} />
    </div>
  )
}