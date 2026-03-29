import alkoholData from '@/data/ik-alkohol.json'

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

export type CertificateStatus = 'Gyldig' | 'Utløper snart' | 'Utgått'

const SOON_DAYS = 120

const dailyControls = alkoholData['daily-control'] as DailyControlItem[]
const certificationTypes = alkoholData.certifications.types
const employees = alkoholData.certifications.employees as EmployeeCertification[]
const laws = alkoholData.law_framework.laws as LawItem[]
const demands = alkoholData.law_framework.demands as DemandItem[]

const sectionsForLaw = (law: LawItem): LawSection[] => law.sub_sections ?? law['sub-sections'] ?? []

const toDate = (dateValue: string): Date | null => {
  const parsed = new Date(dateValue)
  if (Number.isNaN(parsed.getTime())) {
    return null
  }

  parsed.setHours(0, 0, 0, 0)
  return parsed
}

const certificateStatus = (expireDate: string): CertificateStatus => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  const expiryDate = toDate(expireDate)
  if (!expiryDate) {
    return 'Utgått'
  }

  const daysUntilExpiry = Math.floor((expiryDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))

  if (daysUntilExpiry < 0) {
    return 'Utgått'
  }

  if (daysUntilExpiry <= SOON_DAYS) {
    return 'Utløper snart'
  }

  return 'Gyldig'
}

const formattedDate = (value: string): string => {
  const parsedDate = toDate(value)
  if (!parsedDate) {
    return value
  }

  return parsedDate.toLocaleDateString('nb-NO')
}

const totalCertificates = employees.reduce((sum, employee) => sum + employee.certifications.length, 0)

const certificateCounts = employees.reduce(
  (counts, employee) => {
    employee.certifications.forEach((certification) => {
      const status = certificateStatus(certification.expire_date)
      counts[status] += 1
    })

    return counts
  },
  {
    Gyldig: 0,
    'Utløper snart': 0,
    Utgått: 0,
  } as Record<CertificateStatus, number>,
)

const staffWithExpired = employees
  .filter((employee) => employee.certifications.some((certification) => certificateStatus(certification.expire_date) === 'Utgått'))
  .map((employee) => employee.name)

const completedControls = dailyControls.filter((item) => item.is_checked).length
const pendingControls = dailyControls.length - completedControls
const completionRate = dailyControls.length > 0 ? Math.round((completedControls / dailyControls.length) * 100) : 0

export const useAlkoholData = () => ({
  dailyControls,
  certificationTypes,
  employees,
  laws,
  demands,
  totalCertificates,
  certificateCounts,
  staffWithExpired,
  completedControls,
  pendingControls,
  completionRate,
  sectionsForLaw,
  certificateStatus,
  formattedDate,
})
