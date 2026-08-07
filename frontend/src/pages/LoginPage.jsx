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
      /*
       * Signing in lands on the public hotel site, not straight into a console. The header
       * there shows who you are and its "My Dashboard" item goes to homeFor(role), so the
       * console is one click away — and sign-in and sign-out now end up in the same place.
       */
      navigate('/')
    } catch (err) {
      setError(err.message || 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-bg">
      <Card className="shadow auth-card">
        <Card.Body className="p-4">
          <h3 className="text-center mb-1 brand-wordmark">StayNest</h3>
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
            <Button type="submit" className="w-100" disabled={loading}>
              {loading ? 'Signing in…' : 'Sign In'}
            </Button>
          </Form>
          <div className="text-center mt-3">
            <span className="small text-muted">Don't have an account? </span>
            <Link to="/register" className="small fw-bold text-primary text-decoration-none">
              Register as Guest
            </Link>
          </div>
          {/* "/" is the public hotel site, so someone who came here from a Book
              button needs a way back out. */}
          <div className="text-center mt-2">
            <Link to="/" className="small text-muted text-decoration-none">← Back to hotel site</Link>
          </div>
        </Card.Body>
      </Card>
    </div>
  )
}