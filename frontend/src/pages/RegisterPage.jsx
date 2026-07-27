import { useState } from 'react'
import { Form, Button, Card, Alert, Row, Col } from 'react-bootstrap'
import { useNavigate, Link } from 'react-router-dom'
import { api, unwrap } from '../services/api'
import { useAuth } from '../context/AuthContext'

export default function RegisterPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({
    name: '',
    email: '',
    phone: '',
    password: '',
    confirmPassword: ''
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleChange = (e) => {
    const { name, value } = e.target
    setForm(prev => ({ ...prev, [name]: value }))
  }

  const submit = async (e) => {
    e.preventDefault()
    setError('')

    if (form.password !== form.confirmPassword) {
      setError('Passwords do not match')
      return
    }

    if (form.password.length < 6) {
      setError('Password must be at least 6 characters')
      return
    }

    setLoading(true)
    try {
      const res = await api.post('/api/auth/register', {
        name: form.name,
        email: form.email,
        phone: form.phone,
        password: form.password,
        role: 'GUEST'
      })
      const data = unwrap(res)
      login(data)
      navigate('/book')
    } catch (err) {
      setError(err.message || 'Registration failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-bg py-5">
      <Card style={{ width: 440 }} className="shadow border-0">
        <Card.Body className="p-4">
          <h3 className="text-center mb-1" style={{ color: '#1e3a5f' }}>StayNest</h3>
          <p className="text-center text-muted small mb-4">Create Guest Account</p>
          
          {error && <Alert variant="danger" className="py-2 small">{error}</Alert>}

          <Form onSubmit={submit}>
            <Form.Group className="mb-3">
              <Form.Label className="small fw-bold">Full Name</Form.Label>
              <Form.Control
                type="text"
                name="name"
                value={form.name}
                onChange={handleChange}
                required
                placeholder="e.g. John Doe"
              />
            </Form.Group>

            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label className="small fw-bold">Email Address</Form.Label>
                  <Form.Control
                    type="email"
                    name="email"
                    value={form.email}
                    onChange={handleChange}
                    required
                    placeholder="john@example.com"
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label className="small fw-bold">Phone Number</Form.Label>
                  <Form.Control
                    type="tel"
                    name="phone"
                    value={form.phone}
                    onChange={handleChange}
                    required
                    placeholder="+1 234 567 890"
                  />
                </Form.Group>
              </Col>
            </Row>

            <Form.Group className="mb-3">
              <Form.Label className="small fw-bold">Password</Form.Label>
              <Form.Control
                type="password"
                name="password"
                value={form.password}
                onChange={handleChange}
                required
                placeholder="At least 6 characters"
              />
            </Form.Group>

            <Form.Group className="mb-4">
              <Form.Label className="small fw-bold">Confirm Password</Form.Label>
              <Form.Control
                type="password"
                name="confirmPassword"
                value={form.confirmPassword}
                onChange={handleChange}
                required
                placeholder="Re-enter password"
              />
            </Form.Group>

            <Button
              type="submit"
              className="w-100 py-2 fw-semibold mb-3"
              style={{ background: '#1e3a5f', borderColor: '#1e3a5f' }}
              disabled={loading}
            >
              {loading ? 'Creating Account…' : 'Register & Start Booking'}
            </Button>
          </Form>

          <div className="text-center mt-3">
            <span className="small text-muted">Already have an account? </span>
            <Link to="/login" className="small fw-bold" style={{ color: '#1e3a5f', textDecoration: 'none' }}>
              Sign In
            </Link>
          </div>
        </Card.Body>
      </Card>
    </div>
  )
}
