import adminData from '@/data/admin.json'

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

type RawUser = {
  user_id: number
  display_name: string
  email: string | null
  is_active: number
  global_last_seen_at: string | null
  created_at: string
}

type RawMembership = {
  user_id: number
  org_number: number
  is_active: number
  last_seen_at: string | null
}

type RawRole = {
  role_id: number
  role_name: string
}

type RawUserRole = {
  user_id: number
  org_number: number
  role_id: number
}

type RawTraining = {
  user_id: number
  title: string
  expires_at: string | null
  status: 'assigned' | 'completed' | 'expired'
}

type RawOrgSettings = {
  timezone_name: string
  locale_code: string
  reminder_email_enabled: number
  enable_food_module: number
  enable_alcohol_module: number
  retention_audit_months: number | null
}

type RawAuditLog = {
  audit_log_id: number
  acted_by_user_id: number | null
  action_type: string
  entity_type: string
  entity_id: number | null
  old_values_json: Record<string, unknown> | null
  new_values_json: Record<string, unknown> | null
  user_agent: string | null
  created_at: string
}

const rawUsers = adminData.app_user as RawUser[]
const memberships = adminData.user_organization as RawMembership[]
const roles = adminData.role as RawRole[]
const userRoles = adminData.user_organization_role as RawUserRole[]
const trainings = adminData.training_record as RawTraining[]
const rawSettings = (adminData.organization_settings as RawOrgSettings[])[0]
const rawAuditLog = adminData.audit_log as RawAuditLog[]

const roleById = new Map(roles.map((role) => [role.role_id, role.role_name]))
const membershipByUserId = new Map(memberships.map((membership) => [membership.user_id, membership]))
const roleAssignmentByUserId = new Map(userRoles.map((assignment) => [assignment.user_id, assignment]))
const userNameById = new Map(rawUsers.map((user) => [user.user_id, user.display_name]))

const toUiRole = (roleName: string | undefined): UserRole => {
  if (roleName === 'ADMIN') return 'ADMIN'
  if (roleName === 'MANAGER' || roleName === 'KITCHEN_MANAGER') return 'MANAGER'
  return 'STAFF'
}

const toDepartment = (roleName: string | undefined): string => {
  if (roleName === 'ADMIN' || roleName === 'MANAGER') return 'Ledelse'
  if (roleName === 'KITCHEN_MANAGER' || roleName === 'COOK') return 'Kjokken'
  if (roleName === 'BARTENDER') return 'Bar'
  if (roleName === 'WAITER') return 'Service'
  return 'Drift'
}

const isValidCertificationDate = (expiresAt: string | null): boolean => {
  if (!expiresAt) {
    return false
  }

  const date = new Date(expiresAt)
  if (Number.isNaN(date.getTime())) {
    return false
  }

  return date.getTime() > Date.now()
}

const users = rawUsers.map((user) => {
  const membership = membershipByUserId.get(user.user_id)
  const assignment = roleAssignmentByUserId.get(user.user_id)
  const roleName = assignment ? roleById.get(assignment.role_id) : undefined

  const userCertifications = trainings
    .filter((training) => training.user_id === user.user_id && training.status === 'completed')
    .map((training) => training.title)

  const certificationsValid =
    userCertifications.length > 0 &&
    trainings
      .filter((training) => training.user_id === user.user_id && training.status === 'completed')
      .every((training) => isValidCertificationDate(training.expires_at))

  return {
    id: user.user_id,
    name: user.display_name,
    email: user.email ?? '-',
    role: toUiRole(roleName),
    department: toDepartment(roleName),
    status: user.is_active === 1 && membership?.is_active === 1 ? 'active' : 'inactive',
    created_date: user.created_at,
    certifications: userCertifications,
    certifications_valid: certificationsValid,
    last_login: user.global_last_seen_at ?? membership?.last_seen_at ?? user.created_at,
  } satisfies AdminUser
})

const settings = {
  system: {
    section_title: 'Systeminnstillinger',
    items: [
      {
        id: 'locale_code',
        label: 'Sprak',
        description: 'Velg systemsprak',
        type: 'select',
        current_value: rawSettings?.locale_code ?? 'nb-NO',
        options: ['nb-NO', 'en-GB', 'sv-SE'],
      },
      {
        id: 'timezone_name',
        label: 'Tidssone',
        description: 'Velg tidssone for registreringer',
        type: 'select',
        current_value: rawSettings?.timezone_name ?? 'Europe/Oslo',
        options: ['Europe/Oslo', 'UTC', 'Europe/Stockholm', 'Europe/Copenhagen'],
      },
    ],
  },
  notification_preferences: {
    section_title: 'Varslingsinnstillinger',
    items: [
      {
        id: 'reminder_email_enabled',
        label: 'E-postvarsler aktivert',
        description: 'Motta e-post ved viktige hendelser',
        type: 'toggle',
        current_value: rawSettings?.reminder_email_enabled === 1,
      },
      {
        id: 'enable_food_module',
        label: 'IK-Mat modul aktiv',
        description: 'Aktiver matmodul i systemet',
        type: 'toggle',
        current_value: rawSettings?.enable_food_module === 1,
      },
      {
        id: 'enable_alcohol_module',
        label: 'IK-Alkohol modul aktiv',
        description: 'Aktiver alkoholmodul i systemet',
        type: 'toggle',
        current_value: rawSettings?.enable_alcohol_module === 1,
      },
    ],
  },
  security: {
    section_title: 'Sikkerhet',
    items: [
      {
        id: 'retention_audit_months',
        label: 'Retensjon revisjonslogg (maneder)',
        description: 'Antall maneder revisjonslogg beholdes',
        type: 'number',
        current_value: rawSettings?.retention_audit_months ?? 36,
        min: 1,
        max: 120,
      },
      {
        id: 'password_expires',
        label: 'Passord utløper',
        description: 'Krev passord-endring hver 90. dag',
        type: 'toggle',
        current_value: true,
      },
    ],
  },
  backup: {
    section_title: 'Sikkerhetskopi',
    items: [
      {
        id: 'last_backup',
        label: 'Siste sikkerhetskopi',
        type: 'info',
        current_value: '2024-06-01 02:00',
        description: 'Automatisk sikkerhetskopi kjores daglig',
      },
      {
        id: 'backup_retention',
        label: 'Oppbevar sikkerhetskopier i (dager)',
        type: 'number',
        current_value: 30,
        min: 7,
        max: 365,
      },
    ],
  },
}

const stringifyValues = (values: Record<string, unknown> | null): string => {
  if (!values || Object.keys(values).length === 0) {
    return 'Ingen detaljer'
  }

  return Object.entries(values)
    .map(([key, value]) => `${key}: ${String(value)}`)
    .join(', ')
}

const auditLog = rawAuditLog.map((entry) => ({
  id: entry.audit_log_id,
  timestamp: entry.created_at,
  user: entry.acted_by_user_id ? (userNameById.get(entry.acted_by_user_id) ?? 'Ukjent bruker') : 'System',
  action: entry.action_type,
  resource: `${entry.entity_type}${entry.entity_id ? ` #${entry.entity_id}` : ''}`,
  details: stringifyValues(entry.new_values_json ?? entry.old_values_json),
  ip_address: entry.user_agent ?? 'system',
  result: entry.action_type.includes('FAIL') ? 'FAILED' : 'SUCCESS',
}))

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

const sortedAuditLog = [...auditLog].sort((a, b) => {
  return new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
})

export const useAdminData = () => ({
  users,
  settings,
  auditLog,
  sortedAuditLog,
  roleLabel,
  roleTone,
  roleDescription,
  statusLabel,
  formatDate,
  formatDateTime,
})