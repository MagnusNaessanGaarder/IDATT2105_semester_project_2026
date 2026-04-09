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
  localeCode: 'nb_NO',
  enableFoodModule: true,
  enableAlcoholModule: false,
  defaultTempMinC: 2,
  defaultTempMaxC: 8,
  reminderEmailEnabled: true,
  notificationEmail: 'alerts@example.com',
  retentionUserMonths: 12,
  retentionAuditMonths: 3,
  createdAt: '2026-01-01T00:00:00.000Z',
  updatedAt: '2026-01-02T00:00:00.000Z',
  ...overrides,
})

const findItem = (state: SettingsState, id: string): SettingItem => {
  const item = Object.values(state)
    .flatMap(section => section.items)
    .find(entry => entry.id === id)
  if (!item) {
    throw new Error(`Missing setting item: ${id}`)
  }
  return item
}

const createMockLocalStorage = (): Storage => {
  const store = new Map<string, string>()

  return {
    get length() {
      return store.size
    },
    clear: () => {
      store.clear()
    },
    getItem: (key: string) => {
      return store.has(key) ? store.get(key)! : null
    },
    key: (index: number) => {
      return Array.from(store.keys())[index] ?? null
    },
    removeItem: (key: string) => {
      store.delete(key)
    },
    setItem: (key: string, value: string) => {
      store.set(key, value)
    },
  }
}

describe('useAdminData settings persistence', () => {
  beforeEach(() => {
    vi.stubGlobal('localStorage', createMockLocalStorage())
    localStorage.clear()
    vi.clearAllMocks()
    const data = useAdminData()
    data.error.value = null
    data.isLoading.value = false
  })

  it('maps backend locale codes to language options', () => {
    const data = useAdminData()

    const english = data.mapBackendSettingsToFrontend(
      buildBackendSettings({ localeCode: 'en_US' })
    )
    const swedish = data.mapBackendSettingsToFrontend(
      buildBackendSettings({ localeCode: 'sv_SE' })
    )
    const fallback = data.mapBackendSettingsToFrontend(
      buildBackendSettings({ localeCode: 'xx_XX' })
    )

    expect(findItem(english, 'language').current_value).toBe('English')
    expect(findItem(swedish, 'language').current_value).toBe('Swedish')
    expect(findItem(fallback, 'language').current_value).toBe('Norwegian')
  })

  it('maps frontend language values back to locale codes and handles fallback', () => {
    const data = useAdminData()
    const backend = buildBackendSettings({ retentionAuditMonths: 7 })
    const state = data.mapBackendSettingsToFrontend(backend)

    findItem(state, 'language').current_value = 'en_US'
    findItem(state, 'timezone').current_value = 'UTC'
    findItem(state, 'email_critical').current_value = false
    findItem(state, 'backup_retention').current_value = 'invalid'

    const request = data.mapFrontendSettingsToBackend(state, backend)

    expect(request.localeCode).toBe('en_US')
    expect(request.timezoneName).toBe('UTC')
    expect(request.reminderEmailEnabled).toBe(false)
    expect(request.retentionAuditMonths).toBe(7)
  })

  it('applies only local settings from localStorage', () => {
    const data = useAdminData()
    const orgNumber = 123456789
    const state = data.mapBackendSettingsToFrontend(buildBackendSettings())

    localStorage.setItem(
      'admin_settings_local_123456789',
      JSON.stringify({
        date_format: 'yyyy-MM-dd',
        email_updates: true,
        timezone: 'UTC',
      })
    )

    const applied = data.applyLocalSettings(state, orgNumber)

    expect(findItem(applied, 'date_format').current_value).toBe('yyyy-MM-dd')
    expect(findItem(applied, 'email_updates').current_value).toBe(true)
    expect(findItem(applied, 'timezone').current_value).toBe('Europe/Oslo')
  })

  it('handles malformed localStorage settings', () => {
    const data = useAdminData()
    const orgNumber = 123456789
    const state = data.mapBackendSettingsToFrontend(buildBackendSettings())

    localStorage.setItem('admin_settings_local_123456789', '{invalid-json')
    const applied = data.applyLocalSettings(state, orgNumber)

    expect(applied).toBeTruthy()
    expect(data.error.value).toBeTruthy()
  })

  it('returns unchanged settings when localStorage has no entry', () => {
    const data = useAdminData()
    const orgNumber = 111222333
    const state = data.mapBackendSettingsToFrontend(buildBackendSettings())

    const applied = data.applyLocalSettings(state, orgNumber)

    expect(findItem(applied, 'date_format').current_value).toBe('dd.MM.yyyy')
  })

  it('handles missing localStorage environment gracefully', async () => {
    const data = useAdminData()
    const backend = buildBackendSettings({ orgNumber: 404 })
    const state = data.mapBackendSettingsToFrontend(backend)

    vi.stubGlobal('localStorage', undefined)
    vi.mocked(settingsApi.updateSettings).mockResolvedValueOnce(backend)

    const applied = data.applyLocalSettings(state, 404)
    const saved = await data.saveSettings(state, backend, 404)

    expect(findItem(applied, 'date_format').current_value).toBe('dd.MM.yyyy')
    expect(saved).toEqual(backend)
  })

  it('fetches backend settings and exposes loading/error state', async () => {
    const data = useAdminData()
    const backend = buildBackendSettings({ orgNumber: 42 })

    vi.mocked(settingsApi.getSettings).mockResolvedValueOnce(backend)
    const loaded = await data.fetchSettingsFromBackend(42)

    expect(loaded).toEqual(backend)
    expect(data.isLoading.value).toBe(false)
    expect(data.error.value).toBeNull()

    vi.mocked(settingsApi.getSettings).mockRejectedValueOnce(new Error('fetch failed'))
    const failed = await data.fetchSettingsFromBackend(42)

    expect(failed).toBeNull()
    expect(data.error.value).toBe('fetch failed')
    expect(data.isLoading.value).toBe(false)
  })

  it('saves backend settings and persists only local preferences', async () => {
    const data = useAdminData()
    const backend = buildBackendSettings({
      orgNumber: 555,
      retentionAuditMonths: 2,
    })
    const updated = buildBackendSettings({
      orgNumber: 555,
      localeCode: 'en_US',
      timezoneName: 'UTC',
      reminderEmailEnabled: false,
      retentionAuditMonths: 4,
    })
    const state = data.mapBackendSettingsToFrontend(backend)

    findItem(state, 'language').current_value = 'English'
    findItem(state, 'timezone').current_value = 'UTC'
    findItem(state, 'email_critical').current_value = false
    findItem(state, 'email_updates').current_value = true
    findItem(state, 'in_app_notifications').current_value = false
    findItem(state, 'backup_retention').current_value = 120
    findItem(state, 'password_expires').current_value = false

    vi.mocked(settingsApi.updateSettings).mockResolvedValueOnce(updated)

    const result = await data.saveSettings(state, backend, 555)

    expect(result).toEqual(updated)
    expect(settingsApi.updateSettings).toHaveBeenCalledWith(
      555,
      expect.objectContaining({
        localeCode: 'en_US',
        timezoneName: 'UTC',
        reminderEmailEnabled: false,
        retentionAuditMonths: 4,
      })
    )

    const stored = JSON.parse(localStorage.getItem('admin_settings_local_555') || '{}')
    expect(stored).toEqual({
      date_format: 'dd.MM.yyyy',
      email_updates: true,
      in_app_notifications: false,
    })
    expect(stored).not.toHaveProperty('email_critical')
    expect(stored).not.toHaveProperty('password_expires')
  })

  it('returns null and sets error when saving fails', async () => {
    const data = useAdminData()
    const backend = buildBackendSettings({ orgNumber: 888 })
    const state = data.mapBackendSettingsToFrontend(backend)

    vi.mocked(settingsApi.updateSettings).mockRejectedValueOnce(new Error('save failed'))
    const result = await data.saveSettings(state, backend, 888)

    expect(result).toBeNull()
    expect(data.error.value).toBe('save failed')
    expect(data.isLoading.value).toBe(false)
  })

  it('exports settings with persistence metadata and snapshots', async () => {
    const data = useAdminData()
    const backend = buildBackendSettings({ orgNumber: 321 })
    const state = data.mapBackendSettingsToFrontend(backend)
    findItem(state, 'language').current_value = 'Swedish'
    findItem(state, 'backup_retention').current_value = 90

    localStorage.setItem(
      'admin_settings_local_321',
      JSON.stringify({ date_format: 'yyyy-MM-dd', in_app_notifications: false })
    )

    const originalCreateElement = document.createElement.bind(document)
    const mockAnchor = originalCreateElement('a')
    const clickSpy = vi.spyOn(mockAnchor, 'click').mockImplementation(() => undefined)
    const createElementSpy = vi
      .spyOn(document, 'createElement')
      .mockImplementation(((tagName: string) => {
        if (tagName.toLowerCase() === 'a') {
          return mockAnchor
        }
        return originalCreateElement(tagName)
      }) as typeof document.createElement)

    const createObjectURLSpy = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:settings')
    const revokeObjectURLSpy = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined)

    data.exportSettings(state, backend, 321)

    expect(createObjectURLSpy).toHaveBeenCalledTimes(1)
    const exportedBlob = createObjectURLSpy.mock.calls[0]?.[0]
    expect(exportedBlob).toBeInstanceOf(Blob)

    const text = await (exportedBlob as Blob).text()
    const parsed = JSON.parse(text)

    expect(parsed.organization).toBe(321)
    expect(parsed.persistenceStrategy.backend).toContain('language')
    expect(parsed.persistenceStrategy.local).toContain('date_format')
    expect(parsed.persistenceStrategy.readonly).toContain('session_timeout')
    expect(parsed.backendSettings.localeCode).toBe('sv_SE')
    expect(parsed.localSettings).toEqual({
      date_format: 'yyyy-MM-dd',
      in_app_notifications: false,
    })
    expect(parsed.readonlySnapshot).toHaveProperty('password_expires')

    expect(clickSpy).toHaveBeenCalledTimes(1)
    expect(revokeObjectURLSpy).toHaveBeenCalledWith('blob:settings')

    clickSpy.mockRestore()
    createElementSpy.mockRestore()
    createObjectURLSpy.mockRestore()
    revokeObjectURLSpy.mockRestore()
  })

  it('formats role/status labels and date values', () => {
    const data = useAdminData()

    expect(data.roleLabel('ADMIN')).toBe('Admin')
    expect(data.roleLabel('MANAGER')).toBe('Leder')
    expect(data.roleLabel('STAFF')).toBe('Ansatt')
    expect(data.roleTone('ADMIN')).toBe('red')
    expect(data.roleTone('MANAGER')).toBe('amber')
    expect(data.roleTone('STAFF')).toBe('blue')
    expect(data.roleDescription('ADMIN')).toContain('Full tilgang')
    expect(data.roleDescription('MANAGER')).toContain('Operativ styring')
    expect(data.roleDescription('STAFF')).toContain('Daglig bruk')
    expect(data.statusLabel('active')).toBe('Aktiv')
    expect(data.statusLabel('inactive')).toBe('Inaktiv')
    expect(data.formatDate('2026-01-03T00:00:00.000Z')).not.toBe('2026-01-03T00:00:00.000Z')
    expect(data.formatDateTime('2026-01-03T13:14:00.000Z')).not.toBe('2026-01-03T13:14:00.000Z')
    expect(data.formatDate('not-a-date')).toBe('not-a-date')
    expect(data.formatDateTime('not-a-date')).toBe('not-a-date')
  })
})
