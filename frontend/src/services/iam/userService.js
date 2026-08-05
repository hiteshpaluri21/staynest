import { authError } from '../../utils/session'

const BASE_URL = '/api/users'

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

export const getUsers = () => request(BASE_URL)

export const getUserById = (id) => request(`${BASE_URL}/${id}`)

export const getUserByEmail = (email) => request(`${BASE_URL}/email/${email}`)

// Active users of a given role, e.g. the housekeeping staff a task can be assigned to.
export const getUsersByRole = (role) => request(`${BASE_URL}/role/${role}`)

export const createUser = (data) => request(BASE_URL, { method: 'POST', body: JSON.stringify(data) })

export const updateUserStatus = (id, status) => request(`${BASE_URL}/${id}/status${toQuery({ status })}`, { method: 'PATCH' })

export const deleteUser = (id) => request(`${BASE_URL}/${id}`, { method: 'DELETE' })
