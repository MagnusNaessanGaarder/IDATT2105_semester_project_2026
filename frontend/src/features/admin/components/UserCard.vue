<script setup lang="ts">
import { computed } from 'vue'

interface User {
  id: number
  name: string
  email: string
  role: 'ADMIN' | 'MANAGER' | 'STAFF'
  department: string
  status: 'active' | 'inactive'
  created_date: string
  certifications: string[]
  certifications_valid: boolean
  last_login: string
}

const props = defineProps<{
  user: User
}>()

const emit = defineEmits<{
  edit: []
  deactivate: []
}>()

const roleLabel = computed(() => {
  const { role } = props.user
  if (role === 'ADMIN') return 'Administrator'
  if (role === 'MANAGER') return 'Leder'
  return 'Ansatt'
})

const certificationStatus = computed(() => {
  if (props.user.certifications.length === 0) return 'Ingen'
  if (props.user.certifications_valid) return 'Gyldig'
  return 'Utløpt'
})
</script>

<template>
  <div class="user-card" :class="{ 'user-card--inactive': user.status === 'inactive' }">
    <div class="user-card__header">
      <div class="user-card__avatar">{{ user.name.charAt(0) }}</div>
      <div class="user-card__info">
        <h3 class="user-card__name">{{ user.name }}</h3>
        <p class="user-card__email">{{ user.email }}</p>
      </div>
      <span class="user-card__status" :class="`user-card__status--${user.status}`">
        {{ user.status === 'active' ? 'Aktiv' : 'Inaktiv' }}
      </span>
    </div>

    <div class="user-card__body">
      <div class="user-card__detail">
        <span class="user-card__label">Rolle:</span>
        <span class="user-card__value">{{ roleLabel }}</span>
      </div>
      <div class="user-card__detail">
        <span class="user-card__label">Avdeling:</span>
        <span class="user-card__value">{{ user.department }}</span>
      </div>
      <div class="user-card__detail">
        <span class="user-card__label">Sertifikater:</span>
        <span class="user-card__value">{{ certificationStatus }}</span>
      </div>
      <div class="user-card__detail">
        <span class="user-card__label">Sist innlogget:</span>
        <span class="user-card__value">{{ user.last_login.split('T')[0] }}</span>
      </div>
    </div>

    <div v-if="user.certifications.length > 0" class="user-card__certifications">
      <p class="user-card__cert-title">Sertifikater:</p>
      <ul class="user-card__cert-list">
        <li v-for="cert in user.certifications" :key="cert" class="user-card__cert-item">
          {{ cert }}
        </li>
      </ul>
    </div>

    <div class="user-card__footer">
      <button class="user-card__action-btn" @click="emit('edit')">Rediger</button>
      <button class="user-card__action-btn user-card__action-btn--danger" @click="emit('deactivate')">
        {{ user.status === 'active' ? 'Deaktiver' : 'Aktiver' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.user-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: box-shadow var(--transition-base);
  display: flex;
  flex-direction: column;
}

.user-card:hover {
  box-shadow: var(--shadow-md);
}

.user-card--inactive {
  opacity: 0.6;
  background: var(--color-accent);
}

.user-card__header {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
  border-bottom: 1px solid var(--color-border);
}

.user-card__avatar {
  width: 3rem;
  height: 3rem;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-foreground);
  color: var(--color-background);
  border-radius: var(--radius-full);
  font-weight: 700;
  font-size: var(--text-lg);
  flex-shrink: 0;
}

.user-card__info {
  flex: 1;
  min-width: 0;
}

.user-card__name {
  margin: 0;
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--color-foreground);
}

.user-card__email {
  margin: 0.25rem 0 0;
  font-size: var(--text-sm);
  color: var(--color-gray-600);
  word-break: break-all;
}

.user-card__status {
  padding: 0.375rem 0.75rem;
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  font-weight: 600;
  text-transform: uppercase;
  white-space: nowrap;
}

.user-card__status--active {
  background: rgba(16, 185, 129, 0.1);
  color: #047857;
}

.user-card__status--inactive {
  background: rgba(239, 68, 68, 0.1);
  color: #991b1b;
}

.user-card__body {
  padding: 1rem 1.5rem;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.user-card__detail {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.user-card__label {
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--color-gray-600);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.user-card__value {
  font-size: var(--text-sm);
  color: var(--color-foreground);
}

.user-card__certifications {
  padding: 1rem 1.5rem;
  background: var(--color-accent);
  border-top: 1px solid var(--color-border);
}

.user-card__cert-title {
  margin: 0;
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--color-gray-600);
  text-transform: uppercase;
  margin-bottom: 0.5rem;
}

.user-card__cert-list {
  margin: 0;
  padding-left: 1.25rem;
  list-style: disc;
}

.user-card__cert-item {
  font-size: var(--text-sm);
  color: var(--color-foreground);
}

.user-card__footer {
  display: flex;
  gap: 0.5rem;
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--color-border);
}

.user-card__action-btn {
  flex: 1;
  padding: 0.5rem 1rem;
  background: var(--color-foreground);
  color: var(--color-background);
  border: none;
  border-radius: var(--radius-sm);
  font-size: var(--text-sm);
  font-weight: 600;
  cursor: pointer;
  transition: background-color var(--transition-fast);
}

.user-card__action-btn:hover {
  background: var(--color-gray-900);
}

.user-card__action-btn--danger {
  background: #ef4444;
}

.user-card__action-btn--danger:hover {
  background: #991b1b;
}

@media (max-width: 48rem) {
  .user-card__header {
    flex-direction: column;
    text-align: left;
  }

  .user-card__body {
    grid-template-columns: 1fr;
  }
}
</style>
