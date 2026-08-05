import { authError } from '../../utils/session'

const BASE_URL = '/api/guests'

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

export const getGuests = () => request(BASE_URL)

export const getGuestById = (id) => request(`${BASE_URL}/${id}`)

// The signed-in user's own guest profile, resolved from the JWT. Use the guestId from here —
// it is NOT the same as the IAM userId, and passing userId as guestId silently returns nothing.
export const getMyGuestProfile = () => request(`${BASE_URL}/me`)

export const getGuestByEmail = (email) => request(`${BASE_URL}/email/${email}`)

export const createGuest = (data) => request(BASE_URL, { method: 'POST', body: JSON.stringify(data) })

export const updateGuest = (id, data) => request(`${BASE_URL}/${id}`, { method: 'PUT', body: JSON.stringify(data) })

export const updateLoyaltyTier = (id, tier) => request(`${BASE_URL}/${id}/loyalty${toQuery({ tier })}`, { method: 'PATCH' })

export const blacklistGuest = (id) => request(`${BASE_URL}/${id}/blacklist`, { method: 'PATCH' })
