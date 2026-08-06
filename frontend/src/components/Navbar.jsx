import { Navbar as BSNavbar, Nav, Dropdown, Button } from 'react-bootstrap'
import { FaUserCircle, FaSignOutAlt, FaBars } from 'react-icons/fa'
import { Link, useNavigate } from 'react-router-dom'
import NotificationBell from './NotificationBell'
import ThemeToggle from './ThemeToggle'
import { useAuth } from '../context/AuthContext'

export default function Navbar({ onToggleSidebar }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <BSNavbar className="top-navbar px-3 px-md-4 py-2" expand="lg">
      <div className="d-flex align-items-center gap-2 gap-md-3">
        <Button
          variant="link"
          className="sidebar-burger"
          onClick={onToggleSidebar}
          aria-label="Toggle navigation menu"
          title="Toggle menu"
        >
          <FaBars size={18} />
        </Button>

        {/* The wordmark is the way back to the public hotel site. */}
        <Link to="/" className="brand-wordmark text-decoration-none">
          StayNest
        </Link>
        {/* Dropped on phones, where the row needs the space for the controls. */}
        <span className="text-muted small d-none d-md-inline">Hotel Management</span>
      </div>

      <Nav className="ms-auto align-items-center gap-1">
        <ThemeToggle />
        <NotificationBell />
        <Dropdown align="end">
          {/* variant="light" would stay a near-white pill in dark mode, so this is themed by class. */}
          <Dropdown.Toggle variant="link" id="user-dd" className="user-menu-toggle d-flex align-items-center gap-2">
            <FaUserCircle size={22} />
            {/* Name and role are hidden on phones; the avatar alone opens the menu. */}
            <span className="small d-none d-sm-inline">
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
