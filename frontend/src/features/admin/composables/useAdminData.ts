import { ref } from 'vue'
import {
  settingsApi,
  type BackendSettings,
  type BackendSettingsRequest,
} from '../api/settingsApi'
import { getOrgNumber } from '@/shared/utils/orgContext'
import { formatDateForOrganization, formatDateTimeForOrganization } from '@/shared/utils/orgSettings'

export type UserRole = 'ADMIN' | 'MANAGER' | 'EMPLOYEE'
export type UserStatus = 'active' | 'inactive'

export interface SettingItem {
  id: string
  label: string
  description?: string
  type: 'select' | 'toggle' | 'number' | 'email' | 'text' | 'tel'
  current_value: unknown
  options?: string[]
  min?: number
  max?: number
  step?: number
  placeholder?: string
}

export interface SettingSection {
  id: string
  section_title: string
  items: SettingItem[]
}

export interface SettingsState {
  profile: SettingSection
  organization: SettingSection
  modules: SettingSection
  temperature: SettingSection
  alerts_retention: SettingSection
}

const createSettingsTemplate = (): SettingsState => ({
  profile: {
    id: 'profile',
    section_title: 'Organisasjonsprofil',
    items: [
      {
        id: 'display_name',
        label: 'Visningsnavn',
        description: 'Navnet som vises i applikasjonen',
        type: 'text',
        current_value: '',
        placeholder: 'Bedrift AS',
      },
      {
        id: 'legal_name',
        label: 'Juridisk navn',
        description: 'Offisielt registrert navn på organisasjonen',
        type: 'text',
        current_value: '',
        placeholder: 'Bedrift AS',
      },
      {
        id: 'contact_email',
        label: 'Kontakt e-post',
        description: 'Hovede-postadresse for organisasjonen',
        type: 'email',
        current_value: '',
        placeholder: 'kontakt@bedrift.no',
      },
      {
        id: 'contact_phone',
        label: 'Kontakt telefon',
        description: 'Telefonnummer for organisasjonen',
        type: 'tel',
        current_value: '',
        placeholder: '+47 123 45 678',
      },
    ],
  },
  organization: {
    id: 'organization',
    section_title: 'Språk og tidssone',
    items: [
      {
        id: 'locale_code',
        label: 'Locale',
        description: 'Felles locale (språk og regionformat) for organisasjonen',
        type: 'select',
        current_value: 'nb-NO',
        options: ['nb-NO', 'nn-NO', 'en-US', 'en-GB', 'sv-SE', 'da-DK', 'fi-FI', 'de-DE', 'fr-FR', 'pl-PL'],
      },
      {
        id: 'timezone_name',
        label: 'Tidssone',
        description: 'Tidssone brukt i tidsstempler og registreringer',
        type: 'select',
        current_value: 'Europe/Oslo',
        options: [
          'Europe/Oslo',
          'Europe/Stockholm',
          'Europe/Copenhagen',
          'Europe/Helsinki',
          'Europe/London',
          'Europe/Berlin',
          'Europe/Paris',
          'Europe/Warsaw',
          'UTC',
          'America/New_York',
          'America/Chicago',
          'America/Denver',
          'America/Los_Angeles',
        ],
      },
    ],
  },
  modules: {
    id: 'modules',
    section_title: 'Moduler',
    items: [
      {
        id: 'enable_food_module',
        label: 'Aktiver IK-Mat',
        description: 'Skru av/på IK-Mat-modulen for organisasjonen',
        type: 'toggle',
        current_value: true,
      },
      {
        id: 'enable_alcohol_module',
        label: 'Aktiver IK-Alkohol',
        description: 'Skru av/på IK-Alkohol-modulen for organisasjonen',
        type: 'toggle',
        current_value: true,
      },
    ],
  },
  temperature: {
    id: 'temperature',
    section_title: 'Temperaturstandard',
    items: [
      {
        id: 'default_temp_min_c',
        label: 'Standard min temperatur (°C)',
        description: 'Minimumstemperatur brukt som standard for nye temperaturpunkter',
        type: 'number',
        current_value: 2,
        min: -25,
        max: 25,
        step: 0.5,
      },
      {
        id: 'default_temp_max_c',
        label: 'Standard maks temperatur (°C)',
        description: 'Maksimumstemperatur brukt som standard for nye temperaturpunkter',
        type: 'number',
        current_value: 8,
        min: -25,
        max: 25,
        step: 0.5,
      },
    ],
  },
  alerts_retention: {
    id: 'alerts_retention',
    section_title: 'Varsling og lagring',
    items: [
      {
        id: 'reminder_email_enabled',
        label: 'E-postpåminnelser aktivert',
        description: 'Send automatiske e-postpåminnelser om kommende kontroller',
        type: 'toggle',
        current_value: true,
      },
      {
        id: 'notification_email',
        label: 'E-post for varsler',
        description: 'Mottaker for kritiske varslingshendelser',
        type: 'email',
        current_value: '',
        placeholder: 'din.epost@bedrift.no',
      },
      {
        id: 'retention_user_months',
        label: 'Behold brukerdata (måneder)',
        description: 'Hvor lenge brukerdata (brukeraktivitet, pålogginger) lagres før automatisk sletting',
        type: 'number',
        current_value: 12,
        min: 1,
        max: 120,
        step: 1,
      },
      {
        id: 'retention_audit_months',
        label: 'Behold revisjonsdata (måneder)',
        description: 'Hvor lenge revisjonslogger og sporbarhetsdata beholdes (GDPR-krav: minimum 5 år)',
        type: 'number',
        current_value: 60,
        min: 12,
        max: 120,
        step: 1,
      },
    ],
  },
})

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
  return formatDateForOrganization(value, getOrgNumber(), {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

const formatDateTime = (value: string): string => {
  return formatDateTimeForOrganization(value, getOrgNumber(), {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const cloneSettings = (source: SettingsState): SettingsState => {
  return JSON.parse(JSON.stringify(source)) as SettingsState
}

const settings = createSettingsTemplate()

const normalizeLocaleCode = (value: string | null | undefined): string => {
  if (!value) {
    return 'nb-NO'
  }
  if (value === 'nb_NO') return 'nb-NO'
  if (value === 'en_US') return 'en-US'
  if (value === 'sv_SE') return 'sv-SE'
  return value
}

const toBackendLocaleCode = (value: unknown): string => {
  if (value === 'nb_NO') return 'nb-NO'
  if (value === 'en_US') return 'en-US'
  if (value === 'sv_SE') return 'sv-SE'
  return typeof value === 'string' && value.trim().length > 0 ? value : 'nb-NO'
}

const findItem = (source: SettingsState, section: keyof SettingsState, id: string): SettingItem | undefined => {
  return source[section].items.find((entry) => entry.id === id)
}

const getItemValue = (source: SettingsState, section: keyof SettingsState, id: string): unknown => {
  return findItem(source, section, id)?.current_value
}

const toNullableNumber = (value: unknown): number | null => {
  if (value === null || value === undefined || value === '') {
    return null
  }

  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : null
}

const toNullableInteger = (value: unknown): number | null => {
  const numeric = toNullableNumber(value)
  if (numeric === null) {
    return null
  }

  return Math.round(numeric)
}

const mapBackendSettingsToFrontend = (backendSettings: BackendSettings): SettingsState => {
  const mapped = cloneSettings(settings)

  const applyValue = (section: keyof SettingsState, id: string, value: unknown) => {
    const item = findItem(mapped, section, id)
    if (item) {
      item.current_value = value
    }
  }

  applyValue('organization', 'locale_code', normalizeLocaleCode(backendSettings.localeCode))
  applyValue('organization', 'timezone_name', backendSettings.timezoneName)
  applyValue('modules', 'enable_food_module', backendSettings.enableFoodModule)
  applyValue('modules', 'enable_alcohol_module', backendSettings.enableAlcoholModule)
  applyValue('temperature', 'default_temp_min_c', backendSettings.defaultTempMinC)
  applyValue('temperature', 'default_temp_max_c', backendSettings.defaultTempMaxC)
  applyValue('alerts_retention', 'reminder_email_enabled', backendSettings.reminderEmailEnabled)
  applyValue('alerts_retention', 'notification_email', backendSettings.notificationEmail ?? '')
  applyValue('profile', 'display_name', backendSettings.displayName ?? '')
  applyValue('profile', 'legal_name', backendSettings.legalName ?? '')
  applyValue('profile', 'contact_email', backendSettings.contactEmail ?? '')
  applyValue('profile', 'contact_phone', backendSettings.contactPhone ?? '')
  applyValue('alerts_retention', 'retention_user_months', backendSettings.retentionUserMonths)
  applyValue('alerts_retention', 'retention_audit_months', backendSettings.retentionAuditMonths)

  return mapped
}

const mapFrontendSettingsToBackend = (frontendSettings: SettingsState): BackendSettingsRequest => {
  const notificationEmailRaw = getItemValue(frontendSettings, 'alerts_retention', 'notification_email')
  const notificationEmail = typeof notificationEmailRaw === 'string' ? notificationEmailRaw.trim() : ''

  const displayNameRaw = getItemValue(frontendSettings, 'profile', 'display_name')
  const legalNameRaw = getItemValue(frontendSettings, 'profile', 'legal_name')
  const contactEmailRaw = getItemValue(frontendSettings, 'profile', 'contact_email')
  const contactPhoneRaw = getItemValue(frontendSettings, 'profile', 'contact_phone')

  return {
    timezoneName: String(getItemValue(frontendSettings, 'organization', 'timezone_name') ?? 'Europe/Oslo'),
    localeCode: toBackendLocaleCode(getItemValue(frontendSettings, 'organization', 'locale_code')),
    enableFoodModule: getItemValue(frontendSettings, 'modules', 'enable_food_module') === true,
    enableAlcoholModule: getItemValue(frontendSettings, 'modules', 'enable_alcohol_module') === true,
    defaultTempMinC: toNullableNumber(getItemValue(frontendSettings, 'temperature', 'default_temp_min_c')),
    defaultTempMaxC: toNullableNumber(getItemValue(frontendSettings, 'temperature', 'default_temp_max_c')),
    reminderEmailEnabled: getItemValue(frontendSettings, 'alerts_retention', 'reminder_email_enabled') === true,
    notificationEmail: notificationEmail.length > 0 ? notificationEmail : null,
    displayName: typeof displayNameRaw === 'string' && displayNameRaw.trim().length > 0 ? displayNameRaw.trim() : null,
    legalName: typeof legalNameRaw === 'string' && legalNameRaw.trim().length > 0 ? legalNameRaw.trim() : null,
    contactEmail: typeof contactEmailRaw === 'string' && contactEmailRaw.trim().length > 0 ? contactEmailRaw.trim() : null,
    contactPhone: typeof contactPhoneRaw === 'string' && contactPhoneRaw.trim().length > 0 ? contactPhoneRaw.trim() : null,
    retentionUserMonths: toNullableInteger(getItemValue(frontendSettings, 'alerts_retention', 'retention_user_months')),
    retentionAuditMonths: toNullableInteger(getItemValue(frontendSettings, 'alerts_retention', 'retention_audit_months')),
  }
}

const saveSettings = async (
  frontendSettings: SettingsState,
  orgNumber: number
): Promise<BackendSettings | null> => {
  isLoading.value = true
  error.value = null

  try {
    const backendRequest = mapFrontendSettingsToBackend(frontendSettings)
    const updatedBackendSettings = await settingsApi.updateSettings(orgNumber, backendRequest)
    return updatedBackendSettings
  } catch (err: unknown) {
    const axiosError = err as { response?: { status?: number; data?: { message?: string } } }
    const status = axiosError?.response?.status
    const message = axiosError?.response?.data?.message

    if (status === 401) {
      error.value = 'Du må logge inn på nytt for å lagre innstillinger.'
    } else if (status === 403) {
      error.value = 'Du har ikke tilgang til å lagre innstillinger.'
    } else if (status === 400 && message) {
      error.value = `Valideringsfeil: ${message}`
    } else if (status === 422 && message) {
      error.value = `Ugyldige data: ${message}`
    } else if (status && status >= 500) {
      error.value = 'Serverfeil. Prøv igjen senere eller kontakt support.'
    } else {
      error.value = 'Kunne ikke lagre innstillinger. Sjekk nettverksforbindelsen og prøv igjen.'
    }
    return null
  } finally {
    isLoading.value = false
  }
}

const fetchSettingsFromBackend = async (orgNumber: number): Promise<BackendSettings | null> => {
  isLoading.value = true
  error.value = null

  try {
    const backendSettings = await settingsApi.getSettings(orgNumber)
    return backendSettings
  } catch (err: unknown) {
    const axiosError = err as { response?: { status?: number } }
    const status = axiosError?.response?.status

    if (status === 401) {
      error.value = 'Du må logge inn på nytt for å se innstillinger.'
    } else if (status === 403) {
      error.value = 'Du har ikke tilgang til å se innstillinger.'
    } else if (status === 404) {
      error.value = 'Innstillinger ikke funnet for denne organisasjonen.'
    } else {
      error.value = 'Kunne ikke laste inn innstillinger. Sjekk nettverksforbindelsen og prøv igjen.'
    }
    return null
  } finally {
    isLoading.value = false
  }
}

export const useAdminData = () => ({
  settings,
  roleLabel,
  roleTone,
  roleDescription,
  statusLabel,
  formatDate,
  formatDateTime,
  saveSettings,
  fetchSettingsFromBackend,
  mapBackendSettingsToFrontend,
  mapFrontendSettingsToBackend,
  isLoading,
  error,
})
