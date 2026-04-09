import { beforeEach, describe, expect, it, vi } from 'vitest'
import { client } from '@/api/client'
import { auditLogApi } from '../auditLogApi'

vi.mock('@/api/client', () => ({
  client: {
    get: vi.fn(),
  },
}))

describe('auditLogApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('fetches audit logs from admin audit endpoint', async () => {
    const responsePayload = [
      {
        auditLogId: 1,
        orgNumber: 123,
        actionType: 'CREATE_USER',
        entityType: 'AppUser',
        createdAt: '2026-01-01T00:00:00Z',
      },
    ]
    vi.mocked(client.get).mockResolvedValueOnce({ data: responsePayload })

    const result = await auditLogApi.getAuditLogs(123)

    expect(client.get).toHaveBeenCalledWith('/admin/audit-log', {
      params: { orgNumber: 123 },
    })
    expect(result).toEqual(responsePayload)
  })
})
