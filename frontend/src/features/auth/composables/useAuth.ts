import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'

interface DecodedToken {
  exp?: number
  [key: string]: unknown
}

export function useAuth() {
  const authStore = useAuthStore()
  const router = useRouter()
  const { isAuthenticated, isAdmin, userDisplayName, role, loading, error } = storeToRefs(authStore)

  /**
   * Logger ut og redirecter til login-siden.
   */
  function logoutAndRedirect() {
    authStore.logout()
    router.push('/login')
  }

  /**
   * Decoder JWT payload uten a verifisere signatur.
   * Nyttig for a vise brukerinfo pa frontend.
   * NB: Aldri stol pa dette for sikkerhet, backend validerer alltid.
   */
  function decodeToken(token: string | null): DecodedToken | null {
    if (!token) return null

    try {
      const payload = token.split('.')[1]
      if (!payload) {
        return null
      }

      return JSON.parse(atob(payload)) as DecodedToken
    } catch {
      return null
    }
  }

  /**
   * Sjekker om access token utloper innen gitt antall sekunder.
   */
  function tokenExpiresWithin(seconds = 60) {
    const token = sessionStorage.getItem('accessToken')
    if (!token) return true

    const payload = decodeToken(token)
    if (!payload?.exp) return true

    const expiresAt = payload.exp * 1000
    return expiresAt - Date.now() < seconds * 1000
  }

  return {
    isAuthenticated,
    isAdmin,
    userDisplayName,
    role,
    loading,
    error,
    logoutAndRedirect,
    decodeToken,
    tokenExpiresWithin,
  }
}
