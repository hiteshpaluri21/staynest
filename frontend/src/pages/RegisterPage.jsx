import { useState } from 'react'
import { Form, Button, Card, Alert, Row, Col } from 'react-bootstrap'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { validateEmail, validatePhone, validateName, normalizePhone, isClean } from '../utils/validation'

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
  const [fieldErrors, setFieldErrors] = useState({})
  // A field only shows its error once the user has interacted with it (or tried to submit),
  // so the form does not open covered in red.
  const [touched, setTouched] = useState({})

  const validateAll = (values) => ({
    name: validateName(values.name),
    email: validateEmail(values.email),
    phone: validatePhone(values.phone),
    password: values.password.length < 6 ? 'Password must be at least 6 characters' : '',
    confirmPassword: values.password !== values.confirmPassword ? 'Passwords do not match' : '',
  })

  const handleChange = (e) => {
    const { name, value } = e.target
    const next = { ...form, [name]: value }
    setForm(next)
    setFieldErrors(validateAll(next))
  }

  const handleBlur = (e) => setTouched(prev => ({ ...prev, [e.target.name]: true }))

  const showError = (field) => (touched[field] ? fieldErrors[field] || '' : '')

  const submit = async (e) => {
    e.preventDefault()
    setError('')

    const errors = validateAll(form)
    setFieldErrors(errors)
    if (!isClean(errors)) {
      setTouched({ name: true, email: true, phone: true, password: true, confirmPassword: true })
      return
    }

    setLoading(true)
    try {
      const headers = { 'Content-Type': 'application/json' }
      const token = localStorage.getItem('token') || ''
      if (token) headers['Authorization'] = `Bearer ${token}`
      const res = await fetch('/api/auth/register', {
        method: 'POST',
        headers,
        body: JSON.stringify({
          name: form.name.trim(),
          email: form.email.trim(),
          // Send the separator-free form so it matches the backend pattern exactly.
          phone: normalizePhone(form.phone),
          password: form.password,
          role: 'GUEST'
        }),
      })
      const ct = res.headers.get('content-type') || ''
      const payload = ct.includes('application/json') ? await res.json() : await res.text()
      if (!res.ok) {
        const msg = (payload && typeof payload === 'object' && (payload.message || payload.error)) || (typeof payload === 'string' && payload) || `Request failed (${res.status})`
        throw new Error(msg)
      }
      const data = payload && typeof payload === 'object' && 'data' in payload ? payload.data : payload
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
      <Card className="shadow border-0 auth-card-wide">
        <Card.Body className="p-4">
          <h3 className="text-center mb-1 brand-wordmark">StayNest</h3>
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
                onBlur={handleBlur}
                isInvalid={!!showError('name')}
                required
                placeholder="e.g. John Doe"
              />
              <Form.Control.Feedback type="invalid">{showError('name')}</Form.Control.Feedback>
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
                    onBlur={handleBlur}
                    isInvalid={!!showError('email')}
                    required
                    placeholder="john@example.com"
                  />
                  <Form.Control.Feedback type="invalid">{showError('email')}</Form.Control.Feedback>
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
                    onBlur={handleBlur}
                    isInvalid={!!showError('phone')}
                    required
                    placeholder="9876543210"
                  />
                  <Form.Control.Feedback type="invalid">{showError('phone')}</Form.Control.Feedback>
                  {!showError('phone') && (
                    <Form.Text className="text-muted">10 digits, optionally with a +country code.</Form.Text>
                  )}
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
                onBlur={handleBlur}
                isInvalid={!!showError('password')}
                required
                placeholder="At least 6 characters"
              />
              <Form.Control.Feedback type="invalid">{showError('password')}</Form.Control.Feedback>
            </Form.Group>

            <Form.Group className="mb-4">
              <Form.Label className="small fw-bold">Confirm Password</Form.Label>
              <Form.Control
                type="password"
                name="confirmPassword"
                value={form.confirmPassword}
                onChange={handleChange}
                onBlur={handleBlur}
                isInvalid={!!showError('confirmPassword')}
                required
                placeholder="Re-enter password"
              />
              <Form.Control.Feedback type="invalid">{showError('confirmPassword')}</Form.Control.Feedback>
            </Form.Group>

            <Button
              type="submit"
              className="w-100 py-2 fw-semibold mb-3"
              disabled={loading}
            >
              {loading ? 'Creating Account…' : 'Register & Start Booking'}
            </Button>
          </Form>

          <div className="text-center mt-3">
            <span className="small text-muted">Already have an account? </span>
            <Link to="/login" className="small fw-bold text-primary text-decoration-none">
              Sign In
            </Link>
          </div>
          <div className="text-center mt-2">
            <Link to="/" className="small text-muted text-decoration-none">← Back to hotel site</Link>
          </div>
        </Card.Body>
      </Card>
    </div>
  )
}
