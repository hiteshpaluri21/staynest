import { api, unwrap, patchParam } from '../api'
export const getRooms = (params) => api.get('/api/rooms', params).then(unwrap)
export const getRoomById = (id) => api.get(`/api/rooms/${id}`).then(unwrap)
export const createRoom = (data) => api.post('/api/rooms', data).then(unwrap)
export const updateRoomStatus = (id, status) => patchParam(`/api/rooms/${id}/status`, { status }).then(unwrap)
export const getAvailableRooms = (checkIn, checkOut) => api.get('/api/rooms/available', { checkIn, checkOut }).then(unwrap)