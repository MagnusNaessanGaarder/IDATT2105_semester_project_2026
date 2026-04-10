import { beforeEach, describe, expect, it, vi } from 'vitest'
import { settingsApi } from '@/features/admin/api/settingsApi'
import {
  cacheOrganizationSettings,
  clearOrganizationSettingsCache,
  ensureOrganizationSettings,
  formatDateForOrganization,
  getCachedOrganizationSettings,
  getOrganizationLocale,
  getOrganizationTempDefaults,
  getOrganizationTimezone,
  isModuleEnabled,
} from '../orgSettings'

vi.mock('@/features/admin/api/settingsApi', () => ({
  settingsApi: {
    getSettings: vi.fn(),
  },
}))

describe('orgSettings runtime helpers', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.clearAllMocks()
  })

  it('caches and reads organization settings', () => {
    cacheOrganizationSettings({
      orgNumber: 42,
      localeCode: 'en_US',
      timezoneName: 'UTC',
      enableFoodModule: false,
      enableAlcoholModule: true,
      defaultTempMinC: 1,
      defaultTempMaxC: 5,
      reminderEmailEnabled: true,
      notificationEmail: 'alerts@example.com',
      reminderRecipientScope: 'ADMIN_MANAGER',
      reminderLeadHours: 0,
      reminderRepeatHours: 24,
      retentionUserMonths: 12,
      retentionAuditMonths: 6,
      createdAt: '',
      updatedAt: '',
    })

    const cached = getCachedOrganizationSettings(42)
    expect(cached?.localeCode).toBe('en-US')
    expect(cached?.timezoneName).toBe('UTC')
    expect(isModuleEnabled('food', 42)).toBe(false)
    expect(isModuleEnabled('alcohol', 42)).toBe(true)
    expect(getOrganizationTempDefaults(42)).toEqual({ min: 1, max: 5 })
    expect(getOrganizationLocale(42)).toBe('en-US')
    expect(getOrganizationTimezone(42)).toBe('UTC')
  })

  it('loads settings from backend when cache is empty', async () => {
    vi.mocked(settingsApi.getSettings).mockResolvedValueOnce({
      orgNumber: 99,
      localeCode: 'nb-NO',
      timezoneName: 'Europe/Oslo',
      enableFoodModule: true,
      enableAlcoholModule: true,
      defaultTempMinC: null,
      defaultTempMaxC: null,
      reminderEmailEnabled: true,
      notificationEmail: null,
      reminderRecipientScope: 'ADMIN_MANAGER',
      reminderLeadHours: 0,
      reminderRepeatHours: 24,
      retentionUserMonths: 12,
      retentionAuditMonths: 6,
      createdAt: '',
      updatedAt: '',
    })

    const loaded = await ensureOrganizationSettings(99)
    expect(loaded?.orgNumber).toBe(99)
    expect(settingsApi.getSettings).toHaveBeenCalledWith(99)
    expect(getCachedOrganizationSettings(99)?.orgNumber).toBe(99)
  })

  it('formats date using organization locale/timezone', () => {
    cacheOrganizationSettings({
      orgNumber: 1,
      localeCode: 'en-US',
      timezoneName: 'UTC',
      enableFoodModule: true,
      enableAlcoholModule: true,
      defaultTempMinC: null,
      defaultTempMaxC: null,
      reminderEmailEnabled: true,
      notificationEmail: null,
      reminderRecipientScope: 'ADMIN_MANAGER',
      reminderLeadHours: 0,
      reminderRepeatHours: 24,
      retentionUserMonths: null,
      retentionAuditMonths: null,
      createdAt: '',
      updatedAt: '',
    })

    const formatted = formatDateForOrganization('2026-01-02T12:00:00.000Z', 1, {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    })
    expect(formatted).toContain('01')
    expect(formatted).toContain('02')
  })

  it('clears settings cache', () => {
    cacheOrganizationSettings({
      orgNumber: 7,
      localeCode: 'nb-NO',
      timezoneName: 'Europe/Oslo',
      enableFoodModule: true,
      enableAlcoholModule: true,
      defaultTempMinC: null,
      defaultTempMaxC: null,
      reminderEmailEnabled: true,
      notificationEmail: null,
      reminderRecipientScope: 'ADMIN_MANAGER',
      reminderLeadHours: 0,
      reminderRepeatHours: 24,
      retentionUserMonths: null,
      retentionAuditMonths: null,
      createdAt: '',
      updatedAt: '',
    })

    clearOrganizationSettingsCache()
    expect(getCachedOrganizationSettings(7)).toBeNull()
  })
})
