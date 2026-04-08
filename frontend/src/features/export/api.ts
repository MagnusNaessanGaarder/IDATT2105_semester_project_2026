import { client } from '@/api/client'

export type ExportType =
    | 'AUDIT_REPORT'
    | 'CHECKLIST_REPORT'
    | 'TEMPERATURE_REPORT'
    | 'DEVIATION_REPORT'
    | 'TRAINING_REPORT'
    | 'FULL_COMPLIANCE_REPORT'

export type ExportFormat = 'PDF' | 'JSON'

export type ExportStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'

export interface ExportRequest {
  exportType: ExportType
  format: ExportFormat
  dateFrom?: string | null
  dateTo?: string | null
  locationId?: number | null
  checklistType?: string | null
}

export interface ExportResponse {
  exportJobId: number
  exportType: ExportType
  format: ExportFormat
  status: ExportStatus
  downloadUrl?: string | null
  fileName?: string | null
  recordCount?: number | null
  failureReason?: string | null
  requestedAt: string
  completedAt?: string | null
}

export interface PagedExportResponse {
  content: ExportResponse[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export const exportApi = {
  createExport(orgNumber: number, request: ExportRequest): Promise<ExportResponse> {
    return client
        .post<ExportResponse>('/exports', request, { params: { orgNumber } })
        .then((r) => r.data)
  },

  getExportStatus(orgNumber: number, exportJobId: number): Promise<ExportResponse> {
    return client
        .get<ExportResponse>(`/exports/${exportJobId}`, { params: { orgNumber } })
        .then((r) => r.data)
  },

  getDownloadUrl(orgNumber: number, exportJobId: number): Promise<string> {
    return client
        .get<string>(`/exports/${exportJobId}/download`, { params: { orgNumber } })
        .then((r) => r.data)
  },

  listExports(
      orgNumber: number,
      page = 0,
      size = 20,
  ): Promise<PagedExportResponse> {
    return client
        .get<PagedExportResponse>('/exports', {
          params: { orgNumber, page, size, sort: 'requestedAt,desc' },
        })
        .then((r) => r.data)
  },
}