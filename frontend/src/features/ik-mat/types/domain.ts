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
  // Tracking fields for persistence
  run_id: number | null
  run_date: string | null
}

export interface TemperatureRecord {
  id: number
  log_point_id: number
  log_point_name: string
  location_id: number | null
  location: string
  temperature_c: number
  min_temp: number
  max_temp: number
  recorded_by: string
  note_text: string | null
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
  assigned_to_user_id: number | null
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

export interface HaccpPlan {
  plan_name: string
  version: string
  last_updated: string
  critical_control_points: HaccpPoint[]
  supporting_documents: SupportingDocument[]
}
