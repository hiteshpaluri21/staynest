import { useEffect, useState } from 'react'
import { Card, Row, Col, ProgressBar } from 'react-bootstrap'
import { getSummary } from '../../services/har/reportService'
import Loader from '../../components/Loader'

export default function AnalyticsDashboardPage() {
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    getSummary().then(setSummary).catch(e => setError(e.message)).finally(() => setLoading(false))
  }, [])

  if (loading) return <Loader />
  if (error) return <div className="alert alert-danger">{error}</div>
  if (!summary) return null

  const kpis = [
    { label: 'Occupancy Rate', value: `${summary.occupancyRate}%`, pct: summary.occupancyRate, color: 'success' },
    { label: 'ADR (Avg Daily Rate)', value: `₹${summary.adr}`, pct: 75, color: 'primary' },
    { label: 'RevPAR', value: `₹${summary.revPAR}`, pct: 70, color: 'info' },
    { label: 'Avg Length of Stay', value: `${summary.avgLengthOfStay} nights`, pct: 60, color: 'warning' },
    { label: 'F&B Revenue', value: `₹${summary.fbRevenue}`, pct: 80, color: 'danger' },
    { label: 'Guest Satisfaction', value: `${summary.guestSatisfactionScore}/5`, pct: (summary.guestSatisfactionScore / 5) * 100, color: 'primary' },
  ]

  return (
    <div>
      <h4 className="mb-4">Hospitality Analytics Dashboard</h4>
      <Row>
        {kpis.map((k, i) => (
          <Col md={4} key={i} className="mb-4">
            <Card className="kpi-card shadow-sm h-100">
              <Card.Body>
                <p className="text-muted small mb-1">{k.label}</p>
                <div className="kpi-value">{k.value}</div>
                <ProgressBar now={k.pct} variant={k.color} className="mt-3" style={{ height: 6 }} />
              </Card.Body>
            </Card>
          </Col>
        ))}
      </Row>
    </div>
  )
}