import { request, toQuery } from '../http'

const BASE_URL = '/api/guests'

export const getGuests = () => request(BASE_URL)

export const getGuestById = (id) => request(`${BASE_URL}/${id}`)

// The signed-in user's own guest profile, resolved from the JWT. Use the guestId from here —
// it is NOT the same as the IAM userId, and passing userId as guestId silently returns nothing.
export const getMyGuestProfile = () => request(`${BASE_URL}/me`)

export const getGuestByEmail = (email) => request(`${BASE_URL}/email/${email}`)

export const createGuest = (data) => request(BASE_URL, { method: 'POST', body: JSON.stringify(data) })

export const updateGuest = (id, data) => request(`${BASE_URL}/${id}`, { method: 'PUT', body: JSON.stringify(data) })

export const updateLoyaltyTier = (id, tier) => request(`${BASE_URL}/${id}/loyalty${toQuery({ tier })}`, { method: 'PATCH' })

export const blacklistGuest = (id) => request(`${BASE_URL}/${id}/blacklist`, { method: 'PATCH' })
