/**
 * Composable for checklist management operations.
 * Provides reactive state and CRUD operations for IK-MAT checklists.
 * Aligned with backend ChecklistTemplate DTOs (uses 'title' not 'name').
 */
import { ref, computed } from 'vue'
import { useApi } from '@/shared/composables/useApi'
import { getOrgNumber } from '@/shared/utils/orgContext'
import { formatDateForOrganization } from '@/shared/utils/orgSettings'
import * as checklistsApi from '../api/checklists'
import type {
  ChecklistTemplate,
  ChecklistTemplateCreateRequest,
  ChecklistTemplateUpdateRequest,
  ChecklistRun,
  ChecklistRunCreateRequest,
} from '../api/checklists'

export type {
  ChecklistTemplate,
  ChecklistTemplateCreateRequest,
  ChecklistTemplateUpdateRequest,
  ChecklistRun,
  ChecklistRunCreateRequest,
  ChecklistTemplateItem,
  ChecklistRunItem,
} from '../api/checklists'

export function useChecklists() {
  // State
  const templates = ref<ChecklistTemplate[]>([])
  const runs = ref<ChecklistRun[]>([])
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  // API composables for individual operations
  const createApi = useApi(checklistsApi.createTemplate)
  const updateApi = useApi(checklistsApi.updateTemplate)
  const deleteApi = useApi(checklistsApi.deleteTemplate)
  const createRunApi = useApi(checklistsApi.createRun)

  /**
   * Fetch all checklist templates for an organization.
   * @param orgNumber - The organization number
   */
  async function fetchTemplates(orgNumber: number): Promise<void> {
    isLoading.value = true
    error.value = null
    try {
      const data = await checklistsApi.getTemplates(orgNumber)
      templates.value = data
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      error.value = err.response?.data?.message ?? 'Kunne ikke hente sjekklister'
      throw e
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Fetch all checklist runs for an organization.
   * @param orgNumber - The organization number
   */
  async function fetchRuns(orgNumber: number): Promise<void> {
    isLoading.value = true
    error.value = null
    try {
      const data = await checklistsApi.getRuns(orgNumber)
      runs.value = data
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } }
      error.value = err.response?.data?.message ?? 'Kunne ikke hente sjekklistekjøringer'
      throw e
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Create a new checklist template.
   * @param orgNumber - The organization number
   * @param templateData - The template creation data
   */
  async function createTemplate(
    orgNumber: number,
    templateData: ChecklistTemplateCreateRequest
  ): Promise<ChecklistTemplate> {
    const newTemplate = await createApi.execute(orgNumber, templateData)
    if (!newTemplate) {
      throw new Error('Failed to create checklist template')
    }
    templates.value.push(newTemplate)
    return newTemplate
  }

  /**
   * Update an existing checklist template.
   * Note: Backend requires full DTO (title, moduleType, frequency required).
   * @param templateId - The template ID
   * @param orgNumber - The organization number
   * @param templateData - The template update data (full DTO)
   */
  async function updateTemplate(
    templateId: number,
    orgNumber: number,
    templateData: ChecklistTemplateUpdateRequest
  ): Promise<ChecklistTemplate> {
    const updatedTemplate = await updateApi.execute(templateId, orgNumber, templateData)
    if (!updatedTemplate) {
      throw new Error('Failed to update checklist template')
    }
    const index = templates.value.findIndex((t) => t.templateId === templateId)
    if (index !== -1) {
      templates.value[index] = updatedTemplate
    }
    return updatedTemplate
  }

  /**
   * Delete (deactivate) a checklist template.
   * @param templateId - The template ID
   * @param orgNumber - The organization number
   */
  async function deleteTemplate(templateId: number, orgNumber: number): Promise<void> {
    await deleteApi.execute(templateId, orgNumber)
    // Mark as inactive in local state
    const index = templates.value.findIndex((t) => t.templateId === templateId)
    if (index !== -1) {
      templates.value[index].isActive = false
    }
  }

  /**
   * Create a new checklist run from a template.
   * @param orgNumber - The organization number
   * @param runData - The run creation data
   */
  async function createRun(
    orgNumber: number,
    runData: ChecklistRunCreateRequest
  ): Promise<ChecklistRun> {
    const newRun = await createRunApi.execute(orgNumber, runData)
    if (!newRun) {
      throw new Error('Failed to create checklist run')
    }
    runs.value.push(newRun)
    return newRun
  }

  /**
   * Complete a checklist run.
   * @param runId - The run ID
   * @param orgNumber - The organization number
   */
  async function completeRun(runId: number, orgNumber: number): Promise<ChecklistRun> {
    const completedRun = await checklistsApi.completeRun(runId, orgNumber)
    const index = runs.value.findIndex((r) => r.runId === runId)
    if (index !== -1) {
      runs.value[index] = completedRun
    }
    return completedRun
  }

  // Computed getters
  const activeTemplates = computed(() => templates.value.filter((t) => t.isActive))
  const dailyTemplates = computed(() =>
    templates.value.filter((t) => t.frequency === 'DAILY' && t.isActive)
  )
  const weeklyTemplates = computed(() =>
    templates.value.filter((t) => t.frequency === 'WEEKLY' && t.isActive)
  )
  const monthlyTemplates = computed(() =>
    templates.value.filter((t) => t.frequency === 'MONTHLY' && t.isActive)
  )

  const pendingRuns = computed(() =>
    runs.value.filter((r) => r.status === 'PENDING' || r.status === 'IN_PROGRESS')
  )
  const completedRuns = computed(() => runs.value.filter((r) => r.status === 'COMPLETED'))

  /**
   * Get completion percentage for a template based on recent runs.
   * @param templateId - The template ID
   * @returns Completion percentage (0-100)
   */
  function getTemplateCompletion(templateId: number): number {
    const templateRuns = runs.value.filter((r) => r.templateId === templateId)
    if (templateRuns.length === 0) return 0

    const completed = templateRuns.filter((r) => r.status === 'COMPLETED').length
    return Math.round((completed / templateRuns.length) * 100)
  }

  /**
   * Format frequency for display.
   * @param frequency - The frequency enum
   * @returns Formatted frequency in Norwegian
   */
  function formatFrequency(frequency: string): string {
    const map: Record<string, string> = {
      DAILY: 'Daglig',
      WEEKLY: 'Ukentlig',
      MONTHLY: 'Månedlig',
      QUARTERLY: 'Kvartalsvis',
      YEARLY: 'Årlig',
    }
    return map[frequency] || frequency
  }

  /**
   * Format date for display.
   * @param dateString - ISO date string
   * @returns Formatted date
   */
  function formatDate(dateString?: string): string {
    if (!dateString) return '-'
    const date = new Date(dateString)
    if (Number.isNaN(date.getTime())) return dateString
    return formatDateForOrganization(date, getOrgNumber(), {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    })
  }

  return {
    // State
    templates,
    runs,
    isLoading,
    error,

    // Operations
    fetchTemplates,
    fetchRuns,
    createTemplate,
    updateTemplate,
    deleteTemplate,
    createRun,
    completeRun,

    // Computed
    activeTemplates,
    dailyTemplates,
    weeklyTemplates,
    monthlyTemplates,
    pendingRuns,
    completedRuns,

    // Helpers
    getTemplateCompletion,
    formatFrequency,
    formatDate,

    // Individual API states
    isCreating: createApi.isLoading,
    createError: createApi.error,
    isUpdating: updateApi.isLoading,
    updateError: updateApi.error,
    isDeleting: deleteApi.isLoading,
    deleteError: deleteApi.error,
    isCreatingRun: createRunApi.isLoading,
    createRunError: createRunApi.error,
  }
}
