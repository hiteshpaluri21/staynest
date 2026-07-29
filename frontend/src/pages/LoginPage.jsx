import { useState } from 'react'
import { Form, Button, Card, Alert } from 'react-bootstrap'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function LoginPage() {
  
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const headers = { 'Content-Type': 'application/json' }
      const token = localStorage.getItem('token') || ''
      if (token) headers['Authorization'] = `Bearer ${token}`
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers,
        body: JSON.stringify({ email, password }),
      })
      const ct = res.headers.get('content-type') || ''
      const payload = ct.includes('application/json') ? await res.json() : await res.text()
      if (!res.ok) {
        const msg = (payload && typeof payload === 'object' && (payload.message || payload.error)) || (typeof payload === 'string' && payload) || `Request failed (${res.status})`
        throw new Error(msg)
      }
      const data = payload && typeof payload === 'object' && 'data' in payload ? payload.data : payload
      login(data)
      const role = data.role
      const home =
        role === 'ADMIN' ? '/users' :
        role === 'FRONTDESK' ? '/front-desk' :
        role === 'HOUSEKEEPING' ? '/housekeeping' :
        role === 'FBMANAGER' ? '/orders' :
        role === 'REVENUEMANAGER' ? '/analytics' : '/book'
      navigate(home)
    } catch (err) {
      setError(err.message || 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-bg">
      <Card style={{ width: 400 }} className="shadow">
        <Card.Body className="p-4">
          <h3 className="text-center mb-1" style={{ color: '#1e3a5f' }}>StayNest</h3>
          <p className="text-center text-muted small mb-4">Hotel & Hospitality Management</p>
          {error && <Alert variant="danger" className="py-2">{error}</Alert>}
          <Form onSubmit={submit}>
            <Form.Group className="mb-3">
              <Form.Label>Email</Form.Label>
              <Form.Control type="email" value={email} onChange={e => setEmail(e.target.value)} required placeholder="you@staynest.com" />
            </Form.Group>
            <Form.Group className="mb-4">
              <Form.Label>Password</Form.Label>
              <Form.Control type="password" value={password} onChange={e => setPassword(e.target.value)} required placeholder="••••••" />
            </Form.Group>
            <Button type="submit" className="w-100" style={{ background: '#1e3a5f', borderColor: '#1e3a5f' }} disabled={loading}>
              {loading ? 'Signing in…' : 'Sign In'}
            </Button>
          </Form>
          <div className="text-center mt-3">
            <span className="small text-muted">Don't have an account? </span>
            <Link to="/register" className="small fw-bold" style={{ color: '#1e3a5f', textDecoration: 'none' }}>
              Register as Guest
            </Link>
          </div>
        </Card.Body>
      </Card>
    </div>
  )
}