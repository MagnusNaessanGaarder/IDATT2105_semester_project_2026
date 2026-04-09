import { client } from '@/api/client'

export type AuditActionType = 'CREATE' | 'UPDATE' | 'DELETE' | 'LOGIN' | 'LOGOUT' | 'EXPORT' | 'VIEW'

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

  async getAuditLogsByActionType(
    orgNumber: number,
    actionType: AuditActionType
  ): Promise<AuditLogResponse[]> {
    const response = await client.get(`/admin/audit-log/action/${actionType}`, {
      params: { orgNumber },
    })
    return response.data
  },

  async getAuditLogsByDateRange(
    orgNumber: number,
    fromDate: string,
    toDate: string
  ): Promise<AuditLogResponse[]> {
    const response = await client.get('/admin/audit-log/date-range', {
      params: { orgNumber, fromDate, toDate },
    })
    return response.data
  },

  async getEntityAuditLogs(
    orgNumber: number,
    entityType: string,
    entityId: number
  ): Promise<AuditLogResponse[]> {
    const response = await client.get(`/admin/audit-log/entity/${entityType}/${entityId}`, {
      params: { orgNumber },
    })
    return response.data
  },
}
