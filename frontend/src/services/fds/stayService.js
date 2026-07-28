import { api, unwrap } from '../api'
export const checkIn = (data) => api.post('/api/stay-records/checkin', data).then(unwrap)
export const getStays = (params) => api.get('/api/stay-records', params).then(unwrap)
export const getStayById = (id) => api.get(`/api/stay-records/${id}`).then(unwrap)
export const postFolioItem = (stayId, data) => api.post(`/api/stay-records/${stayId}/folio-items`, data).then(unwrap)
export const updateFolioItem = (stayId, folioItemId, data) => api.put(`/api/stay-records/${stayId}/folio-items/${folioItemId}`, data).then(unwrap)
export const getFolioItems = (stayId) => api.get(`/api/folio-items/stay/${stayId}`).then(unwrap)
export const checkOut = (stayId) => api.post(`/api/stay-records/${stayId}/checkout`).then(unwrap)