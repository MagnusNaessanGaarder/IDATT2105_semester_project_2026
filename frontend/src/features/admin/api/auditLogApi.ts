import { client } from '@/api/client'

export interface AuditLogResponse {
  auditLogId: number
  orgNumber: number
  actionType: string
  entityType: string
  entityId?: number | null
  oldValuesJson?: string | null
  newValuesJson?: string | null
  userAgent?: string | null
  createdAt: string
  actedByUser?: {
    userId?: number
    displayName?: string
    email?: string
  } | null
}

export const auditLogApi = {
  async getAuditLogs(orgNumber: number): Promise<AuditLogResponse[]> {
    const response = await client.get('/admin/audit-log', {
      params: { orgNumber },
    })
    return response.data
  },
}
