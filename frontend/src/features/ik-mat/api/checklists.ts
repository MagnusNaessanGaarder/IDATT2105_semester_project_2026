/**
 * Checklist API service for IK-MAT checklist management.
 * Provides CRUD operations for checklist templates and runs.
 * Aligned with backend DTOs:
 * - ChecklistTemplateResponse (title, not name)
 * - ChecklistTemplateCreateRequest
 * - ChecklistRunResponse
 * - ChecklistRunCreateRequest (requires runDate)
 */
import { client } from '@/api/client'

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
  items: Omit<ChecklistTemplateItem, 'itemId' | 'templateId'>[]
}

export interface ChecklistTemplateUpdateRequest {
  title: string
  description?: string
  moduleType: 'IK_MAT'
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY'
  isActive?: boolean
  items: Omit<ChecklistTemplateItem, 'itemId' | 'templateId'>[]
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
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'OVERDUE'
  notes?: string
  createdAt?: string
  updatedAt?: string
  items?: ChecklistRunItem[]
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

/**
 * Fetch all checklist templates for an organization.
 * @param orgNumber - The organization number
 * @returns Promise with array of checklist templates
 */
export async function getTemplates(orgNumber: number): Promise<ChecklistTemplate[]> {
  const response = await client.get('/api/v1/checklists/templates', {
    params: { orgNumber },
  })
  return response.data
}

/**
 * Fetch a specific checklist template by ID.
 * @param templateId - The template ID
 * @param orgNumber - The organization number
 * @returns Promise with checklist template data
 */
export async function getTemplate(templateId: number, orgNumber: number): Promise<ChecklistTemplate> {
  const response = await client.get(`/api/v1/checklists/templates/${templateId}`, {
    params: { orgNumber },
  })
  return response.data
}

/**
 * Create a new checklist template.
 * Note: orgNumber is passed as query param, not in body.
 * @param orgNumber - The organization number
 * @param templateData - The template creation data
 * @returns Promise with created template
 */
export async function createTemplate(
  orgNumber: number,
  templateData: ChecklistTemplateCreateRequest
): Promise<ChecklistTemplate> {
  const response = await client.post('/api/v1/checklists/templates', templateData, {
    params: { orgNumber },
  })
  return response.data
}

/**
 * Update an existing checklist template.
 * Note: Backend expects full DTO (title, moduleType, frequency required).
 * @param templateId - The template ID
 * @param orgNumber - The organization number
 * @param templateData - The template update data (full DTO required)
 * @returns Promise with updated template
 */
export async function updateTemplate(
  templateId: number,
  orgNumber: number,
  templateData: ChecklistTemplateUpdateRequest
): Promise<ChecklistTemplate> {
  const response = await client.put(`/api/v1/checklists/templates/${templateId}`, templateData, {
    params: { orgNumber },
  })
  return response.data
}

/**
 * Delete (deactivate) a checklist template.
 * @param templateId - The template ID
 * @param orgNumber - The organization number
 * @returns Promise that resolves when deleted
 */
export async function deleteTemplate(templateId: number, orgNumber: number): Promise<void> {
  await client.delete(`/api/v1/checklists/templates/${templateId}`, {
    params: { orgNumber },
  })
}

/**
 * Fetch all checklist runs for an organization.
 * @param orgNumber - The organization number
 * @returns Promise with array of checklist runs
 */
export async function getRuns(orgNumber: number): Promise<ChecklistRun[]> {
  const response = await client.get('/api/v1/checklists/runs', {
    params: { orgNumber },
  })
  return response.data
}

/**
 * Create a new checklist run from a template.
 * Note: Requires runDate in body, orgNumber as query param.
 * @param orgNumber - The organization number
 * @param runData - The run creation data (includes templateId and runDate)
 * @returns Promise with created run
 */
export async function createRun(
  orgNumber: number,
  runData: ChecklistRunCreateRequest
): Promise<ChecklistRun> {
  const response = await client.post('/api/v1/checklists/runs', runData, {
    params: { orgNumber },
  })
  return response.data
}

/**
 * Complete a checklist run.
 * @param runId - The run ID
 * @param orgNumber - The organization number
 * @returns Promise with completed run
 */
export async function completeRun(runId: number, orgNumber: number): Promise<ChecklistRun> {
  const response = await client.put(`/api/v1/checklists/runs/${runId}/complete`, null, {
    params: { orgNumber },
  })
  return response.data
}

/**
 * Update a checklist run item (answer a question).
 * @param runId - The run ID
 * @param itemId - The run item ID
 * @param orgNumber - The organization number
 * @param answerData - The answer data
 * @returns Promise with updated run item
 */
export async function updateRunItem(
  runId: number,
  itemId: number,
  orgNumber: number,
  answerData: ChecklistRunItemUpdateRequest
): Promise<ChecklistRunItem> {
  const response = await client.put(`/api/v1/checklists/runs/${runId}/items/${itemId}`, answerData, {
    params: { orgNumber },
  })
  return response.data
}
