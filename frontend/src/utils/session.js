// Shared handling for auth failures returned by the API.
//
// The backend services use Spring Security's default Http403ForbiddenEntryPoint, so they
// answer 403 for BOTH cases:
//   * no token / expired token  -> the session is over, the user must sign in again
//   * valid token, wrong role   -> the user is signed in but lacks permission
// Status alone cannot tell these apart, so we inspect the stored JWT: only clear the
// session and bounce to /login when we do not hold a currently-valid token. Otherwise a
// GUEST touching an admin-only endpoint would be silently logged out.

const decodeJwtPayload = (token) => {
  const parts = String(token).split('.')
  if (parts.length !== 3) return null
  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
    return JSON.parse(atob(padded))
  } catch {
    return null
  }
}

/** True when a stored token exists and its exp claim (if any) is still in the future. */
export const hasValidToken = () => {
  const token = localStorage.getItem('token')
  if (!token) return false
  const payload = decodeJwtPayload(token)
  // A stored token we cannot even parse is bogus — treat it as no session at all.
  if (!payload) return false
  // No exp claim: nothing to check, so assume it is still usable.
  if (!payload.exp) return true
  return payload.exp * 1000 > Date.now()
}

export const clearSession = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('staynest_auth')
}

/**
 * Builds the Error to throw for a 401/403 response, redirecting to /login first when the
 * failure means the session is gone rather than merely insufficient permissions.
 */
export const authError = (status) => {
  if (status === 401 || !hasValidToken()) {
    clearSession()
    if (!window.location.pathname.startsWith('/login')) window.location.href = '/login'
    return new Error('Your session has expired. Please sign in again.')
  }
  return new Error('You do not have permission to perform this action.')
}
