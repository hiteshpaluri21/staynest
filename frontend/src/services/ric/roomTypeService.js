const BASE_URL = '/api/room-types'

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

export const getRoomTypes = () => request(BASE_URL)

export const getRoomTypeById = (id) => request(`${BASE_URL}/${id}`)

export const createRoomType = (data) => request(BASE_URL, { method: 'POST', body: JSON.stringify(data) })

export const updateRoomType = (id, data) => request(`${BASE_URL}/${id}`, { method: 'PUT', body: JSON.stringify(data) })

export const updateRoomTypeStatus = (id, status) => request(`${BASE_URL}/${id}/status${toQuery({ status })}`, { method: 'PATCH' })
