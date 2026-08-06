import { request, toQuery } from '../http'

const BASE_URL = '/api/stay-records'

export const checkIn = (data) => request(`${BASE_URL}/checkin`, { method: 'POST', body: JSON.stringify(data) })

export const getStays = (params) => request(`${BASE_URL}${toQuery(params)}`)

export const getStayById = (id) => request(`${BASE_URL}/${id}`)

export const postFolioItem = (stayId, data) => request(`${BASE_URL}/${stayId}/folio-items`, { method: 'POST', body: JSON.stringify(data) })

export const updateFolioItem = (stayId, folioItemId, data) => request(`${BASE_URL}/${stayId}/folio-items/${folioItemId}`, { method: 'PUT', body: JSON.stringify(data) })

export const getFolioItems = (stayId) => request(`/api/folio-items/stay/${stayId}`)

// housekeepingStaffId owns the post-checkout cleaning task raised automatically by the backend.
export const checkOut = (stayId, housekeepingStaffId) =>
  request(`${BASE_URL}/${stayId}/checkout${toQuery({ housekeepingStaffId })}`, { method: 'POST' })
