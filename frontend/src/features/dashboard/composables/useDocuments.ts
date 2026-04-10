import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { formatDateForOrganization } from '@/shared/utils/orgSettings'
import {
  documentsApi,
  type OrganizationDocument,
  type UploadNewDocumentPayload,
  type UploadNewVersionPayload,
} from '../api/documents'

export function useDocuments() {
  const authStore = useAuthStore()
  const orgNumber = computed(() => authStore.currentOrg?.orgNumber ?? null)

  const documents = ref<OrganizationDocument[]>([])
  const isLoading = ref(false)
  const error     = ref<string | null>(null)

  // Upload modal state
  const showUploadModal      = ref(false)
  const versionTargetDoc     = ref<OrganizationDocument | null>(null)
  const isUploading          = ref(false)
  const uploadError          = ref<string | null>(null)

  // Preview state
  const previewDoc       = ref<OrganizationDocument | null>(null)
  const previewUrl       = ref<string | null>(null)
  const isLoadingPreview = ref(false)
  const previewError     = ref<string | null>(null)

  const downloadingId = ref<number | null>(null)

  function openUploadNew() {
    versionTargetDoc.value = null
    uploadError.value = null
    showUploadModal.value = true
  }

  function openUploadVersion(doc: OrganizationDocument) {
    versionTargetDoc.value = doc
    uploadError.value = null
    showUploadModal.value = true
  }

  function closeUploadModal() {
    showUploadModal.value = false
  }

  async function fetchDocuments() {
    if (!orgNumber.value) { error.value = 'Ingen organisasjon funnet'; return }
    isLoading.value = true
    error.value = null
    try {
      documents.value = await documentsApi.listDocuments(orgNumber.value)
    } catch (e: any) {
      error.value = e.response?.data?.message ?? 'Kunne ikke laste dokumenter'
    } finally {
      isLoading.value = false
    }
  }

  async function uploadDocument(payload: UploadNewDocumentPayload) {
    if (!orgNumber.value) return
    isUploading.value = true
    uploadError.value = null
    try {
      const newDoc = await documentsApi.uploadDocument(orgNumber.value, payload)
      documents.value = [newDoc, ...documents.value]
      showUploadModal.value = false
    } catch (e: any) {
      uploadError.value = e.response?.data?.message ?? 'Opplasting feilet. Prøv igjen.'
    } finally {
      isUploading.value = false
    }
  }

  async function uploadVersion(payload: UploadNewVersionPayload) {
    if (!orgNumber.value || !versionTargetDoc.value) return
    const targetId = versionTargetDoc.value.documentId
    isUploading.value = true
    uploadError.value = null
    try {
      const updatedDoc = await documentsApi.uploadNewVersion(orgNumber.value, targetId, payload)
      // Replace the updated doc in the list in-place so the row refreshes immediately
      documents.value = documents.value.map((d) =>
        d.documentId === updatedDoc.documentId ? updatedDoc : d
      )
      showUploadModal.value = false
    } catch (e: any) {
      uploadError.value = e.response?.data?.message ?? 'Opplasting feilet. Prøv igjen.'
    } finally {
      isUploading.value = false
    }
  }

  async function downloadDocument(doc: OrganizationDocument) {
    if (!orgNumber.value) return
    downloadingId.value = doc.documentId
    try {
      await documentsApi.downloadDocument(orgNumber.value, doc.documentId, doc.title)
    } catch {
      // silent
    } finally {
      downloadingId.value = null
    }
  }

  async function openPreview(doc: OrganizationDocument) {
    if (!orgNumber.value) return
    closePreview()
    previewDoc.value = doc
    isLoadingPreview.value = true
    previewError.value = null
    try {
      previewUrl.value = await documentsApi.getPreviewUrl(orgNumber.value, doc.documentId)
    } catch {
      previewError.value = 'Forhåndsvisning er ikke tilgjengelig for dette dokumentet.'
    } finally {
      isLoadingPreview.value = false
    }
  }

  function closePreview() {
    if (previewUrl.value) { URL.revokeObjectURL(previewUrl.value); previewUrl.value = null }
    previewDoc.value = null
    previewError.value = null
    isLoadingPreview.value = false
  }

  function formatDate(iso: string): string {
    const d = new Date(iso)
    if (isNaN(d.getTime())) return iso
    return formatDateForOrganization(d, orgNumber.value ?? undefined, {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    })
  }

  onMounted(fetchDocuments)

  return {
    documents, isLoading, error,
    showUploadModal, versionTargetDoc, isUploading, uploadError,
    previewDoc, previewUrl, isLoadingPreview, previewError,
    downloadingId,
    openUploadNew, openUploadVersion, closeUploadModal,
    fetchDocuments, uploadDocument, uploadVersion, downloadDocument,
    openPreview, closePreview,
    formatDate,
  }
}
