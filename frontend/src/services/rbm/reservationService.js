import { api, unwrap, patchParam } from '../api'
export const getReservations = (params) => api.get('/api/reservations', params).then(unwrap)
export const getReservationById = (id) => api.get(`/api/reservations/${id}`).then(unwrap)
export const createReservation = (data) => api.post('/api/reservations', data).then(unwrap)
export const cancelReservation = (id) => patchParam(`/api/reservations/${id}/cancel`).then(unwrap)
export const updateReservationStatus = (id, status) => patchParam(`/api/reservations/${id}/status`, { status }).then(unwrap)
export const getUpcoming = (date) => api.get('/api/reservations/upcoming', { date }).then(unwrap)