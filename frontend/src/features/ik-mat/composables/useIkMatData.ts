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

const dashboardStats = ikMatData.dashboard.stats as DashboardStat[]
const recentChecks = ikMatData.dashboard.recent_checks as RecentCheck[]
const checklists = ikMatData.checklists as Checklist[]
const temperatureRecords = ikMatData.temperature as TemperatureRecord[]
const deviations = ikMatData.deviations as Deviation[]
const haccpPlan = ikMatData.haccp

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
