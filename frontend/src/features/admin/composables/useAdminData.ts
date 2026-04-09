import { ref } from 'vue'
import adminData from '@/data/admin.json'
import { settingsApi, type BackendSettings, type BackendSettingsRequest } from '../api/settingsApi'

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
  persistence: 'backend' | 'local' | 'readonly'
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

export interface SettingsState {
  system: SettingSection
  notification_preferences: SettingSection
  security: SettingSection
  backup: SettingSection
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

const users = adminData.users as AdminUser[]
const rawSettings = adminData.settings as SettingsState
const auditLog = adminData.audit_log_sample as AuditLogEntry[]

const LOCAL_STORAGE_KEY_PREFIX = 'admin_settings_local_'
const PERSISTENCE_BY_ITEM_ID: Record<string, SettingItem['persistence']> = {
  language: 'backend',
  timezone: 'backend',
  date_format: 'local',
  email_critical: 'backend',
  email_updates: 'local',
  in_app_notifications: 'local',
  password_expires: 'readonly',
  session_timeout: 'readonly',
  two_factor: 'readonly',
  last_backup: 'readonly',
  backup_retention: 'backend',
}

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

/**
 * Persistence strategy:
 * - Backend settings: Stored in database, shared across organization, requires Admin/Manager role
 *   Includes: language, timezone, critical email alerts, backup retention
 * - Local client settings: Stored in localStorage, user-specific non-sensitive UI preferences
 *   Includes: date format, non-critical update preferences, in-app notifications
 * - Read-only settings: Displayed to users but not editable on this page
 *   Includes: security policy flags, last backup timestamp
 */

const isLoading = ref(false)
const error = ref<string | null>(null)

const cloneSettings = (source: SettingsState): SettingsState => {
  return JSON.parse(JSON.stringify(source)) as SettingsState
}

const applyPersistenceMetadata = (source: SettingsState): SettingsState => {
  const cloned = cloneSettings(source)
  ;(Object.keys(cloned) as (keyof SettingsState)[]).forEach((sectionKey) => {
    cloned[sectionKey].items = cloned[sectionKey].items.map((item) => ({
      ...item,
      persistence: PERSISTENCE_BY_ITEM_ID[item.id] ?? 'readonly',
    }))
  })
  return cloned
}

const settings = applyPersistenceMetadata(rawSettings)

const localeToLanguageOption = (localeCode: string | undefined): string => {
  if (localeCode === 'en_US') return 'English'
  if (localeCode === 'sv_SE') return 'Swedish'
  return 'Norwegian'
}

const languageOptionToLocale = (value: unknown): string => {
  if (value === 'English' || value === 'en_US') return 'en_US'
  if (value === 'Swedish' || value === 'sv_SE') return 'sv_SE'
  return 'nb_NO'
}

const readLocalSettings = (orgNumber: number): Record<string, unknown> => {
  if (typeof localStorage === 'undefined') {
    return {}
  }

  const raw = localStorage.getItem(`${LOCAL_STORAGE_KEY_PREFIX}${orgNumber}`)
  if (!raw) {
    return {}
  }

  try {
    const parsed = JSON.parse(raw)
    if (parsed && typeof parsed === 'object') {
      return parsed as Record<string, unknown>
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to read local settings'
  }

  return {}
}

const writeLocalSettings = (orgNumber: number, localSettings: Record<string, unknown>) => {
  if (typeof localStorage === 'undefined') {
    return
  }

  localStorage.setItem(`${LOCAL_STORAGE_KEY_PREFIX}${orgNumber}`, JSON.stringify(localSettings))
}

const applyLocalSettings = (source: SettingsState, orgNumber: number): SettingsState => {
  const localSettings = readLocalSettings(orgNumber)
  const cloned = cloneSettings(source)

  ;(Object.keys(cloned) as (keyof SettingsState)[]).forEach((sectionKey) => {
    cloned[sectionKey].items = cloned[sectionKey].items.map((item) => {
      if (item.persistence !== 'local') {
        return item
      }

      if (!(item.id in localSettings)) {
        return item
      }

      return {
        ...item,
        current_value: localSettings[item.id],
      }
    })
  })

  return cloned
}

/**
 * Map backend settings to frontend SettingItem structure
 * @param backendSettings Backend settings from API
 * @returns Frontend settings structure
 */
const mapBackendSettingsToFrontend = (
  backendSettings: BackendSettings
): SettingsState => {
  return {
    system: {
      section_title: 'Systeminnstillinger',
      items: [
        {
          id: 'language',
          label: 'Språk',
          description: 'Velg systemspråk',
          type: 'select',
          persistence: 'backend',
          current_value: localeToLanguageOption(backendSettings.localeCode),
          options: ['Norwegian', 'English', 'Swedish'],
        },
        {
          id: 'timezone',
          label: 'Tidssone',
          description: 'Velg tidssone for registreringer',
          type: 'select',
          persistence: 'backend',
          current_value: backendSettings.timezoneName,
          options: ['Europe/Oslo', 'UTC', 'Europe/Stockholm', 'Europe/Copenhagen'],
        },
        {
          id: 'date_format',
          label: 'Datoformat',
          description: 'Format for datovisning',
          type: 'select',
          persistence: 'local',
          current_value: 'dd.MM.yyyy',
          options: ['dd.MM.yyyy', 'yyyy-MM-dd', 'MM/dd/yyyy'],
        },
      ],
    },
    notification_preferences: {
      section_title: 'Varslingsinnstillinger',
      items: [
        {
          id: 'email_critical',
          label: 'E-post for kritiske varsler',
          description: 'Motta e-post ved kritiske hendelser',
          type: 'toggle',
          persistence: 'backend',
          current_value: backendSettings.reminderEmailEnabled,
        },
        {
          id: 'email_updates',
          label: 'E-post for systemoppdateringer',
          description: 'Motta e-post ved systemvedlikehold',
          type: 'toggle',
          persistence: 'local',
          current_value: false,
        },
        {
          id: 'in_app_notifications',
          label: 'Varsler i systemet',
          description: 'Vis varsler i systemet',
          type: 'toggle',
          persistence: 'local',
          current_value: true,
        },
      ],
    },
    security: {
      section_title: 'Sikkerhet',
      items: [
        {
          id: 'password_expires',
          label: 'Passord utløper',
          description: 'Sikkerhetspolicy administreres sentralt (skrivebeskyttet)',
          type: 'info',
          persistence: 'readonly',
          current_value: true,
        },
        {
          id: 'session_timeout',
          label: 'Sesjonstimeout (minutter)',
          description: 'Session policy administreres sentralt (skrivebeskyttet)',
          type: 'info',
          persistence: 'readonly',
          current_value: 30,
        },
        {
          id: 'two_factor',
          label: 'To-faktor autentisering',
          description: 'Autentiseringspolicy administreres sentralt (skrivebeskyttet)',
          type: 'info',
          persistence: 'readonly',
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
          persistence: 'readonly',
          current_value: '2024-06-01 02:00',
          description: 'Automatisk sikkerhetskopi kjøres daglig',
        },
        {
          id: 'backup_retention',
          label: 'Oppbevar sikkerhetskopier i (dager)',
          type: 'number',
          persistence: 'backend',
          current_value: backendSettings.retentionAuditMonths ? backendSettings.retentionAuditMonths * 30 : 30,
          min: 7,
          max: 365,
        },
      ],
    },
  }
}

/**
 * Convert frontend settings back to backend request format
 * @param frontendSettings Frontend settings structure
 * @param currentBackendSettings Current backend settings (for unmapped fields)
 * @returns Request object for backend API
 */
const mapFrontendSettingsToBackend = (
  frontendSettings: SettingsState,
  currentBackendSettings: BackendSettings
): BackendSettingsRequest => {
  const systemItems = frontendSettings.system.items
  const notificationItems = frontendSettings.notification_preferences.items
  const backupItems = frontendSettings.backup.items

  const languageValue = systemItems.find(i => i.id === 'language')?.current_value ?? 'Norwegian'
  const localeCode = languageOptionToLocale(languageValue)

  const timezone = systemItems.find(i => i.id === 'timezone')?.current_value || 'Europe/Oslo'
  const reminderEnabled = notificationItems.find(i => i.id === 'email_critical')?.current_value === true
  const retentionDays = Number(backupItems.find(i => i.id === 'backup_retention')?.current_value)
  const retentionAuditMonths = Number.isFinite(retentionDays)
    ? Math.max(1, Math.round(retentionDays / 30))
    : currentBackendSettings.retentionAuditMonths

  return {
    timezoneName: String(timezone),
    localeCode,
    enableFoodModule: currentBackendSettings.enableFoodModule,
    enableAlcoholModule: currentBackendSettings.enableAlcoholModule,
    defaultTempMinC: currentBackendSettings.defaultTempMinC,
    defaultTempMaxC: currentBackendSettings.defaultTempMaxC,
    reminderEmailEnabled: reminderEnabled,
    notificationEmail: currentBackendSettings.notificationEmail,
    retentionUserMonths: currentBackendSettings.retentionUserMonths,
    retentionAuditMonths,
  }
}

/**
 * Save settings to backend and localStorage
 * @param frontendSettings Updated settings from UI
 * @param backendSettings Current backend settings
 * @param orgNumber Organization number
 */
const saveSettings = async (
  frontendSettings: SettingsState,
  backendSettings: BackendSettings,
  orgNumber: number
) => {
  isLoading.value = true
  error.value = null

  try {
    // Save to backend
    const backendRequest = mapFrontendSettingsToBackend(
      frontendSettings,
      backendSettings
    )
    const updatedBackendSettings = await settingsApi.updateSettings(orgNumber, backendRequest)

    // Persist only non-sensitive local preferences.
    const localSettings = Object.fromEntries(
      Object.values(frontendSettings)
        .flatMap((section) => section.items)
        .filter((item) => item.persistence === 'local')
        .map((item) => [item.id, item.current_value])
    )
    writeLocalSettings(orgNumber, localSettings)

    return updatedBackendSettings
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to save settings'
    return null
  } finally {
    isLoading.value = false
  }
}

/**
 * Export all settings as JSON file
 * @param frontendSettings Settings to export
 * @param backendSettings Backend settings for reference
 * @param orgNumber Organization number
 */
const exportSettings = (
  frontendSettings: SettingsState,
  backendSettings: BackendSettings,
  orgNumber: number
) => {
  const allItems = Object.values(frontendSettings).flatMap((section) => section.items)
  const localSettings = readLocalSettings(orgNumber)
  const backendPayload = mapFrontendSettingsToBackend(frontendSettings, backendSettings)

  const exportData = {
    organization: orgNumber,
    exportedAt: new Date().toISOString(),
    persistenceStrategy: {
      backend: allItems.filter((item) => item.persistence === 'backend').map((item) => item.id),
      local: allItems.filter((item) => item.persistence === 'local').map((item) => item.id),
      readonly: allItems.filter((item) => item.persistence === 'readonly').map((item) => item.id),
    },
    backendSettings: {
      ...backendPayload,
      orgNumber: backendSettings.orgNumber,
      createdAt: backendSettings.createdAt,
      updatedAt: backendSettings.updatedAt,
    },
    localSettings,
    readonlySnapshot: Object.fromEntries(
      allItems
        .filter((item) => item.persistence === 'readonly')
        .map((item) => [item.id, item.current_value])
    ),
  }

  const jsonString = JSON.stringify(exportData, null, 2)
  const blob = new Blob([jsonString], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `org-settings-${orgNumber}-${new Date().toISOString().split('T')[0]}.json`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

/**
 * Fetch settings from backend
 * @param orgNumber Organization number
 * @returns Backend settings
 */
const fetchSettingsFromBackend = async (orgNumber: number): Promise<BackendSettings | null> => {
  isLoading.value = true
  error.value = null

  try {
    const backendSettings = await settingsApi.getSettings(orgNumber)
    return backendSettings
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to fetch settings'
    return null
  } finally {
    isLoading.value = false
  }
}

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
  // New backend integration methods
  saveSettings,
  exportSettings,
  applyLocalSettings,
  fetchSettingsFromBackend,
  mapBackendSettingsToFrontend,
  mapFrontendSettingsToBackend,
  isLoading,
  error,
})
