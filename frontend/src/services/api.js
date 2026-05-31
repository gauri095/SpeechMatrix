import axios from 'axios'

/**
 * Central Axios instance for all API calls.
 *
 * Base URL:
 *   - Dev:  empty string → Vite proxy forwards /api/* to :8080
 *   - Prod: VITE_API_BASE_URL from .env.production
 *
 * Interceptors:
 *   request  → inject JWT token from localStorage
 *   response → normalise errors, auto-logout on 401
 */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 30_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// ── Request interceptor — attach JWT ──────────────────────────────────────────
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // Forward any request ID for debugging
    const requestId = crypto.randomUUID().replace(/-/g, '').slice(0, 12)
    config.headers['X-Request-ID'] = requestId
    return config
  },
  (error) => Promise.reject(error)
)

// ── Response interceptor — normalise errors ───────────────────────────────────
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status  = error.response?.status
    const apiError = error.response?.data   // ApiError shape from Spring

    // 401 — token expired or invalid → clear auth and redirect
    if (status === 401) {
      const isLoginPage = window.location.pathname === '/login'
      if (!isLoginPage) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        window.location.href = '/login?reason=session_expired'
      }
    }

    // Attach the ApiError to the thrown error for easy access in catch blocks
    // e.g.  catch (e) { console.log(e.apiError.message) }
    if (apiError) {
      error.apiError   = apiError
      error.apiMessage = apiError.message ?? 'An error occurred'
      error.apiHint    = apiError.hint    ?? null
    }

    return Promise.reject(error)
  }
)

export default api