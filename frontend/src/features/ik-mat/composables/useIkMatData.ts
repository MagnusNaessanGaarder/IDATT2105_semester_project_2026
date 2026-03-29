import ikMatData from '@/data/ik-mat.json'

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
  frequency: 'Daglig' | 'Ukentlig' | 'Maanedlig' | string
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

type RawUser = {
  user_id: number
  display_name: string
}

type RawLocation = {
  location_id: number
  name: string
  temp_min_c: number | null
  temp_max_c: number | null
}

type RawChecklistTemplate = {
  template_id: number
  title: string
  module_type: 'food' | 'alcohol'
  frequency: 'daily' | 'weekly' | 'monthly' | 'custom'
  description: string | null
  created_at: string
}

type RawChecklistTemplateItem = {
  item_id: number
  template_id: number
  label: string
  description: string | null
  is_required: number
}

type RawChecklistRun = {
  run_id: number
  template_id: number
  performed_by_user_id: number
  run_date: string
  completed_at: string | null
  status: 'draft' | 'in_progress' | 'completed' | 'overdue' | 'cancelled'
}

type RawChecklistRunItem = {
  run_item_id: number
  run_id: number
  template_item_id: number
  boolean_value: number | null
  text_value: string | null
  numeric_value: number | null
  comment_text: string | null
}

type RawTemperaturePoint = {
  log_point_id: number
  location_id: number
}

type RawTemperatureEntry = {
  entry_id: number
  log_point_id: number
  recorded_by_user_id: number
  measured_at: string
  temperature_c: number
  is_alert: number
}

type RawDeviation = {
  report_id: number
  title: string
  description: string
  severity: 'minor' | 'major' | 'critical'
  reported_by_user_id: number
  report_date: string
  occurred_time: string | null
  location_text: string | null
  immediate_action_text: string | null
  corrective_action_text: string | null
  status:
    | 'draft'
    | 'reported'
    | 'under_investigation'
    | 'corrective_action_planned'
    | 'corrective_action_completed'
    | 'closed'
}

const users = ikMatData.app_user as RawUser[]
const locations = ikMatData.location as RawLocation[]
const checklistTemplates = ikMatData.checklist_template as RawChecklistTemplate[]
const checklistTemplateItems = ikMatData.checklist_template_item as RawChecklistTemplateItem[]
const checklistRuns = ikMatData.checklist_run as RawChecklistRun[]
const checklistRunItems = ikMatData.checklist_run_item as RawChecklistRunItem[]
const temperaturePoints = ikMatData.temperature_log_point as RawTemperaturePoint[]
const temperatureEntries = ikMatData.temperature_log_entry as RawTemperatureEntry[]
const deviationReports = ikMatData.deviation_report as RawDeviation[]

const userNameById = new Map(users.map((user) => [user.user_id, user.display_name]))
const locationById = new Map(locations.map((location) => [location.location_id, location]))
const pointById = new Map(temperaturePoints.map((point) => [point.log_point_id, point]))

const frequencyLabel = (frequency: RawChecklistTemplate['frequency']): Checklist['frequency'] => {
  if (frequency === 'daily') return 'Daglig'
  if (frequency === 'weekly') return 'Ukentlig'
  if (frequency === 'monthly') return 'Maanedlig'
  return 'Tilpasset'
}

const checklistStatus = (status: RawChecklistRun['status']): Checklist['status'] => {
  if (status === 'completed') return 'completed'
  if (status === 'overdue') return 'overdue'
  return 'pending'
}

const checklistCategory = (title: string): string => {
  const lower = title.toLowerCase()
  if (lower.includes('haccp')) return 'HACCP'
  if (lower.includes('hygiene')) return 'Hygiene'
  if (lower.includes('temperatur') || lower.includes('kjoleskap')) return 'Temperatur'
  return 'Internkontroll'
}

const latestRunByTemplateId = new Map<number, RawChecklistRun>()
checklistRuns.forEach((run) => {
  const current = latestRunByTemplateId.get(run.template_id)
  if (!current) {
    latestRunByTemplateId.set(run.template_id, run)
    return
  }

  if (new Date(run.run_date).getTime() > new Date(current.run_date).getTime()) {
    latestRunByTemplateId.set(run.template_id, run)
  }
})

const checklists = checklistTemplates.map((template) => {
  const latestRun = latestRunByTemplateId.get(template.template_id)
  const templateItems = checklistTemplateItems
    .filter((item) => item.template_id === template.template_id)
    .map((item) => {
      const runItem = latestRun
        ? checklistRunItems.find((candidate) => candidate.run_id === latestRun.run_id && candidate.template_item_id === item.item_id)
        : undefined

      const completed =
        (runItem?.boolean_value ?? null) === 1 ||
        runItem?.text_value !== null ||
        runItem?.numeric_value !== null

      return {
        id: item.item_id,
        task: item.label,
        required: item.is_required === 1,
        completed,
        notes: runItem?.comment_text ?? runItem?.text_value ?? null,
      } satisfies ChecklistItem
    })

  const completedAt = latestRun?.completed_at
  const completionDate = completedAt ? completedAt.slice(0, 10) : null
  const completionTime = completedAt ? completedAt.slice(11, 16) : null

  return {
    id: template.template_id,
    name: template.title,
    category: checklistCategory(template.title),
    frequency: frequencyLabel(template.frequency),
    description: template.description ?? '-',
    created_date: template.created_at,
    law_unit: 'Internkontroll',
    items: templateItems,
    completed_by: latestRun ? (userNameById.get(latestRun.performed_by_user_id) ?? 'Ukjent') : null,
    completion_date: completionDate,
    completion_time: completionTime,
    status: latestRun ? checklistStatus(latestRun.status) : 'pending',
  } satisfies Checklist
})

const recentChecks = checklistRuns
  .filter((run) => run.completed_at)
  .sort((a, b) => (a.completed_at && b.completed_at ? new Date(b.completed_at).getTime() - new Date(a.completed_at).getTime() : 0))
  .slice(0, 5)
  .map((run) => {
    const template = checklistTemplates.find((item) => item.template_id === run.template_id)
    const completedAt = run.completed_at ?? ''

    return {
      id: run.run_id,
      name: template?.title ?? 'Sjekkliste',
      completed_by: userNameById.get(run.performed_by_user_id) ?? 'Ukjent',
      completed_date: completedAt.slice(0, 10),
      completed_time: completedAt.slice(11, 16),
      status: 'completed',
    } satisfies RecentCheck
  })

const temperatureRecords = temperatureEntries.map((entry) => {
  const point = pointById.get(entry.log_point_id)
  const location = point ? locationById.get(point.location_id) : undefined
  const minTemp = location?.temp_min_c ?? -999
  const maxTemp = location?.temp_max_c ?? 999

  let status: TemperatureRecord['status'] = 'ok'
  if (entry.is_alert === 1) {
    status = 'warning'
  }

  if (entry.temperature_c < minTemp || entry.temperature_c > maxTemp) {
    status = 'critical'
  }

  return {
    id: entry.entry_id,
    location: location?.name ?? 'Ukjent lokasjon',
    temperature_c: entry.temperature_c,
    min_temp: minTemp,
    max_temp: maxTemp,
    recorded_by: userNameById.get(entry.recorded_by_user_id) ?? 'Ukjent',
    recorded_date: entry.measured_at.slice(0, 10),
    recorded_time: entry.measured_at.slice(11, 16),
    status,
  } satisfies TemperatureRecord
})

const toSeverity = (severity: RawDeviation['severity']): Deviation['severity'] => {
  if (severity === 'minor') return 'low'
  if (severity === 'major') return 'medium'
  return 'high'
}

const toDeviationStatus = (status: RawDeviation['status']): Deviation['status'] => {
  if (status === 'closed') return 'resolved'
  if (status === 'under_investigation' || status === 'corrective_action_planned') return 'in-progress'
  return 'open'
}

const deviations = deviationReports.map((report) => ({
  id: report.report_id,
  title: report.title,
  description: report.description,
  severity: toSeverity(report.severity),
  reported_by: userNameById.get(report.reported_by_user_id) ?? 'Ukjent',
  reported_date: report.report_date,
  reported_time: report.occurred_time?.slice(0, 5) ?? '00:00',
  location: report.location_text ?? 'Ukjent',
  immediate_action: report.immediate_action_text ?? '-',
  corrective_action: report.corrective_action_text ?? '-',
  status: toDeviationStatus(report.status),
}))

const haccpTemplate = checklistTemplates.find((template) => template.title.toLowerCase().includes('haccp'))
const haccpTemplateItems = haccpTemplate
  ? checklistTemplateItems.filter((item) => item.template_id === haccpTemplate.template_id)
  : []

const criticalControlPoints: HaccpPoint[] = haccpTemplateItems.map((item, index) => ({
  id: item.item_id,
  number: `CCP ${index + 1}`,
  name: item.label,
  description: item.description ?? 'Kontrollpunkt i internkontrollsystemet',
  hazards: ['Mikrobiologisk'],
  critical_limits: 'I henhold til rutine',
  monitoring: 'Visuell kontroll og registrering',
  corrective_actions: 'Registrer avvik og iverksett tiltak',
  verification: 'Ukentlig gjennomgang',
  responsible: 'Kjokkenansvarlig',
}))

const supportingDocuments: SupportingDocument[] = [
  {
    id: 1,
    name: 'Leverandormatrise',
    date_updated: '2024-05-01',
    description: 'Oversikt over godkjente leverandorer',
  },
  {
    id: 2,
    name: 'Rengjoringsprogram',
    date_updated: '2024-04-15',
    description: 'Detaljert rengjoringsinstruks for hele kjokkenet',
  },
  {
    id: 3,
    name: 'Allergen-prosedyrer',
    date_updated: '2024-03-20',
    description: 'Handtering av allergener i kjokkenet',
  },
]

const haccpPlan = {
  plan_name: 'IK-Mat HACCP Plan for Everest Sushi & Fusion',
  created_date: '2024-01-01',
  last_updated: '2024-05-15',
  version: '2.1',
  critical_control_points: criticalControlPoints,
  supporting_documents: supportingDocuments,
}

const dashboardStats: DashboardStat[] = [
  {
    label: 'Aktive sjekklister',
    value: checklistTemplates.length,
    trend: 'up',
    color: 'success',
  },
  {
    label: 'Avvik i dag',
    value: deviations.filter((item) => item.status !== 'resolved').length,
    trend: 'down',
    color: 'warning',
  },
  {
    label: 'Samsvar',
    value:
      checklists.length === 0
        ? 0
        : Math.round(
            checklists.reduce((sum, checklist) => {
              const completed = checklist.items.filter((item) => item.completed).length
              const total = checklist.items.length === 0 ? 1 : checklist.items.length
              return sum + (completed / total) * 100
            }, 0) / checklists.length,
          ),
    unit: '%',
    trend: 'neutral',
    color: 'info',
  },
  {
    label: 'Kritiske temp.',
    value: temperatureRecords.filter((record) => record.status === 'critical').length,
    trend: 'neutral',
    color: 'success',
  },
]

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
  checklists,
  temperatureRecords,
  deviations,
  haccpPlan,
  formatDate,
  completionForChecklist,
  isTemperatureInRange,
})