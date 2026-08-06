import { Navbar as BSNavbar, Dropdown, Button } from 'react-bootstrap'
import { FaUserCircle, FaSignOutAlt, FaBars } from 'react-icons/fa'
import { Link, useNavigate } from 'react-router-dom'
import NotificationBell from './NotificationBell'
import ThemeToggle from './ThemeToggle'
import { useAuth } from '../context/AuthContext'
import { canViewProfile } from '../utils/home'

export default function Navbar({ onToggleSidebar }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  // Signing out drops you on the public hotel site, not the staff sign-in form —
  // leaving the app should look like leaving, with a way back in rather than a
  // login prompt you did not ask for.
  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    /*
     * No `expand` prop: there is no collapsible section here, and Bootstrap's
     * .navbar-expand-lg only sets flex-wrap/dropdown rules *above* 992px, which left
     * the bar below that width wrapping onto two rows. .top-navbar pins it to one row.
     */
    <BSNavbar className="top-navbar px-3 px-md-4 py-2">
      <div className="navbar-lead d-flex align-items-center gap-2 gap-md-3">
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

      {/*
        * A plain flex row rather than <Nav>. Inside a <Navbar>, react-bootstrap's Nav
        * picks up Bootstrap's .navbar-nav, which is flex-direction: column and pins
        * child dropdown menus to position: static until the expand breakpoint. That
        * stacked these three controls vertically on tablets and phones, and made the
        * profile menu push the bar open instead of floating over the page.
        */}
      <div className="navbar-actions d-flex align-items-center gap-1 ms-auto">
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
            {/* /profile rejects housekeeping and F&B, so they are not offered the link. */}
            {canViewProfile(user?.role) &&
              <Dropdown.Item as={Link} to="/profile">My Profile</Dropdown.Item>}
            <Dropdown.Item as={Link} to="/notifications">Notifications</Dropdown.Item>
            <Dropdown.Divider />
            <Dropdown.Item onClick={handleLogout}>
              <FaSignOutAlt className="me-2" /> Logout
            </Dropdown.Item>
          </Dropdown.Menu>
        </Dropdown>
      </div>
    </BSNavbar>
  )
}
