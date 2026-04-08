import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { exportApi, type ExportRequest, type ExportResponse } from '../api'
import axios from 'axios'

type ApiErrorShape = {
  response?: {
    data?: {
      message?: string
    }
  }
}

const POLL_INTERVAL_RUNNING_MS = 3000
const POLL_INTERVAL_PENDING_MS = 8000
const PENDING_STALL_THRESHOLD = 5
const MAX_POLL_ATTEMPTS = 30

export function useExport() {
  const authStore = useAuthStore()
  const orgNumber = computed(() => authStore.currentOrg?.orgNumber ?? null)

  const exports = ref<ExportResponse[]>([])
  const activeJob = ref<ExportResponse | null>(null)
  const isCreating = ref(false)
  const isPolling = ref(false)
  const isLoadingList = ref(false)
  const error = ref<string | null>(null)
  const stalledWarning = ref<string | null>(null)

  let pollTimer: ReturnType<typeof setTimeout> | null = null
  let pollAttempts = 0
  let consecutivePendingCount = 0

  function clearPoll() {
    if (pollTimer) {
      clearTimeout(pollTimer)
      pollTimer = null
    }
    pollAttempts = 0
    consecutivePendingCount = 0
  }

  async function pollJobStatus(exportJobId: number) {
    if (!orgNumber.value) return

    isPolling.value = true
    pollAttempts++

    try {
      const job = await exportApi.getExportStatus(orgNumber.value, exportJobId)
      activeJob.value = job

      // Update the job in the list if present
      const idx = exports.value.findIndex((e) => e.exportJobId === exportJobId)
      if (idx !== -1) {
        exports.value[idx] = job
      }

      if (job.status === 'COMPLETED' || job.status === 'FAILED') {
        isPolling.value = false
        stalledWarning.value = null
        clearPoll()
        return
      }

      // Track how long we've been stuck at PENDING
      if (job.status === 'PENDING') {
        consecutivePendingCount++
        if (consecutivePendingCount >= PENDING_STALL_THRESHOLD) {
          stalledWarning.value =
              'Jobben venter fortsatt på å bli behandlet. Dette kan skyldes høy serverbelastning.'
        }
      } else {
        // Status advanced to RUNNING — clear any stall warning
        consecutivePendingCount = 0
        stalledWarning.value = null
      }

      if (pollAttempts >= MAX_POLL_ATTEMPTS) {
        isPolling.value = false
        error.value = 'Eksportjobben tok for lang tid. Sjekk historikken igjen litt senere.'
        clearPoll()
        return
      }

      // Back off if still pending, poll faster if actively running
      const delay = job.status === 'PENDING' ? POLL_INTERVAL_PENDING_MS : POLL_INTERVAL_RUNNING_MS
      pollTimer = setTimeout(() => pollJobStatus(exportJobId), delay)
    } catch (err: unknown) {
      isPolling.value = false
      const apiError = err as ApiErrorShape
      error.value = apiError.response?.data?.message ?? 'Klarte ikke hente status for eksportjobb.'
      clearPoll()
    }
  }

  async function createExport(request: ExportRequest) {
    if (!orgNumber.value) {
      error.value = 'Ingen organisasjon valgt.'
      return null
    }

    error.value = null
    stalledWarning.value = null
    isCreating.value = true
    clearPoll()

    try {
      const job = await exportApi.createExport(orgNumber.value, request)
      activeJob.value = job
      exports.value = [job, ...exports.value]
      pollAttempts = 0
      consecutivePendingCount = 0
      pollTimer = setTimeout(() => pollJobStatus(job.exportJobId), POLL_INTERVAL_PENDING_MS)
      return job
    } catch (err: unknown) {
      const apiError = err as ApiErrorShape
      error.value = apiError.response?.data?.message ?? 'Klarte ikke opprette eksportjobb.'
      return null
    } finally {
      isCreating.value = false
    }
  }

  async function downloadExport(exportJobId: number) {
    if (!orgNumber.value) return

    try {
      const path = await exportApi.getDownloadUrl(orgNumber.value, exportJobId)
      const pathWithOrg = `${path}?orgNumber=${orgNumber.value}`

      const baseUrl = (import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1').replace('/api/v1', '')
      const response = await axios.get(baseUrl + pathWithOrg, {
        responseType: 'blob',
        headers: {
          Authorization: `Bearer ${sessionStorage.getItem('accessToken')}`,
        },
      })

      const job = exports.value.find((e) => e.exportJobId === exportJobId) ?? activeJob.value
      const ext = job?.format === 'JSON' ? 'json' : 'pdf'
      const mimeType = job?.format === 'JSON' ? 'application/json' : 'application/pdf'

      const blob = new Blob([response.data], { type: mimeType })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `export-${exportJobId}.${ext}`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
    } catch (err: unknown) {
      const apiError = err as ApiErrorShape
      error.value = apiError.response?.data?.message ?? 'Klarte ikke laste ned eksport.'
    }
  }

  async function loadExports() {
    if (!orgNumber.value) return

    isLoadingList.value = true
    error.value = null

    try {
      const page = await exportApi.listExports(orgNumber.value)
      exports.value = page.content
    } catch (err: unknown) {
      const apiError = err as ApiErrorShape
      error.value = apiError.response?.data?.message ?? 'Klarte ikke laste eksporthistorikk.'
    } finally {
      isLoadingList.value = false
    }
  }

  function dismissError() {
    error.value = null
  }

  function resetActiveJob() {
    clearPoll()
    activeJob.value = null
    isPolling.value = false
    error.value = null
    stalledWarning.value = null
  }

  return {
    exports,
    activeJob,
    isCreating,
    isPolling,
    isLoadingList,
    error,
    stalledWarning,
    createExport,
    downloadExport,
    loadExports,
    dismissError,
    resetActiveJob,
  }
}

export const exportTypeLabels: Record<string, string> = {
  AUDIT_REPORT: 'Revisjonslogg',
  CHECKLIST_REPORT: 'Sjekklisterrapport',
  TEMPERATURE_REPORT: 'Temperaturlogg',
  DEVIATION_REPORT: 'Avviksrapport',
  TRAINING_REPORT: 'Opplæringsrapport',
  FULL_COMPLIANCE_REPORT: 'Full samsvarsrapport',
}

export const exportFormatLabels: Record<string, string> = {
  PDF: 'PDF',
  JSON: 'JSON',
}

export const exportStatusLabels: Record<string, string> = {
  PENDING: 'Venter',
  RUNNING: 'Behandler',
  COMPLETED: 'Ferdig',
  FAILED: 'Feilet',
}

export const exportStatusTone: Record<string, string> = {
  PENDING: 'gray',
  RUNNING: 'blue',
  COMPLETED: 'green',
  FAILED: 'red',
}
