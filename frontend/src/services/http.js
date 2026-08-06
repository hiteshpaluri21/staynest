import { authError } from '../utils/session'

/*
 * The single HTTP client every service module uses.
 *
 * This was copy-pasted, byte for byte, into all 13 service files. Keeping one copy means
 * auth handling, the ApiResponse unwrapping and the error-message extraction are defined
 * once instead of needing 13 edits to stay in step.
 */

/**
 * Builds a query string, dropping empty values so `?status=` never appears.
 * Returns '' when there is nothing to add.
 */
export const toQuery = (params) => {
  const qs = new URLSearchParams()
  Object.entries(params || {}).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') qs.append(k, v)
  })
  const s = qs.toString()
  return s ? `?${s}` : ''
}

/**
 * Calls the API with the bearer token attached and returns the payload.
 *
 * Every service wraps its result in an ApiResponse envelope, so the `data` field is
 * unwrapped here and callers get the useful part. 401/403 become an authError so the
 * session layer can react; any other failure throws with the server's own message.
 */
export const request = async (url, options = {}) => {
  const headers = { 'Content-Type': 'application/json' }
  const token = localStorage.getItem('token') || ''
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(url, { ...options, headers })

  if (res.status === 401 || res.status === 403) {
    throw authError(res.status)
  }

  const ct = res.headers.get('content-type') || ''
  const payload = ct.includes('application/json') ? await res.json() : await res.text()

  if (!res.ok) {
    const msg = (payload && typeof payload === 'object' && (payload.message || payload.error))
      || (typeof payload === 'string' && payload)
      || `Request failed (${res.status})`
    throw new Error(msg)
  }

  return payload && typeof payload === 'object' && 'data' in payload ? payload.data : payload
}
