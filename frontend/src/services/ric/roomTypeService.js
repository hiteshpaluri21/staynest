import { api, unwrap, patchParam } from '../api'
export const getRoomTypes = () => api.get('/api/room-types').then(unwrap)
export const getRoomTypeById = (id) => api.get(`/api/room-types/${id}`).then(unwrap)
export const createRoomType = (data) => api.post('/api/room-types', data).then(unwrap)
export const updateRoomTypeStatus = (id, status) => patchParam(`/api/room-types/${id}/status`, { status }).then(unwrap)