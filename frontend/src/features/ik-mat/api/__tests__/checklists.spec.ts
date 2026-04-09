import { describe, it, expect, vi, beforeEach } from 'vitest'
import { client } from '@/api/client'
import {
  getTemplates,
  getTemplate,
  createTemplate,
  updateTemplate,
  deleteTemplate,
  getRuns,
  createRun,
  completeRun,
  updateRunItem,
  type ChecklistTemplate,
  type ChecklistTemplateCreateRequest,
} from '../checklists'

// Mock the axios client
vi.mock('@/api/client', () => ({
  client: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('checklists API', () => {
  const mockTemplate: ChecklistTemplate = {
    templateId: 1,
    orgNumber: 123456789,
    moduleType: 'IK_MAT',
    title: 'Daily Cleaning',
    description: 'Daily cleaning checklist',
    frequency: 'DAILY',
    isActive: true,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getTemplates', () => {
    it('fetches templates with correct URL and params', async () => {
      vi.mocked(client.get).mockResolvedValue({ data: [mockTemplate] })

      const result = await getTemplates(123456789)

      expect(client.get).toHaveBeenCalledWith('/api/v1/checklists/templates', {
        params: { orgNumber: 123456789 },
      })
      expect(result).toEqual([mockTemplate])
    })

    it('returns empty array when no templates exist', async () => {
      vi.mocked(client.get).mockResolvedValue({ data: [] })

      const result = await getTemplates(123456789)

      expect(result).toEqual([])
    })

    it('throws error when request fails', async () => {
      const error = new Error('Network error')
      vi.mocked(client.get).mockRejectedValue(error)

      await expect(getTemplates(123456789)).rejects.toThrow('Network error')
    })
  })

  describe('getTemplate', () => {
    it('fetches single template by id', async () => {
      vi.mocked(client.get).mockResolvedValue({ data: mockTemplate })

      const result = await getTemplate(1, 123456789)

      expect(client.get).toHaveBeenCalledWith('/api/v1/checklists/templates/1', {
        params: { orgNumber: 123456789 },
      })
      expect(result).toEqual(mockTemplate)
    })

    it('throws error when template not found', async () => {
      vi.mocked(client.get).mockRejectedValue(new Error('Not found'))

      await expect(getTemplate(999, 123456789)).rejects.toThrow('Not found')
    })
  })

  describe('createTemplate', () => {
    const createRequest: ChecklistTemplateCreateRequest = {
      title: 'New Template',
      description: 'Test description',
      moduleType: 'IK_MAT',
      frequency: 'DAILY',
      items: [
        {
          label: 'Task 1',
          sortOrder: 1,
          itemType: 'YES_NO',
          isRequired: true,
        },
      ],
    }

    it('creates template with correct payload and query params', async () => {
      vi.mocked(client.post).mockResolvedValue({ data: mockTemplate })

      const result = await createTemplate(123456789, createRequest)

      expect(client.post).toHaveBeenCalledWith(
        '/api/v1/checklists/templates',
        createRequest,
        { params: { orgNumber: 123456789 } }
      )
      expect(result).toEqual(mockTemplate)
    })

    it('throws error when creation fails', async () => {
      vi.mocked(client.post).mockRejectedValue(new Error('Validation error'))

      await expect(createTemplate(123456789, createRequest)).rejects.toThrow('Validation error')
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

    it('updates template with correct payload (full DTO)', async () => {
      const updatedTemplate = { ...mockTemplate, title: 'Updated Name' }
      vi.mocked(client.put).mockResolvedValue({ data: updatedTemplate })

      const result = await updateTemplate(1, 123456789, updateData)

      expect(client.put).toHaveBeenCalledWith(
        '/api/v1/checklists/templates/1',
        updateData,
        { params: { orgNumber: 123456789 } }
      )
      expect(result.title).toBe('Updated Name')
    })

    it('throws error when update fails', async () => {
      vi.mocked(client.put).mockRejectedValue(new Error('Not found'))

      await expect(updateTemplate(999, 123456789, updateData)).rejects.toThrow('Not found')
    })
  })

  describe('deleteTemplate', () => {
    it('deletes template with correct id', async () => {
      vi.mocked(client.delete).mockResolvedValue({ data: undefined })

      await deleteTemplate(1, 123456789)

      expect(client.delete).toHaveBeenCalledWith('/api/v1/checklists/templates/1', {
        params: { orgNumber: 123456789 },
      })
    })

    it('throws error when deletion fails', async () => {
      vi.mocked(client.delete).mockRejectedValue(new Error('Cannot delete'))

      await expect(deleteTemplate(1, 123456789)).rejects.toThrow('Cannot delete')
    })
  })

  describe('getRuns', () => {
    it('fetches runs with correct URL and params', async () => {
      const mockRuns = [
        {
          runId: 1,
          templateId: 1,
          orgNumber: 123456789,
          status: 'PENDING',
          runDate: '2024-01-01',
        },
      ]
      vi.mocked(client.get).mockResolvedValue({ data: mockRuns })

      const result = await getRuns(123456789)

      expect(client.get).toHaveBeenCalledWith('/api/v1/checklists/runs', {
        params: { orgNumber: 123456789 },
      })
      expect(result).toEqual(mockRuns)
    })
  })

  describe('createRun', () => {
    it('creates run with correct body and query params', async () => {
      const mockRun = {
        runId: 1,
        templateId: 1,
        orgNumber: 123456789,
        status: 'PENDING',
        runDate: '2024-01-01',
      }
      const runData = {
        templateId: 1,
        runDate: '2024-01-01',
        notes: 'Test run',
      }
      vi.mocked(client.post).mockResolvedValue({ data: mockRun })

      const result = await createRun(123456789, runData)

      expect(client.post).toHaveBeenCalledWith(
        '/api/v1/checklists/runs',
        runData,
        { params: { orgNumber: 123456789 } }
      )
      expect(result).toEqual(mockRun)
    })

    it('throws error when creation fails', async () => {
      vi.mocked(client.post).mockRejectedValue(new Error('Validation error'))

      await expect(createRun(123456789, { templateId: 1, runDate: '2024-01-01' })).rejects.toThrow('Validation error')
    })
  })

  describe('completeRun', () => {
    it('completes run with correct id', async () => {
      const mockRun = {
        runId: 1,
        templateId: 1,
        orgNumber: 123456789,
        status: 'COMPLETED',
        runDate: '2024-01-01',
      }
      vi.mocked(client.put).mockResolvedValue({ data: mockRun })

      const result = await completeRun(1, 123456789)

      expect(client.put).toHaveBeenCalledWith(
        '/api/v1/checklists/runs/1/complete',
        null,
        { params: { orgNumber: 123456789 } }
      )
      expect(result.status).toBe('COMPLETED')
    })
  })

  describe('updateRunItem', () => {
    it('updates run item with answer data', async () => {
      const mockItem = {
        runItemId: 1,
        runId: 1,
        templateItemId: 1,
        answerValue: 'Yes',
        isAnswered: true,
        isDeviation: false,
      }
      vi.mocked(client.put).mockResolvedValue({ data: mockItem })

      const result = await updateRunItem(1, 1, 123456789, {
        answerValue: 'Yes',
        notes: 'Test note',
        isDeviation: false,
      })

      expect(client.put).toHaveBeenCalledWith(
        '/api/v1/checklists/runs/1/items/1',
        {
          answerValue: 'Yes',
          notes: 'Test note',
          isDeviation: false,
        },
        { params: { orgNumber: 123456789 } }
      )
      expect(result).toEqual(mockItem)
    })
  })
})