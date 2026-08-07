import { request, toQuery } from '../http'

const BASE_URL = '/api/menu-items'

export const getMenuItems = (params) => request(`${BASE_URL}${toQuery(params)}`)

export const getMenuItemById = (id) => request(`${BASE_URL}/${id}`)

export const createMenuItem = (data) => request(BASE_URL, { method: 'POST', body: JSON.stringify(data) })

export const updateMenuItem = (id, data) => request(`${BASE_URL}/${id}`, { method: 'PUT', body: JSON.stringify(data) })

// Refused by the server while the dish is on an order the kitchen has not finished; the
// caller shows that message and can fall back to marking it unavailable.
export const deleteMenuItem = (id) => request(`${BASE_URL}/${id}`, { method: 'DELETE' })

export const updateAvailability = (id, value) => request(`${BASE_URL}/${id}/availability${toQuery({ value })}`, { method: 'PATCH' })

export const updatePrice = (id, value) => request(`${BASE_URL}/${id}/price${toQuery({ value })}`, { method: 'PATCH' })
