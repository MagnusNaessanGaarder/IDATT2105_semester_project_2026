/**
 * IK-MAT feature types.
 * Shared UI models and API DTOs for checklists, temperatures, deviations,
 * HACCP, locations, documents, exports, and organization users.
 */

// ---------------------------------------------------------------------------
// Shared domain types used by the ik-mat views and composables
// ---------------------------------------------------------------------------

export type ChecklistStatus = 'completed' | 'pending' | 'overdue'
export type ChecklistFrequency = 'Daglig' | 'Ukentlig' | 'Maanedlig' | 'Månedlig' | string
export type DeviationSeverity = 'low' | 'medium' | 'high'
export type DeviationStatus = 'open' | 'resolved' | 'in-progress'
export type TemperatureStatus = 'ok' | 'warning' | 'critical'
export type ChecklistRunStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'OVERDUE'
export type ChecklistRunApiStatus = 'DRAFT' | 'IN_PROGRESS' | 'COMPLETED' | 'OVERDUE' | 'CANCELLED' | string

export interface ChecklistItem {
  id: number
  task: string
  required: boolean
  completed: boolean
  notes: string | null
}

export interface Checklist {
  id: number
  name: string
  category: string
  frequency: ChecklistFrequency
  description: string
  created_date: string
  law_unit: string
  items: ChecklistItem[]
  completed_by: string | null
  completion_date: string | null
  completion_time: string | null
  status: ChecklistStatus
}

export interface TemperatureRecord {
  id: number
  log_point_id: number
  location: string
  temperature_c: number
  min_temp: number
  max_temp: number
  recorded_by: string
  recorded_date: string
  recorded_time: string
  status: TemperatureStatus
}

export interface Deviation {
  id: number
  title: string
  description: string
  severity: DeviationSeverity
  reported_by: string
  reported_date: string
  reported_time: string
  location: string
  immediate_action: string
  corrective_action: string
  status: DeviationStatus
}

export interface HaccpPoint {
  id: number
  number: string
  name: string
  description: string
  hazards: string[]
  critical_limits: string
  monitoring: string
  corrective_actions: string
  verification: string
  responsible: string
}

export interface SupportingDocument {
  id: number
  name: string
  date_updated: string
  description: string
}

export interface HaccpPlan {
  plan_name: string
  version: string
  last_updated: string
  created_date?: string
  critical_control_points: HaccpPoint[]
  supporting_documents: SupportingDocument[]
}

// ---------------------------------------------------------------------------
// Checklist API models used by the ik-mat checklist service
// ---------------------------------------------------------------------------

export interface ChecklistTemplateItem {
  itemId?: number
  templateId?: number
  sortOrder: number
  label: string
  description?: string
  itemType: 'YES_NO' | 'TEXT' | 'NUMERIC' | 'TEMPERATURE' | 'CHOICE'
  isRequired?: boolean
  expectedText?: string
  expectedNumericMin?: number
  expectedNumericMax?: number
  choiceOptionsJson?: string
}

export interface ChecklistTemplate {
  templateId: number
  orgNumber: number
  moduleType: 'IK_MAT' | 'IK_ALKOHOL'
  title: string
  description?: string
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY'
  isActive: boolean
  createdByUserId?: number
  createdAt?: string
  updatedAt?: string
  items?: ChecklistTemplateItem[]
}

export interface ChecklistTemplateCreateRequest {
  title: string
  description?: string
  moduleType: 'IK_MAT'
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY'
  items: Array<Omit<ChecklistTemplateItem, 'itemId' | 'templateId'>>
}

export interface ChecklistTemplateUpdateRequest {
  title: string
  description?: string
  moduleType: 'IK_MAT'
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY'
  isActive?: boolean
  items: Array<Omit<ChecklistTemplateItem, 'itemId' | 'templateId'>>
}

export interface ChecklistRunItem {
  runItemId: number
  runId: number
  templateItemId: number
  label?: string
  itemType?: string
  answerValue?: string
  isAnswered: boolean
  isDeviation: boolean
  notes?: string
  answeredAt?: string
}

export interface ChecklistRun {
  runId: number
  templateId: number
  templateTitle?: string
  orgNumber: number
  locationId?: number
  performedByUserId?: number
  assignedToUserId?: number
  runDate: string
  dueAt?: string
  completedAt?: string
  status: ChecklistRunStatus
  notes?: string
  createdAt?: string
  updatedAt?: string
  items?: ChecklistRunItem[]
}

export interface ChecklistRunCreateRequest {
  templateId: number
  runDate: string
  assignedToUserId?: number
  notes?: string
}

export interface ChecklistRunItemUpdateRequest {
  answerValue?: string
  notes?: string
  isDeviation?: boolean
}

// ---------------------------------------------------------------------------
// API DTOs used by the ik-mat HTTP client wrapper
// ---------------------------------------------------------------------------

export interface ChecklistTemplateApi {
  templateId: number
  title: string
  description: string | null
  moduleType: string
  isActive?: boolean
  frequency?: string
  items?: ChecklistTemplateItem[]
}

export interface ChecklistRunApi {
  runId: number
  templateId: number
  templateTitle: string | null
  performedByUserId: number | null
  runDate: string | null
  completedAt: string | null
  status: ChecklistRunApiStatus
  notes: string | null
  orgNumber?: number
  locationId?: number | null
  assignedToUserId?: number | null
  createdAt?: string | null
  updatedAt?: string | null
  items?: ChecklistRunItemApi[]
}

export interface ChecklistRunItemApi {
  runItemId: number
  runId: number
  templateItemId: number
  label?: string
  itemType?: string
  answerValue?: string
  isAnswered: boolean
  isDeviation: boolean
  notes?: string
  answeredAt?: string
}

export interface ChecklistTemplateUpsertRequest {
  title: string
  description?: string
  moduleType: string
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY'
  isActive?: boolean
  items?: Array<Omit<ChecklistTemplateItem, 'itemId' | 'templateId'>>
}

export interface DeviationApi {
  reportId: number
  title: string
  description: string
  severity: DeviationSeverity
  reported_by: string
  reported_date: string | null
  reported_time: string | null
  location: string
  immediate_action: string
  corrective_action: string
  status: DeviationStatus
  assigned_to_user_id?: number | null
  assigned_to_user_name?: string | null
  source_temperature_entry_id?: number | null
}

export interface DeviationUpsertRequest {
  reportType: string
  severity: 'MINOR' | 'MAJOR' | 'CRITICAL' | string
  title: string
  description: string
  locationText?: string
  discoveredByName?: string
  occurredDate?: string
  occurredTime?: string
  sourceTemperatureEntryId?: number
}

export interface DeviationStatusUpdateRequest {
  status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | string
}

export interface DeviationActionRequest {
  actionText: string
  performedByName?: string
}

export interface TemperatureEntryApi {
  id: number
  logPointId: number
  temperatureC: number
  measuredAt: string
  noteText?: string | null
  recordedByUserId?: number | null
  recordedBy?: string | null
  recordedDate?: string | null
  recordedTime?: string | null
  status?: TemperatureStatus | string
}

export interface TemperatureEntryCreateRequest {
  logPointId: number
  temperatureC: number
  measuredAt: string
  noteText?: string
  recordedByUserId?: number
}

export interface TemperatureEntryUpdateRequest {
  logPointId?: number
  temperatureC: number
  measuredAt: string
  noteText?: string
  recordedByUserId?: number
}

export interface TemperatureLogPointApi {
  logPointId: number
  name: string
  locationId: number
  locationName?: string | null
  isActive?: boolean
  createdAt?: string | null
  updatedAt?: string | null
}

export interface TemperatureLogPointUpsertRequest {
  name: string
  locationId: number
  isActive?: boolean
}

export interface LocationApi {
  locationId: number
  name: string
  locationType?: string
  tempMinC: number | string
  tempMaxC: number | string
  isActive?: boolean
}

export interface LocationUpsertRequest {
  name: string
  locationType: string
  tempMinC: number
  tempMaxC: number
  isActive: boolean
}

export interface OrganizationUserApi {
  userId: number
  displayName?: string | null
  email: string
  isActive: boolean
}

export interface FileDocumentApi {
  documentId: number
  title?: string
  name?: string
  description?: string | null
  category?: string | null
  fileName?: string | null
  mimeType?: string | null
  fileSize?: number | null
  uploadedAt?: string | null
  uploadedBy?: string | null
  orgNumber?: number | null
  versionCount?: number | null
}

export interface ExportCreateRequest {
  exportType: string
  format: 'PDF' | 'JSON'
  dateFrom?: string | null
  dateTo?: string | null
}

export interface ExportJobApi {
  exportJobId: number
  orgNumber: number
  exportType: string
  format: 'PDF' | 'JSON'
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | string
  createdAt: string
  requestedAt?: string
  completedAt?: string | null
  recordCount?: number
  failureReason?: string | null
  requestedByDisplayName?: string | null
  requestedById?: number | null
  parametersJson?: string | null
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  currentPage: number
}