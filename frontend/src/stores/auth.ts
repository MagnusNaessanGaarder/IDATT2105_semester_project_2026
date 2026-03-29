/**
 * useAuthStore.js - Pinia store for autentisering.
 *
 * Sentraliserer all auth-logikk:
 * - Token-lagring i sessionStorage (oppgavekrav 4.7)
 * - Brukerinfo (username, role)
 * - Login/logout/register actions
 * - Computed properties for auth-status
 *
 * sessionStorage valgt over localStorage fordi:
 * - Tommes nar fanen lukkes (kortlevd sesjon)
 * - Ikke delt mellom faner (sikrere)
 * - Oppgaveteksten tillater dette eksplisitt
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

export const useAuthStore = defineStore('auth', () => {
  // ======== State ========
  const username = ref(sessionStorage.getItem('username') || null)
  const role = ref(sessionStorage.getItem('role') || null)
  const accessToken = ref(sessionStorage.getItem('accessToken') || null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const hasCheckedAuth = ref(false)

  // ======== Computed ========
  const isAuthenticated = computed(() => !!accessToken.value)
  const isAdmin = computed(() => role.value === 'ADMIN')
  const userDisplayName = computed(() => username.value || 'Gjest')
  const user = computed<AuthUser | null>(() => {
    if (!username.value) {
      return null
    }

    return {
      id: username.value,
      name: username.value,
      email: username.value,
      role: (role.value as AuthRole) ?? 'EMPLOYEE',
    }
  })

  // ======== Actions ========

  /**
   * Logger inn bruker og lagrer tokens i sessionStorage.
   */
  async function login(credentials: LoginCredentials) {
    loading.value = true
    error.value = null

    try {
      const response = await authApi.login(credentials)
      setAuthData(response)
      return response
    } catch (err) {
      error.value = parseError(err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * Registrerer ny bruker og logger inn automatisk.
   */
  async function register(userData: RegisterData) {
    loading.value = true
    error.value = null

    try {
      const response = await authApi.register(userData)
      setAuthData(response)
      return response
    } catch (err) {
      error.value = parseError(err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * Logger ut bruker og fjerner alle tokens.
   */
  function logout() {
    username.value = null
    role.value = null
    accessToken.value = null

    sessionStorage.removeItem('accessToken')
    sessionStorage.removeItem('refreshToken')
    sessionStorage.removeItem('username')
    sessionStorage.removeItem('role')
    hasCheckedAuth.value = true
  }

  /**
   * Sjekker om brukeren fortsatt er autentisert.
   * Kaller refresh hvis access token er utlopet.
   */
  async function checkAuth() {
    const token = sessionStorage.getItem('accessToken')
    if (!token) {
      logout()
      return false
    }

    try {
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

      const isExpired = payload.exp * 1000 < Date.now()

      if (isExpired) {
        const refreshToken = sessionStorage.getItem('refreshToken')
        if (!refreshToken) {
          logout()
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
      return false
    }
  }

  // ======== Hjelpefunksjoner ========

  function setAuthData(response: AuthResponse) {
    accessToken.value = response.accessToken
    username.value = response.username
    role.value = response.role

    sessionStorage.setItem('accessToken', response.accessToken)
    sessionStorage.setItem('refreshToken', response.refreshToken)
    sessionStorage.setItem('username', response.username)
    sessionStorage.setItem('role', response.role)
    hasCheckedAuth.value = true
  }

  function parseError(err: unknown): string {
    if (typeof err !== 'object' || err === null) {
      return 'Noe gikk galt. Prov igjen.'
    }

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
    }

    return 'Noe gikk galt. Prov igjen.'
  }

  function hasRole(...roles: AuthRole[]): boolean {
    if (!role.value) {
      return false
    }

    return roles.includes(role.value as AuthRole)
  }

  return {
    // State
    username,
    role,
    accessToken,
    loading,
    error,
    // Computed
    isAuthenticated,
    isAdmin,
    userDisplayName,
    user,
    hasCheckedAuth,
    // Actions
    login,
    register,
    logout,
    checkAuth,
    hasRole,
  }
})
