import { api, unwrap, patchParam } from '../api'

export const getUsers = () => api.get('/api/users').then(unwrap)
export const getUserById = (id) => api.get(`/api/users/${id}`).then(unwrap)
export const getUserByEmail = (email) => api.get(`/api/users/email/${email}`).then(unwrap)
export const createUser = (data) => api.post('/api/users', data).then(unwrap)
export const updateUserStatus = (id, status) => patchParam(`/api/users/${id}/status`, { status }).then(unwrap)
export const deleteUser = (id) => api.delete(`/api/users/${id}`)