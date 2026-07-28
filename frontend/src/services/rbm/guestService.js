import { api, unwrap, patchParam } from '../api'
export const getGuests = () => api.get('/api/guests').then(unwrap)
export const getGuestById = (id) => api.get(`/api/guests/${id}`).then(unwrap)
export const getGuestByEmail = (email) => api.get(`/api/guests/email/${email}`).then(unwrap)
export const createGuest = (data) => api.post('/api/guests', data).then(unwrap)
export const updateGuest = (id, data) => api.put(`/api/guests/${id}`, data).then(unwrap)
export const updateLoyaltyTier = (id, tier) => patchParam(`/api/guests/${id}/loyalty`, { tier }).then(unwrap)
export const blacklistGuest = (id) => patchParam(`/api/guests/${id}/blacklist`).then(unwrap)