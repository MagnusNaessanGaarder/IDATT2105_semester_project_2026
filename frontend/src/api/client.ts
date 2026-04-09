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

// Add JWT token to all requests
client.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = sessionStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
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
