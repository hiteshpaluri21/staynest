import { createContext, useContext, useEffect, useState } from 'react'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(null)

  useEffect(() => {
    const stored = localStorage.getItem('staynest_auth')
    if (stored) {
      try { setUser(JSON.parse(stored)) } catch { localStorage.removeItem('staynest_auth') }
    }
  }, [])

  const login = (data) => {
    // data: { token, role, userId, name, email }
    const u = { ...data }
    setUser(u)
    localStorage.setItem('staynest_auth', JSON.stringify(u))
    localStorage.setItem('token', data.token)
  }

  const logout = () => {
    setUser(null)
    localStorage.removeItem('staynest_auth')
    localStorage.removeItem('token')
  }

  return (
    <AuthContext.Provider value={{ user, token, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)