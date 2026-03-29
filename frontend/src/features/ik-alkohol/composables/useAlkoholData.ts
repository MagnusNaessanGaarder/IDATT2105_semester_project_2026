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

type RawUser = {
  user_id: number
  display_name: string
}

type RawChecklistTemplateItem = {
  item_id: number
  label: string
  description: string | null
}

type RawChecklistRun = {
  run_id: number
  assigned_to_user_id: number | null
}

type RawChecklistRunItem = {
  run_item_id: number
  run_id: number
  template_item_id: number
  boolean_value: number | null
  text_value: string | null
  comment_text: string | null
  created_at: string
}

type RawTraining = {
  user_id: number
  title: string
  expires_at: string | null
  status: 'assigned' | 'completed' | 'expired'
}

const SOON_DAYS = 120

const users = alkoholData.app_user as RawUser[]
const checklistTemplateItems = alkoholData.checklist_template_item as RawChecklistTemplateItem[]
const checklistRuns = alkoholData.checklist_run as RawChecklistRun[]
const checklistRunItems = alkoholData.checklist_run_item as RawChecklistRunItem[]
const trainingRecords = alkoholData.training_record as RawTraining[]

const userNameById = new Map(users.map((user) => [user.user_id, user.display_name]))
const templateItemById = new Map(checklistTemplateItems.map((item) => [item.item_id, item]))
const runById = new Map(checklistRuns.map((run) => [run.run_id, run]))

const dailyControls = checklistRunItems.map((runItem) => {
  const templateItem = templateItemById.get(runItem.template_item_id)
  const run = runById.get(runItem.run_id)
  const timestamp = runItem.created_at

  return {
    id: runItem.run_item_id,
    name: templateItem?.label ?? 'Kontrollpunkt',
    law_unit: templateItem?.description ?? 'Alkoholloven',
    employee: run?.assigned_to_user_id ? (userNameById.get(run.assigned_to_user_id) ?? 'Ukjent') : 'Ukjent',
    comment: runItem.comment_text ?? runItem.text_value ?? '-',
    completion_date: {
      date: timestamp.slice(0, 10),
      time: timestamp.slice(11, 19),
    },
    attachment: runItem.template_item_id === 8106 ? 'rapport.pdf' : null,
    is_checked: runItem.boolean_value === 1,
  } satisfies DailyControlItem
})

const completedTrainingRecords = trainingRecords.filter((training) => training.status === 'completed')

const certificationTypes = Array.from(new Set(completedTrainingRecords.map((training) => training.title)))

const employees: EmployeeCertification[] = users.map((user) => ({
  name: user.display_name,
  certifications: completedTrainingRecords
    .filter((training) => training.user_id === user.user_id)
    .map((training) => ({
      name: training.title,
      expire_date: training.expires_at ?? '1970-01-01',
    })),
}))

const laws: LawItem[] = [
  {
    name: 'Alkoholloven',
    type: 'Lov',
    short: 'Lov om omsetning av alkoholholdig drikk m.v.',
    description:
      'Hovedloven som regulerer produksjon, innforsel, omsetning og skjenking av alkoholholdige drikkevarer i Norge.',
    link: 'https://lovdata.no/dokument/NL/lov/1989-06-02-27',
    last_updated_code: 'LOV-1989-06-02-27',
    sub_sections: [
      {
        section: '1-5',
        description: 'Aldersgrenser: 18 ar for ol/vin, 20 ar for brennevin',
      },
      {
        section: '4-1',
        description: 'Forbud mot salg/skjenking til mindrearige og berusede',
      },
      {
        section: '4-4',
        description: 'Skjenketider fastsatt av kommunen',
      },
    ],
  },
  {
    name: 'Alkoholforskriften',
    type: 'Forskrift',
    short: 'Forskrift til alkoholloven',
    description:
      'Utdyper og presiserer bestemmelsene i alkoholloven, inkludert krav til bevilling, internkontroll og kunnskapsprove.',
    link: 'https://lovdata.no/dokument/LTI/forskrift/2025-11-28-2361',
    last_updated_code: 'FOR-2025-11-28-2361',
    'sub-sections': [
      {
        section: '2-1',
        description: 'Krav til serveringssted og drift',
      },
      {
        section: '2-4',
        description: 'Krav om styrer med etablererprove',
      },
    ],
  },
]

const demands: DemandItem[] = [
  {
    title: 'Bevilling',
    bullet_points: [
      'Skjenkebevilling kreves for all alkoholservering',
      'Ma sokes hos kommunen',
      'Gjelder for spesifikke lokaler og tidsrom',
      'Kan tilbakekalles ved brudd pa regelverket',
    ],
  },
  {
    title: 'Kunnskapsprove',
    bullet_points: [
      'Alle som skjenker alkohol ma ha bestatt kunnskapsprove',
      'Gyldig i 5 ar',
      'Ma fornyes ved utlop',
      'Dokumentasjon ma oppbevares tilgjengelig',
    ],
  },
  {
    title: 'Internkontroll',
    bullet_points: [
      'Skriftlig internkontrollsystem er pakrevd',
      'Daglige kontrollrutiner ma dokumenteres',
      'Avvik skal registreres og handteres',
      'Tilsyn kan kreve innsyn nar som helst',
    ],
  },
]

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

const certificateCounts: Record<CertificateStatus, number> = employees.reduce(
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
  },
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