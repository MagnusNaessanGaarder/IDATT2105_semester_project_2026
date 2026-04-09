import { computed, ref } from 'vue'
import { client } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import { getRuns } from '../api/checklistsRun'
import type {
  CertificateStatus,
  ChecklistRunApi,
  ChecklistRunItemApi,
  DailyControlItem,
  DemandItem,
  EmployeeCertification,
  LawItem,
  LawSection,
  OrganizationDocumentApi,
} from '../types'

const SOON_DAYS = 120

const dailyControls = ref<DailyControlItem[]>([])
const certificationTypes = ref<string[]>([])
const employees = ref<EmployeeCertification[]>([])
const laws = ref<LawItem[]>([])
const demands = ref<DemandItem[]>([])
const isLoading = ref(false)
const hasLoaded = ref(false)
const error = ref<string | null>(null)
let loadInFlight: Promise<void> | null = null

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

const documentUrl = (documentId: number, orgNumber: number): string =>
  `/api/v1/files/download/${documentId}?orgNumber=${orgNumber}`

const mapDocumentToLaw = (document: OrganizationDocumentApi, orgNumber: number): LawItem => ({
  name: document.title,
  type: document.documentType === 'POLICY' ? 'Regelverk' : document.documentType,
  short: document.title,
  description: asString(document.description) || 'Dokumentert regelverk for alkoholservering.',
  link: documentUrl(document.documentId, orgNumber),
  last_updated_code: `DOC-${document.documentId}`,
  sub_sections: [],
})

const mapDocumentToDemand = (document: OrganizationDocumentApi): DemandItem => ({
  title: document.title,
  bullet_points: [asString(document.description) || 'Eksempeldokument tilgjengelig i dokumentarkivet.'],
})

export const mapRunItemToDailyControl = (
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
    template_item_id: item.templateItemId ?? null,
    run_status: asString(run.status) || null,
    name:
      asString(item.templateItemLabel) ||
      asString(run.templateTitle) ||
      (item.templateItemId ? `Punkt ${item.templateItemId}` : '') ||
      `Punkt ${index + 1}`,
    law_unit: asString(run.templateTitle) || 'Daglig alkoholkontroll',
    employee: asString(run.performedByUserId ?? run.assignedToUserId) || 'Ukjent',
    comment: asString(item.commentText),
    completion_date: completionDate,
    attachment: null,
    is_checked: asString(run.status) === 'COMPLETED' || asBoolean(item.booleanValue),
  }
}

const loadData = async () => {
  if (hasLoaded.value) {
    return
  }

  if (loadInFlight) {
    return loadInFlight
  }

  const authStore = useAuthStore()
  const orgNumber = authStore.currentOrg?.orgNumber ?? 937219997

  loadInFlight = (async () => {
    isLoading.value = true
    error.value = null

    try {
      const [runsResult, documentsResult] = await Promise.allSettled([
        getRuns(),
        client.get<OrganizationDocumentApi[]>('/files', {
          params: { orgNumber },
        }),
      ])

      if (runsResult.status === 'fulfilled' && runsResult.value.ok) {
        dailyControls.value = (runsResult.value.data as ChecklistRunApi[])
          .filter((run) => asString(run.runDate) === todayDateString())
          .flatMap((run) =>
            (run.items ?? []).map((item, index) => mapRunItemToDailyControl(run, item, index)),
          )
      } else {
        dailyControls.value = []
      }

      const documents =
        documentsResult.status === 'fulfilled' ? documentsResult.value.data.filter((doc) => doc.active) : []

      laws.value = documents
        .filter((document) => document.documentType === 'POLICY')
        .map((document) => mapDocumentToLaw(document, orgNumber))

      demands.value = documents
        .filter((document) => document.documentType === 'PROCEDURE' || document.documentType === 'TRAINING_MATERIAL')
        .map(mapDocumentToDemand)

      if (
        (runsResult.status === 'rejected' || (runsResult.status === 'fulfilled' && !runsResult.value.ok)) &&
        documentsResult.status === 'rejected'
      ) {
        error.value = 'Kunne ikke laste IK-Alkohol-data.'
      }
    } catch {
      dailyControls.value = []
      laws.value = []
      demands.value = []
      error.value = 'Kunne ikke laste IK-Alkohol-data.'
    } finally {
      hasLoaded.value = true
      isLoading.value = false
      loadInFlight = null
    }
  })()

  return loadInFlight
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
  void loadData()

  const reload = async () => {
    hasLoaded.value = false
    await loadData()
  }

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
    error,
    reload,
    sectionsForLaw,
    certificateStatus,
    formattedDate,
  }
}
