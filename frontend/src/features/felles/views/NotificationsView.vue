<script setup lang="ts">
import { computed, ref } from 'vue'
import fellesData from '@/data/felles.json'
import NotificationCard from '../components/NotificationCard.vue'

interface NotificationItem {
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

const notifications = ref<NotificationItem[]>(fellesData.notifications as NotificationItem[])
const filter = ref<'all' | 'unread' | 'high-priority'>('all')

const filteredNotifications = computed(() => {
  if (filter.value === 'unread') {
    return notifications.value.filter((item) => !item.read)
  }

  if (filter.value === 'high-priority') {
    return notifications.value.filter((item) => item.priority === 'high')
  }

  return notifications.value
})

const markAsRead = (notificationId: number) => {
  const notification = notifications.value.find((item) => item.id === notificationId)
  if (notification) {
    notification.read = true
  }
}

const dismissNotification = (notificationId: number) => {
  notifications.value = notifications.value.filter((item) => item.id !== notificationId)
}

const handleAction = (notification: NotificationItem) => {
  void notification
}
</script>

<template>
  <div class="view-page">
    <header class="page-header">
      <h1>Varsler</h1>
      <p class="subtitle">Varsler og påminnelser</p>
    </header>

    <div class="notifications-filters">
      <button class="filter-btn" :class="{ 'filter-btn--active': filter === 'all' }" @click="filter = 'all'">
        Alle
      </button>
      <button class="filter-btn" :class="{ 'filter-btn--active': filter === 'unread' }" @click="filter = 'unread'">
        Uleste
      </button>
      <button
        class="filter-btn"
        :class="{ 'filter-btn--active': filter === 'high-priority' }"
        @click="filter = 'high-priority'"
      >
        Høy prioritet
      </button>
    </div>

    <div class="notifications-list">
      <NotificationCard
        v-for="notification in filteredNotifications"
        :key="notification.id"
        :notification="notification"
        @read="markAsRead(notification.id)"
        @dismiss="dismissNotification(notification.id)"
        @action="handleAction(notification)"
      />
    </div>

    <div v-if="filteredNotifications.length === 0" class="empty-state">
      <p>Ingen varsler matcher valgt filter</p>
    </div>
  </div>
</template>

<style scoped>
.view-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}

.page-header {
  margin-bottom: 2rem;
}

.page-header h1 {
  margin: 0;
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--color-foreground);
  margin-bottom: 0.5rem;
}

.subtitle {
  margin: 0;
  font-size: var(--text-base);
  color: var(--color-gray-600);
}

.notifications-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-bottom: 2rem;
}

.filter-btn {
  padding: 0.5rem 1rem;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-base);
  color: var(--color-gray-600);
}

.filter-btn:hover {
  background: var(--color-accent);
}

.filter-btn--active {
  background: var(--color-foreground);
  color: var(--color-background);
  border-color: var(--color-foreground);
}

.notifications-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 2rem;
}

.empty-state {
  text-align: center;
  padding: 3rem 1.5rem;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-gray-600);
}
</style>