import axios, { type InternalAxiosRequestConfig } from 'axios'
import { getOrgNumber } from '@/shared/utils/orgContext'

// Extend Axios config type to allow _retry property
declare module 'axios' {
  interface AxiosRequestConfig {
    skipGlobalErrorLog?: boolean
  }

  interface InternalAxiosRequestConfig {
    _retry?: boolean
    skipGlobalErrorLog?: boolean
  }
}

export const client = axios.create({
  baseURL: import.meta.env.DEV ? '/api/v1' : (import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1'),
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
    if (config.params) {
    }

    if (shouldSkipAuthHeader(config.url)) {
      return config
    }

    let token = sessionStorage.getItem('accessToken')

    if (!token || isTokenExpiringSoon(token)) {
      token = await refreshAccessToken()
    }

    if (!token) {
      console.error('[HTTP]    ❌ No token available after refresh attempt')
      clearSessionTokens()
      window.location.href = '/login'
      return Promise.reject(new Error('Missing Bearer token for protected endpoint'))
    }

    config.headers.Authorization = `Bearer ${token}`

    if (shouldAttachOrgContext(config.url)) {
      const orgNumber = getOrgNumber()
      config.params = {
        ...(config.params || {}),
        orgNumber,
      }

      if (isFilesEndpoint(config.url)) {
        config.headers['X-Org-Number'] = String(orgNumber)
      }
    }

    return config
  },
  (error) => {
    console.error('[HTTP]    ❌ Request interceptor error:', error)
    return Promise.reject(error)
  }
)

// Handle 401 errors with automatic token refresh
let isRefreshing = false
let failedQueue: { resolve: (value: string) => void; reject: (reason: unknown) => void }[] = []
const suppressed500LogUrls = new Set<string>()

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
  (response) => {
    if (response.data) {
      if (Array.isArray(response.data)) {
      } else {
      }
    }
    return response
  },
  async (error) => {
    const originalRequest: InternalAxiosRequestConfig = error.config
    const status = error.response?.status
    const isSuppressed500 = status === 500 && originalRequest.skipGlobalErrorLog === true

    if (isSuppressed500) {
      const key = originalRequest.url || 'unknown-url'
      if (!suppressed500LogUrls.has(key)) {
        suppressed500LogUrls.add(key)
      }
    } else {
      console.error(`[HTTP] ❌ ${status || 'unknown'} ${originalRequest.url}`)
      if (error.response?.data) {
        console.error('[HTTP]    Error response:', error.response.data)
      } else if (error.message) {
        console.error('[HTTP]    Error:', error.message)
      }
    }

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
