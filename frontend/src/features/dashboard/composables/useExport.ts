/**
 * Export Job Management Composable
 * 
 * Handles export job orchestration including:
 * - Job creation with automatic polling
 * - Status polling with smart backoff (8s for PENDING, 3s for RUNNING)
 * - Stall detection (warns user if job stuck for 5+ polling cycles)
 * - Download blob generation
 * - Export list management
 * 
 * @remarks
 * This composable manages complex async state for long-running export jobs.
 * It exposes the three required state refs: exports, isLoading (variants), error.
 * All HTTP calls are delegated to exportsApi.
 */

import { ref, computed } from 'vue'
import axios from 'axios'
import { useAuthStore } from '@/stores/auth.ts'
import { exportsApi } from '../api/exports'
import type { ExportRequest, ExportResponse, ApiErrorResponse } from '../types/index'


const POLL_INTERVAL_RUNNING_MS = 3000 // Poll every 3s while RUNNING
const POLL_INTERVAL_PENDING_MS = 8000 // Poll every 8s while PENDING
const PENDING_STALL_THRESHOLD = 5 // Max consecutive PENDING polls before warning
const MAX_POLL_ATTEMPTS = 30 // Max total poll attempts before timing out

/**
 * Main export composable factory function
 * Returns state refs and action methods for managing export jobs
 */
export function useExport() {
  // ─ Dependencies ────────────────────────────────────────────────────────
  const authStore = useAuthStore()
  const orgNumber = computed(() => authStore.currentOrg?.orgNumber ?? null)

  // ─ State ───────────────────────────────────────────────────────────────
  /** List of all exports for current organization */
  const exports = ref<ExportResponse[]>([])
  
  /** Currently active export job (one being polled) */
  const activeJob = ref<ExportResponse | null>(null)
  
  /** Loading states */
  const isCreating = ref(false)
  const isPolling = ref(false)
  const isLoadingList = ref(false)
  
  /** Error and warning states */
  const error = ref<string | null>(null)
  const stalledWarning = ref<string | null>(null)

  // ─ Polling internals ───────────────────────────────────────────────────
  let pollTimer: ReturnType<typeof setTimeout> | null = null
  let pollAttempts = 0
  let consecutivePendingCount = 0

  /**
   * Clear and cleanup poll timer and state
   */
  function clearPoll() {
    if (pollTimer) {
      clearTimeout(pollTimer)
      pollTimer = null
    }
    pollAttempts = 0
    consecutivePendingCount = 0
  }

  /**
   * Recursively poll export job status with smart backoff
   * Stops when job completes/fails or max attempts exceeded
   */
  async function pollJobStatus(exportJobId: number) {
    if (!orgNumber.value) return

    isPolling.value = true
    pollAttempts++

    try {
      // Fetch current status
      const job = await exportsApi.getExportStatus(orgNumber.value, exportJobId)
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
      const apiError = err as ApiErrorResponse
      error.value = apiError.response?.data?.message ?? 'Klarte ikke hente status for eksportjobb.'
      clearPoll()
    }
  }

  /**
   * Create a new export job and start polling its status
   */
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
      const job = await exportsApi.createExport(orgNumber.value, request)
      activeJob.value = job
      exports.value = [job, ...exports.value]
      pollAttempts = 0
      consecutivePendingCount = 0
      pollTimer = setTimeout(() => pollJobStatus(job.exportJobId), POLL_INTERVAL_PENDING_MS)
      return job
    } catch (err: unknown) {
      const apiError = err as ApiErrorResponse
      error.value = apiError.response?.data?.message ?? 'Klarte ikke opprette eksportjobb.'
      return null
    } finally {
      isCreating.value = false
    }
  }

  /**
   * Download an export file as a blob and trigger browser download
   */
  async function downloadExport(exportJobId: number) {
    if (!orgNumber.value) return

    try {
      // Get download URL from API
      const path = await exportsApi.getDownloadUrl(orgNumber.value, exportJobId)
      const hasOrgNumber = /(?:\?|&)orgNumber=/.test(path)
      const separator = path.includes('?') ? '&' : '?'
      const pathWithOrg = hasOrgNumber ? path : `${path}${separator}orgNumber=${orgNumber.value}`

      const baseUrl = (import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1').replace('/api/v1', '')
      const downloadUrl = pathWithOrg.startsWith('http://') || pathWithOrg.startsWith('https://')
        ? pathWithOrg
        : `${baseUrl}${pathWithOrg}`

      const response = await axios.get(downloadUrl, {
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
      const apiError = err as ApiErrorResponse
      error.value = apiError.response?.data?.message ?? 'Klarte ikke laste ned eksport.'
    }
  }

  /**
   * Load all exports for current organization
   */
  async function loadExports() {
    if (!orgNumber.value) return

    isLoadingList.value = true
    error.value = null

    try {
      const page = await exportsApi.listExports(orgNumber.value)
      exports.value = page.content
    } catch (err: unknown) {
      const apiError = err as ApiErrorResponse
      error.value = apiError.response?.data?.message ?? 'Klarte ikke laste eksporthistorikk.'
    } finally {
      isLoadingList.value = false
    }
  }

  /**
   * Clear error state (called when user dismisses error banner)
   */
  function dismissError() {
    error.value = null
  }

  /**
   * Reset active job and polling state
   * Call when user leaves export view or completes an export
   */
  function resetActiveJob() {
    clearPoll()
    activeJob.value = null
    isPolling.value = false
    error.value = null
    stalledWarning.value = null
  }

  // ─ Return public API ─────────────────────────────────────────────────────
  return {
    // State refs
    exports,
    activeJob,
    isCreating,
    isPolling,
    isLoadingList,
    error,
    stalledWarning,
    
    // Action methods
    createExport,
    downloadExport,
    loadExports,
    dismissError,
    resetActiveJob,
  }
}

// ═════════════════════════════════════════════════════════════════════════════
// UI Label Maps
// Used by ReportsView and ExportView to display human-readable labels
// ═════════════════════════════════════════════════════════════════════════════

/** Map export types to Norwegian display labels */
export const exportTypeLabels: Record<string, string> = {
  AUDIT_REPORT: 'Revisjonslogg',
  CHECKLIST_REPORT: 'Sjekklisterrapport',
  TEMPERATURE_REPORT: 'Temperaturlogg',
  DEVIATION_REPORT: 'Avviksrapport',
  TRAINING_REPORT: 'Opplæringsrapport',
  FULL_COMPLIANCE_REPORT: 'Full samsvarsrapport',
}

/** Map export formats to display labels */
export const exportFormatLabels: Record<string, string> = {
  PDF: 'PDF',
  JSON: 'JSON',
}

/** Map export statuses to Norwegian display labels */
export const exportStatusLabels: Record<string, string> = {
  PENDING: 'Venter',
  RUNNING: 'Behandler',
  COMPLETED: 'Ferdig',
  FAILED: 'Feilet',
}

/** Map export statuses to tone/color for UI display */
export const exportStatusTone: Record<string, string> = {
  PENDING: 'gray',
  RUNNING: 'blue',
  COMPLETED: 'green',
  FAILED: 'red',
}
