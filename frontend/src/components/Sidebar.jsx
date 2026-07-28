import { Nav } from 'react-bootstrap'
import { NavLink } from 'react-router-dom'
import { FaUsers, FaBed, FaDoorOpen, FaTags, FaChartBar, FaSearch, FaCalendarCheck, FaIdCard, FaConciergeBell, FaClipboardList, FaBroom, FaTools, FaUtensils, FaBell, FaClipboardCheck, FaBookOpen, FaListAlt, FaReceipt } from 'react-icons/fa'
import { useAuth } from '../context/AuthContext'

const ALL_LINKS = [
  { to: '/users', label: 'User Management', icon: <FaUsers />, roles: ['ADMIN'] },
  { to: '/room-types', label: 'Room Types', icon: <FaBed />, roles: ['ADMIN'] },
  { to: '/rooms', label: 'Rooms', icon: <FaDoorOpen />, roles: ['ADMIN'] },
  { to: '/rate-plans', label: 'Rate Plans', icon: <FaTags />, roles: ['ADMIN', 'REVENUEMANAGER', 'GUEST'] },
  { to: '/analytics', label: 'Analytics', icon: <FaChartBar />, roles: ['ADMIN', 'REVENUEMANAGER'] },
  { to: '/book', label: 'Book a Room', icon: <FaSearch />, roles: ['GUEST', 'ADMIN'] },
  { to: '/my-reservations', label: 'My Reservations', icon: <FaCalendarCheck />, roles: ['GUEST', 'ADMIN'] },
  { to: '/my-stay', label: 'My Stay & Bill', icon: <FaReceipt />, roles: ['GUEST', 'ADMIN'] },
  { to: '/reservations', label: 'Reservations', icon: <FaListAlt />, roles: ['FRONTDESK', 'ADMIN'] },
  { to: '/profile', label: 'My Profile', icon: <FaIdCard />, roles: ['GUEST', 'FRONTDESK', 'ADMIN'] },
  { to: '/front-desk', label: 'Front Desk', icon: <FaConciergeBell />, roles: ['FRONTDESK', 'ADMIN'] },
  { to: '/stay-records', label: 'Stay Records', icon: <FaBookOpen />, roles: ['FRONTDESK', 'ADMIN'] },
  { to: '/housekeeping', label: 'Housekeeping', icon: <FaBroom />, roles: ['HOUSEKEEPING', 'ADMIN'] },
  { to: '/maintenance', label: 'Maintenance', icon: <FaTools />, roles: ['HOUSEKEEPING', 'ADMIN', 'GUEST'] },
  { to: '/menu', label: 'Menu', icon: <FaUtensils />, roles: ['FBMANAGER', 'ADMIN', 'GUEST'] },
  { to: '/orders', label: 'F&B Orders', icon: <FaClipboardList />, roles: ['FBMANAGER', 'ADMIN'] },
  { to: '/dining-reservations', label: 'Dining Reservations', icon: <FaClipboardCheck />, roles: ['FBMANAGER', 'ADMIN'] },
  { to: '/notifications', label: 'Notifications', icon: <FaBell />, roles: ['GUEST', 'FRONTDESK', 'HOUSEKEEPING', 'FBMANAGER', 'REVENUEMANAGER', 'ADMIN'] },
]

export default function Sidebar() {
  const { user } = useAuth()
  if (!user) return null
  const links = ALL_LINKS.filter(l => l.roles.includes(user.role))

  return (
    <div className="sidebar">
      <div className="brand">Stay<span>Nest</span></div>
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