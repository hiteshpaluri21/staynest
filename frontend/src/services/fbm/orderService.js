import { request, toQuery } from '../http'

const BASE_URL = '/api/fb-orders'

export const getOrders = (params) => request(`${BASE_URL}${toQuery(params)}`)

export const getOrderById = (id) => request(`${BASE_URL}/${id}`)

export const placeOrder = (data) => request(BASE_URL, { method: 'POST', body: JSON.stringify(data) })

export const updateOrderStatus = (id, status) => request(`${BASE_URL}/${id}/status${toQuery({ status })}`, { method: 'PATCH' })

export const cancelOrder = (id) => request(`${BASE_URL}/${id}/cancel`, { method: 'PATCH' })
