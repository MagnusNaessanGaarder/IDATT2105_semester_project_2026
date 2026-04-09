/**
 * IK-Alkohol Feature Types
 * Core types for alcohol control, certifications, employee management, and regulations
 */

// ═══════════════════════════════════════════════════════════════════════════
// Daily Control Types
// ═══════════════════════════════════════════════════════════════════════════

/** Daily control item for alcohol-related tasks */
export interface DailyControlItem {
  id: number
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

// ═══════════════════════════════════════════════════════════════════════════
// Certification Types
// ═══════════════════════════════════════════════════════════════════════════

/** Employee certificate with expiry date */
export interface EmployeeCertificate {
  name: string
  expire_date: string
}

/** Employee with list of certifications */
export interface EmployeeCertification {
  name: string
  certifications: EmployeeCertificate[]
}

/** Possible certificate statuses */
export type CertificateStatus = 'Gyldig' | 'Utløper snart' | 'Utgått'

// ═══════════════════════════════════════════════════════════════════════════
// Regulations / Law Types
// ═══════════════════════════════════════════════════════════════════════════

/** Sub-section within a law/regulation */
export interface LawSection {
  section: string
  description: string
}

/** Law or regulation item */
export interface LawItem {
  name: string
  type: string
  short: string
  description: string
  link: string
  last_updated_code: string
  sub_sections?: LawSection[]
  'sub-sections'?: LawSection[] // API fallback naming
}

/** Demand/requirement item */
export interface DemandItem {
  title: string
  bullet_points: string[]
}

// ═══════════════════════════════════════════════════════════════════════════
// API Types (internal, for mapping backend responses)
// ═══════════════════════════════════════════════════════════════════════════

/** Checklist template from API */
export interface ChecklistTemplateApi {
  templateId: number
  title: string
  description: string | null
  moduleType: string
}

/** Checklist run from API */
export interface ChecklistRunApi {
  runId: number
  templateId: number
  templateTitle: string | null
  performedByUserId: number | null
  runDate: string | null
  completedAt: string | null
  status: 'DRAFT' | 'IN_PROGRESS' | 'COMPLETED' | 'OVERDUE' | string
  notes: string | null
}

// ═══════════════════════════════════════════════════════════════════════════
// Constants
// ═══════════════════════════════════════════════════════════════════════════

/** Days before certificate expiry to show "expiring soon" warning */
export const CERTIFICATE_EXPIRY_SOON_DAYS = 120
