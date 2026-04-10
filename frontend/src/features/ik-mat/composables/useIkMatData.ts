import { reactive, ref } from 'vue'
import { getOrgNumber } from '@/shared/utils/orgContext'
import { getOrganizationTempDefaults } from '@/shared/utils/orgSettings'
import { ikMatApi, resetOptionalEndpointFlags } from '../api/ikMatApi'
import { completionForChecklist, formatDate, isTemperatureInRange } from './useIkMatFormatters'
import type {
  Checklist,
  ChecklistItem,
  DeviationApi,
  DeviationUpsertRequest,
  ChecklistRunApi,
  ChecklistTemplateApi,
  DashboardStat,
  Deviation,
  HaccpPlan,
  HaccpPoint,
  LocationApi,
  RecentCheck,
  SupportingDocument,
  OrganizationUserApi,
  TemperatureEntryApi,
  TemperatureEntryCreateRequest,
  TemperatureEntryUpdateRequest,
  TemperatureLogPointApi,
  TemperatureLogPointUpsertRequest,
  TemperatureRecord,
  LocationUpsertRequest,
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
const temperaturePoints = reactive<TemperatureLogPointApi[]>([])
const locations = reactive<LocationApi[]>([])
const orgUsers = reactive<OrganizationUserApi[]>([])
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

const reporterFromNote = (noteText: string | null | undefined): string | null => {
  if (!noteText) {
    return null
  }

  const prefix = 'Malt av:'
  if (!noteText.startsWith(prefix)) {
    return null
  }

  const reporter = noteText.slice(prefix.length).trim()
  return reporter.length > 0 ? reporter : null
}

const HACCP_META_PREFIX = '[HACCP_META]'

const parseHaccpMeta = (description: string | null | undefined): Partial<HaccpPoint> | null => {
  if (!description || !description.startsWith(HACCP_META_PREFIX)) {
    return null
  }

  try {
    const raw = description.slice(HACCP_META_PREFIX.length)
    return JSON.parse(raw) as Partial<HaccpPoint>
  } catch {
    return null
  }
}

const serializeHaccpMeta = (point: {
  name: string
  description: string
  hazards: string[]
  critical_limits: string
  monitoring: string
  corrective_actions: string
  responsible: string
  verification: string
}): string => {
  return `${HACCP_META_PREFIX}${JSON.stringify(point)}`
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
      const [templatesResponse, runsResponse, entriesResponse, pointsResponse, locationsResponse, deviationsResponse, documentsResponse] = await Promise.allSettled([
        ikMatApi.getChecklistTemplatesByModule('FOOD'),
        ikMatApi.getChecklistRuns(),
        ikMatApi.getTemperatureEntries(),
        ikMatApi.getTemperaturePoints(),
        ikMatApi.getLocations(),
        ikMatApi.getDeviations(),
        ikMatApi.getDocuments(),
      ])

      const templates = templatesResponse.status === 'fulfilled' ? templatesResponse.value : []
      const runs = runsResponse.status === 'fulfilled' ? runsResponse.value : []
      const entries = entriesResponse.status === 'fulfilled' ? entriesResponse.value : []
      const temperatureLogPoints = pointsResponse.status === 'fulfilled' ? pointsResponse.value : []
      const locationList = locationsResponse.status === 'fulfilled' ? locationsResponse.value : []
      const deviationReports = deviationsResponse.status === 'fulfilled' ? deviationsResponse.value : []
      const documents = documentsResponse.status === 'fulfilled' ? documentsResponse.value : []

      const locationById = new Map(locationList.map((location) => [location.locationId, location]))
      const pointById = new Map(temperatureLogPoints.map((point) => [point.logPointId, point]))
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
            runItemId: answer?.runItemId ?? null,
            task: item.label,
            required: Boolean(item.isRequired),
            completed: answer?.hasAnswer === true && answer?.booleanValue === true,
            isDeviation: Boolean(answer?.isDeviation),
            notes: answer?.commentText ?? item.description ?? null,
          }
        })

        const completionDate = latestRun?.completedAt ?? latestRun?.runDate ?? null
        const completionSplit = splitIsoDateTime(completionDate)
        const dueSplit = splitIsoDateTime(latestRun?.dueAt ?? null)
        const location = latestRun?.locationId ? locationById.get(latestRun.locationId)?.name ?? null : null

        return {
          id: template.templateId,
          runId: latestRun?.runId ?? null,
          name: template.title,
          category: 'IK-MAT',
          frequency: frequencyLabel(template.frequency),
          description: template.description ?? 'Ingen beskrivelse registrert',
          location,
          assignedTo: latestRun?.assignedToUserId ? `Bruker ${latestRun.assignedToUserId}` : null,
          items,
          completed_by: latestRun?.performedByUserId ? `Bruker ${latestRun.performedByUserId}` : null,
          completion_date: completionSplit.date || null,
          completion_time: completionSplit.time || null,
          due_date: dueSplit.date || null,
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
          const point = pointById.get(entry.logPointId)
          const location = entry.locationId ? locationById.get(entry.locationId) : point?.locationId ? locationById.get(point.locationId) : undefined
          const tempDefaults = getOrganizationTempDefaults(orgNumber)
          const reporterName = reporterFromNote(entry.noteText) ?? entry.recordedByName
          return {
            id: entry.entryId,
            log_point_id: entry.logPointId,
            log_point_name: entry.logPointName ?? point?.name ?? 'Ukjent malepunkt',
            location_id: entry.locationId ?? point?.locationId ?? null,
            location: entry.locationName ?? point?.locationName ?? location?.name ?? 'Ukjent lokasjon',
            temperature_c: Number(entry.temperatureC),
            min_temp: Number(location?.tempMinC ?? tempDefaults.min ?? 0),
            max_temp: Number(location?.tempMaxC ?? tempDefaults.max ?? 4),
            recorded_by: reporterName ?? 'Ukjent',
            note_text: entry.noteText ?? null,
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
            assigned_to_user_id: item.assignedToUserId ?? null,
          } satisfies Deviation
        })

      const ccpPoints: HaccpPoint[] = templates.map((template) => {
        const meta = parseHaccpMeta(template.description)
        return {
          id: template.templateId,
          number: `CCP-${template.templateId}`,
          name: meta?.name ?? template.title,
          description: meta?.description ?? (template.description ?? 'Ingen beskrivelse registrert'),
          hazards: meta?.hazards ?? ['Biologisk fare', 'Temperaturavvik'],
          critical_limits: meta?.critical_limits ?? `${(template.items ?? []).filter((item) => item.isRequired).length} obligatoriske kontrollpunkter`,
          monitoring: meta?.monitoring ?? frequencyLabel(template.frequency),
          corrective_actions: meta?.corrective_actions ?? 'Registrer avvik og gjennomfør korrigerende tiltak',
          verification: meta?.verification ?? 'Daglig gjennomgang av ansvarlig leder',
          responsible: meta?.responsible ?? 'Driftsansvarlig',
        }
      }).slice(0, 12)

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
      temperaturePoints.splice(0, temperaturePoints.length, ...temperatureLogPoints)
      locations.splice(0, locations.length, ...locationList)
      orgUsers.splice(0, orgUsers.length)
      deviations.splice(0, deviations.length, ...mappedDeviations)
      haccpPlan.plan_name = `HACCP-plan org ${orgNumber}`
      haccpPlan.version = '2.0'
      haccpPlan.last_updated = new Date().toISOString()
      haccpPlan.critical_control_points = ccpPoints
      haccpPlan.supporting_documents = supportingDocs

      const failedCalls = [templatesResponse, runsResponse, entriesResponse, pointsResponse, locationsResponse, deviationsResponse, documentsResponse]
        .filter((result) => result.status === 'rejected').length
      const succeededCalls = 7 - failedCalls

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
      temperaturePoints.splice(0, temperaturePoints.length)
      locations.splice(0, locations.length)
      orgUsers.splice(0, orgUsers.length)
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
  // Reset suppressed-endpoint flags so a recovered backend is retried
  resetOptionalEndpointFlags()
  await loadData()
}

const createTemperaturePoint = async (payload: TemperatureLogPointUpsertRequest): Promise<TemperatureLogPointApi> => {
  const created = await ikMatApi.createTemperaturePoint(payload)
  await reload()
  return created
}

const createTemperaturePointWithLocation = async (
  locationPayload: LocationUpsertRequest,
  pointPayload: Omit<TemperatureLogPointUpsertRequest, 'locationId'>,
): Promise<TemperatureLogPointApi> => {
  const location = await ikMatApi.createLocation(locationPayload)
  const point = await ikMatApi.createTemperaturePoint({
    ...pointPayload,
    locationId: location.locationId,
  })
  await reload()
  return point
}

const updateTemperaturePoint = async (pointId: number, payload: TemperatureLogPointUpsertRequest): Promise<TemperatureLogPointApi> => {
  const updated = await ikMatApi.updateTemperaturePoint(pointId, payload)
  await reload()
  return updated
}

const updateTemperaturePointAndLocation = async (
  pointId: number,
  locationId: number,
  locationPayload: LocationUpsertRequest,
  pointPayload: TemperatureLogPointUpsertRequest,
): Promise<void> => {
  await ikMatApi.updateLocation(locationId, locationPayload)
  await ikMatApi.updateTemperaturePoint(pointId, pointPayload)
  await reload()
}

const deleteTemperaturePoint = async (pointId: number): Promise<void> => {
  await ikMatApi.deleteTemperaturePoint(pointId)
  await reload()
}

const clearTemperatureMeasurementsForPoint = async (pointId: number): Promise<void> => {
  await ikMatApi.clearTemperatureEntriesForPoint(pointId)
  await reload()
}

const createTemperatureMeasurement = async (payload: TemperatureEntryCreateRequest): Promise<TemperatureEntryApi> => {
  const created = await ikMatApi.createTemperatureEntry(payload)
  await reload()
  return created
}

const updateTemperatureMeasurement = async (entryId: number, payload: TemperatureEntryUpdateRequest): Promise<TemperatureEntryApi> => {
  const updated = await ikMatApi.updateTemperatureEntry(entryId, payload)
  await reload()
  return updated
}

const createDeviation = async (payload: DeviationUpsertRequest): Promise<DeviationApi> => {
  const created = await ikMatApi.createDeviation(payload)
  await reload()
  return created
}

const updateDeviation = async (id: number, payload: DeviationUpsertRequest): Promise<DeviationApi> => {
  const updated = await ikMatApi.updateDeviation(id, payload)
  await reload()
  return updated
}

const deleteDeviation = async (id: number): Promise<void> => {
  await ikMatApi.deleteDeviation(id)
  await reload()
}

const resolveDeviation = async (id: number): Promise<void> => {
  await ikMatApi.updateDeviationStatus(id, { status: 'CLOSED' })
  await reload()
}

const startDeviationHandling = async (id: number): Promise<void> => {
  await ikMatApi.updateDeviationStatus(id, { status: 'UNDER_INVESTIGATION' })
  await reload()
}

const createHaccpControlPoint = async (point: {
  name: string
  description: string
  hazards: string[]
  critical_limits: string
  monitoring: string
  corrective_actions: string
  responsible: string
  verification: string
}): Promise<void> => {
  await ikMatApi.createChecklistTemplate({
    title: point.name,
    description: serializeHaccpMeta(point),
    moduleType: 'FOOD',
    frequency: 'DAILY',
    items: [
      {
        label: point.critical_limits,
        itemType: 'TEXT',
        isRequired: true,
      },
    ],
  })
  await reload()
}

const updateHaccpControlPoint = async (templateId: number, point: {
  name: string
  description: string
  hazards: string[]
  critical_limits: string
  monitoring: string
  corrective_actions: string
  responsible: string
  verification: string
}): Promise<void> => {
  await ikMatApi.updateChecklistTemplate(templateId, {
    title: point.name,
    description: serializeHaccpMeta(point),
    moduleType: 'FOOD',
    frequency: 'DAILY',
    items: [
      {
        label: point.critical_limits,
        itemType: 'TEXT',
        isRequired: true,
      },
    ],
  })
  await reload()
}

const deleteHaccpControlPoint = async (templateId: number): Promise<void> => {
  await ikMatApi.deleteChecklistTemplate(templateId)
  await reload()
}

const uploadSupportingDocument = async (file: File, title?: string, description?: string): Promise<void> => {
  const uploaded = await ikMatApi.uploadDocument(file, 'procedure', 'haccp') as { documentId?: number }
  if (uploaded?.documentId && (title || description)) {
    await ikMatApi.updateDocument(uploaded.documentId, {
      title: title?.trim() || file.name,
      description: description?.trim() || undefined,
    })
  }
  await reload()
}

const updateSupportingDocument = async (documentId: number, payload: { title?: string; description?: string }): Promise<void> => {
  await ikMatApi.updateDocument(documentId, payload)
  await reload()
}

const deleteSupportingDocument = async (documentId: number): Promise<void> => {
  await ikMatApi.deleteDocument(documentId)
  await reload()
}

const downloadSupportingDocument = async (documentId: number): Promise<Blob> => {
  return ikMatApi.downloadDocument(documentId)
}

export const useIkMatData = () => {
  void loadData()

  return {
    dashboardStats,
    recentChecks,
    checklists,
    temperatureRecords,
    temperaturePoints,
    locations,
    orgUsers,
    deviations,
    haccpPlan,
    formatDate,
    completionForChecklist,
    isTemperatureInRange,
    isLoading,
    error,
    reload,
    createTemperaturePoint,
    createTemperaturePointWithLocation,
    updateTemperaturePoint,
    updateTemperaturePointAndLocation,
    deleteTemperaturePoint,
    clearTemperatureMeasurementsForPoint,
    createTemperatureMeasurement,
    updateTemperatureMeasurement,
    createDeviation,
    updateDeviation,
    deleteDeviation,
    resolveDeviation,
    startDeviationHandling,
    createHaccpControlPoint,
    updateHaccpControlPoint,
    deleteHaccpControlPoint,
    uploadSupportingDocument,
    updateSupportingDocument,
    deleteSupportingDocument,
    downloadSupportingDocument,
  }
}
