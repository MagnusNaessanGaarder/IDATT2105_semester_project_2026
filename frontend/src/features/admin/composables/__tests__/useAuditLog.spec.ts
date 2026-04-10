import { beforeEach, describe, expect, it, vi } from 'vitest'
import { auditLogApi } from '../../api/auditLogApi'
import { useAuditLog } from '../useAuditLog'

vi.mock('../../api/auditLogApi', () => ({
  auditLogApi: {
    getAuditLogs: vi.fn(),
    getAuditLogsByActionType: vi.fn(),
    getAuditLogsByDateRange: vi.fn(),
    getEntityAuditLogs: vi.fn(),
  },
}))

describe('useAuditLog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    const data = useAuditLog()
    data.auditLog.value = []
    data.error.value = null
    data.isLoading.value = false
    data.filters.value.actionType = 'ALL'
    data.filters.value.fromDate = ''
    data.filters.value.toDate = ''
    data.filters.value.entityType = ''
    data.filters.value.entityId = ''
    data.resetPagination()
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
        action: 'Endret',
        resource: 'Bruker',
        result: 'SUCCESS',
      })
    )
  })

  it('sets error and clears data when request fails', async () => {
    vi.mocked(auditLogApi.getAuditLogs).mockRejectedValueOnce(new Error('audit failed'))

    const data = useAuditLog()
    await data.fetchAuditLog(123)

    expect(data.auditLog.value).toEqual([])
    expect(data.error.value).toBe('Kunne ikke hente revisjonslogg')
    expect(data.isLoading.value).toBe(false)
  })

  it('uses action-type endpoint when action filter is set', async () => {
    vi.mocked(auditLogApi.getAuditLogsByActionType).mockResolvedValueOnce([])

    const data = useAuditLog()
    data.filters.value.actionType = 'CREATE'
    await data.fetchAuditLog(123)

    expect(auditLogApi.getAuditLogsByActionType).toHaveBeenCalledWith(123, 'CREATE')
  })

  it('uses date-range endpoint when date filters are set', async () => {
    vi.mocked(auditLogApi.getAuditLogsByDateRange).mockResolvedValueOnce([])

    const data = useAuditLog()
    data.filters.value.fromDate = '2026-01-01'
    data.filters.value.toDate = '2026-01-31'
    await data.fetchAuditLog(123)

    expect(auditLogApi.getAuditLogsByDateRange).toHaveBeenCalledWith(
      123,
      '2026-01-01T00:00:00.000Z',
      '2026-01-31T00:00:00.000Z'
    )
  })

  it('uses entity endpoint when entity filters are set', async () => {
    vi.mocked(auditLogApi.getEntityAuditLogs).mockResolvedValueOnce([])

    const data = useAuditLog()
    data.filters.value.entityType = 'AppUser'
    data.filters.value.entityId = '42'
    await data.fetchAuditLog(123)

    expect(auditLogApi.getEntityAuditLogs).toHaveBeenCalledWith(123, 'AppUser', 42)
  })

  it('sets user-friendly message on forbidden response', async () => {
    vi.mocked(auditLogApi.getAuditLogs).mockRejectedValueOnce({
      response: { status: 403 },
    })

    const data = useAuditLog()
    await data.fetchAuditLog(123)

    expect(data.error.value).toBe('Du har ikke tilgang til revisjonslogg.')
  })

  it('paginates audit log entries', async () => {
    const mockEntries = Array.from({ length: 25 }, (_, i) => ({
      auditLogId: i + 1,
      orgNumber: 123,
      actionType: 'UPDATE',
      entityType: 'AppUser',
      entityId: i + 1,
      oldValuesJson: null,
      newValuesJson: null,
      createdAt: '2026-01-01T10:00:00Z',
      actedByUser: { displayName: `User ${i + 1}`, email: `user${i + 1}@example.com` },
    }))

    vi.mocked(auditLogApi.getAuditLogs).mockResolvedValueOnce(mockEntries)

    const data = useAuditLog()
    await data.fetchAuditLog(123)

    expect(data.totalEntries.value).toBe(25)
    expect(data.totalPages.value).toBe(3)
    expect(data.paginatedAuditLog.value).toHaveLength(10) // Default page size
    expect(data.paginationInfo.value).toEqual({ start: 1, end: 10, total: 25 })
  })

  it('navigates between pages', async () => {
    const mockEntries = Array.from({ length: 25 }, (_, i) => ({
      auditLogId: i + 1,
      orgNumber: 123,
      actionType: 'UPDATE',
      entityType: 'AppUser',
      entityId: i + 1,
      oldValuesJson: null,
      newValuesJson: null,
      createdAt: '2026-01-01T10:00:00Z',
      actedByUser: { displayName: `User ${i + 1}`, email: `user${i + 1}@example.com` },
    }))

    vi.mocked(auditLogApi.getAuditLogs).mockResolvedValueOnce(mockEntries)

    const data = useAuditLog()
    await data.fetchAuditLog(123)

    data.goToPage(2)
    expect(data.currentPage.value).toBe(2)
    expect(data.paginatedAuditLog.value[0]?.id).toBe(11)

    data.goToNextPage()
    expect(data.currentPage.value).toBe(3)

    data.goToPreviousPage()
    expect(data.currentPage.value).toBe(2)

    data.goToFirstPage()
    expect(data.currentPage.value).toBe(1)

    data.goToLastPage()
    expect(data.currentPage.value).toBe(3)
  })

  it('changes page size and resets to first page', async () => {
    const mockEntries = Array.from({ length: 25 }, (_, i) => ({
      auditLogId: i + 1,
      orgNumber: 123,
      actionType: 'UPDATE',
      entityType: 'AppUser',
      entityId: i + 1,
      oldValuesJson: null,
      newValuesJson: null,
      createdAt: '2026-01-01T10:00:00Z',
      actedByUser: { displayName: `User ${i + 1}`, email: `user${i + 1}@example.com` },
    }))

    vi.mocked(auditLogApi.getAuditLogs).mockResolvedValueOnce(mockEntries)

    const data = useAuditLog()
    await data.fetchAuditLog(123)

    data.goToPage(3)
    expect(data.currentPage.value).toBe(3)

    data.setPageSize(25)
    expect(data.pageSize.value).toBe(25)
    expect(data.currentPage.value).toBe(1) // Reset to first page
  })

  it('validates date range', () => {
    const data = useAuditLog()

    data.filters.value.fromDate = '2026-01-15'
    data.filters.value.toDate = '2026-01-10'

    const validation = data.validateDateRange()
    expect(validation.valid).toBe(false)
    expect(validation.error).toBe('Fra-dato kan ikke være etter til-dato')
  })

  it('accepts valid date range', () => {
    const data = useAuditLog()

    data.filters.value.fromDate = '2026-01-10'
    data.filters.value.toDate = '2026-01-15'

    const validation = data.validateDateRange()
    expect(validation.valid).toBe(true)
  })

  it('exports audit log to CSV', async () => {
    const mockEntries = [
      {
        auditLogId: 1,
        orgNumber: 123,
        actionType: 'CREATE',
        entityType: 'AppUser',
        entityId: 1,
        oldValuesJson: null,
        newValuesJson: '{"name":"Test"}',
        createdAt: '2026-01-01T10:00:00Z',
        actedByUser: { displayName: 'Admin', email: 'admin@example.com' },
      },
    ]

    vi.mocked(auditLogApi.getAuditLogs).mockResolvedValueOnce(mockEntries)

    const data = useAuditLog()
    await data.fetchAuditLog(123)

    const csv = data.exportToCsv('test.csv')
    expect(csv).toContain('Tid;Bruker;Handling;Ressurs;Detaljer;Resultat')
    expect(csv).toContain('La til')
    expect(csv).toContain('Admin')
  })
})
