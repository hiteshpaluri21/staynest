const BASE = '' // Vite proxy handles /api -> http://localhost:8080

function getToken() {
  return localStorage.getItem('token') || ''
}

export class ApiError extends Error {
  constructor(message, status, payload) {
    super(message)
    this.status = status
    this.payload = payload
  }
}

async function request(method, url, { body, params, isFormData = false } = {}) {
  let fullUrl = BASE + url
  if (params) {
    const qs = new URLSearchParams()
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') qs.append(k, v)
    })
    const s = qs.toString()
    if (s) fullUrl += `?${s}`
  }

  const headers = {}
  if (!isFormData) headers['Content-Type'] = 'application/json'
  const tk = getToken()
  if (tk) headers['Authorization'] = `Bearer ${tk}`

  const res = await fetch(fullUrl, {
    method,
    headers,
    body: body === undefined ? undefined : (isFormData ? body : JSON.stringify(body)),
  })

  if (res.status === 401) {
    localStorage.removeItem('token')
    localStorage.removeItem('staynest_auth')
    if (!window.location.pathname.startsWith('/login')) {
      window.location.href = '/login'
    }
    throw new ApiError('Unauthorized', 401)
  }

  let payload = null
  const ct = res.headers.get('content-type') || ''
  if (ct.includes('application/json')) {
    payload = await res.json()
  } else {
    payload = await res.text()
  }

  if (!res.ok) {
    const msg =
      (payload && typeof payload === 'object' && (payload.message || payload.error)) ||
      (typeof payload === 'string' && payload) ||
      `Request failed (${res.status})`
    throw new ApiError(msg, res.status, payload)
  }
  return payload
}

export const api = {
  get: (url, params) => request('GET', url, { params }),
  post: (url, body) => request('POST', url, { body }),
  put: (url, body) => request('PUT', url, { body }),
  patch: (url, body) => request('PATCH', url, { body }),
  delete: (url) => request('DELETE', url),
}

// Helper to unwrap the ApiResponse<T> envelope backend returns ({success,message,data,...})
export const unwrap = (resp) => (resp && typeof resp === 'object' && 'data' in resp ? resp.data : resp)

// Helper for PATCH-by-query-param endpoints
export const patchParam = (url, params) => request('PATCH', url, { params })