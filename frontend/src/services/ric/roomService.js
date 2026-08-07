import { request, toQuery } from '../http'

const BASE_URL = '/api/rooms'

export const getRooms = (params) => request(`${BASE_URL}${toQuery(params)}`)

export const getRoomById = (id) => request(`${BASE_URL}/${id}`)

export const createRoom = (data) => request(BASE_URL, { method: 'POST', body: JSON.stringify(data) })

export const updateRoomStatus = (id, status) => request(`${BASE_URL}/${id}/status${toQuery({ status })}`, { method: 'PATCH' })

export const getAvailableRooms = (checkIn, checkOut) => request(`${BASE_URL}/available${toQuery({ checkIn, checkOut })}`)

/**
 * Resolves room ids to the room numbers guests and staff actually recognise, as a
 * `{ [roomId]: roomNumber }` map.
 *
 * Stays and tasks only carry `assignedRoomId`, a primary key, so every page showing one has
 * to translate it. Callers used to fetch the whole room list and swallow any failure, which
 * left them printing the raw id — so this falls back to fetching the handful of rooms the
 * list did not cover, one by one. Ids that cannot be resolved are simply absent from the map.
 */
export const getRoomNumbers = async (roomIds = []) => {
  const wanted = [...new Set(roomIds.filter(id => id != null))]
  if (wanted.length === 0) return {}

  const all = await getRooms().catch(() => [])
  const numbers = Object.fromEntries(
    (all || []).filter(r => r.roomNumber != null).map(r => [r.roomId, r.roomNumber])
  )

  const missing = wanted.filter(id => numbers[id] == null)
  const looked = await Promise.all(
    missing.map(id => getRoomById(id).then(r => [id, r?.roomNumber]).catch(() => [id, null]))
  )
  looked.forEach(([id, number]) => { if (number != null) numbers[id] = number })

  return numbers
}
