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
  oldValues?: Record<string, unknown>
  newValues?: Record<string, unknown>
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

// Pagination
const currentPage = ref(1)
const pageSize = ref(10)
const pageSizeOptions = [10, 25, 50, 100]

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

const formatFieldName = (key: string): string => {
  const fieldNames: Record<string, string> = {
    localeCode: 'Locale',
    timezoneName: 'Tidssone',
    enableFoodModule: 'IK-Mat modul',
    enableAlcoholModule: 'IK-Alkohol modul',
    defaultTempMinC: 'Min temperatur',
    defaultTempMaxC: 'Maks temperatur',
    reminderEmailEnabled: 'E-postpåminnelser',
    notificationEmail: 'Varslings-e-post',
    displayName: 'Visningsnavn',
    legalName: 'Juridisk navn',
    contactEmail: 'Kontakt e-post',
    contactPhone: 'Kontakt telefon',
    retentionUserMonths: 'Brukerdata lagring',
    retentionAuditMonths: 'Revisjonsdata lagring',
    createdAt: 'Opprettet',
    updatedAt: 'Oppdatert',
    orgNumber: 'Org.nr',
  }
  return fieldNames[key] || key
}

const formatValue = (value: unknown): string => {
  if (value === null || value === undefined) return '(tom)'
  if (typeof value === 'boolean') return value ? 'På' : 'Av'
  if (typeof value === 'string') {
    // Truncate long strings
    if (value.length > 50) return value.substring(0, 47) + '...'
    return value
  }
  return String(value)
}

const parseJson = (jsonStr: string | null | undefined): Record<string, unknown> => {
  if (!jsonStr) return {}
  try {
    return JSON.parse(jsonStr) as Record<string, unknown>
  } catch {
    return {}
  }
}

const generateChangeSummary = (
  actionType: string,
  oldValuesJson: string | null | undefined,
  newValuesJson: string | null | undefined
): string => {
  const oldValues = parseJson(oldValuesJson)
  const newValues = parseJson(newValuesJson)
  const hasOldValues = Object.keys(oldValues).length > 0
  const hasNewValues = Object.keys(newValues).length > 0

  // Determine action type from the action string
  const isCreate = actionType.includes('CREATE') || (!hasOldValues && hasNewValues)
  const isDelete = actionType.includes('DELETE') || (hasOldValues && !hasNewValues)
  const isUpdate = actionType.includes('UPDATE') || (hasOldValues && hasNewValues)

  // Handle different actions
  if (isCreate) {
    return 'La til ny innstilling'
  }

  if (isDelete) {
    return 'Slettet innstilling'
  }

  if (!hasOldValues && !hasNewValues) {
    return 'Viste innstillinger'
  }

  // For updates, find what changed
  const changes: string[] = []
  const allKeys = new Set([...Object.keys(oldValues), ...Object.keys(newValues)])

  for (const key of allKeys) {
    // Skip internal fields
    if (key === 'createdAt' || key === 'updatedAt' || key === 'orgNumber') continue

    const oldVal = oldValues[key]
    const newVal = newValues[key]

    if (JSON.stringify(oldVal) !== JSON.stringify(newVal)) {
      const fieldName = formatFieldName(key)
      const oldFormatted = formatValue(oldVal)
      const newFormatted = formatValue(newVal)
      changes.push(`${fieldName}: ${oldFormatted} → ${newFormatted}`)
    }
  }

  if (changes.length === 0) {
    return 'Ingen endringer'
  }

  return changes.join(', ')
}

const mapAuditLogResponse = (item: AuditLogResponse): AuditLogEntry => {
  const details = generateChangeSummary(item.actionType, item.oldValuesJson, item.newValuesJson)

  return {
    id: item.auditLogId,
    timestamp: item.createdAt,
    user: normalizeUser(item),
    action: formatActionType(item.actionType),
    resource: formatResourceType(item.entityType),
    details: details,
    result: parseResult(item.actionType),
    oldValues: parseJson(item.oldValuesJson),
    newValues: parseJson(item.newValuesJson),
  }
}

const formatActionType = (action: string): string => {
  const actionNames: Record<string, string> = {
    CREATE: 'La til',
    UPDATE: 'Endret',
    DELETE: 'Slettet',
    LOGIN: 'Logget inn',
    LOGOUT: 'Logget ut',
    EXPORT: 'Eksporterte',
    VIEW: 'Så på',
  }
  for (const [key, value] of Object.entries(actionNames)) {
    if (action.includes(key)) return value
  }
  return action
}

const formatResourceType = (resource: string): string => {
  const resourceNames: Record<string, string> = {
    OrganizationSettings: 'Innstillinger',
    AppUser: 'Bruker',
    User: 'Bruker',
    Checklist: 'Sjekkliste',
    Task: 'Oppgave',
    TemperatureLogPoint: 'Temperaturpunkt',
    TemperatureLogEntry: 'Temperaturmåling',
  }
  return resourceNames[resource] || resource
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
      error.value = 'Kunne ikke hente revisjonslogg'
    }
  } finally {
    isLoading.value = false
  }
}

const sortedAuditLog = computed(() => {
  return [...entries.value].sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
})

const totalEntries = computed(() => sortedAuditLog.value.length)

const totalPages = computed(() => Math.ceil(totalEntries.value / pageSize.value))

const paginatedAuditLog = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return sortedAuditLog.value.slice(start, end)
})

const paginationInfo = computed(() => {
  const start = totalEntries.value === 0 ? 0 : (currentPage.value - 1) * pageSize.value + 1
  const end = Math.min(currentPage.value * pageSize.value, totalEntries.value)
  return { start, end, total: totalEntries.value }
})

const goToPage = (page: number) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
  }
}

const goToFirstPage = () => goToPage(1)
const goToLastPage = () => goToPage(totalPages.value)
const goToNextPage = () => goToPage(currentPage.value + 1)
const goToPreviousPage = () => goToPage(currentPage.value - 1)

const setPageSize = (size: number) => {
  pageSize.value = size
  currentPage.value = 1 // Reset to first page when changing page size
}

const resetPagination = () => {
  currentPage.value = 1
}

// Export to CSV
const exportToCsv = (filename?: string): string => {
  const headers = ['Tid', 'Bruker', 'Handling', 'Ressurs', 'Detaljer', 'Resultat']
  const rows = sortedAuditLog.value.map((entry) => [
    entry.timestamp,
    entry.user,
    entry.action,
    entry.resource,
    entry.details,
    entry.result,
  ])

  const csvContent = [
    headers.join(';'),
    ...rows.map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(';')),
  ].join('\n')

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename || `revisjonslogg_${new Date().toISOString().split('T')[0]}.csv`
  link.click()
  URL.revokeObjectURL(link.href)

  return csvContent
}

// Date validation
const validateDateRange = (): { valid: boolean; error?: string } => {
  if (filters.value.fromDate && filters.value.toDate) {
    const from = new Date(filters.value.fromDate)
    const to = new Date(filters.value.toDate)
    if (from > to) {
      return { valid: false, error: 'Fra-dato kan ikke være etter til-dato' }
    }
  }
  return { valid: true }
}

export const useAuditLog = () => ({
  auditLog: entries,
  sortedAuditLog,
  paginatedAuditLog,
  filters,
  isLoading,
  error,
  currentPage,
  pageSize,
  pageSizeOptions,
  totalEntries,
  totalPages,
  paginationInfo,
  fetchAuditLog,
  goToPage,
  goToFirstPage,
  goToLastPage,
  goToNextPage,
  goToPreviousPage,
  setPageSize,
  resetPagination,
  exportToCsv,
  validateDateRange,
})
