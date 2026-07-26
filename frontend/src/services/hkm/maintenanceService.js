import { api, unwrap, patchParam } from '../api'

export const getRequests = (params) => api.get('/api/maintenance-requests', params).then(unwrap)
export const getRequestById = (id) => api.get(`/api/maintenance-requests/${id}`).then(unwrap)
export const reportIssue = (data) => api.post('/api/maintenance-requests', data).then(unwrap)
export const updateRequestStatus = (id, status) => patchParam(`/api/maintenance-requests/${id}/status`, { status }).then(unwrap)
export const resolveRequest = (id) => patchParam(`/api/maintenance-requests/${id}/resolve`).then(unwrap)