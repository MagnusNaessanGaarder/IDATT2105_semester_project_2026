<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

interface User {
  id: string
  name: string
  email: string
  role: 'ADMIN' | 'MANAGER' | 'EMPLOYEE'
}

const props = defineProps<{
  user: User
}>()

const router = useRouter()
const authStore = useAuthStore()

const getRoleLabel = (role: string) => {
  const labels: Record<string, string> = {
    'ADMIN': 'Administrator',
    'MANAGER': 'Leder',
    'EMPLOYEE': 'Ansatt'
  }
  return labels[role] || role
}

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map(n => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)
}

const handleLogout = async () => {
  await authStore.logout()
  router.push({ name: 'Login' })
}
</script>

<template>
  <div class="sidebar-user">
    <div class="user-info">
      <div class="user-avatar">
        {{ getInitials(user.name) }}
      </div>
      <div class="user-details">
        <div class="user-name">{{ user.name }}</div>
        <div class="user-role">{{ getRoleLabel(user.role) }}</div>
      </div>
    </div>
    
    <button 
      class="logout-btn"
      @click="handleLogout"
      aria-label="Logg ut"
      title="Logg ut"
    >
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
        <polyline points="16 17 21 12 16 7"></polyline>
        <line x1="21" y1="12" x2="9" y2="12"></line>
      </svg>
    </button>
  </div>
</template>

<style scoped>
.sidebar-user {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.875rem 1rem;
  min-height: 3.75rem;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.01) 0%, rgba(15, 23, 42, 0) 100%);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1;
  min-width: 0;
}

.user-avatar {
  width: 2.25rem;
  height: 2.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--color-info) 0%, var(--color-primary) 100%);
  color: var(--color-primary-foreground);
  font-weight: 600;
  font-size: 0.8125rem;
  border-radius: var(--radius-lg);
  flex-shrink: 0;
  box-shadow: 0 0.125rem 0.25rem rgba(15, 23, 42, 0.08);
}

.user-details {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.user-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-foreground);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: 0.75rem;
  color: var(--color-gray-500);
}

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 2.25rem;
  min-height: 2.25rem;
  padding: 0.5rem;
  background: transparent;
  border: none;
  border-radius: var(--radius-lg);
  color: var(--color-gray-500);
  cursor: pointer;
  transition: background-color var(--transition-fast), color var(--transition-fast), box-shadow var(--transition-fast);
  flex-shrink: 0;
}

.logout-btn:hover {
  background-color: var(--color-danger-bg);
  color: var(--color-danger);
}

.logout-btn:focus-visible {
  outline: 0.125rem solid var(--color-focus);
  outline-offset: -0.125rem;
  background-color: var(--color-danger-bg);
}
</style>