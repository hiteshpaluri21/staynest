const BASE_URL = '/api/reports'

const request = async (url, options = {}) => {
  const headers = { 'Content-Type': 'application/json' }
  const token = localStorage.getItem('token') || ''
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(url, { ...options, headers })

  if (res.status === 401) {
    localStorage.removeItem('token')
    localStorage.removeItem('staynest_auth')
    if (!window.location.pathname.startsWith('/login')) window.location.href = '/login'
    throw new Error('Unauthorized')
  }

  const ct = res.headers.get('content-type') || ''
  const payload = ct.includes('application/json') ? await res.json() : await res.text()

  if (!res.ok) {
    const msg = (payload && typeof payload === 'object' && (payload.message || payload.error)) || (typeof payload === 'string' && payload) || `Request failed (${res.status})`
    throw new Error(msg)
  }

  return payload && typeof payload === 'object' && 'data' in payload ? payload.data : payload
}

export const getReports = () => request(BASE_URL)

export const getReportById = (id) => request(`${BASE_URL}/${id}`)

export const generateReport = (data) => request(BASE_URL, { method: 'POST', body: JSON.stringify(data) })

export const getSummary = () => request(`${BASE_URL}/summary`)
