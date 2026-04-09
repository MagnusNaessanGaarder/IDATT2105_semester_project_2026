/**
 * Dashboard Feature Types
 * Core types for export, notification, document, and dashboard functionality
 */

// ═══════════════════════════════════════════════════════════════════════════
// Export Types
// ═══════════════════════════════════════════════════════════════════════════

export type ExportType =
  | 'FULL_COMPLIANCE_REPORT'
  | 'AUDIT_REPORT'
  | 'CHECKLIST_REPORT'
  | 'TEMPERATURE_REPORT'
  | 'DEVIATION_REPORT'
  | 'TRAINING_REPORT'

export type ExportFormat = 'PDF' | 'JSON'

export type ExportStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'

/** Request payload for creating a new export job */
export interface ExportRequest {
  exportType: ExportType
  format: ExportFormat
  dateFrom: string | null
  dateTo: string | null
}

/** Response from export API - represents a single export job */
export interface ExportResponse {
  exportJobId: number
  orgNumber: number
  exportType: ExportType
  format: ExportFormat
  status: ExportStatus
  createdAt: string
  requestedAt?: string // Alias for createdAt
  completedAt?: string | null
  recordCount?: number
  failureReason?: string | null
  requestedByDisplayName?: string | null
  requestedById?: number | null
  parametersJson?: string | null
}

/** Paginated response from list exports endpoint */
export interface ExportPage {
  content: ExportResponse[]
  totalElements: number
  totalPages: number
  currentPage: number
}

// ═══════════════════════════════════════════════════════════════════════════
// Document Types
// ═══════════════════════════════════════════════════════════════════════════

export interface OrganizationDocument {
  documentId: number
  orgNumber: number
  name: string
  description?: string | null
  fileSize: number
  mimeType: string
  uploadedAt: string
  uploadedBy: string
  versions: DocumentVersion[]
}

export interface DocumentVersion {
  versionId: number
  documentId: number
  fileSize: number
  uploadedAt: string
  uploadedBy: string
  version: number
}

export interface UploadNewDocumentPayload {
  name: string
  description?: string
  file: File
}

export interface UploadNewVersionPayload {
  file: File
}

// ═══════════════════════════════════════════════════════════════════════════
// Notification Types
// ═══════════════════════════════════════════════════════════════════════════

export interface Notification {
  notificationId: number
  orgNumber: number
  userId: number
  title: string
  message: string
  type: 'INFO' | 'WARNING' | 'ERROR'
  isRead: boolean
  createdAt: string
  readAt?: string | null
  metadata?: Record<string, unknown>
  actionUrl?: string | null
}

export interface MarkNotificationParams {
  notificationId: number
  isRead: boolean
}

// ═══════════════════════════════════════════════════════════════════════════
// API Error Shapes
// ═══════════════════════════════════════════════════════════════════════════

export interface ApiErrorResponse {
  response?: {
    data?: {
      message?: string
      error?: string
      fieldErrors?: Record<string, string>
    }
  }
  message?: string
}
