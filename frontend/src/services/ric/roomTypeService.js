import { request, toQuery } from '../http'

const BASE_URL = '/api/room-types'

export const getRoomTypes = () => request(BASE_URL)

export const getRoomTypeById = (id) => request(`${BASE_URL}/${id}`)

export const createRoomType = (data) => request(BASE_URL, { method: 'POST', body: JSON.stringify(data) })

export const updateRoomType = (id, data) => request(`${BASE_URL}/${id}`, { method: 'PUT', body: JSON.stringify(data) })

export const updateRoomTypeStatus = (id, status) => request(`${BASE_URL}/${id}/status${toQuery({ status })}`, { method: 'PATCH' })
