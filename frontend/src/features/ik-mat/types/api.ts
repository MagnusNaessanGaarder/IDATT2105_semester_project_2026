export interface ChecklistTemplateApi {
  templateId: number
  title: string
  description: string | null
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY' | string
  moduleType: string
  items?: Array<{
    itemId: number
    label: string
    isRequired?: boolean
    description?: string | null
  }>
}

export interface ChecklistTemplateItemRequest {
  label: string
  itemType?: string
  isRequired?: boolean
  description?: string | null
  sortOrder?: number
}

export interface ChecklistTemplateUpsertRequest {
  title: string
  description: string
  moduleType: 'FOOD' | 'ALCOHOL' | string
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY' | string
  items: ChecklistTemplateItemRequest[]
}

export interface ChecklistRunApi {
  runId: number
  templateId: number
  templateTitle: string | null
  performedByUserId: number | null
  runDate: string | null
  completedAt: string | null
  status: 'DRAFT' | 'IN_PROGRESS' | 'COMPLETED' | 'OVERDUE' | string
  items?: Array<{
    templateItemId: number
    templateItemLabel: string | null
    hasAnswer: boolean
    commentText: string | null
  }>
}

export interface ChecklistRunCreateRequest {
  templateId: number
  runDate: string
  assignedToUserId?: number
  notes?: string
}

export interface ChecklistRunItemUpdateRequest {
  booleanValue?: boolean
  textValue?: string
  numericValue?: number
  selectedChoice?: string
  isDeviation?: boolean
  commentText?: string
}

export interface ChecklistRunItemApi {
  runItemId: number
  runId: number
  templateItemId: number
  templateItemLabel: string | null
  hasAnswer: boolean
  commentText: string | null
}

export interface TemperatureEntryApi {
  entryId: number
  locationId: number | null
  locationName: string | null
  logPointName: string | null
  temperatureC: number
  isAlert: boolean
  recordedByName: string | null
  measuredAt: string
}

export interface TemperatureEntryCreateRequest {
  logPointId: number
  temperatureC: number
  measuredAt: string
  noteText?: string
}

export interface TemperatureLogPointApi {
  logPointId: number
  locationId: number
  locationName: string | null
  name: string
  isActive: boolean
  createdAt?: string
  updatedAt?: string
}

export interface TemperatureLogPointUpsertRequest {
  locationId: number
  name: string
  isActive?: boolean
}

export interface LocationApi {
  locationId: number
  orgNumber?: number
  name: string
  description?: string | null
  locationType?: 'KITCHEN' | 'BAR' | 'FREEZER' | 'FRIDGE' | 'STORAGE' | 'SERVING_AREA' | 'HOT_FOOD' | 'OTHER' | string
  tempMinC: number | null
  tempMaxC: number | null
  isActive?: boolean
  createdAt?: string
  updatedAt?: string
}

export interface LocationUpsertRequest {
  name: string
  description?: string
  locationType?: 'KITCHEN' | 'BAR' | 'FREEZER' | 'FRIDGE' | 'STORAGE' | 'SERVING_AREA' | 'HOT_FOOD' | 'OTHER' | string
  tempMinC?: number | null
  tempMaxC?: number | null
  isActive?: boolean
}

export interface DeviationApi {
  reportId: number
  title: string
  description: string
  severity: string
  status: string
  locationText: string | null
  occurredDate: string | null
  occurredTime: string | null
  reportDate: string | null
  reportedBy?: { fullName?: string; email?: string } | null
  immediateActionText?: string | null
  correctiveActionText?: string | null
}

export interface DeviationUpsertRequest {
  reportType: 'INCIDENT' | 'DISCREPANCY' | string
  severity: 'MINOR' | 'MAJOR' | 'CRITICAL' | string
  title: string
  description: string
  locationId?: number
  locationText?: string
  occurredDate?: string
  occurredTime?: string
  discoveredByUserId?: number
  discoveredByName?: string
  reportedToUserId?: number
  reportedToName?: string
  assignedToUserId?: number
}

export interface DeviationStatusUpdateRequest {
  status: 'DRAFT' | 'REPORTED' | 'UNDER_INVESTIGATION' | 'CORRECTIVE_ACTION_PLANNED' | 'CORRECTIVE_ACTION_COMPLETED' | 'CLOSED' | string
}

export interface DeviationActionRequest {
  actionText: string
}

export interface FileDocumentApi {
  documentId: number
  title: string
  description: string | null
  updatedAt: string | null
}

export interface ExportJobApi {
  exportJobId: number
  exportType: string
  format: 'PDF' | 'JSON' | string
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | string
  downloadUrl: string | null
  fileName: string | null
  recordCount: number
  failureReason: string | null
  requestedAt: string
  completedAt: string | null
}

export interface ExportCreateRequest {
  exportType: string
  format: 'PDF' | 'JSON' | string
  dateFrom?: string
  dateTo?: string
  locationId?: number
  checklistType?: string
}

export interface PageResponse<T> {
  totalElements: number
  totalPages: number
  size: number
  number: number
  numberOfElements: number
  first: boolean
  last: boolean
  empty: boolean
  content: T[]
}
