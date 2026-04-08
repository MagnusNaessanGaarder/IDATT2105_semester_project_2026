import { reactive, ref } from 'vue'
import { client } from '@/api/client'
import { withOrgNumber } from '@/shared/utils/orgContext'

export type UserRole = 'ADMIN' | 'MANAGER' | 'STAFF'
export type UserStatus = 'active' | 'inactive'

export interface AdminUser {
  id: number
  name: string
  email: string
  role: UserRole
  department: string
  status: UserStatus
  created_date: string
  certifications: string[]
  certifications_valid: boolean
  last_login: string
}

export interface SettingItem {
  id: string
  label: string
  description?: string
  type: 'select' | 'toggle' | 'number' | 'info'
  current_value: unknown
  active?: boolean
  options?: string[]
  min?: number
  max?: number
}

export interface SettingSection {
  section_title: string
  items: SettingItem[]
}

export interface AuditLogEntry {
  id: number
  timestamp: string
  user: string
  action: string
  resource: string
  details: string
  ip_address: string
  result: 'SUCCESS' | 'FAILED' | string
}

interface DeviationApi {
  reportId: number
  title: string
  status: string
  severity: string
  updatedAt?: string | null
}

interface ExportPageApi {
  content: Array<{
    exportJobId: number
    exportType: string
    status: string
    requestedAt: string | null
  }>
}

interface FileApi {
  documentId: number
  title: string
  updatedAt: string | null
}

interface TemperatureAlertApi {
  entryId: number
  measuredAt: string
  temperatureC: number
}

const users = reactive<AdminUser[]>([])
const settings = reactive({
  system: {
    section_title: 'System',
    items: [
      {
        id: 'language',
        label: 'Sprak',
        description: 'Standardsprak for brukergrensesnitt',
        type: 'select',
        current_value: 'nb-NO',
        active: true,
        options: ['nb-NO', 'en-US'],
      },
      {
        id: 'timezone',
        label: 'Tidssone',
        type: 'select',
        current_value: 'Europe/Oslo',
        active: true,
        options: ['Europe/Oslo', 'UTC'],
      },
    ] as SettingItem[],
  },
  notification_preferences: {
    section_title: 'Varslinger',
    items: [
      {
        id: 'mail_alerts',
        label: 'E-postvarsler',
        description: 'Send varsler ved kritiske avvik',
        type: 'toggle',
        current_value: true,
        active: false,
      },
      {
        id: 'digest_frequency',
        label: 'Oppsummering',
        description: 'Hyppighet for oppsummeringsvarsler',
        type: 'select',
        current_value: 'Ukentlig',
        active: false,
        options: ['Daglig', 'Ukentlig', 'Manedlig'],
      },
    ] as SettingItem[],
  },
  security: {
    section_title: 'Sikkerhet',
    items: [
      {
        id: 'session_timeout',
        label: 'Sesjon utlop (min)',
        type: 'number',
        current_value: 60,
        active: false,
        min: 15,
        max: 240,
      },
      {
        id: 'mfa_required',
        label: 'Krev MFA for administratorer',
        type: 'toggle',
        current_value: true,
        active: false,
      },
    ] as SettingItem[],
  },
  backup: {
    section_title: 'Sikkerhetskopi',
    items: [
      {
        id: 'backup_frequency',
        label: 'Frekvens',
        type: 'select',
        current_value: 'Daglig',
        active: false,
        options: ['Daglig', 'Ukentlig'],
      },
      {
        id: 'last_backup',
        label: 'Sist sikkerhetskopiert',
        type: 'info',
        current_value: new Date().toISOString(),
        active: false,
      },
    ] as SettingItem[],
  },
})

const auditLog = reactive<AuditLogEntry[]>([])
let deviationsEndpointUnavailable = false

let hasLoaded = false
let loadInFlight: Promise<void> | null = null
const isLoading = ref(false)
const error = ref<string | null>(null)

const roleLabel = (role: UserRole): string => {
  if (role === 'ADMIN') return 'Admin'
  if (role === 'MANAGER') return 'Leder'
  return 'Ansatt'
}

const roleTone = (role: UserRole): 'red' | 'amber' | 'blue' => {
  if (role === 'ADMIN') return 'red'
  if (role === 'MANAGER') return 'amber'
  return 'blue'
}

const roleDescription = (role: UserRole): string => {
  if (role === 'ADMIN') return 'Full tilgang til brukere, innstillinger og revisjonslogg'
  if (role === 'MANAGER') return 'Operativ styring av kontroll, rapporter og oppfølging'
  return 'Daglig bruk av sjekklister, rutiner og dokumentasjon'
}

const statusLabel = (status: UserStatus): string => {
  return status === 'active' ? 'Aktiv' : 'Inaktiv'
}

const formatDate = (value: string): string => {
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return value
  }

  return parsed.toLocaleDateString('nb-NO', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

const formatDateTime = (value: string): string => {
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return value
  }

  return parsed.toLocaleString('nb-NO', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const sortedAuditLog = () => [...auditLog].sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())

const roleForUser = (role: string | undefined): UserRole => {
  if (role === 'ADMIN') return 'ADMIN'
  if (role === 'MANAGER') return 'MANAGER'
  return 'STAFF'
}

const updateSetting = (id: string, patch: Partial<SettingItem>) => {
  const sections = [
    settings.system.items,
    settings.notification_preferences.items,
    settings.security.items,
    settings.backup.items,
  ]

  for (const sectionItems of sections) {
    const target = sectionItems.find((item) => item.id === id)
    if (!target) {
      continue
    }

    Object.assign(target, patch)
    break
  }
}

const loadData = async (): Promise<void> => {
  if (hasLoaded) {
    return
  }

  if (loadInFlight) {
    return loadInFlight
  }

  loadInFlight = (async () => {
    isLoading.value = true
    error.value = null

    try {
      const [deviationsResponse, exportsResponse, filesResponse, alertsResponse] = await Promise.allSettled([
        deviationsEndpointUnavailable
          ? Promise.resolve({ data: [] as DeviationApi[] })
          : client.get<DeviationApi[]>('/deviations', {
            params: withOrgNumber({}),
            skipGlobalErrorLog: true,
          }).catch((err: unknown) => {
            if (typeof err === 'object' && err !== null && 'response' in err) {
              const response = (err as { response?: { status?: number } }).response
              if (response?.status === 500) {
                deviationsEndpointUnavailable = true
                return { data: [] as DeviationApi[] }
              }
            }
            throw err
          }),
        client.get<ExportPageApi>('/exports', {
          params: withOrgNumber({ page: 0, size: 50 }),
        }),
        client.get<FileApi[]>('/files', {
          params: withOrgNumber({}),
        }),
        client.get<TemperatureAlertApi[]>('/temperature/alerts', {
          params: withOrgNumber({}),
        }),
      ])

      const fallbackEmail = sessionStorage.getItem('email') || 'ukjent@example.com'
      const fallbackRole = sessionStorage.getItem('role') || 'STAFF'
      const fallbackName = fallbackEmail.split('@')[0] || 'Bruker'
      const fallbackLastLogin = sessionStorage.getItem('lastLogin') || new Date().toISOString()

      users.splice(0, users.length, {
        id: 1,
        name: fallbackName,
        email: fallbackEmail,
        role: roleForUser(fallbackRole),
        department: 'Drift',
        status: 'active',
        created_date: new Date().toISOString(),
        certifications: [],
        certifications_valid: true,
        last_login: fallbackLastLogin,
      })

      const deviations = deviationsResponse.status === 'fulfilled' ? deviationsResponse.value.data : []
      const exports = exportsResponse.status === 'fulfilled' ? exportsResponse.value.data.content : []
      const files = filesResponse.status === 'fulfilled' ? filesResponse.value.data : []
      const alerts = alertsResponse.status === 'fulfilled' ? alertsResponse.value.data : []

      const entries: AuditLogEntry[] = []

      deviations.forEach((report: DeviationApi) => {
        entries.push({
          id: report.reportId,
          timestamp: report.updatedAt ?? new Date().toISOString(),
          user: 'System',
          action: 'DEVIATION_UPDATED',
          resource: report.title,
          details: `Status: ${report.status}, alvorlighet: ${report.severity}`,
          ip_address: '-',
          result: 'SUCCESS',
        })
      })

      exports.forEach((job: { exportJobId: number; exportType: string; status: string; requestedAt: string | null }) => {
        entries.push({
          id: job.exportJobId + 100000,
          timestamp: job.requestedAt ?? new Date().toISOString(),
          user: 'System',
          action: 'EXPORT_REQUESTED',
          resource: String(job.exportType),
          details: `Eksportstatus: ${job.status}`,
          ip_address: '-',
          result: 'SUCCESS',
        })
      })

      files.forEach((doc: FileApi) => {
        entries.push({
          id: doc.documentId + 200000,
          timestamp: doc.updatedAt ?? new Date().toISOString(),
          user: 'System',
          action: 'DOCUMENT_UPDATED',
          resource: doc.title,
          details: 'Dokument oppdatert i organisasjonens bibliotek',
          ip_address: '-',
          result: 'SUCCESS',
        })
      })

      const openDeviations = deviations.filter((report) => report.status !== 'CLOSED').length
      const latestExport = [...exports]
        .sort((a, b) => new Date(b.requestedAt ?? 0).getTime() - new Date(a.requestedAt ?? 0).getTime())[0]

      updateSetting('mail_alerts', {
        active: deviationsResponse.status === 'fulfilled',
        current_value: openDeviations > 0 || alerts.length > 0,
      })

      updateSetting('digest_frequency', {
        active: exportsResponse.status === 'fulfilled',
        current_value: openDeviations > 3 ? 'Daglig' : 'Ukentlig',
      })

      updateSetting('session_timeout', {
        active: deviationsResponse.status === 'fulfilled' || alertsResponse.status === 'fulfilled',
        current_value: alerts.length > 0 ? 30 : 60,
      })

      updateSetting('mfa_required', {
        active: true,
        current_value: openDeviations > 0,
      })

      updateSetting('backup_frequency', {
        active: exportsResponse.status === 'fulfilled',
        current_value: exports.length > 10 ? 'Daglig' : 'Ukentlig',
      })

      updateSetting('last_backup', {
        active: exportsResponse.status === 'fulfilled',
        current_value: latestExport?.requestedAt ?? 'Ingen eksport registrert',
      })

      updateSetting('language', {
        active: true,
        current_value: typeof navigator !== 'undefined' && navigator.language.startsWith('en') ? 'en-US' : 'nb-NO',
      })

      updateSetting('timezone', {
        active: true,
        current_value: Intl.DateTimeFormat().resolvedOptions().timeZone || 'Europe/Oslo',
      })

      auditLog.splice(0, auditLog.length, ...entries)

      const failedCalls = [deviationsResponse, exportsResponse, filesResponse, alertsResponse].filter((result) => result.status === 'rejected').length
      const succeededCalls = 4 - failedCalls

      if (succeededCalls === 0) {
        error.value = 'Admin-data er delvis utilgjengelig akkurat nå. Viser fallback-data.'
        return
      }

      error.value = failedCalls > 0 ? 'Noen admin-endepunkter feilet. Viser data som er tilgjengelig.' : null
      hasLoaded = true
    } catch {
      auditLog.splice(0, auditLog.length)
      error.value = 'Kunne ikke laste administrator-data fra API. Viser lokale standardverdier.'
    } finally {
      isLoading.value = false
      loadInFlight = null
    }
  })()

  return loadInFlight
}

const reload = async () => {
  hasLoaded = false
  await loadData()
}

export const useAdminData = () => {
  void loadData()

  return {
    users,
    settings,
    auditLog,
    get sortedAuditLog() {
      return sortedAuditLog()
    },
    roleLabel,
    roleTone,
    roleDescription,
    statusLabel,
    formatDate,
    formatDateTime,
    isLoading,
    error,
    reload,
  }
}
