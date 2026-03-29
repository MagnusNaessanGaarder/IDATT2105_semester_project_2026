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
  padding: 14px 16px;
  min-height: 64px;
  background: var(--color-card);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.user-avatar {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-primary);
  color: var(--color-primary-foreground);
  font-weight: 600;
  font-size: 13px;
  border-radius: 4px;
  flex-shrink: 0;
}

.user-details {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-foreground);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: 12px;
  color: var(--color-gray-500);
}

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  min-height: 36px;
  padding: 8px;
  background: transparent;
  border: none;
  border-radius: 4px;
  color: var(--color-gray-500);
  cursor: pointer;
  transition: background-color var(--transition-fast), color var(--transition-fast);
  flex-shrink: 0;
}

.logout-btn:hover {
  background-color: var(--color-gray-100);
  color: var(--color-danger);
}

.logout-btn:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}
</style>