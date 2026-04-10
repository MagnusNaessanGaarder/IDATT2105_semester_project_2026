import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
    notificationsApi,
    notificationTone,
    relatedEntityRoute,
    type AppNotification,
} from '../api/notifications'
import { formatDateForOrganization } from '@/shared/utils/orgSettings'

export function useNotifications() {
    const authStore = useAuthStore()
    const router = useRouter()
    const orgNumber = computed(() => authStore.currentOrg?.orgNumber ?? null)

    const notifications  = ref<AppNotification[]>([])
    const isLoading      = ref(false)
    const error          = ref<string | null>(null)

    const unreadCount = computed(() => notifications.value.filter((n) => !n.isRead).length)

    async function fetchNotifications() {
        if (!orgNumber.value) { error.value = 'Ingen organisasjon funnet'; return }
        isLoading.value = true
        error.value = null
        try {
            const data = await notificationsApi.list(orgNumber.value)
            // Sort: unread first, then newest first within each group
            notifications.value = [...data].sort((a, b) => {
                if (a.isRead !== b.isRead) return a.isRead ? 1 : -1
                return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
            })
        } catch (e: any) {
            error.value = e.response?.data?.message ?? 'Kunne ikke laste varsler'
        } finally {
            isLoading.value = false
        }
    }

    async function markRead(notificationId: number) {
        // Optimistic update
        const n = notifications.value.find((n) => n.notificationId === notificationId)
        if (!n || n.isRead) return
        n.isRead = true
        n.readAt = new Date().toISOString()
        try {
            await notificationsApi.markRead(notificationId)
        } catch {
            // Roll back on failure
            n.isRead = false
            n.readAt = null
        }
    }

    async function markAllRead() {
        if (!orgNumber.value) return
        const prev = notifications.value.map((n) => ({ ...n }))
        notifications.value.forEach((n) => { n.isRead = true; n.readAt = new Date().toISOString() })
        try {
            await notificationsApi.markAllRead(orgNumber.value)
        } catch {
            notifications.value = prev
        }
    }

    async function dismiss(notificationId: number) {
        const prev = [...notifications.value]
        notifications.value = notifications.value.filter((n) => n.notificationId !== notificationId)
        try {
            await notificationsApi.delete(notificationId)
        } catch {
            notifications.value = prev
        }
    }

    function handleAction(n: AppNotification) {
        markRead(n.notificationId)
        const route = relatedEntityRoute(n.relatedEntityType)
        if (route) router.push({ name: route })
    }

    function formatDate(iso: string): string {
        const d = new Date(iso)
        const now = new Date()
        const diffMs = now.getTime() - d.getTime()
        const diffMins = Math.floor(diffMs / 60_000)
        const diffHours = Math.floor(diffMins / 60)
        const diffDays = Math.floor(diffHours / 24)

        if (diffMins < 1)   return 'Akkurat nå'
        if (diffMins < 60)  return `${diffMins} min siden`
        if (diffHours < 24) return `${diffHours} t siden`
        if (diffDays < 7)   return `${diffDays} d siden`

        return formatDateForOrganization(d, orgNumber.value ?? undefined, {
            day: '2-digit',
            month: 'short',
            year: 'numeric',
        })
    }

    onMounted(fetchNotifications)

    return {
        notifications,
        isLoading,
        error,
        unreadCount,
        notificationTone,
        fetchNotifications,
        markRead,
        markAllRead,
        dismiss,
        handleAction,
        formatDate,
    }
}
