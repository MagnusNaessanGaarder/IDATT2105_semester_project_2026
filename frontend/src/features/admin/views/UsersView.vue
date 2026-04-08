<script setup lang="ts">
import { computed, ref } from 'vue'
import { type AdminUser, useAdminData } from '../composables/useAdminData'

const data = useAdminData()

const query = ref('')
const roleFilter = ref<'all' | 'ADMIN' | 'MANAGER' | 'STAFF'>('all')

const filteredUsers = computed(() => {
  return data.users.filter((user) => {
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

const activeUsersCount = computed(() => data.users.filter((user) => user.status === 'active').length)

const roleSummaries = computed(() => {
  const roles: Array<'ADMIN' | 'MANAGER' | 'STAFF'> = ['ADMIN', 'MANAGER', 'STAFF']
  return roles.map((role) => ({
    role,
    label: data.roleLabel(role),
    tone: data.roleTone(role),
    description: data.roleDescription(role),
    count: data.users.filter((user) => user.role === role).length,
  }))
})

const handleEditUser = (user: AdminUser) => {
  void user
}

const handleToggleUser = (userId: number) => {
  const nextUsers: AdminUser[] = data.users.map((user) => {
    if (user.id !== userId) {
      return user
    }

    return {
      ...user,
      status: user.status === 'active' ? 'inactive' : 'active',
    }
  })

  data.users.splice(0, data.users.length, ...nextUsers)
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

    <section v-if="data.error" class="warning-state">
      <p>{{ data.error }}</p>
      <button type="button" class="action-btn" @click="data.reload">Prøv igjen</button>
    </section>

    <section v-else-if="data.isLoading" class="empty-state">
      <p>Laster brukere...</p>
    </section>

    <section v-if="!data.isLoading" class="stats-row" aria-label="Brukerstatistikk">
      <article class="stats-card">
        <strong>{{ data.users.length }}</strong>
        <span>Brukere totalt</span>
      </article>
      <article class="stats-card">
        <strong>{{ activeUsersCount }}</strong>
        <span>Aktive brukere</span>
      </article>
      <article class="stats-card">
        <strong>{{ data.users.filter((user) => !user.certifications_valid).length }}</strong>
        <span>Mangler gyldige sertifiseringer</span>
      </article>
      <article class="stats-card">
        <strong>{{ new Set(data.users.map((user) => user.department)).size }}</strong>
        <span>Avdelinger</span>
      </article>
    </section>

    <section v-if="!data.isLoading" class="roles-grid" aria-label="Rolleoversikt">
      <article v-for="role in roleSummaries" :key="role.role" class="role-card">
        <p class="role-pill" :class="`role-pill--${role.tone}`">{{ role.label }}</p>
        <p class="role-description">{{ role.description }}</p>
        <span class="role-count">{{ role.count }} brukere</span>
      </article>
    </section>

    <section v-if="!data.isLoading" class="users-table-card">
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

    <div v-if="!data.isLoading && filteredUsers.length === 0" class="empty-state">
      <p>Ingen brukere funnet</p>
    </div>
  </div>
</template>

<style scoped>
.users-view {
  display: grid;
  gap: var(--spacing-lg);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: var(--spacing-md);
}

.page-header h1 {
  margin: 0;
  font-size: clamp(1.8rem, 2.4vw, var(--font-size-3xl));
  font-weight: 700;
  letter-spacing: -0.015em;
}

.subtitle {
  margin-top: 0.4rem;
  color: var(--color-gray-500);
}

.warning-state {
  border: 1px solid color-mix(in srgb, var(--color-warning) 35%, var(--color-border));
  background: var(--color-warning-bg);
  border-radius: var(--radius-md);
  color: var(--color-warning);
  padding: 0.9rem 1rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.7rem;
}

.create-btn {
  min-height: var(--touch-target);
  padding: var(--button-padding-md);
  background: var(--color-primary);
  color: var(--color-primary-foreground);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: 600;
  box-shadow: var(--shadow-sm);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--spacing-md);
}

.stats-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-card);
  text-align: center;
  padding: 1rem;
  box-shadow: var(--shadow-sm);
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
  gap: var(--spacing-md);
}

.role-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-card);
  padding: 1rem;
  box-shadow: var(--shadow-sm);
}

.role-pill {
  display: inline-flex;
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  font-size: var(--font-size-xs);
  font-weight: 600;
}

.role-pill--red {
  background: var(--color-danger-bg);
  color: var(--color-danger-fg);
}

.role-pill--amber {
  background: var(--color-warning-bg);
  color: var(--color-warning);
}

.role-pill--blue {
  background: var(--color-info-bg);
  color: var(--color-info);
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
  border-radius: var(--radius-lg);
  background: var(--color-card);
  padding: 1rem;
  box-shadow: var(--shadow-sm);
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-md);
}

.table-search,
.role-select {
  min-height: var(--touch-target);
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
  padding: 0.85rem 0.75rem;
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
  width: 2rem;
  height: 2rem;
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
  min-height: 2.25rem;
  padding: 0.35rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-primary);
  color: var(--color-primary-foreground);
  font-size: var(--font-size-xs);
  font-weight: 600;
  box-shadow: var(--shadow-sm);
}

.action-btn--ghost {
  background: var(--color-card);
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
  box-shadow: var(--shadow-sm);
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
