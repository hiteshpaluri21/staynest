import { api, unwrap, patchParam } from '../api'
export const getDiningReservations = (params) => api.get('/api/dining-reservations', params).then(unwrap)
export const getDiningReservationById = (id) => api.get(`/api/dining-reservations/${id}`).then(unwrap)
export const createDiningReservation = (data) => api.post('/api/dining-reservations', data).then(unwrap)
export const updateDiningStatus = (id, status) => patchParam(`/api/dining-reservations/${id}/status`, { status }).then(unwrap)