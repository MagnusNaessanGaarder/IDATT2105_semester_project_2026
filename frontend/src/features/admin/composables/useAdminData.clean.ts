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

interface AuthMeApi {
  id?: number
  fullName?: string
  email?: string
  role?: string
  createdAt?: string
  updatedAt?: string
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
        options: ['nb-NO', 'en-US'],
      },
      {
        id: 'timezone',
        label: 'Tidssone',
        type: 'select',
        current_value: 'Europe/Oslo',
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
      },
      {
        id: 'digest_frequency',
        label: 'Oppsummering',
        description: 'Hyppighet for oppsummeringsvarsler',
        type: 'select',
        current_value: 'Ukentlig',
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
        min: 15,
        max: 240,
      },
      {
        id: 'mfa_required',
        label: 'Krev MFA for administratorer',
        type: 'toggle',
        current_value: true,
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
        options: ['Daglig', 'Ukentlig'],
      },
      {
        id: 'last_backup',
        label: 'Sist sikkerhetskopiert',
        type: 'info',
        current_value: new Date().toISOString(),
      },
    ] as SettingItem[],
  },
})

const auditLog = reactive<AuditLogEntry[]>([])

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
      const [meResponse, deviationsResponse, exportsResponse] = await Promise.all([
        client.get<AuthMeApi>('/auth/me'),
        client.get<DeviationApi[]>('/deviations', {
          params: withOrgNumber({}),
        }),
        client.get<ExportPageApi>('/exports', {
          params: withOrgNumber({ page: 0, size: 50 }),
        }),
      ])

      const me = meResponse.data
      users.splice(0, users.length, {
        id: me.id ?? 1,
        name: me.fullName ?? me.email?.split('@')[0] ?? 'Bruker',
        email: me.email ?? 'ukjent@example.com',
        role: roleForUser(me.role),
        department: 'Drift',
        status: 'active',
        created_date: me.createdAt ?? new Date().toISOString(),
        certifications: [],
        certifications_valid: true,
        last_login: me.updatedAt ?? new Date().toISOString(),
      })

      const entries: AuditLogEntry[] = []

      deviationsResponse.data.forEach((report) => {
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

      exportsResponse.data.content.forEach((job) => {
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

      auditLog.splice(0, auditLog.length, ...entries)
      hasLoaded = true
    } catch {
      users.splice(0, users.length)
      auditLog.splice(0, auditLog.length)
      error.value = 'Kunne ikke laste administrator-data fra API.'
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
