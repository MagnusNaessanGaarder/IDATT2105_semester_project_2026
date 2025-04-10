export type RunStatus =
  | 'DRAFT'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'OVERDUE'
  | 'CANCELLED'

export interface ChecklistRun {
  id: string
  orgNumber: string
  status: RunStatus
  [key: string]: unknown
}

export interface GetRunsSuccess {
  ok: true
  data: ChecklistRunApi[]
}

export interface GetRunsError {
  ok: false
  error: {
    message: string
    status: number | null
    data: unknown
    contentType: string | null
    authorizationSent: boolean
    url: string | null
  }
}

export type GetRunsResult = GetRunsSuccess | GetRunsError

export interface DailyControlItem {
  id: number
  run_id: number | null
  run_item_id: number | null
  run_date: string | null
  template_id: number | null
  template_item_id: number | null
  law_document_id: number | null
  run_status: string | null
  name: string
  law_unit: string
  employee: string
  comment: string
  completion_date: {
    date: string
    time: string
  }
  attachment: string | null
  is_checked: boolean
}

export interface EmployeeCertificate {
  name: string
  expire_date: string
}

export interface EmployeeCertification {
  name: string
  certifications: EmployeeCertificate[]
}

export interface LawSection {
  section: string
  description: string
}

export interface LawItem {
  documentId: number
  name: string
  type: string
  short: string
  description: string
  link: string
  last_updated_code: string
  sub_sections?: LawSection[]
  'sub-sections'?: LawSection[]
}

export interface DemandItem {
  title: string
  bullet_points: string[]
}

export interface OrganizationDocumentApi {
  documentId: number
  orgNumber: number
  documentType: string
  title: string
  description: string | null
  currentVersion: number
  active: boolean
  createdByUserId: number | null
  createdAt?: string | null
  updatedAt?: string | null
}

export type CertificateStatus = 'Gyldig' | 'Utløper snart' | 'Utgått'

export interface ChecklistRunItemApi {
  runItemId?: number
  templateItemId?: number
  templateItemLabel?: string | null
  booleanValue?: boolean | null
  textValue?: string | null
  numericValue?: number | null
  selectedChoice?: string | null
  isDeviation?: boolean | null
  commentText?: string | null
  hasAnswer?: boolean | null
  updatedAt?: string | null
  createdAt?: string | null
}

export interface ChecklistRunApi {
  runId?: number
  templateId?: number
  templateTitle?: string | null
  templateDescription?: string | null
  performedByUserId?: number | null
  assignedToUserId?: number | null
  runDate?: string | null
  completedAt?: string | null
  status?: string | null
  notes?: string | null
  createdAt?: string | null
  updatedAt?: string | null
  items?: ChecklistRunItemApi[]
}

export interface ChecklistTemplateItemApi {
  itemId?: number
  templateId?: number
  sortOrder?: number | null
  label?: string | null
  description?: string | null
  itemType?: string | null
  isRequired?: boolean | null
}

export interface ChecklistTemplateApi {
  templateId?: number
  orgNumber?: number
  moduleType?: string | null
  title?: string | null
  description?: string | null
  frequency?: string | null
  isActive?: boolean | null
  items?: ChecklistTemplateItemApi[]
}

export interface ChecklistTemplateItemCreatePayload {
  sortOrder: number
  label: string
  description?: string
  itemType: 'BOOLEAN'
  isRequired: boolean
}

export interface ChecklistTemplateCreatePayload {
  title: string
  description: string
  moduleType: 'ALCOHOL'
  frequency: 'DAILY'
  items: ChecklistTemplateItemCreatePayload[]
}

export interface ChecklistTemplateResponse {
  templateId: number
  title: string
  description: string | null
  moduleType: string
  frequency: string
}
