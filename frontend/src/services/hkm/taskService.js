import { request, toQuery } from '../http'

const BASE_URL = '/api/housekeeping-tasks'

export const getTasks = (params) => request(`${BASE_URL}${toQuery(params)}`)

export const getTaskById = (id) => request(`${BASE_URL}/${id}`)

export const createTask = (data) => request(BASE_URL, { method: 'POST', body: JSON.stringify(data) })

export const assignTask = (id, staffId) => request(`${BASE_URL}/${id}/assign${toQuery({ staffId })}`, { method: 'PATCH' })

export const updateTaskStatus = (id, status) => request(`${BASE_URL}/${id}/status${toQuery({ status })}`, { method: 'PATCH' })
