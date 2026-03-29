/**
 * Pinia store for authentication.
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/features/auth/api'
import type { RegisterData } from '@/features/auth/api'

type AuthRole = 'ADMIN' | 'MANAGER' | 'EMPLOYEE'

interface AuthUser {
  id: string
  name: string
  email: string
  role: AuthRole
}

interface JwtPayload {
  exp?: number
}

type LoginCredentials = Parameters<typeof authApi.login>[0]
type AuthResponse = Awaited<ReturnType<typeof authApi.login>>

// Custom error type for auth errors
interface AuthError {
  message: string
  code?: string
}

export const useAuthStore = defineStore('auth', () => {
  // Store
  const email = ref<string | null>(sessionStorage.getItem('email') || null)
  const role = ref<string | null>(sessionStorage.getItem('role') || null)
  const accessToken = ref<string | null>(sessionStorage.getItem('accessToken') || null)
  const loading = ref(false)
  const error = ref<AuthError | null>(null)
  const hasCheckedAuth = ref(false)

  // Computed
  const isAuthenticated = computed(() => !!accessToken.value)
  const isAdmin = computed(() => role.value === 'ADMIN')
  const userDisplayName = computed(() => {
    if (email.value) {
      return email.value.split('@')[0]
    }
    return 'Gjest'
  })
  const user = computed(() => {
    if (!email.value) return null
    return {
      id: '',
      name: email.value.split('@')[0],
      email: email.value,
      role: role.value,
    }
  })

  // Actions

  /**
   * Logs in user and stores tokens in sessionStorage.
   */
  async function login(credentials: { email: string; password: string }) {
    loading.value = true
    error.value = null

    try {
      const response = await authApi.login(credentials)
      setAuthData(response)
      return response
    } catch (err: any) {
      error.value = parseError(err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * Registers new user and logs in automatically.
   */
  async function register(userData: { email: string; password: string; name?: string }) {
    loading.value = true
    error.value = null

    try {
      const response = await authApi.register(userData)
      setAuthData(response)
      return response
    } catch (err: any) {
      error.value = parseError(err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * Logs out user and removes all tokens.
   */
  function logout() {
    email.value = null
    role.value = null
    accessToken.value = null

    sessionStorage.removeItem('accessToken')
    sessionStorage.removeItem('refreshToken')
    sessionStorage.removeItem('email')
    sessionStorage.removeItem('role')
    hasCheckedAuth.value = true
  }

  /**
   * Checks if user is still authenticated.
   * Calls refresh if access token is expired.
   */
  async function checkAuth() {
    const token = sessionStorage.getItem('accessToken')
    if (!token) {
      logout()
      hasCheckedAuth.value = true
      return false
    }

    // Decode JWT payload to check expiration
    try {
<<<<<<< HEAD
      const payload = decodeJwtPayload(token)
      if (!payload || !payload.exp) {
        logout()
        hasCheckedAuth.value = true
        return false
      }
      
=======
      const tokenPayload = token.split('.')[1]
      if (!tokenPayload) {
        logout()
        return false
      }

      const payload = JSON.parse(atob(tokenPayload)) as JwtPayload
      if (typeof payload.exp !== 'number') {
        logout()
        return false
      }

>>>>>>> cda3f39dd653208464109e44f6720c140db5c55c
      const isExpired = payload.exp * 1000 < Date.now()

      if (isExpired) {
        const refreshToken = sessionStorage.getItem('refreshToken')
        if (refreshToken) {
          const response = await authApi.refresh(refreshToken)
          setAuthData(response)
          hasCheckedAuth.value = true
          return true
        } else {
          logout()
          hasCheckedAuth.value = true
          return false
        }

        const response = await authApi.refresh(refreshToken)
        setAuthData(response)
        return true
      }
      hasCheckedAuth.value = true
      return true
    } catch {
      logout()
      hasCheckedAuth.value = true
      return false
    }
  }

  /**
   * Checks if user has any of the specified roles.
   */
  function hasRole(...roles: string[]): boolean {
    if (!role.value) return false
    return roles.includes(role.value)
  }

  // Helpers

  function decodeJwtPayload(token: string): { exp?: number } | null {
    try {
      const parts = token.split('.')
      if (parts.length !== 3) return null
      
      // Base64url decode (replace URL-safe chars and add padding)
      const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
      const padding = '='.repeat((4 - base64.length % 4) % 4)
      const decoded = atob(base64 + padding)
      
      return JSON.parse(decoded)
    } catch {
      return null
    }
  }

  function setAuthData(response: {
    accessToken: string
    refreshToken: string
    email: string
    role: string
  }) {
    accessToken.value = response.accessToken
    email.value = response.email
    role.value = response.role

    sessionStorage.setItem('accessToken', response.accessToken)
    sessionStorage.setItem('refreshToken', response.refreshToken)
    sessionStorage.setItem('email', response.email)
    sessionStorage.setItem('role', response.role)
    hasCheckedAuth.value = true
  }

  function parseError(err: any): AuthError {
    if (err.response?.data?.error) {
      return { message: err.response.data.error }
    }
<<<<<<< HEAD
    if (err.response?.data?.fieldErrors) {
      return { message: Object.values(err.response.data.fieldErrors).join(', ') }
=======

    const maybeErr = err as {
      response?: {
        data?: {
          error?: string
          fieldErrors?: Record<string, string>
        }
      }
    }

    if (maybeErr.response?.data?.error) {
      return maybeErr.response.data.error
    }

    if (maybeErr.response?.data?.fieldErrors) {
      return Object.values(maybeErr.response.data.fieldErrors).join(', ')
>>>>>>> cda3f39dd653208464109e44f6720c140db5c55c
    }
    if (err.message) {
      return { message: err.message }
    }
    return { message: 'Something went wrong. Please try again.' }
  }

  return {
    // State
    email,
    role,
    accessToken,
    loading,
    error,
    hasCheckedAuth,
    // Computed
    isAuthenticated,
    isAdmin,
    userDisplayName,
    user,
    // Actions
    login,
    register,
    logout,
    checkAuth,
    hasRole,
  }
})
