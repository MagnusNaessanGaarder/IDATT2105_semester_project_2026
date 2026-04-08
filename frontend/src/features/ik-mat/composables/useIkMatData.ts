import { reactive, ref } from 'vue'
import { getOrgNumber } from '@/shared/utils/orgContext'
import { ikMatApi } from '../api/ikMatApi'
import { completionForChecklist, formatDate, isTemperatureInRange } from './useIkMatFormatters'
import type {
  Checklist,
  ChecklistItem,
  ChecklistRunApi,
  ChecklistTemplateApi,
  DashboardStat,
  Deviation,
  HaccpPlan,
  HaccpPoint,
  RecentCheck,
  SupportingDocument,
  TemperatureRecord,
} from '../types'

export type {
  Checklist,
  ChecklistItem,
  DashboardStat,
  Deviation,
  HaccpPlan,
  HaccpPoint,
  RecentCheck,
  SupportingDocument,
  TemperatureRecord,
} from '../types'

const dashboardStats = reactive<DashboardStat[]>([])
const recentChecks = reactive<RecentCheck[]>([])
const checklists = reactive<Checklist[]>([])
const temperatureRecords = reactive<TemperatureRecord[]>([])
const deviations = reactive<Deviation[]>([])
const haccpPlan = reactive<HaccpPlan>({
  plan_name: 'HACCP-plan',
  version: '1.0',
  last_updated: new Date().toISOString(),
  critical_control_points: [],
  supporting_documents: [],
})

let hasLoaded = false
let loadInFlight: Promise<void> | null = null
let lastLoadedOrgNumber: number | null = null
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

const hasResponse = (value: unknown): value is { response?: unknown } => {
  return typeof value === 'object' && value !== null && 'response' in value
}

const loadData = async (): Promise<void> => {
  const orgNumber = getOrgNumber()

  if (hasLoaded && lastLoadedOrgNumber === orgNumber) {
    return
  }

  if (loadInFlight) {
    return loadInFlight
  }

  loadInFlight = (async () => {
    isLoading.value = true
    error.value = null

    try {
      const [templatesResponse, runsResponse, entriesResponse, locationsResponse, deviationsResponse, documentsResponse] = await Promise.allSettled([
        ikMatApi.getChecklistTemplatesByModule('FOOD'),
        ikMatApi.getChecklistRuns(),
        ikMatApi.getTemperatureEntries(),
        ikMatApi.getLocations(),
        ikMatApi.getDeviations(),
        ikMatApi.getDocuments(),
      ])

      const templates = templatesResponse.status === 'fulfilled' ? templatesResponse.value : []
      const runs = runsResponse.status === 'fulfilled' ? runsResponse.value : []
      const entries = entriesResponse.status === 'fulfilled' ? entriesResponse.value : []
      const locations = locationsResponse.status === 'fulfilled' ? locationsResponse.value : []
      const deviationReports = deviationsResponse.status === 'fulfilled' ? deviationsResponse.value : []
      const documents = documentsResponse.status === 'fulfilled' ? documentsResponse.value : []

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

      error.value = null

      hasLoaded = true
      lastLoadedOrgNumber = orgNumber
    } catch (err: unknown) {
      dashboardStats.splice(0, dashboardStats.length)
      recentChecks.splice(0, recentChecks.length)
      checklists.splice(0, checklists.length)
      temperatureRecords.splice(0, temperatureRecords.length)
      deviations.splice(0, deviations.length)

      if (!hasResponse(err) || !err.response) {
        error.value = 'Backend er ikke tilgjengelig. Kontroller at API kjører på port 8080.'
      } else {
        error.value = 'Kunne ikke laste IK-MAT data fra API.'
      }
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
