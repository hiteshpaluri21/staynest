import { createContext, useContext, useEffect, useState } from 'react'
import { getMyGuestProfile } from '../services/rbm/guestService'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(null)
  const [loading, setLoading] = useState(true)
  // The guest's reservation-service profile. Its guestId is a DIFFERENT key from user.userId
  // (IAM), and it is the one reservations are actually filed under.
  const [guest, setGuest] = useState(null)

  // Only guests have a guest profile — calling /api/guests/me as staff would provision a
  // pointless profile for them, so skip it for other roles.
  const loadGuest = async (u) => {
    if (!u || u.role !== 'GUEST') {
      setGuest(null)
      return null
    }
    try {
      const g = await getMyGuestProfile()
      setGuest(g)
      return g
    } catch {
      setGuest(null)
      return null
    }
  }

  useEffect(() => {
    const stored = localStorage.getItem('staynest_auth')
    const tk = localStorage.getItem('token')
    if (stored) {
      try {
        const u = JSON.parse(stored)
        setUser(u)
        setToken(tk || null)
        loadGuest(u).finally(() => setLoading(false))
        return
      } catch {
        localStorage.removeItem('staynest_auth')
        localStorage.removeItem('token')
      }
    }
    setLoading(false)
  }, [])

  const login = (data) => {
    // data: { token, role, userId, name, email }
    const u = { ...data }
    setUser(u)
    setToken(data.token)
    localStorage.setItem('staynest_auth', JSON.stringify(u))
    localStorage.setItem('token', data.token)
    // Token must be stored before this call so the request carries Authorization.
    loadGuest(u)
  }

  const logout = () => {
    setUser(null)
    setToken(null)
    setGuest(null)
    localStorage.removeItem('staynest_auth')
    localStorage.removeItem('token')
  }

  /** Re-reads the guest profile, e.g. after the guest completes missing details. */
  const refreshGuest = () => loadGuest(user)

  const isAuthenticated = Boolean(user && (token || localStorage.getItem('token')))

  return (
    <AuthContext.Provider value={{ user, token, guest, guestId: guest?.guestId ?? null, isAuthenticated, loading, login, logout, refreshGuest }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)