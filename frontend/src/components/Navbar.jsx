import { Navbar as BSNavbar, Nav, Dropdown } from 'react-bootstrap'
import { FaBell, FaUserCircle, FaSignOutAlt } from 'react-icons/fa'
import { Link, useNavigate } from 'react-router-dom'
import NotificationBell from './NotificationBell'
import { useAuth } from '../context/AuthContext'

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <BSNavbar className="top-navbar px-4 py-2" expand="lg">
      <div className="d-flex align-items-center gap-2">
        <strong style={{ color: '#1e3a5f' }}>StayNest</strong>
        <span className="text-muted small">Hotel Management</span>
      </div>
      <Nav className="ms-auto align-items-center">
        <NotificationBell />
        <Dropdown align="end">
          <Dropdown.Toggle variant="light" id="user-dd" className="border-0 d-flex align-items-center gap-2">
            <FaUserCircle size={22} />
            <span className="small">
              {user?.name || 'User'} <span className="badge bg-secondary">{user?.role}</span>
            </span>
          </Dropdown.Toggle>
          <Dropdown.Menu>
            <Dropdown.Item as={Link} to="/profile">My Profile</Dropdown.Item>
            <Dropdown.Item as={Link} to="/notifications">Notifications</Dropdown.Item>
            <Dropdown.Divider />
            <Dropdown.Item onClick={handleLogout}>
              <FaSignOutAlt className="me-2" /> Logout
            </Dropdown.Item>
          </Dropdown.Menu>
        </Dropdown>
      </Nav>
    </BSNavbar>
  )
}