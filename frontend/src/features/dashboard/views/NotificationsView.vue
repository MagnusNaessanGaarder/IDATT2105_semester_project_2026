<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { type NotificationItem, useFellesData } from '../composables/useFellesData'

const router = useRouter()
const data = useFellesData()

onMounted(() => {
  void data.reload()
})

const filter = ref<'all' | 'unread' | 'high-priority'>('all')

const notifications = computed(() => data.sortedNotifications)

const filteredNotifications = computed(() => {
  if (filter.value === 'unread') {
    return notifications.value.filter((item) => !item.read)
  }

  if (filter.value === 'high-priority') {
    return notifications.value.filter((item) => item.priority === 'high')
  }

  return notifications.value
})

const actionableNotifications = computed(() => {
  return filteredNotifications.value.filter((item) => item.action_url)
})

const informationalNotifications = computed(() => {
  return filteredNotifications.value.filter((item) => !item.action_url)
})

const unreadCount = computed(() => notifications.value.filter((item) => !item.read).length)

const markAsRead = async (notificationId: number) => {
  await data.markNotificationAsRead(notificationId)
}

const markAllAsRead = async () => {
  await data.markAllNotificationsAsRead()
}

const dismissNotification = async (notificationId: number) => {
  await data.dismissNotification(notificationId)
}

const handleAction = (notification: NotificationItem) => {
  if (!notification.action_url) {
    return
  }

  if (notification.action_url.includes('ik-mat/deviations')) {
    router.push({ name: 'Deviations' })
    return
  }

  if (notification.action_url.includes('ik-mat/temperature')) {
    router.push({ name: 'Temperature' })
    return
  }

  if (notification.action_url.includes('felles/reports')) {
    router.push({ name: 'Reports' })
    return
  }

  if (notification.action_url.includes('admin/certifications')) {
    router.push({ name: 'Certifications' })
    return
  }

  router.push({ name: 'Notifications' })
}

const priorityLabel = (priority: NotificationItem['priority']): string => {
  if (priority === 'high') return 'Høy'
  if (priority === 'medium') return 'Middels'
  return 'Lav'
}
</script>

<template>
  <div class="view-page notifications-view">
    <header class="page-header">
      <div>
        <h1>Varsler</h1>
        <p class="subtitle">{{ unreadCount }} uleste varsler</p>
      </div>
      <button class="mark-all-btn" type="button" @click="markAllAsRead">Marker alle som lest</button>
    </header>

    <section class="notifications-filters" aria-label="Varselfiltre">
      <button class="filter-btn" :class="{ 'filter-btn--active': filter === 'all' }" @click="filter = 'all'">Alle</button>
      <button class="filter-btn" :class="{ 'filter-btn--active': filter === 'unread' }" @click="filter = 'unread'">Uleste</button>
      <button class="filter-btn" :class="{ 'filter-btn--active': filter === 'high-priority' }" @click="filter = 'high-priority'">Høy prioritet</button>
    </section>

    <section class="notification-panel">
      <h2>Krever handling</h2>
      <ul class="notification-list">
        <li v-for="notification in actionableNotifications" :key="notification.id" class="notification-item" :class="{ 'notification-item--unread': !notification.read }">
          <button class="notification-item__main" @click="handleAction(notification)">
            <span class="notification-item__dot" :class="`notification-item__dot--${data.notificationTone(notification.type)}`" />
            <span>
              <strong>{{ notification.title }}</strong>
              <small>{{ notification.message }}</small>
            </span>
            <time>{{ notification.created_time }}</time>
          </button>
          <div class="notification-item__actions">
            <span class="priority-pill" :class="`priority-pill--${notification.priority}`">{{ priorityLabel(notification.priority) }}</span>
            <button type="button" class="mini-btn" @click="markAsRead(notification.id)" v-if="!notification.read">Lest</button>
            <button type="button" class="mini-btn mini-btn--danger" @click="dismissNotification(notification.id)">Fjern</button>
          </div>
        </li>
      </ul>
    </section>

    <section class="notification-panel">
      <h2>Informasjon</h2>
      <ul class="notification-list">
        <li v-for="notification in informationalNotifications" :key="notification.id" class="notification-item" :class="{ 'notification-item--unread': !notification.read }">
          <div class="notification-item__main notification-item__main--static">
            <span class="notification-item__dot" :class="`notification-item__dot--${data.notificationTone(notification.type)}`" />
            <span>
              <strong>{{ notification.title }}</strong>
              <small>{{ notification.message }}</small>
            </span>
            <time>{{ notification.created_time }}</time>
          </div>
          <div class="notification-item__actions">
            <span class="priority-pill" :class="`priority-pill--${notification.priority}`">{{ priorityLabel(notification.priority) }}</span>
            <button type="button" class="mini-btn" @click="markAsRead(notification.id)" v-if="!notification.read">Lest</button>
            <button type="button" class="mini-btn mini-btn--danger" @click="dismissNotification(notification.id)">Fjern</button>
          </div>
        </li>
      </ul>
    </section>

    <section v-if="filteredNotifications.length === 0" class="empty-state">
      <p>Ingen varsler matcher filtreringen.</p>
    </section>
  </div>
</template>

<style scoped>
.notifications-view {
  display: grid;
  gap: 1rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 1rem;
}

.page-header h1 {
  margin: 0;
  font-size: var(--font-size-3xl);
  letter-spacing: -0.015em;
}

.subtitle {
  margin-top: 0.4rem;
  color: var(--color-gray-500);
}

.mark-all-btn {
  min-height: 2.65rem;
  padding: 0.45rem 0.85rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  color: var(--color-gray-700);
  font-size: var(--font-size-sm);
  text-decoration: underline;
}

.notifications-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.filter-btn {
  min-height: 2.25rem;
  padding: 0.35rem 0.75rem;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--color-gray-600);
}

.filter-btn--active {
  background: var(--color-foreground);
  color: var(--color-background);
  border-color: var(--color-foreground);
}

.notification-panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  padding: 0.9rem;
}

.notification-panel h2 {
  margin: 0 0 0.7rem;
  font-size: var(--font-size-lg);
}

.notification-list {
  display: grid;
  gap: 0.5rem;
}

.notification-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.85rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  padding: 0.65rem;
}

.notification-item--unread {
  background: var(--color-gray-50);
}

.notification-item__main {
  border: none;
  width: 100%;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 0.6rem;
  text-align: left;
}

.notification-item__main--static {
  pointer-events: none;
}

.notification-item__dot {
  width: 0.55rem;
  height: 0.55rem;
  border-radius: 999px;
}

.notification-item__dot--red {
  background: var(--color-danger);
}

.notification-item__dot--amber {
  background: var(--color-warning);
}

.notification-item__dot--blue {
  background: var(--color-info);
}

.notification-item__dot--green {
  background: var(--color-success);
}

.notification-item strong {
  display: block;
  font-size: var(--font-size-sm);
  color: var(--color-gray-900);
}

.notification-item small {
  display: block;
  margin-top: 0.2rem;
  color: var(--color-gray-600);
  font-size: var(--font-size-xs);
}

.notification-item time {
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
}

.notification-item__actions {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.priority-pill {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 0.2rem 0.5rem;
  font-size: var(--font-size-xs);
  font-weight: 600;
}

.priority-pill--high {
  background: var(--color-danger-bg);
  color: var(--color-danger);
}

.priority-pill--medium {
  background: var(--color-warning-bg);
  color: var(--color-warning);
}

.priority-pill--low {
  background: var(--color-info-bg);
  color: var(--color-info);
}

.mini-btn {
  min-height: 1.95rem;
  padding: 0.25rem 0.55rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  color: var(--color-gray-700);
  font-size: var(--font-size-xs);
}

.mini-btn--danger {
  border-color: var(--color-danger-border);
  color: var(--color-danger);
}

.empty-state {
  text-align: center;
  padding: 2rem;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  color: var(--color-gray-600);
}

@media (max-width: 48rem) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .notification-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .notification-item__main {
    grid-template-columns: auto 1fr;
  }

  .notification-item time {
    grid-column: 2;
  }

  .notification-item__actions {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>