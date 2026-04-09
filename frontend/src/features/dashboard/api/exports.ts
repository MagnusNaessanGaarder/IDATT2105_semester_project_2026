/**
 * Export API Layer
 * Handles all export-related HTTP calls.
 * 
 * @remarks
 * This API layer contains only HTTP calls and type definitions.
 * No business logic, state management, or transformation happens here.
 * All export job orchestration (polling, retry, state) is handled by @/features/dashboard/composables/useExport.ts
 */

import { client } from '@/api/client'
import type { ExportRequest, ExportResponse, ExportPage } from '../types/index'

/**
 * Export API object containing all export-related endpoints
 */
export const exportsApi = {
  /**
   * Create a new export job
   * 
   * @param orgNumber - Organization number
   * @param request - Export request parameters
   * @returns The newly created export job
   */
  async createExport(orgNumber: number, request: ExportRequest): Promise<ExportResponse> {
    const response = await client.post<ExportResponse>(
      `/organizations/${orgNumber}/exports`,
      request
    )
    return response.data
  },

  /**
   * Get the status of a single export job
   * 
   * @param orgNumber - Organization number
   * @param exportJobId - Export job ID
   * @returns The export job with current status
   */
  async getExportStatus(orgNumber: number, exportJobId: number): Promise<ExportResponse> {
    const response = await client.get<ExportResponse>(
      `/organizations/${orgNumber}/exports/${exportJobId}`
    )
    return response.data
  },

  /**
   * Get the download URL for an export
   * 
   * @param orgNumber - Organization number
   * @param exportJobId - Export job ID
   * @returns Relative download URL path
   */
  async getDownloadUrl(orgNumber: number, exportJobId: number): Promise<string> {
    const response = await client.get<{ url: string }>(
      `/organizations/${orgNumber}/exports/${exportJobId}/download-url`
    )
    return response.data.url
  },

  /**
   * List all exports for an organization (paginated)
   * 
   * @param orgNumber - Organization number
   * @param page - Page number (default: 0)
   * @param size - Page size (default: 20)
   * @returns Paginated export list
   */
  async listExports(
    orgNumber: number,
    page: number = 0,
    size: number = 20
  ): Promise<ExportPage> {
    const response = await client.get<ExportPage>(
      `/organizations/${orgNumber}/exports?page=${page}&size=${size}`
    )
    return response.data
  },
}
