import { computed, ref } from 'vue'
import { getRuns, type ChecklistRun } from '../api/checklistsRun'

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

const dailyControls = ref<DailyControlItem[]>([])
const certificationTypes = ref<string[]>([])
const employees = ref<EmployeeCertification[]>([])
const laws = ref<LawItem[]>([])
const demands = ref<DemandItem[]>([])
const isLoading = ref(false)
const hasLoaded = ref(false)

const asString = (value: unknown): string => (typeof value === 'string' ? value : '')

const asBoolean = (value: unknown): boolean => value === true

const toCompletionDateParts = (value: unknown) => {
  const dateTime = asString(value)
  if (!dateTime) {
    return {
      date: '',
      time: '',
    }
  }

  if (dateTime.includes('T')) {
    const [date = '', time = ''] = dateTime.split('T')
    return {
      date,
      time: time.replace('Z', '').slice(0, 8),
    }
  }

  return {
    date: dateTime.slice(0, 10),
    time: '',
  }
}

const mapRunToDailyControl = (run: ChecklistRun, index: number): DailyControlItem => {
  const source = run as Record<string, unknown>
  const completionDate = toCompletionDateParts(
    source.completedAt ?? source.updatedAt ?? source.createdAt ?? source.runDate,
  )

  return {
    id: Number(source.id ?? index + 1),
    name: asString(source.templateTitle ?? source.name ?? source.title) || `Run ${index + 1}`,
    law_unit: asString(source.status) || 'Ukjent status',
    employee: asString(source.performedByUserId ?? source.assignedToUserId) || 'Ukjent',
    comment: asString(source.notes ?? source.description ?? source.comment),
    completion_date: completionDate,
    attachment: null,
    is_checked: asBoolean(source.status === 'COMPLETED' || source.completed),
  }
}

const loadRuns = async () => {
  if (hasLoaded.value || isLoading.value) {
    return
  }

  isLoading.value = true

  try {
    const result = await getRuns()
    dailyControls.value = result.ok ? result.data.map(mapRunToDailyControl) : []
  } catch {
    dailyControls.value = []
  } finally {
    hasLoaded.value = true
    isLoading.value = false
  }
}

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

const totalCertificates = computed(() => employees.value.reduce((sum, employee) => sum + employee.certifications.length, 0))

const certificateCounts = computed(
  () =>
    employees.value.reduce(
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
    ),
)

const staffWithExpired = computed(() =>
  employees.value
    .filter((employee) => employee.certifications.some((certification) => certificateStatus(certification.expire_date) === 'Utgått'))
    .map((employee) => employee.name),
)

const completedControls = computed(() => dailyControls.value.filter((item) => item.is_checked).length)
const pendingControls = computed(() => dailyControls.value.length - completedControls.value)
const completionRate = computed(() =>
  dailyControls.value.length > 0 ? Math.round((completedControls.value / dailyControls.value.length) * 100) : 0,
)

export const useAlkoholData = () => {
  void loadRuns()

  return {
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
    isLoading,
    sectionsForLaw,
    certificateStatus,
    formattedDate,
  }
}
