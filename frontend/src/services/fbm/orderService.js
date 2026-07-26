import { api, unwrap, patchParam } from '../api'
export const getOrders = (params) => api.get('/api/fb-orders', params).then(unwrap)
export const getOrderById = (id) => api.get(`/api/fb-orders/${id}`).then(unwrap)
export const placeOrder = (data) => api.post('/api/fb-orders', data).then(unwrap)
export const updateOrderStatus = (id, status) => patchParam(`/api/fb-orders/${id}/status`, { status }).then(unwrap)
export const cancelOrder = (id) => patchParam(`/api/fb-orders/${id}/cancel`).then(unwrap)