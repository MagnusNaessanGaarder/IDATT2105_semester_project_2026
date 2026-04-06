import { reactive, ref } from 'vue'
import { client } from '@/api/client'
import { getOrgNumber, orgHeaders, withOrgNumber } from '@/shared/utils/orgContext'
import { ensureDemoData } from '@/shared/utils/seedDemoData'

export interface DashboardStat {
  label: string
  value: number
  trend: 'up' | 'down' | 'neutral'
  color: 'success' | 'warning' | 'info'
  unit?: string
}

export interface RecentCheck {
  id: number
  name: string
  completed_by: string
  completed_date: string
  completed_time: string
  status: 'completed' | 'pending' | 'overdue'
}

export interface ChecklistItem {
  id: number
  task: string
  required: boolean
  completed: boolean
  notes: string | null
}

export interface Checklist {
  id: number
  name: string
  category: string
  frequency: 'Daglig' | 'Ukentlig' | 'Månedlig' | string
  description: string
  created_date: string
  law_unit: string
  items: ChecklistItem[]
  completed_by: string | null
  completion_date: string | null
  completion_time: string | null
  status: 'completed' | 'pending' | 'overdue'
}

export interface TemperatureRecord {
  id: number
  location: string
  temperature_c: number
  min_temp: number
  max_temp: number
  recorded_by: string
  recorded_date: string
  recorded_time: string
  status: 'ok' | 'warning' | 'critical'
}

export interface Deviation {
  id: number
  title: string
  description: string
  severity: 'low' | 'medium' | 'high'
  reported_by: string
  reported_date: string
  reported_time: string
  location: string
  immediate_action: string
  corrective_action: string
  status: 'open' | 'resolved' | 'in-progress'
}

export interface HaccpPoint {
  id: number
  number: string
  name: string
  description: string
  hazards: string[]
  critical_limits: string
  monitoring: string
  corrective_actions: string
  verification: string
  responsible: string
}

export interface SupportingDocument {
  id: number
  name: string
  date_updated: string
  description: string
}

interface ChecklistTemplateApi {
  templateId: number
  title: string
  description: string | null
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY' | string
  moduleType: string
  items?: Array<{
    itemId: number
    label: string
    isRequired?: boolean
    description?: string | null
  }>
}

interface ChecklistRunApi {
  runId: number
  templateId: number
  templateTitle: string | null
  performedByUserId: number | null
  runDate: string | null
  completedAt: string | null
  status: 'DRAFT' | 'IN_PROGRESS' | 'COMPLETED' | 'OVERDUE' | string
  items?: Array<{
    templateItemId: number
    templateItemLabel: string | null
    hasAnswer: boolean
    commentText: string | null
  }>
}

interface TemperatureEntryApi {
  entryId: number
  locationId: number | null
  locationName: string | null
  logPointName: string | null
  temperatureC: number
  isAlert: boolean
  recordedByName: string | null
  measuredAt: string
}

interface LocationApi {
  locationId: number
  name: string
  tempMinC: number | null
  tempMaxC: number | null
}

interface DeviationApi {
  reportId: number
  title: string
  description: string
  severity: string
  status: string
  locationText: string | null
  occurredDate: string | null
  occurredTime: string | null
  reportDate: string | null
  reportedBy?: { fullName?: string; email?: string } | null
  immediateActionText?: string | null
  correctiveActionText?: string | null
}

const dashboardStats = reactive<DashboardStat[]>([])
const recentChecks = reactive<RecentCheck[]>([])
const checklists = reactive<Checklist[]>([])
const temperatureRecords = reactive<TemperatureRecord[]>([])
const deviations = reactive<Deviation[]>([])
const haccpPlan = reactive({
  plan_name: 'HACCP-plan',
  version: '1.0',
  last_updated: new Date().toISOString(),
  critical_control_points: [] as HaccpPoint[],
  supporting_documents: [] as SupportingDocument[],
})

let hasLoaded = false
let loadInFlight: Promise<void> | null = null
const isLoading = ref(false)
const error = ref<string | null>(null)

const frequencyLabel = (frequency: ChecklistTemplateApi['frequency']): Checklist['frequency'] => {
  if (frequency === 'DAILY') return 'Daglig'
  if (frequency === 'WEEKLY') return 'Ukentlig'
  if (frequency === 'MONTHLY') return 'Månedlig'
  return frequency
}

const checklistStatusFromRun = (status: ChecklistRunApi['status']): Checklist['status'] => {
  if (status === 'COMPLETED') return 'completed'
  if (status === 'OVERDUE') return 'overdue'
  return 'pending'
}

const deviationStatus = (status: string): Deviation['status'] => {
  if (status === 'CLOSED') return 'resolved'
  if (status === 'CORRECTIVE_ACTION_PLANNED' || status === 'UNDER_INVESTIGATION') return 'in-progress'
  return 'open'
}

const deviationSeverity = (severity: string): Deviation['severity'] => {
  if (severity === 'CRITICAL') return 'high'
  if (severity === 'MAJOR') return 'medium'
  return 'low'
}

const splitIsoDateTime = (value: string | null): { date: string; time: string } => {
  if (!value) {
    return { date: '', time: '' }
  }

  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return { date: value.slice(0, 10), time: value.slice(11, 16) }
  }

  return {
    date: parsed.toISOString().slice(0, 10),
    time: parsed.toISOString().slice(11, 16),
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
      await ensureDemoData()

      const orgNumber = getOrgNumber()

      const [templatesResponse, runsResponse, entriesResponse, locationsResponse, deviationsResponse, documentsResponse] = await Promise.allSettled([
        client.get<ChecklistTemplateApi[]>('/checklists/templates/module/FOOD', {
          params: withOrgNumber({}),
        }),
        client.get<ChecklistRunApi[]>('/checklists/runs', {
          params: withOrgNumber({}),
        }),
        client.get<TemperatureEntryApi[]>('/temperature/entries', {
          params: withOrgNumber({}),
        }),
        client.get<LocationApi[]>('/locations', {
          params: withOrgNumber({}),
        }),
        client.get<DeviationApi[]>('/deviations', {
          params: withOrgNumber({}),
        }),
        client.get<Array<{ documentId: number; title: string; description: string | null; updatedAt: string | null }>>('/files', {
          params: withOrgNumber({}),
          headers: orgHeaders(),
        }),
      ])

      const templates = templatesResponse.status === 'fulfilled' ? templatesResponse.value.data : []
      const runs = runsResponse.status === 'fulfilled' ? runsResponse.value.data : []
      const entries = entriesResponse.status === 'fulfilled' ? entriesResponse.value.data : []
      const locations = locationsResponse.status === 'fulfilled' ? locationsResponse.value.data : []
      const deviationReports = deviationsResponse.status === 'fulfilled' ? deviationsResponse.value.data : []
      const documents = documentsResponse.status === 'fulfilled' ? documentsResponse.value.data : []

      const locationById = new Map(locations.map((location) => [location.locationId, location]))
      const runsByTemplateId = new Map<number, ChecklistRunApi[]>()

      runs.forEach((run) => {
        const current = runsByTemplateId.get(run.templateId) ?? []
        current.push(run)
        runsByTemplateId.set(run.templateId, current)
      })

      const mappedChecklists = templates.map((template) => {
        const templateRuns = runsByTemplateId.get(template.templateId) ?? []
        const latestRun = [...templateRuns].sort((a, b) => {
          const left = new Date(b.completedAt ?? b.runDate ?? 0).getTime()
          const right = new Date(a.completedAt ?? a.runDate ?? 0).getTime()
          return left - right
        })[0]

        const itemByTemplateId = new Map((latestRun?.items ?? []).map((item) => [item.templateItemId, item]))

        const items: ChecklistItem[] = (template.items ?? []).map((item) => {
          const answer = itemByTemplateId.get(item.itemId)
          return {
            id: item.itemId,
            task: item.label,
            required: Boolean(item.isRequired),
            completed: Boolean(answer?.hasAnswer),
            notes: answer?.commentText ?? item.description ?? null,
          }
        })

        const completionDate = latestRun?.completedAt ?? latestRun?.runDate ?? null
        const completionSplit = splitIsoDateTime(completionDate)

        return {
          id: template.templateId,
          name: template.title,
          category: 'IK-MAT',
          frequency: frequencyLabel(template.frequency),
          description: template.description ?? 'Ingen beskrivelse registrert',
          created_date: latestRun?.runDate ?? '',
          law_unit: 'NARINGSMIDDELHYGIENE',
          items,
          completed_by: latestRun?.performedByUserId ? `Bruker ${latestRun.performedByUserId}` : null,
          completion_date: completionSplit.date || null,
          completion_time: completionSplit.time || null,
          status: latestRun ? checklistStatusFromRun(latestRun.status) : 'pending',
        } satisfies Checklist
      })

      const mappedRecentChecks = runs
        .slice()
        .sort((a, b) => new Date(b.completedAt ?? b.runDate ?? 0).getTime() - new Date(a.completedAt ?? a.runDate ?? 0).getTime())
        .slice(0, 8)
        .map((run) => {
          const split = splitIsoDateTime(run.completedAt ?? run.runDate)
          return {
            id: run.runId,
            name: run.templateTitle ?? `Sjekkliste ${run.templateId}`,
            completed_by: run.performedByUserId ? `Bruker ${run.performedByUserId}` : 'Ukjent',
            completed_date: split.date,
            completed_time: split.time,
            status: run.status === 'COMPLETED' ? 'completed' : run.status === 'OVERDUE' ? 'overdue' : 'pending',
          } satisfies RecentCheck
        })

      const mappedTemperature = entries
        .slice()
        .sort((a, b) => new Date(b.measuredAt).getTime() - new Date(a.measuredAt).getTime())
        .map((entry) => {
          const split = splitIsoDateTime(entry.measuredAt)
          const location = entry.locationId ? locationById.get(entry.locationId) : undefined
          return {
            id: entry.entryId,
            location: entry.locationName ?? location?.name ?? entry.logPointName ?? 'Ukjent lokasjon',
            temperature_c: Number(entry.temperatureC),
            min_temp: Number(location?.tempMinC ?? 0),
            max_temp: Number(location?.tempMaxC ?? 4),
            recorded_by: entry.recordedByName ?? 'Ukjent',
            recorded_date: split.date,
            recorded_time: split.time,
            status: entry.isAlert ? 'critical' : 'ok',
          } satisfies TemperatureRecord
        })

      const mappedDeviations = deviationReports
        .slice()
        .sort((a, b) => new Date(b.reportDate ?? 0).getTime() - new Date(a.reportDate ?? 0).getTime())
        .map((item) => {
          const reportSplit = splitIsoDateTime(item.reportDate)
          return {
            id: item.reportId,
            title: item.title,
            description: item.description,
            severity: deviationSeverity(item.severity),
            reported_by: item.reportedBy?.fullName ?? item.reportedBy?.email ?? 'Ukjent',
            reported_date: item.occurredDate ?? reportSplit.date,
            reported_time: item.occurredTime ?? reportSplit.time,
            location: item.locationText ?? 'Ukjent lokasjon',
            immediate_action: item.immediateActionText ?? 'Ingen umiddelbar handling registrert',
            corrective_action: item.correctiveActionText ?? 'Ingen korrigerende handling registrert',
            status: deviationStatus(item.status),
          } satisfies Deviation
        })

      const ccpPoints: HaccpPoint[] = mappedChecklists.slice(0, 8).map((checklist) => ({
        id: checklist.id,
        number: `CCP-${checklist.id}`,
        name: checklist.name,
        description: checklist.description,
        hazards: ['Biologisk fare', 'Temperaturavvik'],
        critical_limits: `${checklist.items.filter((item) => item.required).length} obligatoriske kontrollpunkter`,
        monitoring: checklist.frequency,
        corrective_actions: 'Registrer avvik og gjennomfør korrigerende tiltak',
        verification: 'Daglig gjennomgang av ansvarlig leder',
        responsible: checklist.completed_by ?? 'Driftsansvarlig',
      }))

      const supportingDocs: SupportingDocument[] = documents
        .slice()
        .sort((a, b) => new Date(b.updatedAt ?? 0).getTime() - new Date(a.updatedAt ?? 0).getTime())
        .slice(0, 6)
        .map((doc) => ({
          id: doc.documentId,
          name: doc.title,
          date_updated: doc.updatedAt ?? new Date().toISOString(),
          description: doc.description ?? 'Organisasjonsdokument',
        }))

      const alerts = mappedTemperature.filter((record) => record.status !== 'ok').length

      dashboardStats.splice(0, dashboardStats.length,
        {
          label: 'Sjekklister',
          value: mappedChecklists.length,
          trend: 'neutral',
          color: 'info',
        },
        {
          label: 'Temperaturavvik',
          value: alerts,
          trend: alerts > 0 ? 'up' : 'neutral',
          color: alerts > 0 ? 'warning' : 'success',
        },
        {
          label: 'Apne avvik',
          value: mappedDeviations.filter((item) => item.status !== 'resolved').length,
          trend: 'neutral',
          color: 'warning',
        },
      )

      recentChecks.splice(0, recentChecks.length, ...mappedRecentChecks)
      checklists.splice(0, checklists.length, ...mappedChecklists)
      temperatureRecords.splice(0, temperatureRecords.length, ...mappedTemperature)
      deviations.splice(0, deviations.length, ...mappedDeviations)
      haccpPlan.plan_name = `HACCP-plan org ${orgNumber}`
      haccpPlan.version = '2.0'
      haccpPlan.last_updated = new Date().toISOString()
      haccpPlan.critical_control_points = ccpPoints
      haccpPlan.supporting_documents = supportingDocs

      const failedCalls = [templatesResponse, runsResponse, entriesResponse, locationsResponse, deviationsResponse, documentsResponse]
        .filter((result) => result.status === 'rejected').length
      const succeededCalls = 6 - failedCalls

      if (succeededCalls === 0) {
        error.value = 'Alle IK-MAT endepunkter feilet. Kontroller innlogging og prov igjen.'
        return
      }

      // Keep rendering available data when one or more endpoint calls fail.
      error.value = null

      hasLoaded = true
    } catch {
      dashboardStats.splice(0, dashboardStats.length)
      recentChecks.splice(0, recentChecks.length)
      checklists.splice(0, checklists.length)
      temperatureRecords.splice(0, temperatureRecords.length)
      deviations.splice(0, deviations.length)
      error.value = 'Kunne ikke laste IK-MAT data fra API.'
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

const formatDate = (value: string | null): string => {
  if (!value) {
    return '-'
  }

  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return value
  }

  return parsed.toLocaleDateString('nb-NO')
}

const completionForChecklist = (checklist: Checklist): number => {
  if (checklist.items.length === 0) {
    return 0
  }

  const completed = checklist.items.filter((item) => item.completed).length
  return Math.round((completed / checklist.items.length) * 100)
}

const isTemperatureInRange = (record: TemperatureRecord): boolean => {
  return record.temperature_c >= record.min_temp && record.temperature_c <= record.max_temp
}

export const useIkMatData = () => {
  void loadData()

  return {
    dashboardStats,
    recentChecks,
    checklists,
    temperatureRecords,
    deviations,
    haccpPlan,
    formatDate,
    completionForChecklist,
    isTemperatureInRange,
    isLoading,
    error,
    reload,
  }
}
