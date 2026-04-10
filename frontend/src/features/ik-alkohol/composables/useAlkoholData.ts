import { reactive, ref } from 'vue'
import { client } from '@/api/client'
import { withOrgNumber } from '@/shared/utils/orgContext'
import { formatDateForOrganization } from '@/shared/utils/orgSettings'

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

const dailyControls = reactive<DailyControlItem[]>([])
const certificationTypes = reactive<string[]>([])
const employees = reactive<EmployeeCertification[]>([])
const laws = reactive<LawItem[]>([])
const demands = reactive<DemandItem[]>([])
let checklistRunsEndpointUnavailable = false

let hasLoaded = false
let loadInFlight: Promise<void> | null = null
const isLoading = ref(false)
const error = ref<string | null>(null)

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

  return formatDateForOrganization(parsedDate)
}

const totalCertificates = () => employees.reduce((sum: number, employee: EmployeeCertification) => sum + employee.certifications.length, 0)

const certificateCounts = (): Record<CertificateStatus, number> => employees.reduce(
  (counts: Record<CertificateStatus, number>, employee: EmployeeCertification) => {
    employee.certifications.forEach((certification: EmployeeCertificate) => {
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

const staffWithExpired = () => employees
  .filter((employee: EmployeeCertification) => employee.certifications.some((certification: EmployeeCertificate) => certificateStatus(certification.expire_date) === 'Utgått'))
  .map((employee: EmployeeCertification) => employee.name)

const completedControls = () => dailyControls.filter((item: DailyControlItem) => item.is_checked).length
const pendingControls = () => dailyControls.length - completedControls()
const completionRate = () => (dailyControls.length > 0 ? Math.round((completedControls() / dailyControls.length) * 100) : 0)

const splitIsoDateTime = (value: string | null): { date: string; time: string } => {
  if (!value) {
    return { date: '', time: '' }
  }

  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return { date: value.slice(0, 10), time: value.slice(11, 16) }
  }

  const toTwo = (part: number) => String(part).padStart(2, '0')

  return {
    date: `${parsed.getFullYear()}-${toTwo(parsed.getMonth() + 1)}-${toTwo(parsed.getDate())}`,
    time: `${toTwo(parsed.getHours())}:${toTwo(parsed.getMinutes())}`,
  }
}

const loadData = async (): Promise<void> => {
  if (hasLoaded) {
    return
  }

  if (loadInFlight) {
    return loadInFlight
  }

  loadInFlight = (async () => {
    isLoading.value = true
    error.value = null

    try {
      const [templatesResponse, runsResponse] = await Promise.allSettled([
        client.get<ChecklistTemplateApi[]>('/checklists/templates/module/ALCOHOL', {
          params: withOrgNumber({}),
        }),
        checklistRunsEndpointUnavailable
          ? Promise.resolve({ data: [] as ChecklistRunApi[] })
          : client.get<ChecklistRunApi[]>('/checklists/runs', {
            params: withOrgNumber({}),
            skipGlobalErrorLog: true,
          }).catch((err: unknown) => {
            if (typeof err === 'object' && err !== null && 'response' in err) {
              const response = (err as { response?: { status?: number } }).response
              if (response?.status === 500) {
                checklistRunsEndpointUnavailable = true
                return { data: [] as ChecklistRunApi[] }
              }
            }
            throw err
          }),
      ])

      const templates = (templatesResponse.status === 'fulfilled' ? templatesResponse.value.data : []) as ChecklistTemplateApi[]
      const allRuns = (runsResponse.status === 'fulfilled' ? runsResponse.value.data : []) as ChecklistRunApi[]
      const alcoholTemplateIds = new Set(templates.map((template: ChecklistTemplateApi) => template.templateId))
      const alcoholRuns = allRuns.filter((run: ChecklistRunApi) => alcoholTemplateIds.has(run.templateId))

      const mappedControlsFromRuns = alcoholRuns
        .slice()
        .sort((a: ChecklistRunApi, b: ChecklistRunApi) => new Date(b.completedAt ?? b.runDate ?? 0).getTime() - new Date(a.completedAt ?? a.runDate ?? 0).getTime())
        .map((run: ChecklistRunApi) => {
          const split = splitIsoDateTime(run.completedAt ?? run.runDate)
          return {
            id: run.runId,
            name: run.templateTitle ?? `Kontroll ${run.templateId}`,
            law_unit: 'ALKOHOLLOVEN',
            employee: run.performedByUserId ? `Bruker ${run.performedByUserId}` : 'Ukjent',
            comment: run.notes ?? '',
            completion_date: {
              date: split.date,
              time: split.time,
            },
            attachment: null,
            is_checked: run.status === 'COMPLETED',
          } satisfies DailyControlItem
        })

      const mappedControlsFromTemplates = templates.map((template: ChecklistTemplateApi) => ({
        id: template.templateId,
        name: template.title,
        law_unit: 'ALKOHOLLOVEN',
        employee: 'Ikke registrert',
        comment: template.description ?? '',
        completion_date: {
          date: '',
          time: '',
        },
        attachment: null,
        is_checked: false,
      } satisfies DailyControlItem))

      const mappedControls = mappedControlsFromRuns.length > 0
        ? mappedControlsFromRuns
        : mappedControlsFromTemplates

      const employeeMap = new Map<string, EmployeeCertification>()
      alcoholRuns.forEach((run: ChecklistRunApi) => {
        const employeeName = run.performedByUserId ? `Bruker ${run.performedByUserId}` : 'Ukjent'
        const current = employeeMap.get(employeeName) ?? { name: employeeName, certifications: [] }
        const completedDate = splitIsoDateTime(run.completedAt ?? run.runDate).date
        const base = completedDate ? new Date(completedDate) : new Date()
        const expiry = new Date(base)
        expiry.setFullYear(expiry.getFullYear() + 1)
        const certName = run.templateTitle ?? 'Kunnskapsprove alkoholloven'

        if (!current.certifications.some((cert) => cert.name === certName)) {
          current.certifications.push({
            name: certName,
            expire_date: expiry.toISOString().slice(0, 10),
          })
        }

        employeeMap.set(employeeName, current)
      })

      const typeSet = new Set<string>()
      employeeMap.forEach((employee) => {
        employee.certifications.forEach((certification) => typeSet.add(certification.name))
      })

      templates.forEach((template: ChecklistTemplateApi) => {
        typeSet.add(template.title)
      })

      if (employeeMap.size === 0 && templates.length > 0) {
        employeeMap.set('Ikke registrert', {
          name: 'Ikke registrert',
          certifications: templates.map((template: ChecklistTemplateApi) => ({
            name: template.title,
            expire_date: '',
          })),
        })
      }

      const mappedLaws: LawItem[] = templates.map((template: ChecklistTemplateApi) => ({
        name: template.title,
        type: 'Forskrift',
        short: template.description ?? 'Operativ kontroll for ansvarlig servering',
        description: template.description ?? 'Kontrollpunkt definert i internkontrollsystemet',
        link: 'https://lovdata.no',
        last_updated_code: 'Internkontroll',
        sub_sections: [
          {
            section: 'Operativt krav',
            description: 'Kontrollpunktet skal gjennomfores og dokumenteres i henhold til virksomhetens rutiner.',
          },
        ],
      }))

      const mappedDemands: DemandItem[] = [
        {
          title: 'Daglig etterlevelse',
          bullet_points: [
            'Alle alkoholrelaterte kontroller skal registreres med dato og ansvarlig person.',
            'Manglende kontroll folges opp samme dag med korrigerende handling.',
          ],
        },
        {
          title: 'Kompetanse',
          bullet_points: [
            'Ansatte skal ha gyldig kunnskapsprove for sine oppgaver.',
            'Leder skal planlegge fornyelse i god tid for utlopende sertifiseringer.',
          ],
        },
      ]

      dailyControls.splice(0, dailyControls.length, ...mappedControls)
      employees.splice(0, employees.length, ...Array.from(employeeMap.values()))
      certificationTypes.splice(0, certificationTypes.length, ...Array.from(typeSet).sort())
      laws.splice(0, laws.length, ...mappedLaws)
      demands.splice(0, demands.length, ...mappedDemands)

      const failedCalls = [templatesResponse, runsResponse].filter((result) => result.status === 'rejected').length
      const succeededCalls = 2 - failedCalls

      if (succeededCalls === 0) {
        error.value = 'Alle IK-Alkohol endepunkter feilet. Kontroller innlogging og prov igjen.'
        return
      }

      error.value = null
      hasLoaded = true
    } catch {
      dailyControls.splice(0, dailyControls.length)
      employees.splice(0, employees.length)
      certificationTypes.splice(0, certificationTypes.length)
      laws.splice(0, laws.length)
      demands.splice(0, demands.length)
      error.value = 'Kunne ikke laste IK-Alkohol data fra API.'
    } finally {
      isLoading.value = false
      loadInFlight = null
    }
  })()

  return loadInFlight
}

const reload = async () => {
  hasLoaded = false
  await loadData()
}

export const useAlkoholData = () => {
  void loadData()

  return {
    dailyControls,
    certificationTypes,
    employees,
    laws,
    demands,
    get totalCertificates() {
      return totalCertificates()
    },
    get certificateCounts() {
      return certificateCounts()
    },
    get staffWithExpired() {
      return staffWithExpired()
    },
    get completedControls() {
      return completedControls()
    },
    get pendingControls() {
      return pendingControls()
    },
    get completionRate() {
      return completionRate()
    },
    sectionsForLaw,
    certificateStatus,
    formattedDate,
    isLoading,
    error,
    reload,
  }
}
