import { settingsApi, type BackendSettings } from '@/features/admin/api/settingsApi'

const ORG_SETTINGS_CACHE_KEY = 'orgSettings'
const DEFAULT_LOCALE = 'nb-NO'
const DEFAULT_TIMEZONE = 'Europe/Oslo'

export interface OrganizationRuntimeSettings {
  orgNumber: number
  localeCode: string
  timezoneName: string
  enableFoodModule: boolean
  enableAlcoholModule: boolean
  defaultTempMinC: number | null
  defaultTempMaxC: number | null
}

const normalizeLocale = (value: string | null | undefined): string => {
  if (!value) return DEFAULT_LOCALE
  if (value === 'nb_NO') return 'nb-NO'
  if (value === 'en_US') return 'en-US'
  if (value === 'sv_SE') return 'sv-SE'
  return value
}

const toRuntimeSettings = (settings: BackendSettings): OrganizationRuntimeSettings => {
  return {
    orgNumber: settings.orgNumber,
    localeCode: normalizeLocale(settings.localeCode),
    timezoneName: settings.timezoneName || DEFAULT_TIMEZONE,
    enableFoodModule: settings.enableFoodModule,
    enableAlcoholModule: settings.enableAlcoholModule,
    defaultTempMinC: settings.defaultTempMinC ?? null,
    defaultTempMaxC: settings.defaultTempMaxC ?? null,
  }
}

const readCachedMap = (): Record<string, OrganizationRuntimeSettings> => {
  try {
    const raw = sessionStorage.getItem(ORG_SETTINGS_CACHE_KEY)
    if (!raw) return {}
    const parsed = JSON.parse(raw) as Record<string, OrganizationRuntimeSettings>
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

const writeCachedMap = (map: Record<string, OrganizationRuntimeSettings>) => {
  sessionStorage.setItem(ORG_SETTINGS_CACHE_KEY, JSON.stringify(map))
}

export const cacheOrganizationSettings = (settings: BackendSettings) => {
  const map = readCachedMap()
  map[String(settings.orgNumber)] = toRuntimeSettings(settings)
  writeCachedMap(map)
}

export const clearOrganizationSettingsCache = () => {
  sessionStorage.removeItem(ORG_SETTINGS_CACHE_KEY)
}

export const getCachedOrganizationSettings = (orgNumber?: number): OrganizationRuntimeSettings | null => {
  if (!orgNumber) return null
  const map = readCachedMap()
  return map[String(orgNumber)] ?? null
}

export const ensureOrganizationSettings = async (orgNumber: number): Promise<OrganizationRuntimeSettings | null> => {
  const cached = getCachedOrganizationSettings(orgNumber)
  if (cached) {
    return cached
  }

  try {
    const settings = await settingsApi.getSettings(orgNumber)
    cacheOrganizationSettings(settings)
    return toRuntimeSettings(settings)
  } catch {
    return null
  }
}

export const isModuleEnabled = (moduleKey: 'food' | 'alcohol', orgNumber?: number): boolean => {
  const cached = getCachedOrganizationSettings(orgNumber)
  if (!cached) {
    return true
  }

  return moduleKey === 'food' ? cached.enableFoodModule : cached.enableAlcoholModule
}

export const getOrganizationLocale = (orgNumber?: number): string => {
  const cached = getCachedOrganizationSettings(orgNumber)
  return cached?.localeCode ?? DEFAULT_LOCALE
}

export const getOrganizationTimezone = (orgNumber?: number): string => {
  const cached = getCachedOrganizationSettings(orgNumber)
  return cached?.timezoneName ?? DEFAULT_TIMEZONE
}

export const getOrganizationTempDefaults = (orgNumber?: number): { min: number | null; max: number | null } => {
  const cached = getCachedOrganizationSettings(orgNumber)
  return {
    min: cached?.defaultTempMinC ?? null,
    max: cached?.defaultTempMaxC ?? null,
  }
}

export const formatDateForOrganization = (
  value: string | Date,
  orgNumber?: number,
  options: Intl.DateTimeFormatOptions = { day: '2-digit', month: 'short', year: 'numeric' }
): string => {
  const parsed = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return String(value)
  }

  return parsed.toLocaleDateString(getOrganizationLocale(orgNumber), {
    ...options,
    timeZone: getOrganizationTimezone(orgNumber),
  })
}

export const formatDateTimeForOrganization = (
  value: string | Date,
  orgNumber?: number,
  options: Intl.DateTimeFormatOptions = {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }
): string => {
  const parsed = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return String(value)
  }

  return parsed.toLocaleString(getOrganizationLocale(orgNumber), {
    ...options,
    timeZone: getOrganizationTimezone(orgNumber),
  })
}
