import { computed, ref } from 'vue'
import { getRuns, type ChecklistRun } from '../api/checklistsRun'

export interface DailyControlItem {
  id: number
  run_id: number | null
  run_status: string | null
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

interface ChecklistTemplateApi {
  templateId: number
  title: string
  description: string | null
  moduleType: string
}

interface ChecklistRunApi {
  runId: number
  templateId: number
  templateTitle: string | null
  performedByUserId: number | null
  runDate: string | null
  completedAt: string | null
  status: 'DRAFT' | 'IN_PROGRESS' | 'COMPLETED' | 'OVERDUE' | string
  notes: string | null
}

const SOON_DAYS = 120

const dailyControls = ref<DailyControlItem[]>([])
const certificationTypes = ref<string[]>([])
const employees = ref<EmployeeCertification[]>([])
const laws = ref<LawItem[]>([])
const demands = ref<DemandItem[]>([])
const isLoading = ref(false)
const hasLoaded = ref(false)

interface ChecklistRunItemApi {
  runItemId?: number
  templateItemId?: number
  templateItemLabel?: string | null
  booleanValue?: boolean | null
  commentText?: string | null
  updatedAt?: string | null
  createdAt?: string | null
}

interface ChecklistRunApi extends ChecklistRun {
  runId?: number
  templateTitle?: string | null
  performedByUserId?: number | null
  assignedToUserId?: number | null
  runDate?: string | null
  items?: ChecklistRunItemApi[]
}

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

const todayDateString = () => {
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

const mapRunItemToDailyControl = (
  run: ChecklistRunApi,
  item: ChecklistRunItemApi,
  index: number,
): DailyControlItem => {
  const completionDate = toCompletionDateParts(
    item.updatedAt ?? item.createdAt ?? run.runDate,
  )

  return {
    id: Number(item.runItemId ?? `${run.runId ?? 0}${index + 1}`),
    run_id: run.runId ?? null,
    run_status: asString(run.status) || null,
    name:
      asString(item.templateItemLabel) ||
      (item.templateItemId ? `Kontrollpunkt ${item.templateItemId}` : '') ||
      asString(run.templateTitle) ||
      `Kontrollpunkt ${index + 1}`,
    law_unit: asString(run.templateTitle) || 'Daglig alkoholkontroll',
    employee: asString(run.performedByUserId ?? run.assignedToUserId) || 'Ukjent',
    comment: asString(item.commentText),
    completion_date: completionDate,
    attachment: null,
    is_checked: asString(run.status) === 'COMPLETED' || asBoolean(item.booleanValue),
  }
}

const loadRuns = async () => {
  if (hasLoaded.value || isLoading.value) {
    return
  }

  isLoading.value = true

  try {
    const result = await getRuns()
    dailyControls.value = result.ok
      ? (result.data as ChecklistRunApi[])
          .filter((run) => asString(run.runDate) === todayDateString())
          .flatMap((run) =>
          (run.items ?? []).map((item, index) => mapRunItemToDailyControl(run, item, index)),
          )
      : []
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
