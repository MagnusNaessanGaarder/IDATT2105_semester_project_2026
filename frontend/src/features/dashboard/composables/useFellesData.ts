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

type RawUser = {
  user_id: number
  display_name: string
}

type RawDocument = {
  document_id: number
  document_type: string
  title: string
  description: string | null
  current_version: number
  is_active: number
  created_by_user_id: number | null
}

type RawDocumentVersion = {
  document_id: number
  version_number: number
  azure_blob_name: string
  mime_type: string
  file_size_bytes: number
  uploaded_by_user_id: number | null
  uploaded_at: string
}

type RawExportJob = {
  export_job_id: number
  requested_by_user_id: number
  export_type: string
  status: 'pending' | 'running' | 'completed' | 'failed'
  parameters_json: Record<string, unknown> | null
  result_document_id: number | null
  requested_at: string
}

type RawNotification = {
  notification_id: number
  notification_type: string
  title: string
  body_text: string
  related_entity_type: string | null
  is_read: number
  created_at: string
}

const users = fellesData.app_user as RawUser[]
const rawDocuments = fellesData.organization_document as RawDocument[]
const rawVersions = fellesData.organization_document_version as RawDocumentVersion[]
const rawReports = fellesData.export_job as RawExportJob[]
const rawNotifications = fellesData.notification as RawNotification[]

const userNameById = new Map(users.map((user) => [user.user_id, user.display_name]))
const currentVersionByDocumentId = new Map(
  rawVersions.map((version) => [
    version.document_id,
    {
      version_number: version.version_number,
      azure_blob_name: version.azure_blob_name,
      mime_type: version.mime_type,
      file_size_bytes: version.file_size_bytes,
      uploaded_by_user_id: version.uploaded_by_user_id,
      uploaded_at: version.uploaded_at,
    },
  ]),
)

const asMbLabel = (bytes: number): string => `${(bytes / (1024 * 1024)).toFixed(1)} MB`

const docTypeLabel = (documentType: string): string => {
  const labels: Record<string, string> = {
    policy: 'Policy',
    procedure: 'Prosedyrer',
    training_material: 'Opplaering',
    certificate: 'Sertifikat',
    attachment: 'Vedlegg',
    report_export: 'Rapporter',
    other: 'Annet',
  }

  return labels[documentType] ?? 'Annet'
}

const mimeShort = (mimeType: string): string => {
  if (mimeType.includes('pdf')) return 'PDF'
  if (mimeType.includes('sheet') || mimeType.includes('excel')) return 'Excel'
  if (mimeType.includes('json')) return 'JSON'
  return 'Fil'
}

const documents = rawDocuments.map((document) => {
  const version = currentVersionByDocumentId.get(document.document_id)
  const uploaderName = version?.uploaded_by_user_id ? userNameById.get(version.uploaded_by_user_id) : undefined
  const creatorName = document.created_by_user_id ? userNameById.get(document.created_by_user_id) : undefined

  return {
    id: document.document_id,
    name: document.title,
    category: docTypeLabel(document.document_type),
    file_type: version ? mimeShort(version.mime_type) : 'Fil',
    uploaded_by: uploaderName ?? creatorName ?? 'Ukjent',
    uploaded_date: version?.uploaded_at ?? '1970-01-01T00:00:00',
    size: version ? asMbLabel(version.file_size_bytes) : '0.0 MB',
    version: String(document.current_version),
    status: document.is_active === 1 ? 'active' : 'archived',
    description: document.description ?? '-',
  } satisfies DocumentItem
})

const reportTypeLabel = (type: string): string => {
  const labels: Record<string, string> = {
    audit_report: 'audit',
    checklist_report: 'checklist',
    temperature_report: 'temperature',
    deviation_report: 'deviation',
    training_report: 'training',
    full_compliance_report: 'monthly',
  }

  return labels[type] ?? type
}

const reports = rawReports.map((report) => {
  const resultDocument = report.result_document_id
    ? documents.find((document) => document.id === report.result_document_id)
    : undefined

  const periodFrom = report.parameters_json?.period_from
  const periodTo = report.parameters_json?.period_to
  const period =
    typeof periodFrom === 'string' && typeof periodTo === 'string'
      ? `${periodFrom} til ${periodTo}`
      : 'Ikke spesifisert'

  return {
    id: report.export_job_id,
    title: `${report.export_type.replaceAll('_', ' ')} #${report.export_job_id}`,
    type: reportTypeLabel(report.export_type),
    created_by: userNameById.get(report.requested_by_user_id) ?? 'Ukjent',
    created_date: report.requested_at,
    period,
    status: report.status === 'completed' ? 'finalized' : 'draft',
    sections: [
      {
        name: 'Eksportdetaljer',
        content: `Status: ${report.status}`,
      },
    ],
    file_url: resultDocument ? `reports/${resultDocument.name}` : null,
    file_size: resultDocument?.size ?? null,
  } satisfies ReportItem
})

const notificationTypeToUi = (
  type: RawNotification['notification_type'],
): Pick<NotificationItem, 'type' | 'priority' | 'action_url' | 'action_label'> => {
  if (type === 'training_expiring') {
    return { type: 'warning', priority: 'high', action_url: 'admin/certifications', action_label: 'Se sertifikater' }
  }

  if (type === 'deviation_assigned') {
    return { type: 'warning', priority: 'high', action_url: 'ik-mat/deviations', action_label: 'Vis avvik' }
  }

  if (type === 'temperature_alert') {
    return { type: 'warning', priority: 'high', action_url: 'ik-mat/temperature', action_label: 'Vis temperaturer' }
  }

  if (type === 'document_uploaded') {
    return { type: 'info', priority: 'medium', action_url: 'felles/reports', action_label: 'Vis rapport' }
  }

  return { type: 'info', priority: 'low', action_url: null, action_label: null }
}

const notifications = rawNotifications.map((notification) => {
  const mapped = notificationTypeToUi(notification.notification_type)
  const created = new Date(notification.created_at)
  const createdDate = Number.isNaN(created.getTime())
    ? notification.created_at.slice(0, 10)
    : created.toISOString().slice(0, 10)
  const createdTime = Number.isNaN(created.getTime())
    ? notification.created_at.slice(11, 16)
    : created.toISOString().slice(11, 16)

  return {
    id: notification.notification_id,
    title: notification.title,
    message: notification.body_text,
    type: mapped.type,
    priority: mapped.priority,
    created_date: createdDate,
    created_time: createdTime,
    read: notification.is_read === 1,
    action_url: mapped.action_url,
    action_label: mapped.action_label,
  } satisfies NotificationItem
})

const dashboardStats: DashboardStat[] = [
  {
    label: 'Dokumenter',
    value: documents.length,
    trend: 'neutral',
    color: 'info',
  },
  {
    label: 'Åpne varsler',
    value: notifications.filter((item) => !item.read).length,
    trend: 'up',
    color: 'warning',
  },
  {
    label: 'Ferdige rapporter',
    value: reports.filter((item) => item.status === 'finalized').length,
    trend: 'up',
    color: 'success',
  },
  {
    label: 'Arkiverte dokumenter',
    value: documents.filter((item) => item.status === 'archived').length,
    trend: 'neutral',
    color: 'info',
  },
]

const quickActions: QuickAction[] = [
  {
    title: 'Opprett rapport',
    icon: 'FileText',
    route: 'Reports',
  },
  {
    title: 'Last opp dokument',
    icon: 'Upload',
    route: 'Documents',
  },
  {
    title: 'Se varsler',
    icon: 'AlertCircle',
    route: 'Notifications',
  },
]

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

const reportStatusLabel = (status: ReportItem['status']): string => {
  return status === 'finalized' ? 'Ferdig' : 'Pagar'
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