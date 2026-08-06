import { request } from '../http'

const BASE_URL = '/api/notifications'

export const getByUser = (userId) => request(`${BASE_URL}/user/${userId}`)

export const getUnreadByUser = (userId) => request(`${BASE_URL}/user/${userId}/unread`)

export const getUnreadCount = (userId) => request(`${BASE_URL}/user/${userId}/count`)

export const sendNotification = (data) => request(BASE_URL, { method: 'POST', body: JSON.stringify(data) })

export const markAsRead = (id) => request(`${BASE_URL}/${id}/read`, { method: 'PATCH' })

export const markAllAsRead = (userId) => request(`${BASE_URL}/user/${userId}/read-all`, { method: 'PATCH' })
