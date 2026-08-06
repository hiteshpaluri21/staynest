import { request, toQuery } from '../http'

const BASE_URL = '/api/maintenance-requests'

export const getRequests = (params) => request(`${BASE_URL}${toQuery(params)}`)

export const getRequestById = (id) => request(`${BASE_URL}/${id}`)

export const reportIssue = (data) => request(BASE_URL, { method: 'POST', body: JSON.stringify(data) })

export const updateRequestStatus = (id, status) => request(`${BASE_URL}/${id}/status${toQuery({ status })}`, { method: 'PATCH' })

export const resolveRequest = (id) => request(`${BASE_URL}/${id}/resolve`, { method: 'PATCH' })
