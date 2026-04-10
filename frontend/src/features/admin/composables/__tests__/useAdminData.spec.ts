import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { BackendSettings } from '../../api/settingsApi'
import type { SettingItem, SettingsState } from '../useAdminData'
import { useAdminData } from '../useAdminData'
import { settingsApi } from '../../api/settingsApi'

vi.mock('../../api/settingsApi', () => ({
  settingsApi: {
    getSettings: vi.fn(),
    updateSettings: vi.fn(),
  },
}))

const buildBackendSettings = (overrides: Partial<BackendSettings> = {}): BackendSettings => ({
  orgNumber: 999,
  timezoneName: 'Europe/Oslo',
  localeCode: 'nb-NO',
  enableFoodModule: true,
  enableAlcoholModule: false,
  defaultTempMinC: 2,
  defaultTempMaxC: 8,
  reminderEmailEnabled: true,
  notificationEmail: 'alerts@example.com',
  displayName: 'Test Organization',
  legalName: 'Test Organization AS',
  contactEmail: 'contact@example.com',
  contactPhone: '+47 123 45 678',
  retentionUserMonths: 12,
  retentionAuditMonths: 6,
  createdAt: '2026-01-01T00:00:00.000Z',
  updatedAt: '2026-01-02T00:00:00.000Z',
  ...overrides,
})

const findItem = (state: SettingsState, id: string): SettingItem => {
  const item = Object.values(state)
    .flatMap((section) => section.items)
    .find((entry) => entry.id === id)
  if (!item) {
    throw new Error(`Missing setting item: ${id}`)
  }
  return item
}

describe('useAdminData settings mapping', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    const data = useAdminData()
    data.error.value = null
    data.isLoading.value = false
  })

  it('maps backend settings to organization-settings fields', () => {
    const data = useAdminData()
    const state = data.mapBackendSettingsToFrontend(
      buildBackendSettings({
        localeCode: 'en_US',
        defaultTempMinC: null,
        notificationEmail: null,
      })
    )

    expect(findItem(state, 'locale_code').current_value).toBe('en-US')
    expect(findItem(state, 'timezone_name').current_value).toBe('Europe/Oslo')
    expect(findItem(state, 'enable_food_module').current_value).toBe(true)
    expect(findItem(state, 'enable_alcohol_module').current_value).toBe(false)
    expect(findItem(state, 'default_temp_min_c').current_value).toBeNull()
    expect(findItem(state, 'notification_email').current_value).toBe('')
    expect(findItem(state, 'retention_audit_months').current_value).toBe(6)
  })

  it('maps frontend state back to canonical backend payload', () => {
    const data = useAdminData()
    const state = data.mapBackendSettingsToFrontend(buildBackendSettings())

    findItem(state, 'locale_code').current_value = 'sv-SE'
    findItem(state, 'timezone_name').current_value = 'UTC'
    findItem(state, 'enable_food_module').current_value = false
    findItem(state, 'enable_alcohol_module').current_value = true
    findItem(state, 'default_temp_min_c').current_value = '1.5'
    findItem(state, 'default_temp_max_c').current_value = '6.8'
    findItem(state, 'reminder_email_enabled').current_value = false
    findItem(state, 'notification_email').current_value = ''
    findItem(state, 'retention_user_months').current_value = '11'
    findItem(state, 'retention_audit_months').current_value = 9.4

    const request = data.mapFrontendSettingsToBackend(state)

    expect(request).toEqual({
      timezoneName: 'UTC',
      localeCode: 'sv-SE',
      enableFoodModule: false,
      enableAlcoholModule: true,
      defaultTempMinC: 1.5,
      defaultTempMaxC: 6.8,
      reminderEmailEnabled: false,
      notificationEmail: null,
      displayName: 'Test Organization',
      legalName: 'Test Organization AS',
      contactEmail: 'contact@example.com',
      contactPhone: '+47 123 45 678',
      retentionUserMonths: 11,
      retentionAuditMonths: 9,
    })
  })

  it('saves settings using organization settings endpoint payload', async () => {
    const data = useAdminData()
    const current = buildBackendSettings({ orgNumber: 555 })
    const state = data.mapBackendSettingsToFrontend(current)
    const updated = buildBackendSettings({
      orgNumber: 555,
      timezoneName: 'UTC',
      localeCode: 'en-US',
    })

    findItem(state, 'timezone_name').current_value = 'UTC'
    findItem(state, 'locale_code').current_value = 'en-US'

    vi.mocked(settingsApi.updateSettings).mockResolvedValueOnce(updated)
    const result = await data.saveSettings(state, 555)

    expect(result).toEqual(updated)
    expect(settingsApi.updateSettings).toHaveBeenCalledWith(
      555,
      expect.objectContaining({
        timezoneName: 'UTC',
        localeCode: 'en-US',
      })
    )
  })

  it('fetches backend settings and reports errors on failure', async () => {
    const data = useAdminData()
    const backend = buildBackendSettings({ orgNumber: 42 })

    vi.mocked(settingsApi.getSettings).mockResolvedValueOnce(backend)
    const loaded = await data.fetchSettingsFromBackend(42)
    expect(loaded).toEqual(backend)
    expect(data.error.value).toBeNull()

    vi.mocked(settingsApi.getSettings).mockRejectedValueOnce(new Error('fetch failed'))
    const failed = await data.fetchSettingsFromBackend(42)
    expect(failed).toBeNull()
    expect(data.error.value).toBe('Kunne ikke laste inn innstillinger. Sjekk nettverksforbindelsen og prøv igjen.')
  })

  it('formats role/status labels and date values', () => {
    const data = useAdminData()

    expect(data.roleLabel('ADMIN')).toBe('Admin')
    expect(data.roleLabel('MANAGER')).toBe('Leder')
    expect(data.roleLabel('EMPLOYEE')).toBe('Ansatt')
    expect(data.roleTone('ADMIN')).toBe('red')
    expect(data.roleTone('MANAGER')).toBe('amber')
    expect(data.roleTone('EMPLOYEE')).toBe('blue')
    expect(data.roleDescription('ADMIN')).toContain('Full tilgang')
    expect(data.roleDescription('MANAGER')).toContain('Operativ styring')
    expect(data.roleDescription('EMPLOYEE')).toContain('Daglig bruk')
    expect(data.statusLabel('active')).toBe('Aktiv')
    expect(data.statusLabel('inactive')).toBe('Inaktiv')
    expect(data.formatDate('2026-01-03T00:00:00.000Z')).not.toBe('2026-01-03T00:00:00.000Z')
    expect(data.formatDateTime('2026-01-03T13:14:00.000Z')).not.toBe('2026-01-03T13:14:00.000Z')
    expect(data.formatDate('not-a-date')).toBe('not-a-date')
    expect(data.formatDateTime('not-a-date')).toBe('not-a-date')
  })
})
