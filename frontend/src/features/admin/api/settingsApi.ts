import { client } from '@/api/client'

/**
 * Backend organization settings response
 * These are persisted in the database and shared across all users in the organization
 */
export interface BackendSettings {
  orgNumber: number
  timezoneName: string
  localeCode: string
  enableFoodModule: boolean
  enableAlcoholModule: boolean
  defaultTempMinC: number | null
  defaultTempMaxC: number | null
  reminderEmailEnabled: boolean
  reminderRecipientScope?: string | null
  reminderLeadHours?: number | null
  notificationEmail: string | null
  displayName: string | null
  legalName: string | null
  contactEmail: string | null
  contactPhone: string | null
  retentionUserMonths: number | null
  retentionAuditMonths: number | null
  createdAt: string
  updatedAt: string
}

/**
 * Request payload for updating backend settings
 */
export interface BackendSettingsRequest {
  timezoneName: string
  localeCode: string
  enableFoodModule: boolean
  enableAlcoholModule: boolean
  defaultTempMinC?: number | null
  defaultTempMaxC?: number | null
  reminderEmailEnabled: boolean
  notificationEmail: string | null
  displayName?: string | null
  legalName?: string | null
  contactEmail?: string | null
  contactPhone?: string | null
  retentionUserMonths?: number | null
  retentionAuditMonths?: number | null
}

/**
 * Settings API client for organization-wide configuration
 * All settings are persisted to the backend and require Admin/Manager role to modify
 */
export const settingsApi = {
  /**
   * Fetch current organization settings from backend
   * @param orgNumber Organization number to fetch settings for
   * @returns Backend settings for the organization
   */
  async getSettings(orgNumber: number): Promise<BackendSettings> {
    const response = await client.get(`/organizations/${orgNumber}/settings`)
    return response.data
  },

  /**
   * Update organization settings on backend
   * Requires Admin or Manager role
   * Changes affect all users in the organization
   * @param orgNumber Organization number
   * @param settings Updated settings
   * @returns Updated backend settings
   */
  async updateSettings(
    orgNumber: number,
    settings: BackendSettingsRequest
  ): Promise<BackendSettings> {
    const response = await client.put(
      `/organizations/${orgNumber}/settings`,
      settings
    )
    return response.data
  },
}
