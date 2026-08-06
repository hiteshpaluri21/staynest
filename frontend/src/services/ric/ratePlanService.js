import { request, toQuery } from '../http'

const BASE_URL = '/api/rate-plans'

export const getRatePlans = (params) => request(`${BASE_URL}${toQuery(params)}`)

export const getRatePlanById = (id) => request(`${BASE_URL}/${id}`)

export const createRatePlan = (data) => request(BASE_URL, { method: 'POST', body: JSON.stringify(data) })

export const updateRatePlan = (id, data) => request(`${BASE_URL}/${id}`, { method: 'PUT', body: JSON.stringify(data) })

export const deleteRatePlan = (id) => request(`${BASE_URL}/${id}`, { method: 'DELETE' })

export const updateRatePlanStatus = (id, status) => request(`${BASE_URL}/${id}/status${toQuery({ status })}`, { method: 'PATCH' })
