import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

const ROLE_ACCESS = {
  ADMIN: ['*'],
  // /my-reservations is a guest's own booking list, so only GUEST lists it.
  GUEST: ['/book', '/my-reservations', '/my-stay', '/profile', '/notifications', '/menu', '/rate-plans', '/maintenance', '/dining-reservations'],
  FRONTDESK: ['/book', '/reservations', '/profile', '/notifications', '/front-desk', '/stays', '/stay-records', '/housekeeping'],
  HOUSEKEEPING: ['/book', '/profile', '/notifications', '/housekeeping', '/maintenance'],
  FBMANAGER: ['/book', '/profile', '/notifications', '/menu', '/orders', '/dining-reservations'],
}

function hasAccess(role, path) {
  if (role === 'ADMIN') return true
  const allowed = ROLE_ACCESS[role] || []
  return allowed.some(p => path === p || path.startsWith(p + '/'))
}

/**
 * `strict` makes the `roles` list authoritative for ADMIN too. ADMIN normally bypasses every
 * check, so it is the only way to keep a genuinely role-specific page (e.g. a guest's own
 * "My Reservations") off an admin's hands.
 */
export default function ProtectedRoute({ children, requiredRole, roles, strict = false }) {
  const { isAuthenticated, user, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center vh-100">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading...</span>
        </div>
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  if (roles && roles.length > 0 && !roles.includes(user?.role) && (strict || user?.role !== 'ADMIN')) {
    return <Navigate to="/unauthorized" replace />
  }

  if (requiredRole && user?.role !== requiredRole && user?.role !== 'ADMIN') {
    return <Navigate to="/unauthorized" replace />
  }

  if (!hasAccess(user?.role, location.pathname)) {
    return <Navigate to="/unauthorized" replace />
  }

  return children
}
