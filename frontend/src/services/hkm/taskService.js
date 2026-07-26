import { api, unwrap, patchParam } from '../api'
export const getTasks = (params) => api.get('/api/housekeeping-tasks', params).then(unwrap)
export const getTaskById = (id) => api.get(`/api/housekeeping-tasks/${id}`).then(unwrap)
export const createTask = (data) => api.post('/api/housekeeping-tasks', data).then(unwrap)
export const assignTask = (id, staffId) => patchParam(`/api/housekeeping-tasks/${id}/assign`, { staffId }).then(unwrap)
export const updateTaskStatus = (id, status) => patchParam(`/api/housekeeping-tasks/${id}/status`, { status }).then(unwrap)