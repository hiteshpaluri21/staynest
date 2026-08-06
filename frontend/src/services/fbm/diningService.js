import { request, toQuery } from '../http'

const BASE_URL = '/api/dining-reservations'

export const getDiningReservations = (params) => request(`${BASE_URL}${toQuery(params)}`)

export const getDiningReservationById = (id) => request(`${BASE_URL}/${id}`)

export const createDiningReservation = (data) => request(BASE_URL, { method: 'POST', body: JSON.stringify(data) })

export const updateDiningStatus = (id, status) => request(`${BASE_URL}/${id}/status${toQuery({ status })}`, { method: 'PATCH' })

export const cancelDiningReservation = (id) => request(`${BASE_URL}/${id}/cancel`, { method: 'PATCH' })
