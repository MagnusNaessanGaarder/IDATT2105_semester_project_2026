import { beforeEach, describe, expect, it, vi } from 'vitest'
import { auditLogApi } from '../../api/auditLogApi'
import { useAuditLog } from '../useAuditLog'

vi.mock('../../api/auditLogApi', () => ({
  auditLogApi: {
    getAuditLogs: vi.fn(),
  },
}))

describe('useAuditLog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    const data = useAuditLog()
    data.auditLog.value = []
    data.error.value = null
    data.isLoading.value = false
  })

  it('loads and maps audit logs from backend', async () => {
    vi.mocked(auditLogApi.getAuditLogs).mockResolvedValueOnce([
      {
        auditLogId: 1,
        orgNumber: 123,
        actionType: 'UPDATE_USER',
        entityType: 'AppUser',
        entityId: 7,
        oldValuesJson: '{"displayName":"Old"}',
        newValuesJson: '{"displayName":"New"}',
        createdAt: '2026-01-01T10:00:00Z',
        actedByUser: {
          displayName: 'Admin User',
          email: 'admin@example.com',
        },
      },
    ])

    const data = useAuditLog()
    await data.fetchAuditLog(123)

    expect(auditLogApi.getAuditLogs).toHaveBeenCalledWith(123)
    expect(data.error.value).toBeNull()
    expect(data.auditLog.value).toHaveLength(1)
    expect(data.auditLog.value[0]).toEqual(
      expect.objectContaining({
        id: 1,
        user: 'Admin User',
        action: 'UPDATE_USER',
        resource: 'AppUser',
        result: 'SUCCESS',
      })
    )
  })

  it('sets error and clears data when request fails', async () => {
    vi.mocked(auditLogApi.getAuditLogs).mockRejectedValueOnce(new Error('audit failed'))

    const data = useAuditLog()
    await data.fetchAuditLog(123)

    expect(data.auditLog.value).toEqual([])
    expect(data.error.value).toBe('audit failed')
    expect(data.isLoading.value).toBe(false)
  })
})
