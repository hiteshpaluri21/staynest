import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

const ROLE_ACCESS = {
  ADMIN: ['*'],
  GUEST: ['/book', '/my-reservations', '/my-stay', '/profile', '/notifications', '/menu', '/rate-plans', '/maintenance'],
  FRONTDESK: ['/book', '/my-reservations', '/reservations', '/profile', '/notifications', '/front-desk', '/stays', '/stay-records'],
  HOUSEKEEPING: ['/book', '/my-reservations', '/profile', '/notifications', '/housekeeping', '/maintenance'],
  FBMANAGER: ['/book', '/my-reservations', '/profile', '/notifications', '/menu', '/orders', '/dining-reservations'],
  REVENUEMANAGER: ['/book', '/my-reservations', '/profile', '/notifications', '/analytics', '/rate-plans'],
}

function hasAccess(role, path) {
  if (role === 'ADMIN') return true
  const allowed = ROLE_ACCESS[role] || []
  return allowed.some(p => path === p || path.startsWith(p + '/'))
}

export default function ProtectedRoute({ children, requiredRole, roles }) {
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

  if (roles && roles.length > 0 && !roles.includes(user?.role) && user?.role !== 'ADMIN') {
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
