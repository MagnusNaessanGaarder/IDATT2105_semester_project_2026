/**
 * usePermissions - Composable for rollebasert tilgangskontroll
 * 
 * Brukes for å sjekke om innlogget bruker har tilgang til funksjonalitet.
 * Returnerer computed properties for ulike tillatelser.
 * 
 * Eksempel:
 * const { can } = usePermissions()
 * 
 * <BaseButton v-if="can.viewReports">Se rapporter</BaseButton>
 * <BaseButton v-if="can.manageUsers">Administrer brukere</BaseButton>
 */
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

export function usePermissions() {
  const auth = useAuthStore()

  const can = {
    viewChecklists: computed(() => auth.isAuthenticated),
    completeChecklist: computed(() => auth.hasRole('EMPLOYEE', 'MANAGER', 'ADMIN')),

    createDeviation: computed(() => auth.isAuthenticated),
    resolveDeviation: computed(() => auth.hasRole('MANAGER', 'ADMIN')),
    deleteDeviation: computed(() => auth.isAdmin),

    viewReports: computed(() => auth.hasRole('MANAGER', 'ADMIN')),
    exportReports: computed(() => auth.hasRole('MANAGER', 'ADMIN')),

    manageUsers: computed(() => auth.isAdmin),
    viewAllTenants: computed(() => auth.isAdmin),
  }

  return { can }
}