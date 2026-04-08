/**
 * useApi - Composable for API-kall med loading/error-håndtering
 * 
 * Brukes for å håndtere asynkrone API-kall med automatisk loading-tilstand
 * og feilhåndtering. Returnerer data, isLoading, error og execute-funksjon.
 * 
 * Eksempel:
 * const { data: users, isLoading, error, execute: fetchUsers } = useApi(() => api.getUsers())
 * 
 * // I template:
 * <BaseSpinner v-if="isLoading" />
 * <ErrorMessage v-else-if="error" :message="error" />
 * <UserList v-else :users="data" />
 */
import { ref } from 'vue'

export function useApi<T, Args extends unknown[] = unknown[]>(
  apiFn: (...args: Args) => Promise<T>
) {
  const data = ref<T | null>(null)
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  async function execute(...args: Args): Promise<T | null> {
    isLoading.value = true
    error.value = null
    try {
      const result = await apiFn(...args)
      data.value = result
      return result
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      error.value = err.response?.data?.message ?? 'Noe gikk galt'
      throw e
    } finally {
      isLoading.value = false
    }
  }

  function reset() {
    data.value = null
    error.value = null
    isLoading.value = false
  }

  return { data, isLoading, error, execute, reset }
}