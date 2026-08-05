import { authError } from '../../utils/session'

const BASE_URL = '/api/notifications'

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

export const getByUser = (userId) => request(`${BASE_URL}/user/${userId}`)

export const getUnreadByUser = (userId) => request(`${BASE_URL}/user/${userId}/unread`)

export const getUnreadCount = (userId) => request(`${BASE_URL}/user/${userId}/count`)

export const sendNotification = (data) => request(BASE_URL, { method: 'POST', body: JSON.stringify(data) })

export const markAsRead = (id) => request(`${BASE_URL}/${id}/read`, { method: 'PATCH' })

export const markAllAsRead = (userId) => request(`${BASE_URL}/user/${userId}/read-all`, { method: 'PATCH' })
