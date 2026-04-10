import { beforeEach, describe, expect, it, vi } from 'vitest'
import { client } from '@/api/client'
import { settingsApi, type BackendSettingsRequest } from '../settingsApi'

vi.mock('@/api/client', () => ({
  client: {
    get: vi.fn(),
    put: vi.fn(),
    post: vi.fn(),
  },
}))

describe('settingsApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('fetches settings from canonical organization endpoint', async () => {
    vi.mocked(client.get).mockResolvedValueOnce({ data: { orgNumber: 937219997 } })

    await settingsApi.getSettings(937219997)

    expect(client.get).toHaveBeenCalledWith('/organizations/937219997/settings')
  })

  it('updates settings through canonical organization endpoint', async () => {
    const payload: BackendSettingsRequest = {
      timezoneName: 'UTC',
      localeCode: 'en-US',
      enableFoodModule: true,
      enableAlcoholModule: false,
      defaultTempMinC: 1,
      defaultTempMaxC: 6,
      reminderEmailEnabled: true,
      notificationEmail: 'alerts@example.com',
      retentionUserMonths: 12,
      retentionAuditMonths: 6,
    }

    vi.mocked(client.put).mockResolvedValueOnce({ data: { ...payload, orgNumber: 937219997 } })

    await settingsApi.updateSettings(937219997, payload)

    expect(client.put).toHaveBeenCalledWith('/organizations/937219997/settings', payload)
  })
})
