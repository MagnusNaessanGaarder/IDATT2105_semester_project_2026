<script setup lang="ts">
interface Notification {
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

const props = defineProps<{
  notification: Notification
}>()

const emit = defineEmits<{
  read: []
  action: []
  dismiss: []
}>()

const typeIcon = () => {
  if (props.notification.type === 'warning') return '⚠️'
  if (props.notification.type === 'success') return '✓'
  if (props.notification.type === 'error') return '✕'
  return 'ℹ️'
}
</script>

<template>
  <div class="notification-card" :class="[`notification-card--${notification.type}`, { 'notification-card--unread': !notification.read }]">
    <div class="notification-card__header">
      <span class="notification-card__icon">{{ typeIcon() }}</span>
      <h3 class="notification-card__title">{{ notification.title }}</h3>
      <button class="notification-card__dismiss" @click="emit('dismiss')" aria-label="Lukk varsel">✕</button>
    </div>

    <div class="notification-card__body">
      <p class="notification-card__message">{{ notification.message }}</p>
    </div>

    <div class="notification-card__footer">
      <time class="notification-card__time">{{ notification.created_date }} kl. {{ notification.created_time }}</time>
      <div class="notification-card__actions">
        <button v-if="notification.action_url" class="notification-card__action-btn" @click="emit('action')">
          {{ notification.action_label }}
        </button>
        <button v-if="!notification.read" class="notification-card__mark-read" @click="emit('read')">Marker som lest</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.notification-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-left: 4px solid var(--color-brand-mid-teal);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: transform var(--transition-base), box-shadow var(--transition-base), border-color var(--transition-base);
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-sm);
}

.notification-card--unread {
  background: color-mix(in srgb, var(--color-brand-ice-teal) 38%, var(--color-card));
}

.notification-card--warning {
  border-left-color: var(--color-warning);
}

.notification-card--info {
  border-left-color: var(--color-info);
}

.notification-card--success {
  border-left-color: var(--color-cta);
}

.notification-card--error {
  border-left-color: var(--color-danger);
}

.notification-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.notification-card__header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--color-border);
}

.notification-card__icon {
  font-size: 1.25rem;
  flex-shrink: 0;
}

.notification-card__title {
  margin: 0;
  flex: 1;
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--color-foreground);
}

.notification-card__dismiss {
  flex-shrink: 0;
  width: 2rem;
  height: 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  cursor: pointer;
  color: var(--color-gray-600);
  font-size: 1.25rem;
  transition: color var(--transition-fast), background-color var(--transition-fast);
  border-radius: var(--radius-sm);
}

.notification-card__dismiss:hover {
  color: var(--color-primary);
  background: var(--color-info-bg);
}

.notification-card__body {
  padding: 1rem 1.25rem;
}

.notification-card__message {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-gray-700);
  line-height: 1.6;
}

.notification-card__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1.25rem;
  background: color-mix(in srgb, var(--color-brand-ice-teal) 34%, var(--color-card));
  border-top: 1px solid var(--color-border);
}

.notification-card__time {
  font-size: var(--font-size-xs);
  color: var(--color-gray-600);
}

.notification-card__actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.notification-card__action-btn {
  padding: 0.375rem 0.75rem;
  background: var(--color-primary);
  color: var(--color-primary-foreground);
  border: none;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: 600;
  cursor: pointer;
  transition: background-color var(--transition-fast);
  white-space: nowrap;
}

.notification-card__action-btn:hover {
  background: var(--color-primary-hover);
}

.notification-card__mark-read {
  padding: 0.375rem 0.75rem;
  background: transparent;
  color: var(--color-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  cursor: pointer;
  transition: all var(--transition-fast);
  white-space: nowrap;
}

.notification-card__mark-read:hover {
  border-color: var(--color-brand-light-teal);
  background: var(--color-info-bg);
}

@media (max-width: 48rem) {
  .notification-card__footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .notification-card__actions {
    width: 100%;
  }
}
</style>
