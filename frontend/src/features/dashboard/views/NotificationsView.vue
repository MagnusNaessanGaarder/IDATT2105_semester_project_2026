<script setup lang="ts">
import { computed, ref } from 'vue'
import { useNotifications } from '../composables/useNotifications'
import { notificationTypeLabels, relatedEntityRoute } from '../api/notifications'

const {
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
} = useNotifications()

// ── Filter ────────────────────────────────────────────────────────────────

const filter = ref<'all' | 'unread'>('all')

const filtered = computed(() => {
  if (filter.value === 'unread') return notifications.value.filter((n) => !n.isRead)
  return notifications.value
})

const hasActionable = computed(() => filtered.value.some((n) => relatedEntityRoute(n.relatedEntityType)))
</script>

<template>
  <div class="view-page notifications-view">

    <!-- ── Header ─────────────────────────────────────────────────────── -->
    <header class="page-header">
      <div>
        <h1>Varsler</h1>
        <p class="subtitle">
          <template v-if="isLoading">Laster…</template>
          <template v-else-if="unreadCount > 0">{{ unreadCount }} uleste varsler</template>
          <template v-else>Alle varsler er lest</template>
        </p>
      </div>
      <button
          v-if="unreadCount > 0"
          type="button"
          class="mark-all-btn"
          @click="markAllRead"
      >
        Marker alle som lest
      </button>
    </header>

    <!-- ── Error ──────────────────────────────────────────────────────── -->
    <div v-if="error" class="error-banner" role="alert">
      <span>{{ error }}</span>
      <button type="button" class="retry-btn" @click="fetchNotifications">Prøv igjen</button>
    </div>

    <!-- ── Filter tabs ────────────────────────────────────────────────── -->
    <section class="filter-bar" aria-label="Varselfiltre">
      <button
          type="button"
          class="filter-btn"
          :class="{ 'filter-btn--active': filter === 'all' }"
          @click="filter = 'all'"
      >
        Alle
        <span v-if="notifications.length" class="filter-count">{{ notifications.length }}</span>
      </button>
      <button
          type="button"
          class="filter-btn"
          :class="{ 'filter-btn--active': filter === 'unread' }"
          @click="filter = 'unread'"
      >
        Uleste
        <span v-if="unreadCount" class="filter-count filter-count--unread">{{ unreadCount }}</span>
      </button>
    </section>

    <!-- ── Skeleton ───────────────────────────────────────────────────── -->
    <div v-if="isLoading" class="skeleton-list">
      <div v-for="i in 4" :key="i" class="skeleton-row" />
    </div>

    <!-- ── Notification list ──────────────────────────────────────────── -->
    <template v-else-if="filtered.length">
      <ul class="notification-list" aria-label="Varsler">
        <li
            v-for="n in filtered"
            :key="n.notificationId"
            class="notification-item"
            :class="{ 'notification-item--unread': !n.isRead }"
        >
          <!-- Colour dot -->
          <span
              class="dot"
              :class="`dot--${notificationTone(n.notificationType)}`"
              aria-hidden="true"
          />

          <!-- Main content -->
          <div class="notification-item__body">
            <div class="notification-item__top">
              <strong class="notification-item__title">{{ n.title }}</strong>
              <span class="notification-item__type">{{ notificationTypeLabels[n.notificationType] }}</span>
            </div>
            <p class="notification-item__message">{{ n.bodyText }}</p>
            <time class="notification-item__time" :datetime="n.createdAt">
              {{ formatDate(n.createdAt) }}
              <template v-if="n.isRead && n.readAt"> · Lest {{ formatDate(n.readAt) }}</template>
            </time>
          </div>

          <!-- Actions -->
          <div class="notification-item__actions">
            <button
                v-if="relatedEntityRoute(n.relatedEntityType)"
                type="button"
                class="action-btn action-btn--primary"
                @click="handleAction(n)"
            >
              Gå til
            </button>
            <button
                v-if="!n.isRead"
                type="button"
                class="action-btn action-btn--ghost"
                @click="markRead(n.notificationId)"
            >
              Marker lest
            </button>
            <button
                type="button"
                class="action-btn action-btn--danger"
                :aria-label="`Fjern varsel: ${n.title}`"
                @click="dismiss(n.notificationId)"
            >
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg>
            </button>
          </div>
        </li>
      </ul>
    </template>

    <!-- ── Empty state ────────────────────────────────────────────────── -->
    <section v-else-if="!isLoading" class="empty-state">
      <svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
      <p>{{ filter === 'unread' ? 'Ingen uleste varsler.' : 'Ingen varsler.' }}</p>
      <button
          v-if="filter === 'unread'"
          type="button"
          class="action-btn action-btn--ghost"
          @click="filter = 'all'"
      >Vis alle</button>
    </section>

  </div>
</template>

<style scoped>
.notifications-view { display: grid; gap: 1rem; }

/* Header */
.page-header { display: flex; justify-content: space-between; align-items: flex-end; gap: 1rem; }
.page-header h1 { margin: 0; font-size: var(--font-size-3xl); font-weight: 700; letter-spacing: -0.015em; }
.subtitle { margin-top: 0.4rem; color: var(--color-gray-500); font-size: var(--font-size-sm); }

.mark-all-btn {
  min-height: 2.65rem; padding: 0.45rem 0.85rem;
  border: 1px solid var(--color-border); border-radius: var(--radius-md);
  background: var(--color-card); color: var(--color-gray-700);
  font-size: var(--font-size-sm); font-family: inherit; cursor: pointer;
  transition: background var(--transition-fast);
}
.mark-all-btn:hover { background: var(--color-gray-50); }

/* Error */
.error-banner {
  display: flex; align-items: center; justify-content: space-between; gap: 1rem;
  padding: 0.75rem 1rem; border-radius: var(--radius-md);
  background: var(--color-danger-bg); color: var(--color-danger);
  border: 1px solid var(--color-danger-bg); font-size: var(--font-size-sm);
}
.retry-btn {
  padding: 0.25rem 0.6rem; border-radius: var(--radius-sm);
  border: 1px solid currentColor; background: transparent;
  font-size: var(--font-size-xs); font-weight: 600; cursor: pointer;
}

/* Filter bar */
.filter-bar { display: flex; gap: 0.5rem; flex-wrap: wrap; }
.filter-btn {
  display: inline-flex; align-items: center; gap: 0.4rem;
  min-height: 2.25rem; padding: 0.35rem 0.75rem;
  border: 1px solid var(--color-border); border-radius: var(--radius-md);
  background: var(--color-card); font-size: var(--font-size-sm);
  font-weight: 500; color: var(--color-gray-600); cursor: pointer;
  transition: all var(--transition-fast);
}
.filter-btn--active {
  background: var(--color-foreground); color: var(--color-card);
  border-color: var(--color-foreground);
}
.filter-count {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 1.25rem; height: 1.25rem; padding: 0 0.3rem;
  border-radius: 999px; background: var(--color-gray-200);
  color: var(--color-gray-700); font-size: 10px; font-weight: 700;
}
.filter-btn--active .filter-count { background: rgba(255,255,255,0.2); color: #fff; }
.filter-count--unread { background: var(--color-danger); color: #fff; }
.filter-btn--active .filter-count--unread { background: rgba(255,255,255,0.3); }

/* Skeleton */
.skeleton-list { display: grid; gap: 0.55rem; }
.skeleton-row {
  height: 4.5rem; border-radius: var(--radius-md);
  background: linear-gradient(90deg, var(--color-gray-100) 25%, var(--color-gray-50) 50%, var(--color-gray-100) 75%);
  background-size: 200% 100%; animation: shimmer 1.4s infinite;
}
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

/* Notification list */
.notification-list { display: grid; gap: 0.5rem; list-style: none; margin: 0; padding: 0; }

.notification-item {
  display: flex; align-items: flex-start; gap: 0.85rem;
  padding: 0.9rem 1rem;
  background: var(--color-card); border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  transition: background var(--transition-fast);
}
.notification-item--unread {
  background: var(--color-gray-50);
  border-left: 3px solid var(--color-foreground);
  padding-left: calc(1rem - 2px); /* compensate for the thicker border */
}

/* Dot */
.dot {
  flex-shrink: 0; width: 0.6rem; height: 0.6rem;
  border-radius: 999px; margin-top: 0.3rem;
}
.dot--red   { background: var(--color-danger); }
.dot--amber { background: var(--color-warning); }
.dot--blue  { background: var(--color-info); }
.dot--green { background: var(--color-success); }
.dot--gray  { background: var(--color-gray-400); }

/* Body */
.notification-item__body { flex: 1; min-width: 0; }
.notification-item__top {
  display: flex; align-items: baseline; gap: 0.5rem; flex-wrap: wrap;
}
.notification-item__title {
  font-size: var(--font-size-sm); font-weight: 600; color: var(--color-gray-900);
}
.notification-item__type {
  font-size: var(--font-size-xs); color: var(--color-gray-400);
  font-weight: 400;
}
.notification-item__message {
  margin: 0.25rem 0 0; font-size: var(--font-size-sm); color: var(--color-gray-600);
}
.notification-item__time {
  display: block; margin-top: 0.35rem;
  font-size: var(--font-size-xs); color: var(--color-gray-400);
}

/* Actions */
.notification-item__actions {
  display: flex; align-items: center; gap: 0.35rem; flex-shrink: 0;
}

/* Action buttons */
.action-btn {
  display: inline-flex; align-items: center; gap: 0.35rem;
  padding: 0.3rem 0.65rem; border-radius: var(--radius-sm);
  font-size: var(--font-size-xs); font-weight: 600; font-family: inherit;
  cursor: pointer; white-space: nowrap; transition: all var(--transition-fast);
}
.action-btn--primary {
  background: var(--color-foreground); color: var(--color-card); border: none;
}
.action-btn--primary:hover { opacity: 0.85; }
.action-btn--ghost {
  background: transparent; color: var(--color-gray-600);
  border: 1px solid var(--color-border);
}
.action-btn--ghost:hover { background: var(--color-gray-100); }
.action-btn--danger {
  background: transparent; color: var(--color-danger);
  border: 1px solid var(--color-danger-bg);
  padding: 0.3rem 0.45rem;
}
.action-btn--danger:hover { background: var(--color-danger-bg); }

/* Empty */
.empty-state {
  display: flex; flex-direction: column; align-items: center; gap: 0.75rem;
  padding: 3rem 2rem; background: var(--color-card);
  border: 1px solid var(--color-border); border-radius: var(--radius-lg);
  color: var(--color-gray-500); text-align: center;
}
.empty-state svg { opacity: 0.35; }

/* Responsive */
@media (max-width: 48rem) {
  .page-header { flex-direction: column; align-items: flex-start; }
  .mark-all-btn { width: 100%; }
  .notification-item { flex-wrap: wrap; }
  .notification-item__actions { width: 100%; justify-content: flex-start; }
}
</style>