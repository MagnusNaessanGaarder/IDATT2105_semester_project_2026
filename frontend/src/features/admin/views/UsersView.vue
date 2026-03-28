<script setup lang="ts">
import { computed, ref } from 'vue'
import { type AdminUser, useAdminData } from '../composables/useAdminData'

const data = useAdminData()
const users = ref<AdminUser[]>([...data.users])

const query = ref('')
const roleFilter = ref<'all' | 'ADMIN' | 'MANAGER' | 'STAFF'>('all')

const filteredUsers = computed(() => {
  return users.value.filter((user) => {
    const matchesRole = roleFilter.value === 'all' || user.role === roleFilter.value
    const search = query.value.trim().toLowerCase()
    const matchesQuery =
      search.length === 0 ||
      user.name.toLowerCase().includes(search) ||
      user.email.toLowerCase().includes(search) ||
      user.department.toLowerCase().includes(search)

    return matchesRole && matchesQuery
  })
})

const activeUsersCount = computed(() => users.value.filter((user) => user.status === 'active').length)

const roleSummaries = computed(() => {
  const roles: Array<'ADMIN' | 'MANAGER' | 'STAFF'> = ['ADMIN', 'MANAGER', 'STAFF']
  return roles.map((role) => ({
    role,
    label: data.roleLabel(role),
    tone: data.roleTone(role),
    description: data.roleDescription(role),
    count: users.value.filter((user) => user.role === role).length,
  }))
})

const handleEditUser = (user: AdminUser) => {
  void user
}

const handleToggleUser = (userId: number) => {
  users.value = users.value.map((user) => {
    if (user.id !== userId) {
      return user
    }

    return {
      ...user,
      status: user.status === 'active' ? 'inactive' : 'active',
    }
  })
}
</script>

<template>
  <div class="view-page users-view">
    <header class="page-header">
      <div>
        <h1>Brukere</h1>
        <p class="subtitle">Administrer brukere, roller og tilgang i systemet</p>
      </div>
      <button class="create-btn" type="button">+ Ny bruker</button>
    </header>

    <section class="stats-row" aria-label="Brukerstatistikk">
      <article class="stats-card">
        <strong>{{ users.length }}</strong>
        <span>Brukere totalt</span>
      </article>
      <article class="stats-card">
        <strong>{{ activeUsersCount }}</strong>
        <span>Aktive brukere</span>
      </article>
      <article class="stats-card">
        <strong>{{ users.filter((user) => !user.certifications_valid).length }}</strong>
        <span>Mangler gyldige sertifiseringer</span>
      </article>
      <article class="stats-card">
        <strong>{{ new Set(users.map((user) => user.department)).size }}</strong>
        <span>Avdelinger</span>
      </article>
    </section>

    <section class="roles-grid" aria-label="Rolleoversikt">
      <article v-for="role in roleSummaries" :key="role.role" class="role-card">
        <p class="role-pill" :class="`role-pill--${role.tone}`">{{ role.label }}</p>
        <p class="role-description">{{ role.description }}</p>
        <span class="role-count">{{ role.count }} brukere</span>
      </article>
    </section>

    <section class="users-table-card">
      <div class="table-toolbar">
        <input v-model="query" type="search" class="table-search" placeholder="Søk etter brukere" />
        <select v-model="roleFilter" class="role-select" aria-label="Filtrer på rolle">
          <option value="all">Alle roller</option>
          <option value="ADMIN">Admin</option>
          <option value="MANAGER">Leder</option>
          <option value="STAFF">Ansatt</option>
        </select>
      </div>

      <div class="table-wrapper" role="region" aria-label="Brukertabell">
        <table>
          <thead>
            <tr>
              <th>Navn</th>
              <th>E-post</th>
              <th>Rolle</th>
              <th>Status</th>
              <th>Sist aktiv</th>
              <th>Handlinger</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in filteredUsers" :key="user.id">
              <td>
                <div class="user-cell">
                  <span class="avatar">{{ user.name.charAt(0) }}</span>
                  <div>
                    <p class="user-name">{{ user.name }}</p>
                    <p class="user-meta">{{ user.department }}</p>
                  </div>
                </div>
              </td>
              <td>{{ user.email }}</td>
              <td>
                <span class="role-pill" :class="`role-pill--${data.roleTone(user.role)}`">{{ data.roleLabel(user.role) }}</span>
              </td>
              <td>
                <span class="status-pill" :class="`status-pill--${user.status}`">{{ data.statusLabel(user.status) }}</span>
              </td>
              <td>{{ data.formatDateTime(user.last_login) }}</td>
              <td>
                <div class="actions">
                  <button type="button" class="action-btn action-btn--ghost" @click="handleEditUser(user)">Rediger</button>
                  <button type="button" class="action-btn" @click="handleToggleUser(user.id)">
                    {{ user.status === 'active' ? 'Deaktiver' : 'Aktiver' }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <div v-if="filteredUsers.length === 0" class="empty-state">
      <p>Ingen brukere funnet</p>
    </div>
  </div>
</template>

<style scoped>
.users-view {
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
  font-weight: 700;
  letter-spacing: -0.015em;
}

.subtitle {
  margin-top: 0.4rem;
  color: var(--color-gray-500);
}

.create-btn {
  min-height: 2.7rem;
  padding: 0.5rem 1rem;
  background: var(--color-foreground);
  color: var(--color-background);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.75rem;
}

.stats-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #fff;
  text-align: center;
  padding: 0.85rem;
}

.stats-card strong {
  color: var(--color-gray-900);
  font-size: var(--font-size-xl);
}

.stats-card span {
  display: block;
  margin-top: 0.2rem;
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
}

.roles-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.role-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #fff;
  padding: 0.8rem;
}

.role-pill {
  display: inline-flex;
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  font-size: var(--font-size-xs);
  font-weight: 600;
}

.role-pill--red {
  background: #fee2e2;
  color: #991b1b;
}

.role-pill--amber {
  background: #fef3c7;
  color: #92400e;
}

.role-pill--blue {
  background: #e0f2fe;
  color: #075985;
}

.role-description {
  margin-top: 0.45rem;
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
}

.role-count {
  display: block;
  margin-top: 0.35rem;
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
}

.users-table-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #fff;
  padding: 0.75rem;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 0.6rem;
  margin-bottom: 0.75rem;
}

.table-search,
.role-select {
  min-height: 2.6rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-gray-50);
  padding: 0 0.8rem;
  color: var(--color-gray-700);
}

.table-search {
  flex: 1;
}

.table-wrapper {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  text-align: left;
  padding: 0.7rem;
  border-bottom: 1px solid var(--color-gray-100);
  vertical-align: middle;
}

th {
  font-size: var(--font-size-xs);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-gray-500);
  background: var(--color-gray-50);
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.avatar {
  width: 1.8rem;
  height: 1.8rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--color-gray-200);
  color: var(--color-gray-700);
  font-size: var(--font-size-xs);
  font-weight: 700;
}

.user-name {
  color: var(--color-gray-900);
  font-size: var(--font-size-sm);
}

.user-meta {
  margin-top: 0.15rem;
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
}

.status-pill {
  display: inline-flex;
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  font-size: var(--font-size-xs);
  font-weight: 600;
}

.status-pill--active {
  background: var(--color-success-bg);
  color: var(--color-success);
}

.status-pill--inactive {
  background: var(--color-gray-100);
  color: var(--color-gray-600);
}

.actions {
  display: flex;
  gap: 0.35rem;
}

.action-btn {
  min-height: 2rem;
  padding: 0.25rem 0.6rem;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--color-foreground);
  color: #fff;
  font-size: var(--font-size-xs);
  font-weight: 600;
}

.action-btn--ghost {
  background: #fff;
  border: 1px solid var(--color-border);
  color: var(--color-gray-700);
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

  .stats-row,
  .roles-grid {
    grid-template-columns: 1fr;
  }

  .table-toolbar {
    flex-direction: column;
  }

  th:nth-child(2),
  td:nth-child(2),
  th:nth-child(5),
  td:nth-child(5) {
    display: none;
  }

  .actions {
    flex-direction: column;
  }
}
</style>
