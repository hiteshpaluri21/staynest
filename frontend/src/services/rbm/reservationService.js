import { request, toQuery } from '../http'

const BASE_URL = '/api/reservations'

export const getReservations = (params) => request(`${BASE_URL}${toQuery(params)}`)

export const getReservationById = (id) => request(`${BASE_URL}/${id}`)

export const createReservation = (data) => request(BASE_URL, { method: 'POST', body: JSON.stringify(data) })

export const cancelReservation = (id) => request(`${BASE_URL}/${id}/cancel`, { method: 'PATCH' })

export const updateReservationStatus = (id, status) => request(`${BASE_URL}/${id}/status${toQuery({ status })}`, { method: 'PATCH' })

export const getUpcoming = (date) => request(`${BASE_URL}/upcoming${toQuery({ date })}`)
