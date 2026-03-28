/**
 * client.ts - Axios-instans med JWT interceptors.
 *
 * Request interceptor:  Legger til "Authorization: Bearer <token>" på alle requests.
 * Response interceptor: Fanger 401, prøver token refresh, re-sender original request.
 *
 * OWASP: Token lagres i sessionStorage (kortlevd, per-tab).
 */
import axios from 'axios'

export const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// ======== Request Interceptor ========
// Legger til JWT token på alle utgående requests
client.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ======== Response Interceptor ========
// Håndterer 401 (utløpt token) med automatisk refresh
let isRefreshing = false
let failedQueue: { resolve: (value: string) => void; reject: (reason: any) => void }[] = []

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((promise) => {
    if (error) {
      promise.reject(error)
    } else if (token) {
      promise.resolve(token)
    }
  })
  failedQueue = []
}

client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    // Hvis 401 og ikke allerede prøvd refresh
    if (error.response?.status === 401 && !originalRequest._retry) {
      // Ikke prøv refresh på auth-endepunkter
      if (originalRequest.url?.includes('/auth/')) {
        return Promise.reject(error)
      }

      // Hvis allerede holder på å refreshe, legg i kø
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            return client(originalRequest)
          })
          .catch((err) => Promise.reject(err))
      }

      originalRequest._retry = true
      isRefreshing = true

      const refreshToken = sessionStorage.getItem('refreshToken')

      if (!refreshToken) {
        // Ingen refresh token - logg ut
        sessionStorage.removeItem('accessToken')
        sessionStorage.removeItem('refreshToken')
        window.location.href = '/login'
        return Promise.reject(error)
      }

      try {
        const response = await axios.post(
          `${import.meta.env.VITE_API_URL || 'http://localhost:8080/api'}/auth/refresh`,
          { refreshToken }
        )

        const { accessToken, refreshToken: newRefreshToken } = response.data

        sessionStorage.setItem('accessToken', accessToken)
        sessionStorage.setItem('refreshToken', newRefreshToken)

        // Oppdater header og re-send original request
        originalRequest.headers.Authorization = `Bearer ${accessToken}`
        processQueue(null, accessToken)

        return client(originalRequest)
      } catch (refreshError) {
        // Refresh feilet - logg ut
        processQueue(refreshError, null)
        sessionStorage.removeItem('accessToken')
        sessionStorage.removeItem('refreshToken')
        window.location.href = '/login'
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  }
)
