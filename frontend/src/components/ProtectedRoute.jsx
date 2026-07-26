import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

const ROLE_ACCESS = {
  ADMIN: ['*'],
  GUEST: ['/book', '/my-reservations', '/profile', '/notifications'],
  FRONTDESK: ['/book', '/my-reservations', '/profile', '/notifications', '/front-desk', '/stays'],
  HOUSEKEEPING: ['/book', '/my-reservations', '/profile', '/notifications', '/housekeeping', '/maintenance'],
  FBMANAGER: ['/book', '/my-reservations', '/profile', '/notifications', '/menu', '/orders', '/dining-reservations'],
  REVENUEMANAGER: ['/book', '/my-reservations', '/profile', '/notifications', '/analytics'],
}

function hasAccess(role, path) {
  if (role === 'ADMIN') return true
  const allowed = ROLE_ACCESS[role] || []
  return allowed.some(p => path === p || path.startsWith(p + '/'))
}

export default function ProtectedRoute({ children, requiredRole }) {
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

  if (requiredRole && user.role !== requiredRole && user.role !== 'ADMIN') {
    return <Navigate to="/unauthorized" replace />
  }

  if (!hasAccess(user.role, location.pathname)) {
    return <Navigate to="/unauthorized" replace />
  }

  return children
}
