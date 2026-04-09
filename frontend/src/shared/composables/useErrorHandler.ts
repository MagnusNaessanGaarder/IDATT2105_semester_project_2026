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

type ErrorShape = {
  response?: {
    status?: number
    data?: {
      message?: string
    }
  }
}

export function useErrorHandler() {
  function handleError(error: unknown, context: string = '') {
    const typedError = error as ErrorShape
    const status = typedError.response?.status
    const message = typedError.response?.data?.message

    const errorMap: Record<number, string> = {
      400: message ?? 'Ugyldig forespørsel',
      403: 'Du har ikke tilgang til denne handlingen',
      404: 'Ressursen ble ikke funnet',
      409: message ?? 'Konflikt - ressursen eksisterer allerede',
      422: message ?? 'Valideringsfeil fra server',
      500: 'Serverfeil - prøv igjen senere',
    }

    const mappedMessage = typeof status === 'number' ? errorMap[status] : undefined
    const userMessage = mappedMessage ?? message ?? `Noe gikk galt${context ? ` (${context})` : ''}`
    globalError.value = userMessage

    return userMessage
  }

  function clearError() {
    globalError.value = null
  }

  return { globalError, handleError, clearError }
}
