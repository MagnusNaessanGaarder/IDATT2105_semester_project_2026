import { ref } from 'vue'

export function useApi<T>(apiFn: (...args: any[]) => Promise<T>) {
  const data = ref<T | null>(null)
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  async function execute(...args: any[]): Promise<T | null> {
    isLoading.value = true
    error.value = null
    try {
      const result = await apiFn(...args)
      data.value = result
      return result
    } catch (e: any) {
      error.value = e.response?.data?.message ?? 'Noe gikk galt'
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