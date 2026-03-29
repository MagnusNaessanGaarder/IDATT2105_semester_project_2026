import fellesData from '@/data/felles.json'

export interface DashboardStat {
  label: string
  value: number
  trend: 'up' | 'down' | 'neutral'
  color: 'success' | 'warning' | 'info'
  unit?: string
}

export interface QuickAction {
  title: string
  icon: string
  route: 'Reports' | 'Documents' | 'Notifications'
}

export interface ReportSection {
  name: string
  content: string
}

export interface ReportItem {
  id: number
  title: string
  type: string
  created_by: string
  created_date: string
  period: string
  status: 'draft' | 'finalized'
  sections: ReportSection[]
  file_url: string | null
  file_size: string | null
}

export interface DocumentItem {
  id: number
  name: string
  category: string
  file_type: string
  uploaded_by: string
  uploaded_date: string
  size: string
  version: string
  status: 'active' | 'archived'
  description: string
}

export interface NotificationItem {
  id: number
  title: string
  message: string
  type: 'warning' | 'info' | 'success' | 'error'
  priority: 'low' | 'medium' | 'high'
  created_date: string
  created_time: string
  read: boolean
  action_url: string | null
  action_label: string | null
}

const dashboardStats = fellesData.dashboard.stats as DashboardStat[]
const quickActions = fellesData.dashboard.quick_actions as QuickAction[]
const reports = fellesData.reports as ReportItem[]
const documents = fellesData.documents as DocumentItem[]
const notifications = fellesData.notifications as NotificationItem[]

const parseDateWithOptionalTime = (dateValue: string, timeValue?: string): number => {
  const timestamp = new Date(`${dateValue}${timeValue ? `T${timeValue}` : 'T00:00:00'}`).getTime()
  return Number.isNaN(timestamp) ? 0 : timestamp
}

const formatDate = (dateValue: string): string => {
  const date = new Date(dateValue)
  if (Number.isNaN(date.getTime())) {
    return dateValue
  }

  return date.toLocaleDateString('nb-NO', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

const reportTypeLabel = (type: string): string => {
  const labels: Record<string, string> = {
    monthly: 'Månedlig',
    deviation: 'Avvik',
    haccp: 'HACCP',
  }

  return labels[type] ?? type
}

const reportStatusLabel = (status: ReportItem['status']): string => {
  return status === 'finalized' ? 'Ferdig' : 'Pågår'
}

const reportStatusTone = (status: ReportItem['status']): 'green' | 'amber' => {
  return status === 'finalized' ? 'green' : 'amber'
}

const notificationTone = (type: NotificationItem['type']): 'red' | 'amber' | 'blue' | 'green' => {
  if (type === 'warning') return 'amber'
  if (type === 'error') return 'red'
  if (type === 'success') return 'green'
  return 'blue'
}

const sortedReports = [...reports].sort((a, b) => {
  return parseDateWithOptionalTime(b.created_date) - parseDateWithOptionalTime(a.created_date)
})

const sortedDocuments = [...documents].sort((a, b) => {
  return parseDateWithOptionalTime(b.uploaded_date) - parseDateWithOptionalTime(a.uploaded_date)
})

const sortedNotifications = [...notifications].sort((a, b) => {
  return parseDateWithOptionalTime(b.created_date, b.created_time) - parseDateWithOptionalTime(a.created_date, a.created_time)
})

export const useFellesData = () => ({
  dashboardStats,
  quickActions,
  reports,
  documents,
  notifications,
  sortedReports,
  sortedDocuments,
  sortedNotifications,
  formatDate,
  reportTypeLabel,
  reportStatusLabel,
  reportStatusTone,
  notificationTone,
})