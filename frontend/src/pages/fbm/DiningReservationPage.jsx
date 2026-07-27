import { useEffect, useState } from 'react'
import { Table, Button, Badge, Form, Modal, Alert, Row, Col } from 'react-bootstrap'
import { getDiningReservations, createDiningReservation, updateDiningStatus } from '../../services/fbm/diningService'
import Loader from '../../components/Loader'
import EmptyState from '../../components/EmptyState'
import { statusBadge } from '../../utils/badges'

const OUTLETS = ['The Garden Bistro', 'Sky Lounge', 'Poolside Bar', 'Rooftop Grill']

export default function DiningReservationPage() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [show, setShow] = useState(false)
  const [form, setForm] = useState({ guestId: '', stayId: '', restaurantOutlet: OUTLETS[0], date: '', time: '19:00', covers: 2 })
  const [saveErr, setSaveErr] = useState('')
  const [dateFilter, setDateFilter] = useState('')

  const load = async () => {
    setLoading(true); setError('')
    try { setItems(await getDiningReservations(dateFilter ? { date: dateFilter } : {})) }
    catch (e) { setError(e.message) } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [dateFilter])

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }))

  const submit = async (e) => {
    e.preventDefault(); setSaveErr('')
    try {
      await createDiningReservation({
        ...form,
        guestId: Number(form.guestId),
        stayId: form.stayId ? Number(form.stayId) : null,
        covers: Number(form.covers),
      })
      setShow(false); load()
    } catch (err) { setSaveErr(err.message) }
  }

  const setStatus = async (id, status) => { try { await updateDiningStatus(id, status); load() } catch (e) { alert(e.message) } }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h4 className="mb-0">Dining Reservations</h4>
        <Button style={{ background: '#1e3a5f', borderColor: '#1e3a5f' }} onClick={() => setShow(true)}>+ New Reservation</Button>
      </div>
      <Form.Control type="date" style={{ maxWidth: 200 }} className="mb-3" value={dateFilter} onChange={e => setDateFilter(e.target.value)} />
      {loading ? <Loader /> : error ? <div className="alert alert-danger">{error}</div> :
        items.length === 0 ? <EmptyState message="No dining reservations" /> :
        <Table hover responsive>
          <thead><tr><th>ID</th><th>Guest</th><th>Outlet</th><th>Date</th><th>Time</th><th>Covers</th><th>Status</th><th>Action</th></tr></thead>
          <tbody>
            {items.map(d => (
              <tr key={d.diningResId}>
                <td>{d.diningResId}</td>
                <td>#{d.guestId}</td>
                <td>{d.restaurantOutlet}</td>
                <td>{d.date}</td>
                <td>{d.time}</td>
                <td>{d.covers}</td>
                <td><Badge bg={statusBadge(d.status)}>{d.status}</Badge></td>
                <td>
                  {d.status === 'CONFIRMED' && <Button size="sm" variant="outline-success" onClick={() => setStatus(d.diningResId, 'SEATED')}>Seat</Button>}
                  {d.status === 'SEATED' && <Button size="sm" variant="outline-primary" onClick={() => setStatus(d.diningResId, 'COMPLETED')}>Complete</Button>}
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      }

      <Modal show={show} onHide={() => setShow(false)}>
        <Modal.Header closeButton><Modal.Title>New Dining Reservation</Modal.Title></Modal.Header>
        <Form onSubmit={submit}>
          <Modal.Body>
            {saveErr && <Alert variant="danger" className="py-2">{saveErr}</Alert>}
            <Row>
              <Col md={6}><Form.Group className="mb-3"><Form.Label>Guest ID</Form.Label><Form.Control type="number" required value={form.guestId} onChange={e => set('guestId', e.target.value)} /></Form.Group></Col>
              <Col md={6}><Form.Group className="mb-3"><Form.Label>Stay ID (optional)</Form.Label><Form.Control type="number" value={form.stayId} onChange={e => set('stayId', e.target.value)} /></Form.Group></Col>
            </Row>
            <Form.Group className="mb-3"><Form.Label>Outlet</Form.Label><Form.Select value={form.restaurantOutlet} onChange={e => set('restaurantOutlet', e.target.value)}>{OUTLETS.map(o => <option key={o}>{o}</option>)}</Form.Select></Form.Group>
            <Row>
              <Col md={4}><Form.Group className="mb-3"><Form.Label>Date</Form.Label><Form.Control type="date" required value={form.date} onChange={e => set('date', e.target.value)} /></Form.Group></Col>
              <Col md={4}><Form.Group className="mb-3"><Form.Label>Time</Form.Label><Form.Control type="time" required value={form.time} onChange={e => set('time', e.target.value)} /></Form.Group></Col>
              <Col md={4}><Form.Group className="mb-3"><Form.Label>Covers</Form.Label><Form.Control type="number" min="1" required value={form.covers} onChange={e => set('covers', e.target.value)} /></Form.Group></Col>
            </Row>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShow(false)}>Cancel</Button>
            <Button type="submit" style={{ background: '#1e3a5f', borderColor: '#1e3a5f' }}>Reserve</Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </div>
  )
}