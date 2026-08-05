import { authError } from '../../utils/session'

const BASE_URL = '/api/menu-items'

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

export const getMenuItems = (params) => request(`${BASE_URL}${toQuery(params)}`)

export const getMenuItemById = (id) => request(`${BASE_URL}/${id}`)

export const createMenuItem = (data) => request(BASE_URL, { method: 'POST', body: JSON.stringify(data) })

export const updateMenuItem = (id, data) => request(`${BASE_URL}/${id}`, { method: 'PUT', body: JSON.stringify(data) })

export const updateAvailability = (id, value) => request(`${BASE_URL}/${id}/availability${toQuery({ value })}`, { method: 'PATCH' })

export const updatePrice = (id, value) => request(`${BASE_URL}/${id}/price${toQuery({ value })}`, { method: 'PATCH' })
