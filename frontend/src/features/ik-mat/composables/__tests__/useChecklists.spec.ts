import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useChecklists } from '../useChecklists'
import * as checklistsApi from '../../api/checklists'

// Mock the API module
vi.mock('../../api/checklists', () => ({
  getTemplates: vi.fn(),
  getRuns: vi.fn(),
  createTemplate: vi.fn(),
  updateTemplate: vi.fn(),
  deleteTemplate: vi.fn(),
  createRun: vi.fn(),
  completeRun: vi.fn(),
}))

describe('useChecklists composable', () => {
  const mockTemplate = {
    templateId: 1,
    orgNumber: 123456789,
    moduleType: 'IK_MAT' as const,
    title: 'Daily Cleaning',
    description: 'Daily cleaning checklist',
    frequency: 'DAILY' as const,
    isActive: true,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
  }

  const mockRun = {
    runId: 1,
    templateId: 1,
    orgNumber: 123456789,
    status: 'PENDING' as const,
    runDate: '2024-01-01',
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('initial state', () => {
    it('has empty arrays and false loading state', () => {
      const composable = useChecklists()

      expect(composable.templates.value).toEqual([])
      expect(composable.runs.value).toEqual([])
      expect(composable.isLoading.value).toBe(false)
      expect(composable.error.value).toBeNull()
    })
  })

  describe('fetchTemplates', () => {
    it('sets loading to true while fetching', async () => {
      vi.mocked(checklistsApi.getTemplates).mockReturnValue(new Promise(() => {}))
      const composable = useChecklists()

      composable.fetchTemplates(123456789)

      expect(composable.isLoading.value).toBe(true)
    })

    it('populates templates on success', async () => {
      vi.mocked(checklistsApi.getTemplates).mockResolvedValue([mockTemplate])
      const composable = useChecklists()

      await composable.fetchTemplates(123456789)

      expect(composable.templates.value).toEqual([mockTemplate])
      expect(composable.isLoading.value).toBe(false)
      expect(checklistsApi.getTemplates).toHaveBeenCalledWith(123456789)
    })

    it('sets error message on failure', async () => {
      const apiError = {
        response: {
          data: {
            message: 'Failed to fetch templates',
          },
        },
      }
      vi.mocked(checklistsApi.getTemplates).mockRejectedValue(apiError)
      const composable = useChecklists()

      await expect(composable.fetchTemplates(123456789)).rejects.toThrow()

      expect(composable.error.value).toBe('Failed to fetch templates')
      expect(composable.isLoading.value).toBe(false)
    })

    it('sets generic error message when no response message', async () => {
      vi.mocked(checklistsApi.getTemplates).mockRejectedValue(new Error('Network error'))
      const composable = useChecklists()

      await expect(composable.fetchTemplates(123456789)).rejects.toThrow()

      expect(composable.error.value).toBe('Kunne ikke hente sjekklister')
    })
  })

  describe('fetchRuns', () => {
    it('populates runs on success', async () => {
      vi.mocked(checklistsApi.getRuns).mockResolvedValue([mockRun])
      const composable = useChecklists()

      await composable.fetchRuns(123456789)

      expect(composable.runs.value).toEqual([mockRun])
      expect(composable.isLoading.value).toBe(false)
    })

    it('sets error on failure', async () => {
      vi.mocked(checklistsApi.getRuns).mockRejectedValue({
        response: { data: { message: 'Failed' } },
      })
      const composable = useChecklists()

      await expect(composable.fetchRuns(123456789)).rejects.toThrow()

      expect(composable.error.value).toBe('Failed')
    })
  })

  describe('createTemplate', () => {
    const createRequest = {
      title: 'New Template',
      description: 'Test',
      moduleType: 'IK_MAT' as const,
      frequency: 'DAILY' as const,
      items: [],
    }

    it('adds new template to list on success', async () => {
      vi.mocked(checklistsApi.createTemplate).mockResolvedValue(mockTemplate)
      const composable = useChecklists()

      const result = await composable.createTemplate(123456789, createRequest)

      expect(composable.templates.value).toHaveLength(1)
      expect(composable.templates.value[0]).toEqual(mockTemplate)
      expect(result).toEqual(mockTemplate)
      expect(checklistsApi.createTemplate).toHaveBeenCalledWith(123456789, createRequest)
    })

    it('throws error when creation fails', async () => {
      vi.mocked(checklistsApi.createTemplate).mockResolvedValue(null as any)
      const composable = useChecklists()

      await expect(composable.createTemplate(123456789, createRequest)).rejects.toThrow('Failed to create checklist template')
    })

    it('exposes isCreating state', async () => {
      vi.mocked(checklistsApi.createTemplate).mockReturnValue(new Promise(() => {}))
      const composable = useChecklists()

      composable.createTemplate(123456789, createRequest)

      expect(composable.isCreating.value).toBe(true)
    })
  })

  describe('updateTemplate', () => {
    const updateData = {
      title: 'Updated Name',
      description: 'Updated description',
      moduleType: 'IK_MAT' as const,
      frequency: 'DAILY' as const,
      items: [],
    }

    it('updates existing template in list', async () => {
      vi.mocked(checklistsApi.getTemplates).mockResolvedValue([mockTemplate])
      vi.mocked(checklistsApi.updateTemplate).mockResolvedValue({
        ...mockTemplate,
        ...updateData,
      })

      const composable = useChecklists()
      await composable.fetchTemplates(123456789)

      const result = await composable.updateTemplate(1, 123456789, updateData)

      expect(composable.templates.value[0]?.title).toBe('Updated Name')
      expect(result.title).toBe('Updated Name')
    })

    it('throws error when update fails', async () => {
      vi.mocked(checklistsApi.updateTemplate).mockResolvedValue(null as any)
      const composable = useChecklists()

      await expect(composable.updateTemplate(1, 123456789, updateData)).rejects.toThrow('Failed to update checklist template')
    })

    it('exposes isUpdating state', async () => {
      vi.mocked(checklistsApi.updateTemplate).mockReturnValue(new Promise(() => {}))
      const composable = useChecklists()

      composable.updateTemplate(1, 123456789, updateData)

      expect(composable.isUpdating.value).toBe(true)
    })
  })

  describe('deleteTemplate', () => {
    it('marks template as inactive in list', async () => {
      vi.mocked(checklistsApi.getTemplates).mockResolvedValue([mockTemplate])
      vi.mocked(checklistsApi.deleteTemplate).mockResolvedValue()

      const composable = useChecklists()
      await composable.fetchTemplates(123456789)

      await composable.deleteTemplate(1, 123456789)

      expect(composable.templates.value[0]?.isActive).toBe(false)
    })

    it('exposes isDeleting state', async () => {
      vi.mocked(checklistsApi.deleteTemplate).mockReturnValue(new Promise(() => {}))
      const composable = useChecklists()

      composable.deleteTemplate(1, 123456789)

      expect(composable.isDeleting.value).toBe(true)
    })
  })

  describe('createRun', () => {
    const runData = {
      templateId: 1,
      runDate: '2024-01-01',
    }

    it('adds new run to list', async () => {
      vi.mocked(checklistsApi.createRun).mockResolvedValue(mockRun)
      const composable = useChecklists()

      const result = await composable.createRun(123456789, runData)

      expect(composable.runs.value).toHaveLength(1)
      expect(composable.runs.value[0]).toEqual(mockRun)
      expect(result).toEqual(mockRun)
      expect(checklistsApi.createRun).toHaveBeenCalledWith(123456789, runData)
    })

    it('throws error when creation fails', async () => {
      vi.mocked(checklistsApi.createRun).mockResolvedValue(null as any)
      const composable = useChecklists()

      await expect(composable.createRun(123456789, runData)).rejects.toThrow('Failed to create checklist run')
    })
  })

  describe('completeRun', () => {
    it('updates run status in list', async () => {
      vi.mocked(checklistsApi.getRuns).mockResolvedValue([mockRun])
      vi.mocked(checklistsApi.completeRun).mockResolvedValue({
        ...mockRun,
        status: 'COMPLETED',
      })

      const composable = useChecklists()
      await composable.fetchRuns(123456789)

      await composable.completeRun(1, 123456789)

      expect(composable.runs.value[0]?.status).toBe('COMPLETED')
    })
  })

  describe('computed getters', () => {
    it('activeTemplates filters only active templates', async () => {
      const templates = [
        { ...mockTemplate, templateId: 1, isActive: true },
        { ...mockTemplate, templateId: 2, isActive: false },
        { ...mockTemplate, templateId: 3, isActive: true },
      ]
      vi.mocked(checklistsApi.getTemplates).mockResolvedValue(templates)

      const composable = useChecklists()
      await composable.fetchTemplates(123456789)

      expect(composable.activeTemplates.value).toHaveLength(2)
      expect(composable.activeTemplates.value.every((t) => t.isActive)).toBe(true)
    })

    it('dailyTemplates filters by DAILY frequency', async () => {
      const templates = [
        { ...mockTemplate, templateId: 1, frequency: 'DAILY' as const, isActive: true },
        { ...mockTemplate, templateId: 2, frequency: 'WEEKLY' as const, isActive: true },
        { ...mockTemplate, templateId: 3, frequency: 'DAILY' as const, isActive: true },
      ]
      vi.mocked(checklistsApi.getTemplates).mockResolvedValue(templates)

      const composable = useChecklists()
      await composable.fetchTemplates(123456789)

      // Access templates first to ensure reactivity is triggered
      expect(composable.templates.value).toHaveLength(3)
      expect(composable.dailyTemplates.value).toHaveLength(2)
      expect(composable.dailyTemplates.value.every((t) => t.frequency === 'DAILY')).toBe(true)
    })

    it('pendingRuns filters by pending status', async () => {
      const runs = [
        { ...mockRun, runId: 1, status: 'PENDING' as const },
        { ...mockRun, runId: 2, status: 'COMPLETED' as const },
        { ...mockRun, runId: 3, status: 'IN_PROGRESS' as const },
      ]
      vi.mocked(checklistsApi.getRuns).mockResolvedValue(runs)

      const composable = useChecklists()
      await composable.fetchRuns(123456789)

      expect(composable.pendingRuns.value).toHaveLength(2)
    })

    it('completedRuns filters by completed status', async () => {
      const runs = [
        { ...mockRun, runId: 1, status: 'PENDING' as const },
        { ...mockRun, runId: 2, status: 'COMPLETED' as const },
      ]
      vi.mocked(checklistsApi.getRuns).mockResolvedValue(runs)

      const composable = useChecklists()
      await composable.fetchRuns(123456789)

      expect(composable.completedRuns.value).toHaveLength(1)
      expect(composable.completedRuns.value[0]?.status).toBe('COMPLETED')
    })
  })

  describe('helper functions', () => {
    it('getTemplateCompletion returns 0 when no runs', () => {
      const composable = useChecklists()

      expect(composable.getTemplateCompletion(1)).toBe(0)
    })

    it('getTemplateCompletion calculates correct percentage', async () => {
      const runs = [
        { ...mockRun, runId: 1, templateId: 1, status: 'COMPLETED' as const },
        { ...mockRun, runId: 2, templateId: 1, status: 'PENDING' as const },
        { ...mockRun, runId: 3, templateId: 1, status: 'COMPLETED' as const },
      ]
      vi.mocked(checklistsApi.getRuns).mockResolvedValue(runs)

      const composable = useChecklists()
      await composable.fetchRuns(123456789)

      expect(composable.getTemplateCompletion(1)).toBe(67) // 2/3 = 66.67% rounded
    })

    it('formatFrequency returns Norwegian translation', () => {
      const composable = useChecklists()

      expect(composable.formatFrequency('DAILY')).toBe('Daglig')
      expect(composable.formatFrequency('WEEKLY')).toBe('Ukentlig')
      expect(composable.formatFrequency('MONTHLY')).toBe('Månedlig')
      expect(composable.formatFrequency('QUARTERLY')).toBe('Kvartalsvis')
      expect(composable.formatFrequency('YEARLY')).toBe('Årlig')
      expect(composable.formatFrequency('UNKNOWN')).toBe('UNKNOWN')
    })

    it('formatDate formats ISO date correctly', () => {
      const composable = useChecklists()

      expect(composable.formatDate('2024-03-15')).toBe('15. mars 2024')
    })

    it('formatDate returns dash for null/undefined', () => {
      const composable = useChecklists()

      expect(composable.formatDate()).toBe('-')
      expect(composable.formatDate(null as any)).toBe('-')
    })

    it('formatDate returns original string for invalid date', () => {
      const composable = useChecklists()

      expect(composable.formatDate('invalid')).toBe('invalid')
    })
  })
})
