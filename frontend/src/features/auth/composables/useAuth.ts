/**
 * useAuth.ts - Composable for auth-relatert funksjonalitet i komponenter.
 *
 * Bruk i enhver komponent:
 *   const { isAuthenticated, isAdmin, logout } = useAuth()
 */
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'

export function useAuth() {
  const authStore = useAuthStore()
  const router = useRouter()
  const { isAuthenticated, isAdmin, username, role, loading, error } = storeToRefs(authStore)

  /**
   * Logger ut og redirecter til login-siden.
   */
  function logoutAndRedirect() {
    authStore.logout()
    router.push('/login')
  }

  /**
   * Decoder JWT payload uten å verifisere signatur.
   * Nyttig for å vise brukerinfo på frontend.
   * NB: Aldri stol på dette for sikkerhet — backend validerer alltid.
   */
  function decodeToken(token: string | null) {
    if (!token) return null
    try {
      const payload = token.split('.')[1]
      return JSON.parse(atob(payload))
    } catch {
      return null
    }
  }

  /**
   * Sjekker om access token utløper innen gitt antall sekunder.
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
    username,
    role,
    loading,
    error,
    logoutAndRedirect,
    decodeToken,
    tokenExpiresWithin,
  }
}
