import axios, { type InternalAxiosRequestConfig } from 'axios'

// Extend Axios config type to allow _retry property
declare module 'axios' {
  interface InternalAxiosRequestConfig {
    _retry?: boolean
  }
}

export const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

interface JwtPayload {
  exp?: number
}

const shouldSkipAuthHeader = (url?: string): boolean => {
  if (!url) return false
  return url.includes('/auth/login') || url.includes('/auth/register') || url.includes('/auth/refresh')
}

const clearSessionTokens = () => {
  sessionStorage.removeItem('accessToken')
  sessionStorage.removeItem('refreshToken')
}

const parseJwtPayload = (token: string): JwtPayload | null => {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null

    const payload = parts[1]
    if (!payload) return null

    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const padding = '='.repeat((4 - (base64.length % 4)) % 4)
    const decoded = atob(base64 + padding)

    return JSON.parse(decoded) as JwtPayload
  } catch {
    return null
  }
}

const isTokenExpiringSoon = (token: string, skewSeconds = 30): boolean => {
  const payload = parseJwtPayload(token)
  if (!payload || typeof payload.exp !== 'number') {
    return true
  }

  const expiresAtMs = payload.exp * 1000
  return expiresAtMs - Date.now() < skewSeconds * 1000
}

let refreshPromise: Promise<string | null> | null = null

const refreshAccessToken = async (): Promise<string | null> => {
  if (refreshPromise) {
    return refreshPromise
  }

  const refreshToken = sessionStorage.getItem('refreshToken')
  if (!refreshToken) {
    return null
  }

  const refreshEndpoint = `${client.defaults.baseURL || '/api'}/auth/refresh`

  refreshPromise = axios
    .post(refreshEndpoint, { refreshToken })
    .then((response) => {
      const { accessToken, refreshToken: newRefreshToken } = response.data || {}

      if (!accessToken || !newRefreshToken) {
        throw new Error('Invalid refresh response')
      }

      sessionStorage.setItem('accessToken', accessToken)
      sessionStorage.setItem('refreshToken', newRefreshToken)
      return accessToken as string
    })
    .catch(() => {
      clearSessionTokens()
      return null
    })
    .finally(() => {
      refreshPromise = null
    })

  return refreshPromise
}

// Add JWT token to all requests
client.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    if (shouldSkipAuthHeader(config.url)) {
      return config
    }

    let token = sessionStorage.getItem('accessToken')

    if (!token || isTokenExpiringSoon(token)) {
      token = await refreshAccessToken()
    }

    if (!token) {
      clearSessionTokens()
      window.location.href = '/login'
      return Promise.reject(new Error('Missing Bearer token for protected endpoint'))
    }

    config.headers.Authorization = `Bearer ${token}`

    return config
  },
  (error) => Promise.reject(error)
)

// Handle 401 errors with automatic token refresh
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
    const originalRequest: InternalAxiosRequestConfig = error.config

    if (error.response?.status === 401 && !originalRequest._retry) {
      // Skip refresh for auth endpoints
      if (originalRequest.url?.includes('/auth/')) {
        return Promise.reject(error)
      }

      // Queue requests while refreshing
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

      const accessToken = await refreshAccessToken()

      if (!accessToken) {
        clearSessionTokens()
        window.location.href = '/login'
        return Promise.reject(error)
      }

      try {
        originalRequest.headers.Authorization = `Bearer ${accessToken}`
        processQueue(null, accessToken)

        return client(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError, null)
        clearSessionTokens()
        window.location.href = '/login'
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  }
)
