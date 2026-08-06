import { request, toQuery } from '../http'

const BASE_URL = '/api/rooms'

export const getRooms = (params) => request(`${BASE_URL}${toQuery(params)}`)

export const getRoomById = (id) => request(`${BASE_URL}/${id}`)

export const createRoom = (data) => request(BASE_URL, { method: 'POST', body: JSON.stringify(data) })

export const updateRoomStatus = (id, status) => request(`${BASE_URL}/${id}/status${toQuery({ status })}`, { method: 'PATCH' })

export const getAvailableRooms = (checkIn, checkOut) => request(`${BASE_URL}/available${toQuery({ checkIn, checkOut })}`)
