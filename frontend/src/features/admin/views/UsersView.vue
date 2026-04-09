<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
import { useUsers, type User } from '../composables/useUsers'
import { useAuthStore } from '@/stores/auth'
import BaseModal from '@/shared/components/BaseModal.vue'
import BaseSpinner from '@/shared/components/BaseSpinner.vue'
import ErrorMessage from '@/shared/components/ErrorMessage.vue'

const authStore = useAuthStore()
const usersComposable = useUsers()

// Get current organization from auth store
const currentOrg = computed(() => authStore.currentOrg)
const isAdmin = computed(() => authStore.isAdmin)

// Local state for UI
const query = ref('')
const roleFilter = ref<'all' | 'ADMIN' | 'MANAGER' | 'EMPLOYEE'>('all')
const showCreateModal = ref(false)
const showEditModal = ref(false)
const selectedUser = ref<User | null>(null)

// Form state for create/edit
const formData = ref({
  displayName: '',
  email: '',
  phone: '',
  roleIds: [] as number[],
})

// Available roles for selection
const availableRoles = [
  { id: 1, name: 'ADMIN', label: 'Admin' },
  { id: 2, name: 'MANAGER', label: 'Leder' },
  { id: 3, name: 'EMPLOYEE', label: 'Ansatt' },
]

// Load users on mount and when org changes
onMounted(() => {
  loadUsers()
})

watch(currentOrg, () => {
  loadUsers()
})

async function loadUsers() {
  if (currentOrg.value?.orgNumber) {
    try {
      await usersComposable.fetchUsers(currentOrg.value.orgNumber)
    } catch (e) {
      // Error is handled by composable
    }
  }
}

// Filter users based on search and role
const filteredUsers = computed(() => {
  const users = usersComposable.users
  return users.filter((user) => {
    const matchesRole = roleFilter.value === 'all' || usersComposable.getUserRole(user) === roleFilter.value
    const search = query.value.trim().toLowerCase()
    const matchesQuery =
      search.length === 0 ||
      user.displayName.toLowerCase().includes(search) ||
      user.email.toLowerCase().includes(search)

    return matchesRole && matchesQuery
  })
})

// Statistics
const activeUsersCount = computed(() => {
  const users = usersComposable.users
  return users.filter((u) => u.isActive).length
})

const inactiveUsersCount = computed(() => {
  const users = usersComposable.users
  return users.filter((u) => !u.isActive).length
})

const uniqueRolesCount = computed(() => {
  const users = usersComposable.users
  const allRoleNames = users.flatMap((u) => u.roles?.map((r) => r.roleName) || [])
  const uniqueRoles = new Set(allRoleNames.filter(Boolean))
  return uniqueRoles.size
})

const roleSummaries = computed(() => {
  const users = usersComposable.users
  const roles: Array<'ADMIN' | 'MANAGER' | 'EMPLOYEE'> = ['ADMIN', 'MANAGER', 'EMPLOYEE']
  return roles.map((role) => ({
    role,
    label: roleLabel(role),
    tone: roleTone(role),
    description: roleDescription(role),
    count: users.filter((user) => usersComposable.getUserRole(user) === role).length,
  }))
})

// Role helpers
function roleLabel(role: string): string {
  if (role === 'ADMIN') return 'Admin'
  if (role === 'MANAGER') return 'Leder'
  return 'Ansatt'
}

function roleTone(role: string): 'red' | 'amber' | 'blue' {
  if (role === 'ADMIN') return 'red'
  if (role === 'MANAGER') return 'amber'
  return 'blue'
}

function roleDescription(role: string): string {
  if (role === 'ADMIN') return 'Full tilgang til brukere, innstillinger og revisjonslogg'
  if (role === 'MANAGER') return 'Operativ styring av kontroll, rapporter og oppfølging'
  return 'Daglig bruk av sjekklister, rutiner og dokumentasjon'
}

// Modal handlers
function openCreateModal() {
  formData.value = {
    displayName: '',
    email: '',
    phone: '',
    roleIds: [3], // Default to EMPLOYEE role
  }
  showCreateModal.value = true
}

function openEditModal(user: User) {
  selectedUser.value = user
  formData.value = {
    displayName: user.displayName,
    email: user.email,
    phone: user.phone || '',
    roleIds: user.roles?.map((r) => r.roleId) || [],
  }
  showEditModal.value = true
}

function closeModals() {
  showCreateModal.value = false
  showEditModal.value = false
  selectedUser.value = null
}

// CRUD operations
async function handleCreateUser() {
  if (!currentOrg.value?.orgNumber) return

  try {
    await usersComposable.createUser({
      ...formData.value,
      orgNumber: currentOrg.value.orgNumber,
    })
    closeModals()
  } catch (e) {
    // Error handled by composable
  }
}

async function handleUpdateUser() {
  if (!selectedUser.value || !currentOrg.value?.orgNumber) return

  try {
    await usersComposable.updateUser(selectedUser.value.userId, currentOrg.value.orgNumber, {
      displayName: formData.value.displayName,
      email: formData.value.email,
      phone: formData.value.phone || undefined,
      roleIds: formData.value.roleIds,
    })
    closeModals()
  } catch (e) {
    // Error handled by composable
  }
}

async function handleToggleUser(user: User) {
  if (!currentOrg.value?.orgNumber) return

  try {
    await usersComposable.toggleUserStatus(user.userId, currentOrg.value.orgNumber, user.isActive)
  } catch (e) {
    // Error handled by composable
  }
}

async function handleDeleteUser(user: User) {
  if (!currentOrg.value?.orgNumber) return
  if (!confirm(`Er du sikker på at du vil deaktivere ${user.displayName}?`)) return

  try {
    await usersComposable.deleteUser(user.userId, currentOrg.value.orgNumber)
  } catch (e) {
    // Error handled by composable
  }
}
</script>

<template>
  <div class="view-page users-view">
    <header class="page-header">
      <div>
        <h1>Brukere</h1>
        <p class="subtitle">Administrer brukere, roller og tilgang i systemet</p>
      </div>
      <button 
        v-if="isAdmin" 
        class="create-btn" 
        type="button"
        @click="openCreateModal"
      >
        + Ny bruker
      </button>
    </header>

    <!-- Loading state -->
    <div v-if="usersComposable.isLoading" class="loading-state">
      <BaseSpinner />
      <p>Laster brukere...</p>
    </div>

    <!-- Error state -->
    <ErrorMessage 
      v-else-if="usersComposable.error" 
      :message="usersComposable.error"
      show-retry
      @retry="loadUsers"
    />

    <!-- Content -->
    <template v-else>
      <section class="stats-row" aria-label="Brukerstatistikk">
        <article class="stats-card">
          <strong>{{ usersComposable.users.length }}</strong>
          <span>Brukere totalt</span>
        </article>
        <article class="stats-card">
          <strong>{{ activeUsersCount }}</strong>
          <span>Aktive brukere</span>
        </article>
        <article class="stats-card">
          <strong>{{ inactiveUsersCount }}</strong>
          <span>Inaktive brukere</span>
        </article>
        <article class="stats-card">
          <strong>{{ uniqueRolesCount }}</strong>
          <span>Roller</span>
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
            <option value="EMPLOYEE">Ansatt</option>
          </select>
        </div>

        <div v-if="filteredUsers.length === 0" class="empty-state">
          <p>Ingen brukere funnet</p>
        </div>

        <div v-else class="table-wrapper" role="region" aria-label="Brukertabell">
          <table>
            <thead>
              <tr>
                <th>Navn</th>
                <th>E-post</th>
                <th>Rolle</th>
                <th>Status</th>
                <th>Opprettet</th>
                <th v-if="isAdmin">Handlinger</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in filteredUsers" :key="user.userId">
                <td>
                  <div class="user-cell">
                    <span class="avatar">{{ user.displayName.charAt(0) }}</span>
                    <div>
                      <p class="user-name">{{ user.displayName }}</p>
                      <p v-if="user.phone" class="user-meta">{{ user.phone }}</p>
                    </div>
                  </div>
                </td>
                <td>{{ user.email }}</td>
                <td>
                  <span class="role-pill" :class="`role-pill--${roleTone(usersComposable.getUserRole(user))}`">
                    {{ roleLabel(usersComposable.getUserRole(user)) }}
                  </span>
                </td>
                <td>
                  <span class="status-pill" :class="`status-pill--${user.isActive ? 'active' : 'inactive'}`">
                    {{ user.isActive ? 'Aktiv' : 'Inaktiv' }}
                  </span>
                </td>
                <td>{{ usersComposable.formatDate(user.createdAt) }}</td>
                <td v-if="isAdmin">
                  <div class="actions">
                    <button type="button" class="action-btn action-btn--ghost" @click="openEditModal(user)">
                      Rediger
                    </button>
                    <button type="button" class="action-btn" @click="handleToggleUser(user)">
                      {{ user.isActive ? 'Deaktiver' : 'Aktiver' }}
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>

    <!-- Create User Modal -->
    <BaseModal :open="showCreateModal" title="Ny bruker" @close="closeModals">
      <form id="createUserForm" class="user-form" @submit.prevent="handleCreateUser">
        <div class="form-group">
          <label for="displayName">Navn</label>
          <input id="displayName" v-model="formData.displayName" type="text" required />
        </div>
        <div class="form-group">
          <label for="email">E-post</label>
          <input id="email" v-model="formData.email" type="email" required />
        </div>
        <div class="form-group">
          <label for="phone">Telefon</label>
          <input id="phone" v-model="formData.phone" type="tel" />
        </div>
        <div class="form-group">
          <label for="role">Rolle</label>
          <select id="role" v-model="formData.roleIds" multiple required>
            <option v-for="role in availableRoles" :key="role.id" :value="role.id">
              {{ role.label }}
            </option>
          </select>
        </div>
        <ErrorMessage v-if="usersComposable.createError" :message="usersComposable.createError" />
      </form>
      <template #footer>
        <button type="button" class="action-btn action-btn--ghost" @click="closeModals">Avbryt</button>
        <button type="submit" form="createUserForm" class="action-btn" :disabled="usersComposable.isCreating">
          <BaseSpinner v-if="usersComposable.isCreating" size="small" />
          <span v-else>Opprett bruker</span>
        </button>
      </template>
    </BaseModal>

    <!-- Edit User Modal -->
    <BaseModal :open="showEditModal" title="Rediger bruker" @close="closeModals">
      <form id="editUserForm" class="user-form" @submit.prevent="handleUpdateUser">
        <div class="form-group">
          <label for="editDisplayName">Navn</label>
          <input id="editDisplayName" v-model="formData.displayName" type="text" required />
        </div>
        <div class="form-group">
          <label for="editEmail">E-post</label>
          <input id="editEmail" v-model="formData.email" type="email" required />
        </div>
        <div class="form-group">
          <label for="editPhone">Telefon</label>
          <input id="editPhone" v-model="formData.phone" type="tel" />
        </div>
        <div class="form-group">
          <label for="editRole">Rolle</label>
          <select id="editRole" v-model="formData.roleIds" multiple required>
            <option v-for="role in availableRoles" :key="role.id" :value="role.id">
              {{ role.label }}
            </option>
          </select>
        </div>
        <ErrorMessage v-if="usersComposable.updateError" :message="usersComposable.updateError" />
        <ErrorMessage v-if="usersComposable.deleteError" :message="usersComposable.deleteError" />
      </form>
      <template #footer>
        <button type="button" class="action-btn action-btn--ghost" @click="closeModals">Avbryt</button>
        <button 
          v-if="selectedUser?.isActive" 
          type="button" 
          class="action-btn action-btn--danger" 
          :disabled="usersComposable.isDeleting"
          @click="handleDeleteUser(selectedUser)"
        >
          <BaseSpinner v-if="usersComposable.isDeleting" size="small" />
          <span v-else>Deaktiver</span>
        </button>
        <button type="submit" form="editUserForm" class="action-btn" :disabled="usersComposable.isUpdating">
          <BaseSpinner v-if="usersComposable.isUpdating" size="small" />
          <span v-else>Lagre endringer</span>
        </button>
      </template>
    </BaseModal>
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

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  padding: 3rem;
  color: var(--color-gray-500);
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
  cursor: pointer;
}

.action-btn:hover {
  opacity: 0.9;
}

.action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.action-btn--ghost {
  background: #fff;
  border: 1px solid var(--color-border);
  color: var(--color-gray-700);
}

.action-btn--ghost:hover {
  background: var(--color-gray-50);
}

.action-btn--danger {
  background: #dc2626;
}

.empty-state {
  text-align: center;
  padding: 2rem;
  color: var(--color-gray-600);
}

/* User Form Styles */
.user-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.form-group label {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--color-gray-700);
}

.form-group input,
.form-group select {
  min-height: 2.5rem;
  padding: 0 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: var(--color-foreground);
}

.form-group select[multiple] {
  min-height: 6rem;
  padding: 0.5rem;
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

  th:nth-child(3),
  td:nth-child(3),
  th:nth-child(5),
  td:nth-child(5) {
    display: none;
  }

  .actions {
    flex-direction: column;
  }
}
</style>
