import { ref, type Ref } from 'vue'
import ikMatData from '@/data/ik-mat.json'
import type {
  DeviationApi,
  DeviationUpsertRequest,
  HaccpPlan,
  LocationApi,
  LocationUpsertRequest,
  OrganizationUserApi,
  TemperatureEntryApi,
  TemperatureEntryCreateRequest,
  TemperatureEntryUpdateRequest,
  TemperatureLogPointApi,
  TemperatureLogPointUpsertRequest,
} from '../types/index'

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
  log_point_id: number
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

export interface IkMatDataState {
  dashboardStats: DashboardStat[]
  recentChecks: RecentCheck[]
  checklists: Checklist[]
  temperatureRecords: TemperatureRecord[]
  temperaturePoints: TemperatureLogPointApi[]
  locations: LocationApi[]
  orgUsers: OrganizationUserApi[]
  deviations: Deviation[]
  haccpPlan: HaccpPlan
  isLoading: Ref<boolean>
  error: Ref<string | null>
  reload: () => Promise<void>
  formatDate: (value: string | null) => string
  completionForChecklist: (checklist: Checklist) => number
  isTemperatureInRange: (record: TemperatureRecord) => boolean
  createTemperaturePointWithLocation: (
    locationPayload: LocationUpsertRequest,
    pointPayload: Omit<TemperatureLogPointUpsertRequest, 'locationId'>
  ) => Promise<{ location: LocationApi; point: TemperatureLogPointApi }>
  updateTemperaturePointAndLocation: (
    pointId: number,
    locationId: number,
    locationPayload: LocationUpsertRequest,
    pointPayload: TemperatureLogPointUpsertRequest
  ) => Promise<{ location: LocationApi | null; point: TemperatureLogPointApi | null }>
  deleteTemperaturePoint: (pointId: number) => Promise<void>
  clearTemperatureMeasurementsForPoint: (pointId: number) => Promise<void>
  createTemperatureMeasurement: (payload: TemperatureEntryCreateRequest) => Promise<TemperatureEntryApi>
  updateTemperatureMeasurement: (entryId: number, payload: TemperatureEntryUpdateRequest) => Promise<TemperatureEntryApi>
  createDeviation: (payload: DeviationUpsertRequest) => Promise<DeviationApi>
  updateDeviation: (id: number, payload: DeviationUpsertRequest) => Promise<DeviationApi>
  deleteDeviation: (id: number) => Promise<void>
  resolveDeviation: (id: number) => Promise<DeviationApi>
  startDeviationHandling: (id: number) => Promise<DeviationApi>
  createHaccpControlPoint: (payload: Omit<HaccpPoint, 'id' | 'number'>) => Promise<HaccpPoint>
  updateHaccpControlPoint: (id: number, payload: Omit<HaccpPoint, 'id' | 'number'>) => Promise<HaccpPoint>
  deleteHaccpControlPoint: (id: number) => Promise<void>
  uploadSupportingDocument: (file: File, title: string, description: string) => Promise<SupportingDocument>
  updateSupportingDocument: (id: number, payload: { title: string; description: string }) => Promise<SupportingDocument>
  deleteSupportingDocument: (id: number) => Promise<void>
  downloadSupportingDocument: (id: number) => Promise<Blob>
}

const clone = <T>(value: T): T => JSON.parse(JSON.stringify(value)) as T

const dashboardStats = clone(ikMatData.dashboard.stats as DashboardStat[])
const recentChecks = clone(ikMatData.dashboard.recent_checks as RecentCheck[])
const checklists = ref(clone(ikMatData.checklists as Checklist[]))

const initialTemperatureRecords = (ikMatData.temperature as TemperatureRecord[]).map((record, index) => ({
  ...clone(record),
  log_point_id: index + 1,
}))

const temperatureRecords = ref(initialTemperatureRecords)
const temperaturePoints = ref<TemperatureLogPointApi[]>(initialTemperatureRecords.map((record, index) => ({
  logPointId: index + 1,
  name: record.location,
  locationId: index + 1,
  locationName: record.location,
  isActive: true,
})))

const locations = ref<LocationApi[]>(initialTemperatureRecords.map((record, index) => ({
  locationId: index + 1,
  name: record.location,
  locationType: 'OTHER',
  tempMinC: record.min_temp,
  tempMaxC: record.max_temp,
  isActive: true,
})))

const uniqueUsers = Array.from(
  new Set([
    ...initialTemperatureRecords.map((record) => record.recorded_by),
    ...(ikMatData.deviations as Deviation[]).map((deviation) => deviation.reported_by),
  ])
)

const orgUsers = ref<OrganizationUserApi[]>(uniqueUsers.map((name, index) => ({
  userId: index + 1,
  displayName: name,
  email: `${name.toLowerCase().replace(/[^a-z0-9]+/g, '.').replace(/^\.|\.$/g, '') || 'user'}@example.com`,
  isActive: true,
})))

const deviations = ref(clone(ikMatData.deviations as Deviation[]))
const haccpPlan = ref(clone(ikMatData.haccp as HaccpPlan))
const isLoading = ref(false)
const error = ref<string | null>(null)

const currentDateTime = () => {
  const now = new Date()
  return {
    date: now.toISOString().slice(0, 10),
    time: now.toISOString().slice(11, 16),
  }
}

const nextNumericId = (items: Array<{ id?: number; logPointId?: number; locationId?: number; runItemId?: number; userId?: number }>) => {
  return items.reduce((maxId, item) => {
    const itemId = item.id ?? item.logPointId ?? item.locationId ?? item.runItemId ?? item.userId ?? 0
    return Math.max(maxId, itemId)
  }, 0) + 1
}

const replaceArrayContents = <T>(target: T[], source: T[]) => {
  target.splice(0, target.length, ...source)
}

const resetState = () => {
  replaceArrayContents(checklists.value, clone(ikMatData.checklists as Checklist[]))
  replaceArrayContents(
    temperatureRecords.value,
    (ikMatData.temperature as TemperatureRecord[]).map((record, index) => ({
      ...clone(record),
      log_point_id: index + 1,
    }))
  )

  replaceArrayContents(
    temperaturePoints.value,
    temperatureRecords.value.map((record, index) => ({
      logPointId: index + 1,
      name: record.location,
      locationId: index + 1,
      locationName: record.location,
      isActive: true,
    }))
  )

  replaceArrayContents(
    locations.value,
    temperatureRecords.value.map((record, index) => ({
      locationId: index + 1,
      name: record.location,
      locationType: 'OTHER',
      tempMinC: record.min_temp,
      tempMaxC: record.max_temp,
      isActive: true,
    }))
  )

  replaceArrayContents(deviations.value, clone(ikMatData.deviations as Deviation[]))
  haccpPlan.value = clone(ikMatData.haccp as HaccpPlan)
  error.value = null
}

const findTemperaturePoint = (pointId: number) => temperaturePoints.value.find((point) => point.logPointId === pointId) ?? null

const findLocation = (locationId: number) => locations.value.find((location) => location.locationId === locationId) ?? null

const resolveTemperatureStatus = (temperature: number, minTemp: number, maxTemp: number): 'ok' | 'warning' | 'critical' => {
  if (temperature < minTemp - 2 || temperature > maxTemp + 2) {
    return 'critical'
  }

  if (temperature < minTemp || temperature > maxTemp) {
    return 'warning'
  }

  return 'ok'
}

const mapSeverity = (severity: DeviationUpsertRequest['severity']): Deviation['severity'] => {
  if (severity === 'CRITICAL') return 'high'
  if (severity === 'MAJOR') return 'medium'
  return 'low'
}

const mapDeviationStatus = (status: 'open' | 'resolved' | 'in-progress'): DeviationApi['status'] => {
  return status
}

const createDeviationApiShape = (deviation: Deviation): DeviationApi => ({
  reportId: deviation.id,
  title: deviation.title,
  description: deviation.description,
  severity: deviation.severity,
  reported_by: deviation.reported_by,
  reported_date: deviation.reported_date,
  reported_time: deviation.reported_time,
  location: deviation.location,
  immediate_action: deviation.immediate_action,
  corrective_action: deviation.corrective_action,
  status: deviation.status,
})

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

export const useIkMatData = () => ({
  dashboardStats,
  recentChecks,
  checklists: checklists.value,
  temperatureRecords: temperatureRecords.value,
  temperaturePoints: temperaturePoints.value,
  locations: locations.value,
  orgUsers: orgUsers.value,
  deviations: deviations.value,
  haccpPlan: haccpPlan.value,
  isLoading,
  error,
  formatDate,
  completionForChecklist,
  isTemperatureInRange,
  async reload() {
    isLoading.value = true
    error.value = null
    try {
      resetState()
    } finally {
      isLoading.value = false
    }
  },
  async createTemperaturePointWithLocation(locationPayload: LocationUpsertRequest, pointPayload: Omit<TemperatureLogPointUpsertRequest, 'locationId'>) {
    const locationId = nextNumericId(locations.value)
    const logPointId = nextNumericId(temperaturePoints.value)

    const location: LocationApi = {
      locationId,
      name: locationPayload.name,
      locationType: locationPayload.locationType,
      tempMinC: locationPayload.tempMinC,
      tempMaxC: locationPayload.tempMaxC,
      isActive: locationPayload.isActive,
    }

    const point: TemperatureLogPointApi = {
      logPointId,
      name: pointPayload.name,
      locationId,
      locationName: locationPayload.name,
      isActive: pointPayload.isActive ?? true,
    }

    locations.value.push(location)
    temperaturePoints.value.push(point)

    return { location, point }
  },
  async updateTemperaturePointAndLocation(
    pointId: number,
    locationId: number,
    locationPayload: LocationUpsertRequest,
    pointPayload: TemperatureLogPointUpsertRequest,
  ) {
    const location = findLocation(locationId)
    const point = findTemperaturePoint(pointId)

    if (location) {
      location.name = locationPayload.name
      location.locationType = locationPayload.locationType
      location.tempMinC = locationPayload.tempMinC
      location.tempMaxC = locationPayload.tempMaxC
      location.isActive = locationPayload.isActive
    }

    if (point) {
      point.name = pointPayload.name
      point.locationId = pointPayload.locationId
      point.locationName = locationPayload.name
      point.isActive = pointPayload.isActive ?? point.isActive ?? true
    }

    return { location, point }
  },
  async deleteTemperaturePoint(pointId: number) {
    const pointIndex = temperaturePoints.value.findIndex((point) => point.logPointId === pointId)
    if (pointIndex !== -1) {
      const [point] = temperaturePoints.value.splice(pointIndex, 1)
      if (point) {
        const locationIndex = locations.value.findIndex((location) => location.locationId === point.locationId)
        if (locationIndex !== -1) {
          locations.value.splice(locationIndex, 1)
        }
      }
    }
    replaceArrayContents(
      temperatureRecords.value,
      temperatureRecords.value.filter((record) => record.log_point_id !== pointId)
    )
  },
  async clearTemperatureMeasurementsForPoint(pointId: number) {
    replaceArrayContents(
      temperatureRecords.value,
      temperatureRecords.value.filter((record) => record.log_point_id !== pointId)
    )
  },
  async createTemperatureMeasurement(payload: TemperatureEntryCreateRequest) {
    const point = findTemperaturePoint(payload.logPointId)
    const location = point ? findLocation(point.locationId) : null
    const timestamp = new Date(payload.measuredAt)
    const recordedBy = payload.recordedByUserId
      ? (orgUsers.value.find((user) => user.userId === payload.recordedByUserId)?.displayName ?? String(payload.recordedByUserId))
      : (payload.noteText?.replace(/^Malt av:\s*/i, '') ?? '-')

    const entry: TemperatureRecord = {
      id: nextNumericId(temperatureRecords.value),
      log_point_id: payload.logPointId,
      location: point?.locationName ?? location?.name ?? 'Ukjent lokasjon',
      temperature_c: payload.temperatureC,
      min_temp: Number(location?.tempMinC ?? 0),
      max_temp: Number(location?.tempMaxC ?? 0),
      recorded_by: recordedBy,
      recorded_date: timestamp.toISOString().slice(0, 10),
      recorded_time: timestamp.toISOString().slice(11, 16),
      status: resolveTemperatureStatus(payload.temperatureC, Number(location?.tempMinC ?? 0), Number(location?.tempMaxC ?? 0)),
    }

    temperatureRecords.value.push(entry)

    return {
      id: entry.id,
      logPointId: entry.log_point_id ?? payload.logPointId,
      temperatureC: entry.temperature_c,
      measuredAt: timestamp.toISOString(),
      noteText: payload.noteText ?? null,
      recordedByUserId: payload.recordedByUserId ?? null,
      recordedBy: entry.recorded_by,
      recordedDate: entry.recorded_date,
      recordedTime: entry.recorded_time,
      status: entry.status,
    }
  },
  async updateTemperatureMeasurement(entryId: number, payload: TemperatureEntryUpdateRequest) {
    const entry = temperatureRecords.value.find((record) => record.id === entryId)
    const point = findTemperaturePoint(payload.logPointId ?? entry?.log_point_id ?? 0)
    const location = point ? findLocation(point.locationId) : null
    const timestamp = new Date(payload.measuredAt)
    const recordedBy = payload.recordedByUserId
      ? (orgUsers.value.find((user) => user.userId === payload.recordedByUserId)?.displayName ?? String(payload.recordedByUserId))
      : entry?.recorded_by ?? '-'

    if (entry) {
      entry.log_point_id = payload.logPointId ?? entry.log_point_id
      entry.location = point?.locationName ?? location?.name ?? entry.location
      entry.temperature_c = payload.temperatureC
      entry.min_temp = Number(location?.tempMinC ?? entry.min_temp)
      entry.max_temp = Number(location?.tempMaxC ?? entry.max_temp)
      entry.recorded_by = recordedBy
      entry.recorded_date = timestamp.toISOString().slice(0, 10)
      entry.recorded_time = timestamp.toISOString().slice(11, 16)
      entry.status = resolveTemperatureStatus(payload.temperatureC, Number(location?.tempMinC ?? entry.min_temp), Number(location?.tempMaxC ?? entry.max_temp))
    }

    return {
      id: entry?.id ?? entryId,
      logPointId: entry?.log_point_id ?? payload.logPointId ?? 0,
      temperatureC: payload.temperatureC,
      measuredAt: timestamp.toISOString(),
      noteText: payload.noteText ?? null,
      recordedByUserId: payload.recordedByUserId ?? null,
      recordedBy: entry?.recorded_by ?? recordedBy,
      recordedDate: entry?.recorded_date ?? timestamp.toISOString().slice(0, 10),
      recordedTime: entry?.recorded_time ?? timestamp.toISOString().slice(11, 16),
      status: entry?.status ?? resolveTemperatureStatus(payload.temperatureC, Number(location?.tempMinC ?? 0), Number(location?.tempMaxC ?? 0)),
    }
  },
  async createDeviation(payload: DeviationUpsertRequest) {
    const { date, time } = currentDateTime()
    const deviation: Deviation = {
      id: nextNumericId(deviations.value),
      title: payload.title,
      description: payload.description,
      severity: mapSeverity(payload.severity),
      reported_by: payload.discoveredByName ?? 'Ukjent',
      reported_date: payload.occurredDate ?? date,
      reported_time: payload.occurredTime ?? time,
      location: payload.locationText ?? '-',
      immediate_action: '',
      corrective_action: '',
      status: 'open',
    }

    deviations.value.unshift(deviation)

    return createDeviationApiShape(deviation)
  },
  async updateDeviation(id: number, payload: DeviationUpsertRequest) {
    const deviation = deviations.value.find((item) => item.id === id)

    if (deviation) {
      deviation.title = payload.title
      deviation.description = payload.description
      deviation.severity = mapSeverity(payload.severity)
      deviation.reported_by = payload.discoveredByName ?? deviation.reported_by
      deviation.reported_date = payload.occurredDate ?? deviation.reported_date
      deviation.reported_time = payload.occurredTime ?? deviation.reported_time
      deviation.location = payload.locationText ?? deviation.location
    }

    return createDeviationApiShape(deviation ?? {
      id,
      title: payload.title,
      description: payload.description,
      severity: mapSeverity(payload.severity),
      reported_by: payload.discoveredByName ?? 'Ukjent',
      reported_date: payload.occurredDate ?? currentDateTime().date,
      reported_time: payload.occurredTime ?? currentDateTime().time,
      location: payload.locationText ?? '-',
      immediate_action: '',
      corrective_action: '',
      status: 'open',
    })
  },
  async deleteDeviation(id: number) {
    const index = deviations.value.findIndex((item) => item.id === id)
    if (index !== -1) {
      deviations.value.splice(index, 1)
    }
  },
  async resolveDeviation(id: number) {
    const deviation = deviations.value.find((item) => item.id === id)
    if (deviation) {
      deviation.status = 'resolved'
    }
    return createDeviationApiShape(deviation ?? deviations.value[0] ?? {
      id,
      title: 'Avvik',
      description: '',
      severity: 'low',
      reported_by: '-',
      reported_date: currentDateTime().date,
      reported_time: currentDateTime().time,
      location: '-',
      immediate_action: '',
      corrective_action: '',
      status: 'resolved',
    })
  },
  async startDeviationHandling(id: number) {
    const deviation = deviations.value.find((item) => item.id === id)
    if (deviation) {
      deviation.status = 'in-progress'
    }
    return createDeviationApiShape(deviation ?? deviations.value[0] ?? {
      id,
      title: 'Avvik',
      description: '',
      severity: 'low',
      reported_by: '-',
      reported_date: currentDateTime().date,
      reported_time: currentDateTime().time,
      location: '-',
      immediate_action: '',
      corrective_action: '',
      status: 'in-progress',
    })
  },
  async createHaccpControlPoint(payload: Omit<HaccpPoint, 'id' | 'number'>) {
    const point: HaccpPoint = {
      id: nextNumericId(haccpPlan.value.critical_control_points),
      number: `CCP ${haccpPlan.value.critical_control_points.length + 1}`,
      ...payload,
    }

    haccpPlan.value.critical_control_points.push(point)
    return point
  },
  async updateHaccpControlPoint(id: number, payload: Omit<HaccpPoint, 'id' | 'number'>) {
    const point = haccpPlan.value.critical_control_points.find((item) => item.id === id)
    if (point) {
      point.name = payload.name
      point.description = payload.description
      point.hazards = payload.hazards
      point.critical_limits = payload.critical_limits
      point.monitoring = payload.monitoring
      point.corrective_actions = payload.corrective_actions
      point.verification = payload.verification
      point.responsible = payload.responsible
      return point
    }

    return {
      id,
      number: `CCP ${id}`,
      ...payload,
    }
  },
  async deleteHaccpControlPoint(id: number) {
    const index = haccpPlan.value.critical_control_points.findIndex((item) => item.id === id)
    if (index !== -1) {
      haccpPlan.value.critical_control_points.splice(index, 1)
    }
  },
  async uploadSupportingDocument(file: File, title: string, description: string) {
    const document: SupportingDocument = {
      id: nextNumericId(haccpPlan.value.supporting_documents),
      name: title,
      date_updated: currentDateTime().date,
      description: description || file.name,
    }

    haccpPlan.value.supporting_documents.push(document)
    return document
  },
  async updateSupportingDocument(id: number, payload: { title: string; description: string }) {
    const document = haccpPlan.value.supporting_documents.find((item) => item.id === id)
    if (document) {
      document.name = payload.title
      document.description = payload.description
      document.date_updated = currentDateTime().date
      return document
    }

    return {
      id,
      name: payload.title,
      date_updated: currentDateTime().date,
      description: payload.description,
    }
  },
  async deleteSupportingDocument(id: number) {
    const index = haccpPlan.value.supporting_documents.findIndex((item) => item.id === id)
    if (index !== -1) {
      haccpPlan.value.supporting_documents.splice(index, 1)
    }
  },
  async downloadSupportingDocument(id: number) {
    const document = haccpPlan.value.supporting_documents.find((item) => item.id === id)
    return new Blob([JSON.stringify(document ?? { id })], { type: 'application/json' })
  },
})
