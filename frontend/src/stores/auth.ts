/**
 * Pinia store for autentisering.
 * Sentraliserer all auth-logikk
 * Token-lagring i sessionStorage (oppgavekrav 4.7)
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/features/auth/api'

export const useAuthStore = defineStore('auth', () => {
  const email = ref(sessionStorage.getItem('email') || null)
  const role = ref(sessionStorage.getItem('role') || null)
  const accessToken = ref(sessionStorage.getItem('accessToken') || null)
  const loading = ref(false)
  const error = ref<Error | null>(null)
  const hasCheckedAuth = ref(false)

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
      name: userDisplayName.value,
      email: email.value,
      role: role.value,
    }
  })

  /**
   * Logger inn bruker og lagrer tokens i sessionStorage.
   */
  async function login(credentials: { email: string; password: string }) {
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
  async function register(userData: {
    fullName: string
    email: string
    password: string
    phone?: string
  }) {
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
    email.value = null
    role.value = null
    accessToken.value = null

    sessionStorage.removeItem('accessToken')
    sessionStorage.removeItem('refreshToken')
    sessionStorage.removeItem('email')
    sessionStorage.removeItem('role')
  }

  /**
   * Sjekker om brukeren fortsatt er autentisert.
   * Kaller refresh hvis access token er utløpt.
   */
  async function checkAuth() {
    hasCheckedAuth.value = true
    const token = sessionStorage.getItem('accessToken')
    if (!token) {
      logout()
      return false
    }

    // Decode JWT payload for å sjekke utløpstid
    try {
      const payloadPart = token.split('.')[1]
      if (!payloadPart) {
        logout()
        return false
      }
      const payload = JSON.parse(atob(payloadPart))
      const isExpired = payload.exp * 1000 < Date.now()

      if (isExpired) {
        const refreshToken = sessionStorage.getItem('refreshToken')
        if (refreshToken) {
          const response = await authApi.refresh(refreshToken)
          setAuthData(response)
          return true
        } else {
          logout()
          return false
        }
      }
      return true
    } catch {
      logout()
      return false
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
  }

  function parseError(err: any) {
    if (err.response?.data?.error) {
      return err.response.data.error
    }
    if (err.response?.data?.fieldErrors) {
      return Object.values(err.response.data.fieldErrors).join(', ')
    }
    return 'Noe gikk galt. Prøv igjen.'
  }

  function hasRole(...allowedRoles: string[]) {
    return allowedRoles.includes(role.value || '')
  }

  return {
    email,
    role,
    accessToken,
    loading,
    error,
    hasCheckedAuth,
    isAuthenticated,
    isAdmin,
    userDisplayName,
    user,
    login,
    register,
    logout,
    checkAuth,
    hasRole,
  }
})
