<script setup lang="ts">
import { ref } from 'vue'
import adminData from '@/data/admin.json'
import UserCard from '../components/UserCard.vue'

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

const users = ref<User[]>(adminData.users as User[])

const handleEditUser = (user: User) => {
  void user
}

const handleToggleUser = (user: User) => {
  const idx = users.value.findIndex((u) => u.id === user.id)
  if (idx > -1) {
    const targetUser = users.value[idx]
    if (targetUser) {
      targetUser.status = user.status === 'active' ? 'inactive' : 'active'
    }
  }
}
</script>

<template>
  <div class="view-page">
    <header class="page-header">
      <h1>Brukere</h1>
      <p class="subtitle">Administrer brukere og tilganger</p>
    </header>

    <button class="create-btn">+ Opprett ny bruker</button>

    <div class="users-grid">
      <UserCard
        v-for="user in users"
        :key="user.id"
        :user="user"
        @edit="handleEditUser(user)"
        @deactivate="handleToggleUser(user)"
      />
    </div>

    <div v-if="users.length === 0" class="empty-state">
      <p>Ingen brukere funnet</p>
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

.create-btn {
  display: inline-block;
  padding: 0.75rem 1.5rem;
  background: var(--color-foreground);
  color: var(--color-background);
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  font-weight: 600;
  cursor: pointer;
  margin-bottom: 2rem;
  transition: background-color var(--transition-fast);
}

.create-btn:hover {
  background: var(--color-gray-900);
}

.users-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 1.5rem;
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

@media (max-width: 48rem) {
  .users-grid {
    grid-template-columns: 1fr;
  }
}
</style>
