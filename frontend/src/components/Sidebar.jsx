import { Nav } from 'react-bootstrap'
import { NavLink, Link } from 'react-router-dom'
import { FaUsers, FaBed, FaDoorOpen, FaTags, FaSearch, FaCalendarCheck, FaIdCard, FaConciergeBell, FaClipboardList, FaBroom, FaTools, FaUtensils, FaBell, FaClipboardCheck, FaBookOpen, FaListAlt, FaReceipt, FaHistory } from 'react-icons/fa'
import { useAuth } from '../context/AuthContext'

const ALL_LINKS = [
  { to: '/users', label: 'User Management', icon: <FaUsers />, roles: ['ADMIN'] },
  { to: '/audit-logs', label: 'Audit Logs', icon: <FaHistory />, roles: ['ADMIN'] },
  { to: '/room-types', label: 'Room Types', icon: <FaBed />, roles: ['ADMIN'] },
  { to: '/rooms', label: 'Rooms', icon: <FaDoorOpen />, roles: ['ADMIN'] },
  { to: '/rate-plans', label: 'Rate Plans', icon: <FaTags />, roles: ['ADMIN', 'GUEST'] },
  // Guest-only: an admin manages the inventory, they do not book a room in it.
  { to: '/book', label: 'Book a Room', icon: <FaSearch />, roles: ['GUEST'] },
  // Guest-only: admins get the same data (and more) from the Reservations page.
  { to: '/my-reservations', label: 'My Reservations', icon: <FaCalendarCheck />, roles: ['GUEST'] },
  { to: '/my-stay', label: 'My Stay & Bill', icon: <FaReceipt />, roles: ['GUEST'] },
  { to: '/reservations', label: 'Reservations', icon: <FaListAlt />, roles: ['FRONTDESK', 'ADMIN'] },
  { to: '/profile', label: 'My Profile', icon: <FaIdCard />, roles: ['GUEST', 'FRONTDESK', 'ADMIN'] },
  { to: '/front-desk', label: 'Front Desk', icon: <FaConciergeBell />, roles: ['FRONTDESK', 'ADMIN'] },
  { to: '/stay-records', label: 'Stay Records', icon: <FaBookOpen />, roles: ['FRONTDESK', 'ADMIN'] },
  { to: '/housekeeping', label: 'Housekeeping', icon: <FaBroom />, roles: ['HOUSEKEEPING', 'FRONTDESK', 'ADMIN'] },
  { to: '/maintenance', label: 'Maintenance', icon: <FaTools />, roles: ['HOUSEKEEPING', 'ADMIN', 'GUEST'] },
  { to: '/menu', label: 'Menu', icon: <FaUtensils />, roles: ['FBMANAGER', 'ADMIN', 'GUEST'] },
  { to: '/orders', label: 'F&B Orders', icon: <FaClipboardList />, roles: ['FBMANAGER', 'ADMIN'] },
  { to: '/dining-reservations', label: 'Dining Reservations', icon: <FaClipboardCheck />, roles: ['FBMANAGER', 'ADMIN', 'GUEST'] },
  { to: '/notifications', label: 'Notifications', icon: <FaBell />, roles: ['GUEST', 'FRONTDESK', 'HOUSEKEEPING', 'FBMANAGER', 'ADMIN'] },
]

export default function Sidebar() {
  const { user } = useAuth()
  if (!user) return null
  const links = ALL_LINKS.filter(l => l.roles.includes(user.role))

  return (
    <div className="sidebar">
      {/* Clicking the logo leaves the app for the public hotel site. */}
      <Link to="/" className="brand text-decoration-none d-block">Stay<span>Nest</span></Link>
      <Nav className="flex-column mt-2">
        {links.map(l => (
          <Nav.Link key={l.to} as={NavLink} to={l.to} className={({ isActive }) => isActive ? 'active' : ''}>
            <span className="me-2">{l.icon}</span>{l.label}
          </Nav.Link>
        ))}
      </Nav>
    </div>
  )
}