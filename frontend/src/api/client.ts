import axios, { type InternalAxiosRequestConfig } from 'axios'

// Extend Axios config type to allow _retry property
declare module 'axios' {
  interface InternalAxiosRequestConfig {
    _retry?: boolean
  }
}

const normalizeApiBaseUrl = (value: string | undefined): string => {
  const fallback = 'http://localhost:8080/api/v1'
  if (!value || value.trim().length === 0) {
    return fallback
  }

  const trimmed = value.trim().replace(/\/+$/, '')
  if (trimmed.endsWith('/api/v1')) {
    return trimmed
  }
  if (trimmed.endsWith('/api')) {
    return `${trimmed}/v1`
  }
  return `${trimmed}/api/v1`
}

const apiBaseUrl = import.meta.env.DEV
  ? '/api/v1'
  : normalizeApiBaseUrl(import.meta.env.VITE_API_URL as string | undefined)

export const client = axios.create({
  baseURL: apiBaseUrl,
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

const shouldAttachOrgContext = (url?: string): boolean => {
  if (!url) return false
  return !url.includes('/auth/')
}

const isFilesEndpoint = (url?: string): boolean => {
  if (!url) return false
  return url.includes('/files')
}

export const normalizeRelativeApiPath = (url?: string): string | undefined => {
  if (!url) {
    return url
  }

  if (/^https?:\/\//i.test(url)) {
    return url
  }

  if (url === '/api/v1' || url === 'api/v1') {
    return '/'
  }

  if (url.startsWith('/api/v1/')) {
    return url.slice('/api/v1'.length)
  }

  if (url.startsWith('api/v1/')) {
    return `/${url.slice('api/v1/'.length)}`
  }

  if (url === '/api' || url === 'api') {
    return '/'
  }

  if (url.startsWith('/api/')) {
    return url.slice('/api'.length)
  }

  if (url.startsWith('api/')) {
    return `/${url.slice('api/'.length)}`
  }

  return url
}

const clearSessionTokens = () => {
  sessionStorage.removeItem('accessToken')
  sessionStorage.removeItem('refreshToken')
}

const summarizeResponseData = (data: unknown): string => {
  if (typeof data === 'string') {
    const compact = data.replace(/\s+/g, ' ').trim()
    return compact.length > 180 ? `String(${compact.length} chars)` : compact
  }

  if (Array.isArray(data)) {
    return `Array(${data.length})`
  }

  if (data && typeof data === 'object') {
    const payload = data as Record<string, unknown>
    if (Array.isArray(payload.content)) {
      return `Object{content:Array(${payload.content.length})}`
    }

    const keys = Object.keys(payload).slice(0, 6)
    return `Object{${keys.join(', ')}}`
  }

  return String(data)
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

  const refreshEndpoint = `${client.defaults.baseURL || '/api/v1'}/auth/refresh`

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
    config.url = normalizeRelativeApiPath(config.url)
    console.log(`[HTTP] 📤 ${config.method?.toUpperCase()} ${config.url}`)
    if (config.params) {
      console.log('[HTTP]    Params:', config.params)
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Handle 401 errors with automatic token refresh
let isRefreshing = false
let failedQueue: { resolve: (value: string) => void; reject: (reason: unknown) => void }[] = []

const processQueue = (error: unknown, token: string | null = null) => {
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

      const refreshToken = sessionStorage.getItem('refreshToken')

      if (!refreshToken) {
        sessionStorage.removeItem('accessToken')
        sessionStorage.removeItem('refreshToken')
        window.location.href = '/login'
        return Promise.reject(error)
      }

      try {
        const response = await axios.post(
          `${import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1'}/auth/refresh`,
          { refreshToken }
        )

        const { accessToken, refreshToken: newRefreshToken } = response.data

        sessionStorage.setItem('accessToken', accessToken)
        sessionStorage.setItem('refreshToken', newRefreshToken)
        originalRequest.headers.Authorization = `Bearer ${accessToken}`
        processQueue(null, accessToken)

        return client(originalRequest)
      } catch (refreshError) {
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
