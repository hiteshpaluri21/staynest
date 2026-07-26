import { api, unwrap, patchParam } from '../api'
export const getByUser = (userId) => api.get(`/api/notifications/user/${userId}`).then(unwrap)
export const getUnreadByUser = (userId) => api.get(`/api/notifications/user/${userId}/unread`).then(unwrap)
export const getUnreadCount = (userId) => api.get(`/api/notifications/user/${userId}/count`).then(unwrap)
export const sendNotification = (data) => api.post('/api/notifications', data).then(unwrap)
export const markAsRead = (id) => patchParam(`/api/notifications/${id}/read`).then(unwrap)
export const markAllAsRead = (userId) => patchParam(`/api/notifications/user/${userId}/read-all`).then(unwrap)