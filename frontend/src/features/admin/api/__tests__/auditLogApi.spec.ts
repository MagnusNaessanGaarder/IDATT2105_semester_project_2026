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

  it('fetches audit logs filtered by action type', async () => {
    vi.mocked(client.get).mockResolvedValueOnce({ data: [] })

    await auditLogApi.getAuditLogsByActionType(123, 'CREATE')

    expect(client.get).toHaveBeenCalledWith('/admin/audit-log/action/CREATE', {
      params: { orgNumber: 123 },
    })
  })

  it('fetches audit logs by date range', async () => {
    vi.mocked(client.get).mockResolvedValueOnce({ data: [] })

    await auditLogApi.getAuditLogsByDateRange(
      123,
      '2026-01-01T00:00:00.000Z',
      '2026-01-31T23:59:59.000Z'
    )

    expect(client.get).toHaveBeenCalledWith('/admin/audit-log/date-range', {
      params: {
        orgNumber: 123,
        fromDate: '2026-01-01T00:00:00.000Z',
        toDate: '2026-01-31T23:59:59.000Z',
      },
    })
  })

  it('fetches audit logs for specific entity', async () => {
    vi.mocked(client.get).mockResolvedValueOnce({ data: [] })

    await auditLogApi.getEntityAuditLogs(123, 'AppUser', 42)

    expect(client.get).toHaveBeenCalledWith('/admin/audit-log/entity/AppUser/42', {
      params: { orgNumber: 123 },
    })
  })
})
