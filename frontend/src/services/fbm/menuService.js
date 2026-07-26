import { api, unwrap, patchParam } from '../api'
export const getMenuItems = (params) => api.get('/api/menu-items', params).then(unwrap)
export const getMenuItemById = (id) => api.get(`/api/menu-items/${id}`).then(unwrap)
export const createMenuItem = (data) => api.post('/api/menu-items', data).then(unwrap)
export const updateAvailability = (id, value) => patchParam(`/api/menu-items/${id}/availability`, { value }).then(unwrap)
export const updatePrice = (id, value) => patchParam(`/api/menu-items/${id}/price`, { value }).then(unwrap)