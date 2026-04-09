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
import { getKnownLegalSourceUrl } from '../utils/legalLinks'
import { parseLawReference } from '../utils/lawReference'

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

const toDateOnlyString = (value: unknown): string => {
  if (Array.isArray(value) && value.length >= 3) {
    const [year, month, day] = value
    if (
      typeof year === 'number' &&
      typeof month === 'number' &&
      typeof day === 'number' &&
      Number.isFinite(year) &&
      Number.isFinite(month) &&
      Number.isFinite(day)
    ) {
      return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    }
  }

  const dateTime = asString(value)
  if (!dateTime) {
    return ''
  }

  if (dateTime.includes('T')) {
    return dateTime.split('T')[0] ?? ''
  }

  return dateTime.slice(0, 10)
}

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

const mapDocumentToLaw = (document: OrganizationDocumentApi): LawItem => ({
  documentId: document.documentId,
  name: document.title,
  type: document.documentType === 'POLICY' ? 'Regelverk' : document.documentType,
  short: document.title,
  description: asString(document.description) || 'Dokumentert regelverk for alkoholservering.',
  link: getKnownLegalSourceUrl(document.title, document.description) ?? '',
  last_updated_code: `DOC-${document.documentId}`,
  sub_sections: [],
})

const mapDocumentToDemand = (document: OrganizationDocumentApi): DemandItem => ({
  title: document.title,
  bullet_points: [asString(document.description) || 'Eksempeldokument tilgjengelig i dokumentarkivet.'],
})

const mergeLawItems = (
  documents: OrganizationDocumentApi[],
  runs: ChecklistRunApi[],
): LawItem[] => {
  const lawsByKey = new Map<string, LawItem>()

  documents
    .filter((document) => document.documentType === 'POLICY')
    .map(mapDocumentToLaw)
    .forEach((law) => {
      lawsByKey.set(`doc:${law.documentId}`, law)
    })

  runs.forEach((run) => {
    const parsedLaw = parseLawReference(run.templateDescription)
    const normalizedTitle = parsedLaw.lawTitle?.trim()

    if (parsedLaw.lawDocumentId != null) {
      const existing = lawsByKey.get(`doc:${parsedLaw.lawDocumentId}`)
      if (!existing) {
        const link = getKnownLegalSourceUrl(normalizedTitle || `Lovverk ${parsedLaw.lawDocumentId}`)

        if (!link) {
          return
        }

        lawsByKey.set(`doc:${parsedLaw.lawDocumentId}`, {
          documentId: parsedLaw.lawDocumentId,
          name: normalizedTitle || `Lovverk ${parsedLaw.lawDocumentId}`,
          type: 'Regelverk',
          short: normalizedTitle || `Lovverk ${parsedLaw.lawDocumentId}`,
          description: 'Lovverk lagret pa sjekkpunktet.',
          link,
          last_updated_code: `DOC-${parsedLaw.lawDocumentId}`,
          sub_sections: [],
        })
      }
      return
    }

    if (normalizedTitle) {
      const link = getKnownLegalSourceUrl(normalizedTitle)
      if (!link) {
        return
      }

      lawsByKey.set(`title:${normalizedTitle.toLowerCase()}`, {
        documentId: -(lawsByKey.size + 1),
        name: normalizedTitle,
        type: 'Regelverk',
        short: normalizedTitle,
        description: 'Lovverk lagret pa sjekkpunktet.',
        link,
        last_updated_code: 'SAVED-RULE',
        sub_sections: [],
      })
    }
  })

  return Array.from(lawsByKey.values()).sort((left, right) => left.name.localeCompare(right.name, 'nb'))
}

const todayDateString = () => {
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

const latestRunByTemplateId = (runs: ChecklistRunApi[]): Map<number, ChecklistRunApi> => {
  const runsByTemplateId = new Map<number, ChecklistRunApi>()

  runs.forEach((run) => {
    if (typeof run.templateId !== 'number') {
      return
    }

    const current = runsByTemplateId.get(run.templateId)
    if (!current) {
      runsByTemplateId.set(run.templateId, run)
      return
    }

    const candidateDate = toDateOnlyString(run.runDate) || asString(run.updatedAt) || asString(run.createdAt)
    const currentDate = toDateOnlyString(current.runDate) || asString(current.updatedAt) || asString(current.createdAt)

    if (candidateDate > currentDate) {
      runsByTemplateId.set(run.templateId, run)
    }
  })

  return runsByTemplateId
}

const mapRunItemToDailyControl = (
  run: ChecklistRunApi,
  item: ChecklistRunItemApi | null,
  index: number,
  lawsById: Map<number, LawItem>,
): DailyControlItem => {
  const completionDate = toCompletionDateParts(
    item?.updatedAt ?? item?.createdAt ?? run.runDate,
  )
  const parsedLaw = parseLawReference(run.templateDescription)
  const selectedLaw = parsedLaw.lawDocumentId !== null ? lawsById.get(parsedLaw.lawDocumentId) : null
  const templateDescription = asString(run.templateDescription)
  const lawUnit = selectedLaw?.name
    ?? parsedLaw.lawTitle
    ?? (templateDescription || null)
    ?? 'Daglig alkoholkontroll'

  return {
    id: Number(item?.runItemId ?? `${run.runId ?? 0}${index + 1}`),
    run_id: run.runId ?? null,
    template_id: run.templateId ?? null,
    template_item_id: item?.templateItemId ?? null,
    law_document_id: selectedLaw?.documentId ?? parsedLaw.lawDocumentId,
    run_status: asString(run.status) || null,
    name:
      asString(item?.templateItemLabel) ||
      asString(run.templateTitle) ||
      (item?.templateItemId ? `Punkt ${item.templateItemId}` : '') ||
      `Punkt ${index + 1}`,
    law_unit: lawUnit,
    employee: asString(run.performedByUserId ?? run.assignedToUserId) || 'Ukjent',
    comment: asString(item?.commentText),
    completion_date: completionDate,
    attachment: null,
    is_checked: asString(run.status) === 'COMPLETED' || asBoolean(item?.booleanValue),
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
        getRuns(orgNumber),
        client.get<OrganizationDocumentApi[]>('/files', {
          params: { orgNumber },
        }),
      ])

      const documents =
        documentsResult.status === 'fulfilled' ? documentsResult.value.data.filter((doc) => doc.active) : []

      const runs =
        runsResult.status === 'fulfilled' && runsResult.value.ok
          ? (runsResult.value.data as ChecklistRunApi[])
          : []

      laws.value = mergeLawItems(documents, runs)

      const lawsById = new Map(laws.value.map((law) => [law.documentId, law]))

      const todaysRuns = runs.filter((run) => toDateOnlyString(run.runDate) === todayDateString())
      const runsToDisplay = todaysRuns.length > 0 ? todaysRuns : Array.from(latestRunByTemplateId(runs).values())

      dailyControls.value = runsToDisplay
        .flatMap((run) => {
          const runItems = run.items && run.items.length > 0 ? run.items : [null]
          return runItems.map((item, index) => mapRunItemToDailyControl(run, item, index, lawsById))
        })

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
