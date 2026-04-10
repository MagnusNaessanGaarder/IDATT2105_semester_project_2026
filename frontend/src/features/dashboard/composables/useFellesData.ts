import { reactive, ref } from 'vue'
import { client } from '@/api/client'
import { getOrgNumber, orgHeaders, withOrgNumber } from '@/shared/utils/orgContext'
import { formatDateForOrganization } from '@/shared/utils/orgSettings'

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

interface FileApi {
  documentId: number
  documentType: string
  title: string
  description: string | null
  currentVersion: number
  active: boolean
  createdAt: string | null
  updatedAt: string | null
  createdByUserId: number | null
}

interface ExportPageApi {
  content: ExportApi[]
}

interface ExportApi {
  exportJobId: number
  exportType: string
  status: string
  downloadUrl: string | null
  fileName: string | null
  requestedAt: string | null
  completedAt: string | null
}

interface DeviationApi {
  reportId: number
  title: string
  severity: string
  status: string
  description: string
  reportDate: string | null
}

interface NotificationApi {
  notificationId: number
  notificationType: string
  title: string
  bodyText: string
  relatedEntityType: string | null
  relatedEntityId: number | null
  isRead: boolean
  createdAt: string
}

const dashboardStats = reactive<DashboardStat[]>([])
const quickActions = reactive<QuickAction[]>([
  { title: 'Rapporter', icon: 'report', route: 'Reports' },
  { title: 'Dokumenter', icon: 'document', route: 'Documents' },
  { title: 'Varsler', icon: 'notification', route: 'Notifications' },
])
const reports = reactive<ReportItem[]>([])
const documents = reactive<DocumentItem[]>([])
const notifications = reactive<NotificationItem[]>([])
let deviationsEndpointUnavailable = false

let hasLoaded = false
let loadInFlight: Promise<void> | null = null
let lastLoadedOrgNumber: number | null = null
const isLoading = ref(false)
const error = ref<string | null>(null)

const parseDateWithOptionalTime = (dateValue: string, timeValue?: string): number => {
  const timestamp = new Date(`${dateValue}${timeValue ? `T${timeValue}` : 'T00:00:00'}`).getTime()
  return Number.isNaN(timestamp) ? 0 : timestamp
}

const formatDate = (dateValue: string): string => {
  const date = new Date(dateValue)
  if (Number.isNaN(date.getTime())) {
    return dateValue
  }

  return formatDateForOrganization(date, getOrgNumber(), {
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

const mapDeviationStatus = (status: string): ReportItem['status'] => {
  return status === 'CLOSED' ? 'finalized' : 'draft'
}

const mapNotificationPriority = (type: string): NotificationItem['priority'] => {
  if (type === 'TEMPERATURE_ALERT') return 'high'
  if (type === 'DEVIATION_STATUS_CHANGED') return 'medium'
  return 'low'
}

const mapNotificationType = (type: string): NotificationItem['type'] => {
  if (type === 'TEMPERATURE_ALERT') return 'warning'
  if (type === 'DEVIATION_STATUS_CHANGED') return 'info'
  return 'info'
}

const mapNotificationActionUrl = (entityType: string | null): string | null => {
  if (entityType === 'TEMPERATURE_LOG_ENTRY') return '/ik-mat/temperature'
  if (entityType === 'DEVIATION_REPORT') return '/ik-mat/deviations'
  return null
}

const splitIsoDate = (iso: string | null): { date: string; time: string } => {
  if (!iso) {
    return { date: '', time: '' }
  }

  const parsed = new Date(iso)
  if (Number.isNaN(parsed.getTime())) {
    return { date: iso.slice(0, 10), time: iso.slice(11, 16) }
  }

  const toTwo = (value: number) => String(value).padStart(2, '0')
  return {
    date: `${parsed.getFullYear()}-${toTwo(parsed.getMonth() + 1)}-${toTwo(parsed.getDate())}`,
    time: `${toTwo(parsed.getHours())}:${toTwo(parsed.getMinutes())}`,
  }
}

const hasResponse = (value: unknown): value is { response?: unknown } => {
  return typeof value === 'object' && value !== null && 'response' in value
}

const loadData = async (): Promise<void> => {
  const orgNumber = getOrgNumber()

  if (hasLoaded && lastLoadedOrgNumber === orgNumber && !error.value) {
    return
  }

  if (loadInFlight) {
    return loadInFlight
  }

  loadInFlight = (async () => {
    isLoading.value = true
    error.value = null

    try {
      const [filesResponse, exportsResponse, deviationsResponse, notificationsResponse] = await Promise.allSettled([
        client.get<FileApi[]>('/files', {
          params: withOrgNumber({}),
          headers: orgHeaders(),
        }),
        client.get<ExportPageApi>('/exports', {
          params: withOrgNumber({ page: 0, size: 50 }),
        }),
        deviationsEndpointUnavailable
          ? Promise.resolve({ data: [] as DeviationApi[] })
          : client.get<DeviationApi[]>('/deviations', {
            params: withOrgNumber({}),
            skipGlobalErrorLog: true,
          }).catch((err: unknown) => {
            if (hasResponse(err)) {
              const response = err.response as { status?: number } | undefined
              if (response?.status === 500) {
                deviationsEndpointUnavailable = true
                return { data: [] as DeviationApi[] }
              }
            }
            throw err
          }),
        client.get<NotificationApi[]>('/notifications', {
          params: withOrgNumber({}),
        }),
      ])

      const files = (filesResponse.status === 'fulfilled' ? filesResponse.value.data : []) as FileApi[]
      const exports = (exportsResponse.status === 'fulfilled' ? exportsResponse.value.data.content : []) as ExportApi[]
      const deviations = (deviationsResponse.status === 'fulfilled' ? deviationsResponse.value.data : []) as DeviationApi[]
      const storedNotifications = (notificationsResponse.status === 'fulfilled' ? notificationsResponse.value.data : []) as NotificationApi[]

      const mappedDocuments: DocumentItem[] = files.map((doc) => ({
        id: doc.documentId,
        name: doc.title,
        category: doc.documentType.toUpperCase(),
        file_type: 'Document',
        uploaded_by: doc.createdByUserId ? `Bruker ${doc.createdByUserId}` : 'Ukjent',
        uploaded_date: doc.updatedAt ?? doc.createdAt ?? '',
        size: '-',
        version: String(doc.currentVersion),
        status: doc.active ? 'active' : 'archived',
        description: doc.description ?? 'Ingen beskrivelse',
      }))

      const mappedExportReports: ReportItem[] = exports.map((job) => ({
        id: job.exportJobId,
        title: job.fileName ?? `Eksport ${job.exportJobId}`,
        type: String(job.exportType ?? 'monthly').toLowerCase(),
        created_by: 'System',
        created_date: job.requestedAt ?? '',
        period: splitIsoDate(job.requestedAt).date || 'Lopende',
        status: job.status === 'COMPLETED' ? 'finalized' : 'draft',
        sections: [
          {
            name: 'Status',
            content: job.status,
          },
        ],
        file_url: job.downloadUrl,
        file_size: null,
      }))

      const mappedDeviationReports: ReportItem[] = deviations.map((report) => ({
        id: report.reportId + 100000,
        title: report.title,
        type: 'deviation',
        created_by: 'Kvalitetssystem',
        created_date: report.reportDate ?? '',
        period: splitIsoDate(report.reportDate).date || 'Lopende',
        status: mapDeviationStatus(report.status),
        sections: [
          {
            name: 'Beskrivelse',
            content: report.description,
          },
          {
            name: 'Alvorlighet',
            content: report.severity,
          },
        ],
        file_url: null,
        file_size: null,
      }))

      const mappedNotifications: NotificationItem[] = storedNotifications.map((notification) => {
        const split = splitIsoDate(notification.createdAt)
        const actionUrl = mapNotificationActionUrl(notification.relatedEntityType)
        return {
          id: notification.notificationId,
          title: notification.title,
          message: notification.bodyText,
          type: mapNotificationType(notification.notificationType),
          priority: mapNotificationPriority(notification.notificationType),
          created_date: split.date,
          created_time: split.time,
          read: notification.isRead,
          action_url: actionUrl,
          action_label: actionUrl ? 'Se detaljer' : null,
        } satisfies NotificationItem
      })

      const allReports = [...mappedExportReports, ...mappedDeviationReports]
      const finalizedReports = allReports.filter((report) => report.status === 'finalized').length
      const warningCount = mappedNotifications.filter((item) => item.priority === 'high').length

      dashboardStats.splice(0, dashboardStats.length,
        {
          label: 'Rapporter',
          value: allReports.length,
          trend: 'neutral',
          color: 'info',
        },
        {
          label: 'Dokumenter',
          value: mappedDocuments.length,
          trend: 'neutral',
          color: 'success',
        },
        {
          label: 'Kritiske varsler',
          value: warningCount,
          trend: warningCount > 0 ? 'up' : 'neutral',
          color: warningCount > 0 ? 'warning' : 'success',
        },
        {
          label: 'Ferdigstilte rapporter',
          value: finalizedReports,
          trend: 'neutral',
          color: 'info',
          unit: `/${allReports.length}`,
        },
      )

      reports.splice(0, reports.length, ...allReports)
      documents.splice(0, documents.length, ...mappedDocuments)
      notifications.splice(0, notifications.length, ...mappedNotifications)

      const failedCalls = [filesResponse, exportsResponse, deviationsResponse, notificationsResponse]
        .filter((result) => result.status === 'rejected').length
      const succeededCalls = 4 - failedCalls

      if (succeededCalls === 0) {
        error.value = 'Alle dashboard-endepunkter feilet. Kontroller innlogging og prøv igjen.'
        return
      }

      error.value = null

      hasLoaded = true
      lastLoadedOrgNumber = orgNumber
    } catch (err: unknown) {
      dashboardStats.splice(0, dashboardStats.length)
      reports.splice(0, reports.length)
      documents.splice(0, documents.length)
      notifications.splice(0, notifications.length)

      if (!hasResponse(err) || !err.response) {
        error.value = 'Backend er ikke tilgjengelig. Kontroller at API kjører på port 8080.'
      } else {
        error.value = 'Kunne ikke laste dashboard-data fra API.'
      }
    } finally {
      isLoading.value = false
      loadInFlight = null
    }
  })()

  return loadInFlight
}

const sortedReports = () => [...reports].sort((a, b) => parseDateWithOptionalTime(b.created_date) - parseDateWithOptionalTime(a.created_date))
const sortedDocuments = () => [...documents].sort((a, b) => parseDateWithOptionalTime(b.uploaded_date) - parseDateWithOptionalTime(a.uploaded_date))
const sortedNotifications = () => [...notifications].sort((a, b) => parseDateWithOptionalTime(b.created_date, b.created_time) - parseDateWithOptionalTime(a.created_date, a.created_time))

const reload = async () => {
  hasLoaded = false
  await loadData()
}

const markNotificationAsRead = async (notificationId: number) => {
  await client.put(`/notifications/${notificationId}/read`)
  const notification = notifications.find((item) => item.id === notificationId)
  if (notification) {
    notification.read = true
  }
}

const markAllNotificationsAsRead = async () => {
  await client.put('/notifications/read-all', null, {
    params: withOrgNumber({}),
  })
  notifications.forEach((item) => {
    item.read = true
  })
}

const dismissNotification = async (notificationId: number) => {
  await client.delete(`/notifications/${notificationId}`)
  const index = notifications.findIndex((item) => item.id === notificationId)
  if (index >= 0) {
    notifications.splice(index, 1)
  }
}

export const useFellesData = () => {
  void loadData()

  return {
    dashboardStats,
    quickActions,
    reports,
    documents,
    notifications,
    get sortedReports() {
      return sortedReports()
    },
    get sortedDocuments() {
      return sortedDocuments()
    },
    get sortedNotifications() {
      return sortedNotifications()
    },
    formatDate,
    reportTypeLabel,
    reportStatusLabel,
    reportStatusTone,
    notificationTone,
    isLoading,
    error,
    reload,
    markNotificationAsRead,
    markAllNotificationsAsRead,
    dismissNotification,
  }
}
