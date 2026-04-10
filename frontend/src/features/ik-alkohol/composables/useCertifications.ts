import { ref, computed } from 'vue'
import {
  getCertifications,
  createCertification,
  updateCertification,
  deleteCertification,
  completeCertification,
  type CreateCertificationRequest,
  type UpdateCertificationRequest,
} from '../api/certifications'
import type { CertificationRecord, CertificationResult } from './useAlkoholData'

export interface UseCertificationsReturn {
  items: import('vue').ComputedRef<CertificationRecord[]>
  isLoading: import('vue').ComputedRef<boolean>
  isSubmitting: import('vue').ComputedRef<boolean>
  error: import('vue').ComputedRef<string | null>
  loadItems: (orgNumber: number) => Promise<void>
  addItem: (data: CreateCertificationRequest, orgNumber: number) => Promise<boolean>
  editItem: (id: number, data: UpdateCertificationRequest, orgNumber: number) => Promise<boolean>
  removeItem: (id: number, orgNumber: number) => Promise<boolean>
  completeItem: (id: number, orgNumber: number, certificateDocumentId?: number | null) => Promise<boolean>
  clearError: () => void
}

const hasError = (result: CertificationResult): result is { ok: false; error: { message: string; status: number | null; data: unknown } } =>
  !result.ok

export function useCertifications(): UseCertificationsReturn {
  // Component-level state (fresh for each component instance)
  const items = ref<CertificationRecord[]>([])
  const isLoading = ref(false)
  const isSubmitting = ref(false)
  const error = ref<string | null>(null)

  const loadItems = async (orgNumber: number): Promise<void> => {
    isLoading.value = true
    error.value = null

    try {
      const result = await getCertifications(orgNumber)

      if (hasError(result)) {
        error.value = result.error.message
        items.value = []
      } else if (Array.isArray(result.data)) {
        items.value = result.data
      } else if (result.data === null) {
        items.value = []
      } else {
        // Handle single item case
        items.value = [result.data as CertificationRecord]
      }
    } catch (err) {
      error.value = 'Kunne ikke laste sertifiseringer'
      items.value = []
    } finally {
      isLoading.value = false
    }
  }

  const addItem = async (
    data: CreateCertificationRequest,
    orgNumber: number,
  ): Promise<boolean> => {
    isSubmitting.value = true
    error.value = null

    try {
      const result = await createCertification(data, orgNumber)

      if (hasError(result)) {
        error.value = result.error.message
        return false
      }

      await loadItems(orgNumber)

      return true
    } catch {
      error.value = 'Kunne ikke opprette sertifisering'
      return false
    } finally {
      isSubmitting.value = false
    }
  }

  const editItem = async (
    id: number,
    data: UpdateCertificationRequest,
    orgNumber: number,
  ): Promise<boolean> => {
    isSubmitting.value = true
    error.value = null

    try {
      const result = await updateCertification(id, data, orgNumber)

      if (hasError(result)) {
        error.value = result.error.message
        return false
      }

      await loadItems(orgNumber)

      return true
    } catch {
      error.value = 'Kunne ikke oppdatere sertifisering'
      return false
    } finally {
      isSubmitting.value = false
    }
  }

  const removeItem = async (id: number, orgNumber: number): Promise<boolean> => {
    isSubmitting.value = true
    error.value = null

    try {
      const result = await deleteCertification(id, orgNumber)

      if (hasError(result)) {
        error.value = result.error.message
        return false
      }

      await loadItems(orgNumber)
      return true
    } catch {
      error.value = 'Kunne ikke slette sertifisering'
      return false
    } finally {
      isSubmitting.value = false
    }
  }

  const completeItem = async (
    id: number,
    orgNumber: number,
    certificateDocumentId?: number | null,
  ): Promise<boolean> => {
    isSubmitting.value = true
    error.value = null

    try {
      const result = await completeCertification(id, orgNumber, certificateDocumentId)

      if (hasError(result)) {
        error.value = result.error.message
        return false
      }

      await loadItems(orgNumber)

      return true
    } catch {
      error.value = 'Kunne ikke fullføre sertifisering'
      return false
    } finally {
      isSubmitting.value = false
    }
  }

  const clearError = (): void => {
    error.value = null
  }

  return {
    items: computed(() => items.value),
    isLoading: computed(() => isLoading.value),
    isSubmitting: computed(() => isSubmitting.value),
    error: computed(() => error.value),
    loadItems,
    addItem,
    editItem,
    removeItem,
    completeItem,
    clearError,
  }
}
