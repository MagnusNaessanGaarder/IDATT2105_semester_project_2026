import { computed, ref } from 'vue'
import { auditLogApi, type AuditLogResponse } from '../api/auditLogApi'

export interface AuditLogEntry {
  id: number
  timestamp: string
  user: string
  action: string
  resource: string
  details: string
  result: 'SUCCESS' | 'FAILED'
}

const entries = ref<AuditLogEntry[]>([])
const isLoading = ref(false)
const error = ref<string | null>(null)

const normalizeUser = (item: AuditLogResponse): string => {
  const displayName = item.actedByUser?.displayName?.trim()
  if (displayName) {
    return displayName
  }

  const email = item.actedByUser?.email?.trim()
  if (email) {
    return email
  }

  return 'System'
}

const parseResult = (value?: string | null): 'SUCCESS' | 'FAILED' => {
  if (!value) {
    return 'SUCCESS'
  }

  const normalized = value.toUpperCase()
  if (normalized.includes('FAILED') || normalized.includes('ERROR')) {
    return 'FAILED'
  }

  return 'SUCCESS'
}

const mapAuditLogResponse = (item: AuditLogResponse): AuditLogEntry => {
  const oldValues = item.oldValuesJson?.trim()
  const newValues = item.newValuesJson?.trim()
  const changeSummary = [oldValues ? `Fra: ${oldValues}` : null, newValues ? `Til: ${newValues}` : null]
    .filter((value): value is string => Boolean(value))
    .join(' | ')

  return {
    id: item.auditLogId,
    timestamp: item.createdAt,
    user: normalizeUser(item),
    action: item.actionType,
    resource: item.entityType,
    details: changeSummary.length > 0 ? changeSummary : `Entity ID: ${item.entityId ?? '-'}`,
    result: parseResult(item.actionType),
  }
}

const fetchAuditLog = async (orgNumber: number): Promise<void> => {
  isLoading.value = true
  error.value = null

  try {
    const response = await auditLogApi.getAuditLogs(orgNumber)
    entries.value = response.map(mapAuditLogResponse)
  } catch (err) {
    entries.value = []
    error.value = err instanceof Error ? err.message : 'Kunne ikke hente revisjonslogg'
  } finally {
    isLoading.value = false
  }
}

const sortedAuditLog = computed(() => {
  return [...entries.value].sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
})

export const useAuditLog = () => ({
  auditLog: entries,
  sortedAuditLog,
  isLoading,
  error,
  fetchAuditLog,
})
