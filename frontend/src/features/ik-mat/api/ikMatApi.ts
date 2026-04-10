import { client } from '@/api/client'
import { orgHeaders, withOrgNumber } from '@/shared/utils/orgContext'
import type {
  ChecklistRunApi,
  ChecklistRunCreateRequest,
  ChecklistRunItemApi,
  ChecklistRunItemUpdateRequest,
  ChecklistTemplateApi,
  ChecklistTemplateUpsertRequest,
  DeviationActionRequest,
  DeviationApi,
  DeviationStatusUpdateRequest,
  DeviationUpsertRequest,
  ExportCreateRequest,
  ExportJobApi,
  FileDocumentApi,
  LocationApi,
  LocationUpsertRequest,
  PageResponse,
  TemperatureEntryApi,
  TemperatureEntryCreateRequest,
  TemperatureEntryUpdateRequest,
  TemperatureLogPointApi,
  TemperatureLogPointUpsertRequest,
  OrganizationUserApi,
} from '../types'

const queryFromObject = (params: Record<string, unknown>) => withOrgNumber(params)

const toArrayPayload = <T>(payload: unknown, endpoint: string): T[] => {
  if (Array.isArray(payload)) {
    return payload as T[]
  }

  if (typeof payload === 'string') {
    try {
      const parsed = JSON.parse(payload) as unknown
      return toArrayPayload<T>(parsed, endpoint)
    } catch {
      // Non-JSON string payload - return empty array
      return []
    }
  }

  if (payload && typeof payload === 'object') {
    const record = payload as Record<string, unknown>

    if (Array.isArray(record.content)) {
      return record.content as T[]
    }

    if (Array.isArray(record.data)) {
      return record.data as T[]
    }

    if (Array.isArray(record.items)) {
      return record.items as T[]
    }
  }

  // Unexpected payload shape - return empty array
  return []
}

const hasResponse = (value: unknown): value is { response?: { status?: number; data?: { message?: string } } } => {
  return typeof value === 'object' && value !== null && 'response' in value
}

const optionalEndpointUnavailable = {
  checklistRuns: false,
  locations: false,
  deviations: false,
}

/** Call before a forced reload so that previously-failed optional endpoints are retried. */
export const resetOptionalEndpointFlags = () => {
  optionalEndpointUnavailable.checklistRuns = false
  optionalEndpointUnavailable.locations = false
  optionalEndpointUnavailable.deviations = false
}

export const ikMatApi = {
  async getLocationById(id: number): Promise<LocationApi> {
    const response = await client.get<LocationApi>(`/locations/${id}`, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async getLocations(): Promise<LocationApi[]> {
    if (optionalEndpointUnavailable.locations) {
      return []
    }

    try {
      const response = await client.get<LocationApi[]>('/locations', {
        params: queryFromObject({}),
        skipGlobalErrorLog: true,
      })
      const locations = toArrayPayload<LocationApi>(response.data, 'getLocations')
      return locations
    } catch (err: unknown) {
      if (hasResponse(err) && err.response?.status === 500) {
        optionalEndpointUnavailable.locations = true
        return []
      }
      throw err
    }
  },

  async createLocation(payload: LocationUpsertRequest): Promise<LocationApi> {
    const response = await client.post<LocationApi>('/locations', payload, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async updateLocation(id: number, payload: LocationUpsertRequest): Promise<LocationApi> {
    const response = await client.put<LocationApi>(`/locations/${id}`, payload, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async deleteLocation(id: number): Promise<void> {
    await client.delete(`/locations/${id}`, {
      params: queryFromObject({}),
    })
  },

  async getChecklistTemplates(): Promise<ChecklistTemplateApi[]> {
    const response = await client.get<ChecklistTemplateApi[]>('/checklists/templates/module/FOOD', {
      params: queryFromObject({}),
    })
    const templates = toArrayPayload<ChecklistTemplateApi>(response.data, 'getChecklistTemplates')
    return templates
  },

  async getAllChecklistTemplates(): Promise<ChecklistTemplateApi[]> {
    const response = await client.get<ChecklistTemplateApi[]>('/checklists/templates', {
      params: queryFromObject({}),
    })
    return toArrayPayload<ChecklistTemplateApi>(response.data, 'getAllChecklistTemplates')
  },

  async getChecklistTemplateById(id: number): Promise<ChecklistTemplateApi> {
    const response = await client.get<ChecklistTemplateApi>(`/checklists/templates/${id}`, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async getChecklistTemplatesByModule(moduleType: 'FOOD' | 'ALCOHOL' | string): Promise<ChecklistTemplateApi[]> {
    const response = await client.get<ChecklistTemplateApi[]>(`/checklists/templates/module/${moduleType}`, {
      params: queryFromObject({}),
    })
    return toArrayPayload<ChecklistTemplateApi>(response.data, `getChecklistTemplatesByModule(${moduleType})`)
  },

  async getActiveChecklistTemplates(): Promise<ChecklistTemplateApi[]> {
    const response = await client.get<ChecklistTemplateApi[]>('/checklists/templates/active', {
      params: queryFromObject({}),
    })
    return toArrayPayload<ChecklistTemplateApi>(response.data, 'getActiveChecklistTemplates')
  },

  async createChecklistTemplate(payload: ChecklistTemplateUpsertRequest): Promise<ChecklistTemplateApi> {
    const response = await client.post<ChecklistTemplateApi>('/checklists/templates', payload, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async updateChecklistTemplate(id: number, payload: ChecklistTemplateUpsertRequest): Promise<ChecklistTemplateApi> {
    const response = await client.put<ChecklistTemplateApi>(`/checklists/templates/${id}`, payload, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async deleteChecklistTemplate(id: number): Promise<void> {
    await client.delete(`/checklists/templates/${id}`, {
      params: queryFromObject({}),
    })
  },

  async getChecklistRuns(status?: 'DRAFT' | 'IN_PROGRESS' | 'COMPLETED' | 'OVERDUE' | 'CANCELLED' | string): Promise<ChecklistRunApi[]> {
    if (optionalEndpointUnavailable.checklistRuns) {
      return []
    }

    try {
      const response = await client.get<ChecklistRunApi[]>('/checklists/runs', {
        params: queryFromObject(status ? { status } : {}),
        skipGlobalErrorLog: true,
      })
      const runs = toArrayPayload<ChecklistRunApi>(response.data, 'getChecklistRuns')
      return runs
    } catch (err: unknown) {
      if (hasResponse(err) && err.response?.status === 500) {
        optionalEndpointUnavailable.checklistRuns = true
        return []
      }
      throw err
    }
  },

  async getChecklistRunById(id: number): Promise<ChecklistRunApi> {
    const response = await client.get<ChecklistRunApi>(`/checklists/runs/${id}`, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async getChecklistRunItems(id: number): Promise<ChecklistRunItemApi[]> {
    const response = await client.get<ChecklistRunItemApi[]>(`/checklists/runs/${id}/items`, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async createChecklistRun(payload: ChecklistRunCreateRequest): Promise<ChecklistRunApi> {
    const response = await client.post<ChecklistRunApi>('/checklists/runs', payload, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async updateChecklistRunItem(runId: number, itemId: number, payload: ChecklistRunItemUpdateRequest): Promise<ChecklistRunItemApi> {
    const response = await client.put<ChecklistRunItemApi>(`/checklists/runs/${runId}/items/${itemId}`, payload, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async completeChecklistRun(id: number): Promise<ChecklistRunApi> {
    const response = await client.put<ChecklistRunApi>(`/checklists/runs/${id}/complete`, undefined, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async getTemperatureEntries(): Promise<TemperatureEntryApi[]> {
    const response = await client.get<TemperatureEntryApi[]>('/temperature/entries', {
      params: queryFromObject({}),
    })
    const entries = toArrayPayload<TemperatureEntryApi>(response.data, 'getTemperatureEntries')
    return entries
  },

  async getTemperatureEntryById(entryId: number): Promise<TemperatureEntryApi> {
    const response = await client.get<TemperatureEntryApi>(`/temperature/entries/${entryId}`, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async createTemperatureEntry(payload: TemperatureEntryCreateRequest): Promise<TemperatureEntryApi> {
    const response = await client.post<TemperatureEntryApi>('/temperature/entries', payload, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async updateTemperatureEntry(entryId: number, payload: TemperatureEntryUpdateRequest): Promise<TemperatureEntryApi> {
    const response = await client.put<TemperatureEntryApi>(`/temperature/entries/${entryId}`, payload, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async getTemperatureAlerts(): Promise<TemperatureEntryApi[]> {
    const response = await client.get<TemperatureEntryApi[]>('/temperature/alerts', {
      params: queryFromObject({}),
    })
    return response.data
  },

  async getTemperatureEntriesByPoint(pointId: number): Promise<TemperatureEntryApi[]> {
    const response = await client.get<TemperatureEntryApi[]>(`/temperature/entries/by-point/${pointId}`, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async getTemperatureEntriesByDate(from: string, to: string): Promise<TemperatureEntryApi[]> {
    const response = await client.get<TemperatureEntryApi[]>('/temperature/entries/by-date', {
      params: queryFromObject({ from, to }),
    })
    return response.data
  },

  async getTemperaturePoints(): Promise<TemperatureLogPointApi[]> {
    const response = await client.get<TemperatureLogPointApi[]>('/temperature/points', {
      params: queryFromObject({}),
    })
    return toArrayPayload<TemperatureLogPointApi>(response.data, 'getTemperaturePoints')
  },

  async getActiveTemperaturePoints(): Promise<TemperatureLogPointApi[]> {
    const response = await client.get<TemperatureLogPointApi[]>('/temperature/points/active', {
      params: queryFromObject({}),
    })
    return toArrayPayload<TemperatureLogPointApi>(response.data, 'getActiveTemperaturePoints')
  },

  async getTemperaturePointById(pointId: number): Promise<TemperatureLogPointApi> {
    const response = await client.get<TemperatureLogPointApi>(`/temperature/points/${pointId}`, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async createTemperaturePoint(payload: TemperatureLogPointUpsertRequest): Promise<TemperatureLogPointApi> {
    const response = await client.post<TemperatureLogPointApi>('/temperature/points', payload, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async updateTemperaturePoint(pointId: number, payload: TemperatureLogPointUpsertRequest): Promise<TemperatureLogPointApi> {
    const response = await client.put<TemperatureLogPointApi>(`/temperature/points/${pointId}`, payload, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async deleteTemperaturePoint(pointId: number): Promise<void> {
    await client.delete(`/temperature/points/${pointId}`, {
      params: queryFromObject({}),
    })
  },

  async clearTemperatureEntriesForPoint(pointId: number): Promise<void> {
    await client.delete(`/temperature/points/${pointId}/entries`, {
      params: queryFromObject({}),
    })
  },

  async getDeviations(): Promise<DeviationApi[]> {
    if (optionalEndpointUnavailable.deviations) {
      return []
    }

    try {
      const response = await client.get<DeviationApi[]>('/deviations', {
        params: queryFromObject({}),
        skipGlobalErrorLog: true,
      })
      const deviations = toArrayPayload<DeviationApi>(response.data, 'getDeviations')
      return deviations
    } catch (err: unknown) {
      if (hasResponse(err) && err.response?.status === 500) {
        optionalEndpointUnavailable.deviations = true
        return []
      }
      throw err
    }
  },

  async getDeviationById(id: number): Promise<DeviationApi> {
    const response = await client.get<DeviationApi>(`/deviations/${id}`, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async createDeviation(payload: DeviationUpsertRequest): Promise<DeviationApi> {
    const response = await client.post<DeviationApi>('/deviations', payload, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async updateDeviation(id: number, payload: DeviationUpsertRequest): Promise<DeviationApi> {
    const response = await client.put<DeviationApi>(`/deviations/${id}`, payload, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async updateDeviationStatus(id: number, payload: DeviationStatusUpdateRequest): Promise<DeviationApi> {
    const response = await client.put<DeviationApi>(`/deviations/${id}/status`, payload, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async assignDeviation(id: number, assignedToUserId: number): Promise<DeviationApi> {
    const response = await client.post<DeviationApi>(`/deviations/${id}/assign`, undefined, {
      params: queryFromObject({ assignedToUserId }),
    })
    return response.data
  },

  async addDeviationImmediateAction(id: number, payload: DeviationActionRequest): Promise<DeviationApi> {
    const response = await client.post<DeviationApi>(`/deviations/${id}/immediate-action`, payload, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async addDeviationCorrectiveAction(id: number, payload: DeviationActionRequest): Promise<DeviationApi> {
    const response = await client.post<DeviationApi>(`/deviations/${id}/corrective-action`, payload, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async closeDeviation(id: number): Promise<DeviationApi> {
    const response = await client.post<DeviationApi>(`/deviations/${id}/close`, undefined, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async deleteDeviation(id: number): Promise<void> {
    await client.delete(`/deviations/${id}`, {
      params: queryFromObject({}),
    })
  },

  async getDocuments(category?: string): Promise<FileDocumentApi[]> {
    const response = await client.get<FileDocumentApi[]>('/files', {
      params: queryFromObject(category ? { category } : {}),
      headers: orgHeaders(),
    })
    const documents = toArrayPayload<FileDocumentApi>(response.data, 'getDocuments')
    return documents
  },

  async uploadDocument(file: File, documentType = 'other', directory = 'documents'): Promise<unknown> {
    const formData = new FormData()
    formData.append('file', file)

    const response = await client.post('/files/upload', formData, {
      params: { documentType, directory },
      headers: orgHeaders({ 'Content-Type': 'multipart/form-data' }),
    })
    return response.data
  },

  async downloadDocument(documentId: number): Promise<Blob> {
    const response = await client.get<Blob>(`/files/download/${documentId}`, {
      headers: orgHeaders(),
      responseType: 'blob',
    })
    return response.data
  },

  async updateDocument(documentId: number, payload: { title?: string; description?: string }): Promise<FileDocumentApi> {
    const response = await client.put<FileDocumentApi>(`/files/${documentId}`, payload, {
      headers: orgHeaders(),
    })
    return response.data
  },

  async deleteDocument(documentId: number): Promise<void> {
    await client.delete(`/files/${documentId}`, {
      headers: orgHeaders(),
    })
  },

  async listExports(page = 0, size = 20, sort?: string[]): Promise<PageResponse<ExportJobApi>> {
    const response = await client.get<PageResponse<ExportJobApi>>('/exports', {
      params: queryFromObject({ page, size, sort }),
    })
    return response.data
  },

  async createExportJob(payload: ExportCreateRequest): Promise<ExportJobApi> {
    const response = await client.post<ExportJobApi>('/exports', payload, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async getExportStatus(exportJobId: number): Promise<ExportJobApi> {
    const response = await client.get<ExportJobApi>(`/exports/${exportJobId}`, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async getExportDownloadUrl(exportJobId: number): Promise<string> {
    const response = await client.get<string>(`/exports/${exportJobId}/download`, {
      params: queryFromObject({}),
    })
    return response.data
  },

  async getOrganizationUsers(): Promise<OrganizationUserApi[]> {
    const response = await client.get<OrganizationUserApi[]>('/users', {
      params: queryFromObject({}),
      skipGlobalErrorLog: true,
    })
    return toArrayPayload<OrganizationUserApi>(response.data, 'getOrganizationUsers')
  },

  isAxiosError(error: unknown): boolean {
    return hasResponse(error)
  },

  getAxiosErrorMessage(error: unknown): string {
    if (hasResponse(error)) {
      const errorResponse = error.response?.data
      return errorResponse?.message || 'Ukjent API-feil'
    }

    return 'Ukjent API-feil'
  },
}
