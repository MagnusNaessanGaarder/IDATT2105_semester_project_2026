/**
 * useErrorHandler - Composable for global feilhåndtering
 * 
 * Brukes for å håndtere og vise feilmeldinger fra API.
 * Mapper HTTP-statuskoder til brukervennlige meldinger.
 * 
 * Eksempel:
 * const { handleError, clearError, globalError } = useErrorHandler()
 * 
 * try {
 *   await api.save()
 * } catch (error) {
 *   handleError(error, 'saveData')
 * }
 */
import { ref } from 'vue'

const globalError = ref<string | null>(null)

export function useErrorHandler() {
  function handleError(error: any, context: string = '') {
    const status = error.response?.status
    const message = error.response?.data?.message

    const errorMap: Record<number, string> = {
      400: message ?? 'Ugyldig forespørsel',
      403: 'Du har ikke tilgang til denne handlingen',
      404: 'Ressursen ble ikke funnet',
      409: message ?? 'Konflikt — ressursen eksisterer allerede',
      422: message ?? 'Valideringsfeil fra server',
      500: 'Serverfeil — prøv igjen senere',
    }

    const userMessage = errorMap[status] ?? message ?? `Noe gikk galt${context ? ` (${context})` : ''}`
    globalError.value = userMessage

    console.error(`[${context}]`, error)

    return userMessage
  }

  function clearError() {
    globalError.value = null
  }

  return { globalError, handleError, clearError }
}