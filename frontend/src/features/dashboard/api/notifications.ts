import { client } from '@/api/client'

export type NotificationType =
    | 'TASK_OVERDUE'
    | 'TEMPERATURE_ALERT'
    | 'DEVIATION_ASSIGNED'
    | 'DEVIATION_STATUS_CHANGED'
    | 'TRAINING_EXPIRING'
    | 'DOCUMENT_UPLOADED'
    | 'GENERAL'

export type RelatedEntityType =
    | 'CHECKLIST_RUN'
    | 'TEMPERATURE_LOG_ENTRY'
    | 'DEVIATION_REPORT'
    | 'TRAINING_RECORD'
    | 'ORGANIZATION_DOCUMENT'
    | 'EXPORT_JOB'
    | 'OTHER'

export interface AppNotification {
    notificationId: number
    orgNumber: number | null
    notificationType: NotificationType
    title: string
    bodyText: string
    relatedEntityType: RelatedEntityType | null
    relatedEntityId: number | null
    isRead: boolean
    readAt: string | null
    createdAt: string
}

export const notificationTypeLabels: Record<NotificationType, string> = {
    TASK_OVERDUE:             'Oppgave forfalt',
    TEMPERATURE_ALERT:        'Temperaturvarsel',
    DEVIATION_ASSIGNED:       'Avvik tildelt',
    DEVIATION_STATUS_CHANGED: 'Avviksstatus endret',
    TRAINING_EXPIRING:        'Opplæring utløper',
    DOCUMENT_UPLOADED:        'Dokument lastet opp',
    GENERAL:                  'Generell',
}

/**
 * Maps a notification type to a colour token used for the dot indicator.
 * Returns one of: red | amber | blue | green | gray
 */
export function notificationTone(type: NotificationType): 'red' | 'amber' | 'blue' | 'green' | 'gray' {
    switch (type) {
        case 'TEMPERATURE_ALERT':
        case 'TASK_OVERDUE':
            return 'red'
        case 'TRAINING_EXPIRING':
        case 'DEVIATION_ASSIGNED':
            return 'amber'
        case 'DEVIATION_STATUS_CHANGED':
        case 'DOCUMENT_UPLOADED':
            return 'blue'
        default:
            return 'gray'
    }
}

/**
 * Maps a relatedEntityType to a route name so clicking a notification
 * can navigate to the relevant section of the app.
 */
export function relatedEntityRoute(type: RelatedEntityType | null): string | null {
    switch (type) {
        case 'CHECKLIST_RUN':        return 'Checklists'
        case 'TEMPERATURE_LOG_ENTRY': return 'Temperature'
        case 'DEVIATION_REPORT':     return 'Deviations'
        case 'TRAINING_RECORD':      return 'Certifications'
        case 'ORGANIZATION_DOCUMENT': return 'Documents'
        case 'EXPORT_JOB':           return 'Export'
        default:                     return null
    }
}

export const notificationsApi = {
    list(orgNumber: number): Promise<AppNotification[]> {
        return client
            .get<AppNotification[]>('/notifications', { params: { orgNumber } })
            .then((r) => r.data)
    },

    markRead(notificationId: number): Promise<void> {
        return client
            .put<void>(`/notifications/${notificationId}/read`)
            .then(() => undefined)
    },

    markAllRead(orgNumber: number): Promise<void> {
        return client
            .put<void>('/notifications/read-all', null, { params: { orgNumber } })
            .then(() => undefined)
    },

    delete(notificationId: number): Promise<void> {
        return client
            .delete<void>(`/notifications/${notificationId}`)
            .then(() => undefined)
    },
}