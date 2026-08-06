import { authError } from '../../utils/session'

const BASE_URL = '/api/stay-records'

const toQuery = (params) => {
  const qs = new URLSearchParams()
  Object.entries(params || {}).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') qs.append(k, v)
  })
  const s = qs.toString()
  return s ? `?${s}` : ''
}

const request = async (url, options = {}) => {
  const headers = { 'Content-Type': 'application/json' }
  const token = localStorage.getItem('token') || ''
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(url, { ...options, headers })

  if (res.status === 401 || res.status === 403) {
    throw authError(res.status)
  }

  const ct = res.headers.get('content-type') || ''
  const payload = ct.includes('application/json') ? await res.json() : await res.text()

  if (!res.ok) {
    const msg = (payload && typeof payload === 'object' && (payload.message || payload.error)) || (typeof payload === 'string' && payload) || `Request failed (${res.status})`
    throw new Error(msg)
  }

  return payload && typeof payload === 'object' && 'data' in payload ? payload.data : payload
}

export const checkIn = (data) => request(`${BASE_URL}/checkin`, { method: 'POST', body: JSON.stringify(data) })

export const getStays = (params) => request(`${BASE_URL}${toQuery(params)}`)

export const getStayById = (id) => request(`${BASE_URL}/${id}`)

export const postFolioItem = (stayId, data) => request(`${BASE_URL}/${stayId}/folio-items`, { method: 'POST', body: JSON.stringify(data) })

export const updateFolioItem = (stayId, folioItemId, data) => request(`${BASE_URL}/${stayId}/folio-items/${folioItemId}`, { method: 'PUT', body: JSON.stringify(data) })

export const getFolioItems = (stayId) => request(`/api/folio-items/stay/${stayId}`)

// housekeepingStaffId owns the post-checkout cleaning task raised automatically by the backend.
export const checkOut = (stayId, housekeepingStaffId) =>
  request(`${BASE_URL}/${stayId}/checkout${toQuery({ housekeepingStaffId })}`, { method: 'POST' })
