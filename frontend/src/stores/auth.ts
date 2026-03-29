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
 * - Tømmes når fanen lukkes (kortlevd sesjon)
 * - Ikke delt mellom faner (sikrere)
 * - Oppgaveteksten tillater dette eksplisitt
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/features/auth/api'

export const useAuthStore = defineStore('auth', () => {
  // ======== State ========
  const username = ref(sessionStorage.getItem('username') || null)
  const role = ref(sessionStorage.getItem('role') || null)
  const accessToken = ref(sessionStorage.getItem('accessToken') || null)
  const loading = ref(false)
  const error = ref(null)

  // ======== Computed ========
  const isAuthenticated = computed(() => !!accessToken.value)
  const isAdmin = computed(() => role.value === 'ADMIN')
  const userDisplayName = computed(() => username.value || 'Gjest')

  // ======== Actions ========

  /**
   * Logger inn bruker og lagrer tokens i sessionStorage.
   */
  async function login(credentials) {
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
  async function register(userData) {
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
  }

  /**
   * Sjekker om brukeren fortsatt er autentisert.
   * Kaller refresh hvis access token er utløpt.
   */
  async function checkAuth() {
    const token = sessionStorage.getItem('accessToken')
    if (!token) {
      logout()
      return false
    }

    // Decode JWT payload for å sjekke utløpstid
    try {
      const payload = JSON.parse(atob(token.split('.')[1]))
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

  // ======== Hjelpefunksjoner ========

  function setAuthData(response) {
    accessToken.value = response.accessToken
    username.value = response.username
    role.value = response.role

    sessionStorage.setItem('accessToken', response.accessToken)
    sessionStorage.setItem('refreshToken', response.refreshToken)
    sessionStorage.setItem('username', response.username)
    sessionStorage.setItem('role', response.role)
  }

  function parseError(err) {
    if (err.response?.data?.error) {
      return err.response.data.error
    }
    if (err.response?.data?.fieldErrors) {
      return Object.values(err.response.data.fieldErrors).join(', ')
    }
    return 'Noe gikk galt. Prøv igjen.'
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
    // Actions
    login,
    register,
    logout,
    checkAuth,
  }
})
