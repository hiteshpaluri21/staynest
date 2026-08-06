import { request, toQuery } from '../http'

const BASE_URL = '/api/audit-logs'

/*
 * The activity trail every service writes to through its AuditRecorder.
 *
 * Only the unfiltered listing is paged server-side; the by-user, by-action and
 * by-range endpoints each return a plain list, so the page paginates those itself.
 */

/** One page of the whole trail, newest first. Resolves to a Spring `Page`. */
export const getAuditLogs = ({ page = 0, size = 20 } = {}) =>
  request(`${BASE_URL}${toQuery({ page, size, sort: 'timestamp,desc' })}`)

export const getAuditLogsByUser = (userId) => request(`${BASE_URL}/user/${userId}`)

export const getAuditLogsByAction = (action) => request(`${BASE_URL}/action/${action}`)

/** `start`/`end` are ISO local date-times, e.g. 2026-08-07T00:00:00. */
export const getAuditLogsByRange = (start, end) =>
  request(`${BASE_URL}/range${toQuery({ start, end })}`)
