import { createContext, useContext, useEffect, useState } from 'react'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const stored = localStorage.getItem('staynest_auth')
    const tk = localStorage.getItem('token')
    if (stored) {
      try { 
        setUser(JSON.parse(stored))
        setToken(tk || null)
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
  }

  const logout = () => {
    setUser(null)
    setToken(null)
    localStorage.removeItem('staynest_auth')
    localStorage.removeItem('token')
  }

  const isAuthenticated = Boolean(user && (token || localStorage.getItem('token')))

  return (
    <AuthContext.Provider value={{ user, token, isAuthenticated, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)