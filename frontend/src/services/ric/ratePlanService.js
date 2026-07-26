import { api, unwrap, patchParam } from '../api'
export const getRatePlans = (params) => api.get('/api/rate-plans', params).then(unwrap)
export const getRatePlanById = (id) => api.get(`/api/rate-plans/${id}`).then(unwrap)
export const createRatePlan = (data) => api.post('/api/rate-plans', data).then(unwrap)
export const updateRatePlanStatus = (id, status) => patchParam(`/api/rate-plans/${id}/status`, { status }).then(unwrap)