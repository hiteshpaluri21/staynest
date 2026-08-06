import { request, toQuery } from '../http'

const BASE_URL = '/api/users'

export const getUsers = () => request(BASE_URL)

export const getUserById = (id) => request(`${BASE_URL}/${id}`)

export const getUserByEmail = (email) => request(`${BASE_URL}/email/${email}`)

// Active users of a given role, e.g. the housekeeping staff a task can be assigned to.
export const getUsersByRole = (role) => request(`${BASE_URL}/role/${role}`)

export const createUser = (data) => request(BASE_URL, { method: 'POST', body: JSON.stringify(data) })

export const updateUserStatus = (id, status) => request(`${BASE_URL}/${id}/status${toQuery({ status })}`, { method: 'PATCH' })

export const deleteUser = (id) => request(`${BASE_URL}/${id}`, { method: 'DELETE' })
