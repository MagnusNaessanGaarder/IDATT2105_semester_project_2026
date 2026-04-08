/**
 * Checklist API service for IK-MAT checklist management.
 * Provides CRUD operations for checklist templates and runs.
 */
import { client } from '@/api/client'

export interface ChecklistItem {
  itemId?: number
  templateId?: number
  questionText: string
  answerType: 'YES_NO' | 'TEXT' | 'NUMBER' | 'TEMPERATURE' | 'CHOICE'
  isRequired: boolean
  displayOrder: number
  minValue?: number
  maxValue?: number
  unit?: string
  alertOnDeviation?: boolean
}

export interface ChecklistTemplate {
  templateId: number
  orgNumber: number
  moduleType: 'IK_MAT' | 'IK_ALKOHOL'
  name: string
  description?: string
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY'
  isActive: boolean
  version: number
  createdAt?: string
  updatedAt?: string
  items?: ChecklistItem[]
}

export interface ChecklistTemplateCreateRequest {
  orgNumber: number
  moduleType: 'IK_MAT'
  name: string
  description?: string
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY'
  items: Omit<ChecklistItem, 'itemId' | 'templateId'>[]
}

export interface ChecklistTemplateUpdateRequest {
  name?: string
  description?: string
  frequency?: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY'
  isActive?: boolean
  items?: Omit<ChecklistItem, 'itemId' | 'templateId'>[]
}

export interface ChecklistRun {
  runId: number
  templateId: number
  templateName?: string
  orgNumber: number
  locationId?: number
  locationName?: string
  performedByUserId?: number
  performedByName?: string
  assignedToUserId?: number
  assignedToName?: string
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'OVERDUE'
  scheduledDate?: string
  startedAt?: string
  completedAt?: string
  notes?: string
  items?: ChecklistRunItem[]
}

export interface ChecklistRunItem {
  runItemId: number
  runId: number
  templateItemId: number
  questionText?: string
  answerType?: string
  answerValue?: string
  isAnswered: boolean
  isDeviation: boolean
  notes?: string
  answeredAt?: string
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
 * @param templateData - The template creation data
 * @returns Promise with created template
 */
export async function createTemplate(templateData: ChecklistTemplateCreateRequest): Promise<ChecklistTemplate> {
  const response = await client.post('/api/v1/checklists/templates', templateData)
  return response.data
}

/**
 * Update an existing checklist template.
 * @param templateId - The template ID
 * @param orgNumber - The organization number
 * @param templateData - The template update data
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
 * @param templateId - The template ID
 * @param orgNumber - The organization number
 * @param locationId - The location ID
 * @returns Promise with created run
 */
export async function createRun(
  templateId: number,
  orgNumber: number,
  locationId?: number
): Promise<ChecklistRun> {
  const response = await client.post('/api/v1/checklists/runs', {
    templateId,
    orgNumber,
    locationId,
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
  answerData: {
    answerValue?: string
    notes?: string
    isDeviation?: boolean
  }
): Promise<ChecklistRunItem> {
  const response = await client.put(`/api/v1/checklists/runs/${runId}/items/${itemId}`, answerData, {
    params: { orgNumber },
  })
  return response.data
}
