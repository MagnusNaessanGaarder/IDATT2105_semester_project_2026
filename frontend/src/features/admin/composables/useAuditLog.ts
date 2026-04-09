import { computed, ref } from 'vue'
import {
  auditLogApi,
  type AuditActionType,
  type AuditLogResponse,
} from '../api/auditLogApi'

export interface AuditLogEntry {
  id: number
  timestamp: string
  user: string
  action: string
  resource: string
  details: string
  result: 'SUCCESS' | 'FAILED'
}

export interface AuditLogFilters {
  actionType: 'ALL' | AuditActionType
  fromDate: string
  toDate: string
  entityType: string
  entityId: string
}

const entries = ref<AuditLogEntry[]>([])
const isLoading = ref(false)
const error = ref<string | null>(null)
const filters = ref<AuditLogFilters>({
  actionType: 'ALL',
  fromDate: '',
  toDate: '',
  entityType: '',
  entityId: '',
})

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
    const hasDateRange = filters.value.fromDate.length > 0 && filters.value.toDate.length > 0
    const hasEntityFilter = filters.value.entityType.trim().length > 0 && filters.value.entityId.trim().length > 0
    let response: AuditLogResponse[]

    if (hasEntityFilter) {
      const entityId = Number(filters.value.entityId)
      if (!Number.isFinite(entityId)) {
        error.value = 'Entity ID må være et tall.'
        entries.value = []
        return
      }
      response = await auditLogApi.getEntityAuditLogs(
        orgNumber,
        filters.value.entityType.trim(),
        entityId
      )
    } else if (hasDateRange) {
      response = await auditLogApi.getAuditLogsByDateRange(
        orgNumber,
        new Date(filters.value.fromDate).toISOString(),
        new Date(filters.value.toDate).toISOString()
      )
    } else if (filters.value.actionType !== 'ALL') {
      response = await auditLogApi.getAuditLogsByActionType(orgNumber, filters.value.actionType)
    } else {
      response = await auditLogApi.getAuditLogs(orgNumber)
    }

    entries.value = response.map(mapAuditLogResponse)
  } catch (err: unknown) {
    entries.value = []
    const status = (err as { response?: { status?: number } })?.response?.status
    if (status === 401) {
      error.value = 'Du må logge inn på nytt for å se revisjonslogg.'
    } else if (status === 403) {
      error.value = 'Du har ikke tilgang til revisjonslogg.'
    } else {
      error.value = err instanceof Error ? err.message : 'Kunne ikke hente revisjonslogg'
    }
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
  filters,
  isLoading,
  error,
  fetchAuditLog,
})
