<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
import { useUsers } from '../composables/useUsers'
import { useAuthStore } from '@/stores/auth'
import { client } from '@/api/client'
import BaseModal from '@/shared/components/BaseModal.vue'
import BaseSpinner from '@/shared/components/BaseSpinner.vue'
import ErrorMessage from '@/shared/components/ErrorMessage.vue'

interface AddToOrgResponse {
  userId: number
  displayName: string
  email: string
  phone?: string
  isActive: boolean
  roles: { roleId: number; roleName: string }[]
}

const authStore = useAuthStore()
const usersComposable = useUsers()

const currentOrg = computed(() => authStore.currentOrg)
const isAdmin   = computed(() => authStore.isAdmin)
const currentUserEmail = computed(() => authStore.email?.toLowerCase() ?? '')

const query      = ref('')
const roleFilter = ref<'all' | 'ADMIN' | 'MANAGER' | 'EMPLOYEE'>('all')

const feedback = ref<{ type: 'success' | 'error'; message: string } | null>(null)

const showAddModal    = ref(false)
const addEmail        = ref('')
const addRoleId       = ref<number>(3)      // default: EMPLOYEE
const addLookupResult = ref<{ found: boolean; displayName?: string; email?: string } | null>(null)
const addLookupLoading = ref(false)
const addSubmitLoading = ref(false)
const addError        = ref<string | null>(null)

const showEditModal = ref(false)
const selectedUser  = ref<import('../api/users').User | null>(null)
const editForm = ref({ displayName: '', email: '', phone: '', roleIds: [] as number[] })
const editError = ref<string | null>(null)
const editLoading = ref(false)

const showConfirm   = ref(false)
const confirmTitle  = ref('')
const confirmMsg    = ref('')
const confirmAction = ref<(() => Promise<void>) | null>(null)
const confirmLoading = ref(false)

const usersList      = computed(() => usersComposable.users.value)
const isLoading      = computed(() => usersComposable.isLoading.value)
const usersError     = computed(() => usersComposable.error.value)

const filteredUsers = computed(() => {
  const q = query.value.trim().toLowerCase()
  return usersList.value.filter((u) => {
    const role = userRole(u)
    const matchRole  = roleFilter.value === 'all' || role === roleFilter.value
    const matchQuery = !q || u.displayName.toLowerCase().includes(q) || u.email.toLowerCase().includes(q)
    return matchRole && matchQuery
  })
})

const roleCounts = computed(() => ({
  ADMIN:    usersList.value.filter((u) => userRole(u) === 'ADMIN').length,
  MANAGER:  usersList.value.filter((u) => userRole(u) === 'MANAGER').length,
  EMPLOYEE: usersList.value.filter((u) => userRole(u) === 'EMPLOYEE').length,
}))

onMounted(loadUsers)
watch(currentOrg, loadUsers)

async function loadUsers() {
  if (currentOrg.value?.orgNumber) {
    try { await usersComposable.fetchUsers(currentOrg.value.orgNumber) } catch { /* handled */ }
  }
}

const isSelf = (email: string) => email.toLowerCase() === currentUserEmail.value

function roleLabel(role: string) {
  return role === 'ADMIN' ? 'Admin' : role === 'MANAGER' ? 'Leder' : 'Ansatt'
}
function roleTone(role: string): 'red' | 'amber' | 'blue' {
  return role === 'ADMIN' ? 'red' : role === 'MANAGER' ? 'amber' : 'blue'
}

function normalizeRole(role: string): 'ADMIN' | 'MANAGER' | 'EMPLOYEE' {
  const key = role.trim().toUpperCase().replace(/^ROLE_/, '')
  if (key === 'ADMIN') return 'ADMIN'
  if (key === 'MANAGER' || key === 'LEADER' || key === 'LEDER') return 'MANAGER'
  if (key === 'EMPLOYEE' || key === 'EMPLOYER' || key === 'ANSATT') return 'EMPLOYEE'
  return 'EMPLOYEE'
}

function userRole(user: import('../api/users').User): 'ADMIN' | 'MANAGER' | 'EMPLOYEE' {
  return normalizeRole(usersComposable.getUserRole(user))
}

function initials(name: string) {
  return name.split(' ').map((w) => w[0]).join('').slice(0, 2).toUpperCase()
}

function openAddModal() {
  addEmail.value        = ''
  addRoleId.value       = 3
  addLookupResult.value = null
  addError.value        = null
  showAddModal.value    = true
}

async function lookupEmail() {
  const email = addEmail.value.trim()
  if (!email) return
  addLookupLoading.value = true
  addLookupResult.value  = null
  addError.value         = null
  try {
    // Try to find the user via the add-to-org endpoint with a dry-run
    // We call GET /users and search locally (users already in org are there).
    // For users NOT in the org, we rely on the POST returning 404.
    // Here we just signal readiness to submit — actual lookup happens on submit.
    addLookupResult.value = { found: true, email }
  } finally {
    addLookupLoading.value = false
  }
}

async function submitAddToOrg() {
  if (!currentOrg.value?.orgNumber || !addEmail.value.trim()) return
  addSubmitLoading.value = true
  addError.value         = null
  try {
    const { data } = await client.post<AddToOrgResponse>('/users/add-to-org', {
      email:     addEmail.value.trim(),
      orgNumber: currentOrg.value.orgNumber,
      roleIds:   [addRoleId.value],
    })
    await loadUsers()
    showAddModal.value    = false
    feedback.value = { type: 'success', message: `${data.displayName} er lagt til i teamet.` }
    setTimeout(() => { feedback.value = null }, 5000)
  } catch (err: unknown) {
    const status = (err as { response?: { status?: number; data?: { message?: string } } })?.response?.status
    const msg    = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
    if (status === 404) {
      addError.value = 'Ingen konto er registrert med denne e-postadressen. Be personen registrere seg først.'
    } else if (status === 409) {
      addError.value = 'Brukeren er allerede medlem av denne organisasjonen.'
    } else {
      addError.value = msg ?? 'Noe gikk galt. Prøv igjen.'
    }
  } finally {
    addSubmitLoading.value = false
  }
}

function openEdit(user: import('../api/users').User) {
  selectedUser.value = user
  editForm.value = {
    displayName: user.displayName,
    email:       user.email,
    phone:       user.phone ?? '',
    roleIds:     user.roles?.map((r) => r.roleId) ?? [],
  }
  editError.value   = null
  showEditModal.value = true
}

async function submitEdit() {
  if (!selectedUser.value || !currentOrg.value?.orgNumber) return
  if (isSelf(selectedUser.value.email) && !editForm.value.roleIds.includes(1)) {
    editError.value = 'Du kan ikke fjerne din egen admin-tilgang.'
    return
  }
  editLoading.value = true
  editError.value   = null
  try {
    await usersComposable.updateUser(selectedUser.value.userId, currentOrg.value.orgNumber, {
      displayName: editForm.value.displayName,
      email:       editForm.value.email,
      phone:       editForm.value.phone || undefined,
      roleIds:     editForm.value.roleIds,
    })
    showEditModal.value = false
    feedback.value = { type: 'success', message: 'Bruker oppdatert.' }
    setTimeout(() => { feedback.value = null }, 4000)
  } catch (err: unknown) {
    editError.value = (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Oppdatering feilet.'
  } finally {
    editLoading.value = false
  }
}

function confirmToggle(user: import('../api/users').User) {
  if (isSelf(user.email) && user.isActive) {
    feedback.value = { type: 'error', message: 'Du kan ikke deaktivere din egen bruker.' }
    return
  }
  const action   = user.isActive ? 'deaktivere' : 'aktivere'
  confirmTitle.value = user.isActive ? 'Bekreft deaktivering' : 'Bekreft aktivering'
  confirmMsg.value   = `Er du sikker på at du vil ${action} ${user.displayName}?`
  confirmAction.value = async () => {
    if (!currentOrg.value?.orgNumber) return
    await usersComposable.toggleUserStatus(user.userId, currentOrg.value.orgNumber, user.isActive)
    feedback.value = { type: 'success', message: user.isActive ? 'Bruker deaktivert.' : 'Bruker aktivert.' }
    setTimeout(() => { feedback.value = null }, 4000)
  }
  showConfirm.value = true
}

async function runConfirm() {
  if (!confirmAction.value) return
  confirmLoading.value = true
  try { await confirmAction.value() } finally {
    confirmLoading.value = false
    showConfirm.value    = false
    confirmAction.value  = null
  }
}

const availableRoles = [
  { id: 1, name: 'ADMIN',    label: 'Admin'  },
  { id: 2, name: 'MANAGER',  label: 'Leder'  },
  { id: 3, name: 'EMPLOYEE', label: 'Ansatt' },
]
</script>

<template>
  <div class="users-view">

    <!-- Header -->
    <header class="page-header">
      <div>
        <h1>Teamadministrasjon</h1>
        <p class="subtitle">Administrer tilgang, roller og teammedlemmer</p>
      </div>
      <button v-if="isAdmin" class="add-btn" type="button" @click="openAddModal">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/>
          <circle cx="9" cy="7" r="4"/>
          <line x1="19" y1="8" x2="19" y2="14"/>
          <line x1="22" y1="11" x2="16" y2="11"/>
        </svg>
        Legg til teammedlem
      </button>
    </header>

    <!-- Feedback banner -->
    <div v-if="feedback" class="feedback" :class="`feedback--${feedback.type}`" role="status">
      {{ feedback.message }}
    </div>

    <!-- Loading -->
    <div v-if="isLoading" class="loading-state">
      <BaseSpinner />
      <p>Laster brukere…</p>
    </div>

    <!-- Error -->
    <ErrorMessage v-else-if="usersError" :message="usersError" show-retry @retry="loadUsers" />

    <template v-else>

      <!-- Role summary chips -->
      <div class="role-summary">
        <button
            class="role-chip"
            :class="{ 'role-chip--active': roleFilter === 'all' }"
            @click="roleFilter = 'all'"
        >
          Alle <span class="chip-count">{{ usersList.length }}</span>
        </button>
        <button
            class="role-chip role-chip--admin"
            :class="{ 'role-chip--active': roleFilter === 'ADMIN' }"
            @click="roleFilter = roleFilter === 'ADMIN' ? 'all' : 'ADMIN'"
        >
          Admin <span class="chip-count">{{ roleCounts.ADMIN }}</span>
        </button>
        <button
            class="role-chip role-chip--manager"
            :class="{ 'role-chip--active': roleFilter === 'MANAGER' }"
            @click="roleFilter = roleFilter === 'MANAGER' ? 'all' : 'MANAGER'"
        >
          Leder <span class="chip-count">{{ roleCounts.MANAGER }}</span>
        </button>
        <button
            class="role-chip role-chip--employee"
            :class="{ 'role-chip--active': roleFilter === 'EMPLOYEE' }"
            @click="roleFilter = roleFilter === 'EMPLOYEE' ? 'all' : 'EMPLOYEE'"
        >
          Ansatt <span class="chip-count">{{ roleCounts.EMPLOYEE }}</span>
        </button>
      </div>

      <!-- Search -->
      <div class="search-row">
        <div class="search-wrap">
          <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
          </svg>
          <input
              v-model="query"
              type="search"
              class="table-search"
              placeholder="Søk etter navn eller e-post…"
          />
        </div>
      </div>

      <!-- Empty state -->
      <div v-if="filteredUsers.length === 0" class="empty-state">
        <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>
          <path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
        </svg>
        <p>Ingen brukere funnet</p>
      </div>

      <!-- User table -->
      <div v-else class="table-card app-surface">
        <table>
          <thead>
          <tr>
            <th>Bruker</th>
            <th>E-post</th>
            <th>Rolle</th>
            <th>Status</th>
            <th v-if="isAdmin" class="th-actions">Handlinger</th>
          </tr>
          </thead>
          <tbody>
          <tr
              v-for="user in filteredUsers"
              :key="user.userId"
              :class="{ 'tr--inactive': !user.isActive }"
          >
            <td>
              <div class="user-cell">
                  <span class="avatar" :class="`avatar--${roleTone(userRole(user))}`">
                    {{ initials(user.displayName) }}
                  </span>
                <div>
                  <p class="user-name">
                    {{ user.displayName }}
                    <span v-if="isSelf(user.email)" class="you-badge">deg</span>
                  </p>
                  <p v-if="user.phone" class="user-sub">{{ user.phone }}</p>
                </div>
              </div>
            </td>
            <td class="td-email">{{ user.email }}</td>
            <td>
                <span class="role-pill" :class="`role-pill--${roleTone(userRole(user))}`">
                  {{ roleLabel(userRole(user)) }}
                </span>
            </td>
            <td>
                <span class="status-pill" :class="user.isActive ? 'status-pill--active' : 'status-pill--inactive'">
                  {{ user.isActive ? 'Aktiv' : 'Inaktiv' }}
                </span>
            </td>
            <td v-if="isAdmin">
              <div class="row-actions">
                <button class="btn-ghost" @click="openEdit(user)">Rediger</button>
                <button
                    class="btn-ghost"
                    :class="user.isActive ? 'btn-ghost--warn' : 'btn-ghost--ok'"
                    @click="confirmToggle(user)"
                >
                  {{ user.isActive ? 'Deaktiver' : 'Aktiver' }}
                </button>
              </div>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

    </template>

    <BaseModal :open="showAddModal" title="Legg til teammedlem" @close="showAddModal = false">
      <div class="add-form">
        <p class="add-intro">
          Skriv inn e-postadressen til personen du vil legge til. De må allerede ha opprettet en konto.
        </p>

        <div class="form-group">
          <label for="addEmail">E-postadresse</label>
          <input
              id="addEmail"
              v-model="addEmail"
              type="email"
              placeholder="ansatt@example.no"
              autocomplete="off"
              @keydown.enter.prevent="submitAddToOrg"
          />
        </div>

        <div class="form-group">
          <label for="addRole">Rolle i organisasjonen</label>
          <select id="addRole" v-model="addRoleId">
            <option v-for="r in availableRoles" :key="r.id" :value="r.id">{{ r.label }}</option>
          </select>
        </div>

        <div v-if="addError" class="add-error">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
          {{ addError }}
        </div>
      </div>

      <template #footer>
        <button class="btn btn--ghost" @click="showAddModal = false">Avbryt</button>
        <button
            class="btn-primary"
            :disabled="addSubmitLoading || !addEmail.trim()"
            @click="submitAddToOrg"
        >
          <BaseSpinner v-if="addSubmitLoading" size="sm" />
          <span v-else>Legg til</span>
        </button>
      </template>
    </BaseModal>

    <BaseModal :open="showEditModal" title="Rediger bruker" @close="showEditModal = false">
      <form id="editUserForm" class="user-form" @submit.prevent="submitEdit">
        <div class="form-group">
          <label for="editDisplayName">Navn</label>
          <input id="editDisplayName" v-model="editForm.displayName" type="text" required />
        </div>
        <div class="form-group">
          <label for="editEmail">E-post</label>
          <input id="editEmail" v-model="editForm.email" type="email" required />
        </div>
        <div class="form-group">
          <label for="editPhone">Telefon</label>
          <input id="editPhone" v-model="editForm.phone" type="tel" />
        </div>
        <div class="form-group">
          <label for="editRole">Rolle</label>
          <select id="editRole" v-model="editForm.roleIds" multiple>
            <option v-for="r in availableRoles" :key="r.id" :value="r.id">{{ r.label }}</option>
          </select>
        </div>
        <div v-if="editError" class="add-error">{{ editError }}</div>
      </form>
      <template #footer>
        <button type="button" class="btn btn--ghost" @click="showEditModal = false">Avbryt</button>
        <button type="submit" form="editUserForm" class="btn btn--primary" :disabled="editLoading">
          <BaseSpinner v-if="editLoading" size="sm" />
          <span v-else>Lagre</span>
        </button>
      </template>
    </BaseModal>

    <BaseModal :open="showConfirm" :title="confirmTitle" @close="showConfirm = false">
      <p>{{ confirmMsg }}</p>
      <template #footer>
        <button class="btn btn--ghost" @click="showConfirm = false">Avbryt</button>
        <button class="btn btn--danger" :disabled="confirmLoading" @click="runConfirm">
          <BaseSpinner v-if="confirmLoading" size="sm" />
          <span v-else>Bekreft</span>
        </button>
      </template>
    </BaseModal>

  </div>
</template>

<style scoped>
/* 
  Bruker delte klasser fra components.css:
  - .view-page (wrapper)
  - .page-header (header layout)
  - .btn, .btn--primary, .btn--ghost, .btn--danger (knapper)
  - .app-surface (kort/flater)
  
  WCAG 2.1 AA krav:
  - Kontrast 4.5:1 for tekst
  - Kontrast 3:1 for UI-komponenter
  - Fokus-indikatorer synlige
  - Touch-targets minimum 44x44px
*/

.users-view {
  display: block;
}

.page-header h1 {
  font-family: var(--font-family-display);
  line-height: var(--line-height-heading);
  letter-spacing: -0.01em;
}

.subtitle {
  margin-top: 0.35rem;
  color: var(--color-gray-500);
  font-size: var(--font-size-sm);
}

.add-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  min-height: var(--touch-target);
  padding: var(--button-padding-md);
  background: var(--color-primary);
  color: var(--color-primary-foreground);
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: 600;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: opacity var(--transition-fast), transform var(--transition-fast);
}
.add-btn:hover { opacity: 0.88; transform: translateY(-1px); }
.add-btn:active { transform: none; }

.feedback {
  padding: 0.7rem 1rem;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: 600;
}
.feedback--success { background: var(--color-success-bg); color: var(--color-success); border: 1px solid var(--color-success); }
.feedback--error   { background: var(--color-danger-bg);  color: var(--color-danger);  border: 1px solid var(--color-danger);  }

.role-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.role-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.35rem 0.8rem;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: var(--color-card);
  box-shadow: var(--shadow-sm);
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
  font-weight: 500;
  cursor: pointer;
  transition: background var(--transition-fast), border-color var(--transition-fast), color var(--transition-fast);
}
.role-chip:hover {
  border-color: var(--color-foreground);
  box-shadow: var(--shadow-md);
}
.role-chip--active {
  background: var(--color-foreground);
  color: var(--color-primary-foreground);
  border-color: var(--color-foreground);
  box-shadow: var(--shadow-md);
}

.chip-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.25rem;
  height: 1.25rem;
  padding: 0 0.3rem;
  border-radius: 999px;
  background: color-mix(in srgb, currentColor 15%, transparent);
  font-size: 0.7rem;
  font-weight: 700;
}

.search-row { display: flex; }
.search-wrap { position: relative; flex: 1; max-width: 28rem; }
.search-icon {
  position: absolute;
  left: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--color-gray-400);
  pointer-events: none;
}
.table-search {
  width: 100%;
  padding: 0.55rem 0.75rem 0.55rem 2.25rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  box-shadow: var(--shadow-sm);
  font-size: var(--font-size-sm);
  color: var(--color-foreground);
  min-height: var(--touch-target);
  box-sizing: border-box;
}
.table-search:focus { outline: none; border-color: var(--color-focus); box-shadow: var(--shadow-focus); }

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 3.5rem 1rem;
  color: var(--color-gray-500);
}

.table-card {
  overflow: hidden;
}

table { width: 100%; border-collapse: collapse; }

th, td {
  text-align: left;
  padding: 0.8rem 1rem;
  border-bottom: 1px solid var(--color-gray-100);
  vertical-align: middle;
}

th {
  font-size: var(--font-size-xs);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-gray-500);
  background: var(--color-gray-50);
}
.th-actions { text-align: right; }

tr:last-child td { border-bottom: none; }
.tr--inactive td { opacity: 0.55; }

.user-cell { display: flex; align-items: center; gap: 0.65rem; }

.avatar {
  flex-shrink: 0;
  width: 2.2rem;
  height: 2.2rem;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-xs);
  font-weight: 700;
}
.avatar--red  { background: var(--color-danger); color: var(--color-danger-fg); }
.avatar--amber{ background: var(--color-warning-bg); color: var(--color-warning); }
.avatar--blue { background: var(--color-info-bg); color: var(--color-info); }

.user-name { font-size: var(--font-size-sm); font-weight: 500; color: var(--color-foreground); }
.user-sub  { margin-top: 0.1rem; font-size: var(--font-size-xs); color: var(--color-gray-500); }

.you-badge {
  margin-left: 0.4rem;
  padding: 0.1rem 0.4rem;
  border-radius: 999px;
  background: var(--color-info-bg);
  color: var(--color-info);
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  vertical-align: middle;
}

.td-email { font-size: var(--font-size-sm); color: var(--color-gray-600); }

.role-pill {
  display: inline-flex;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-size: var(--font-size-xs);
  font-weight: 600;
}
.role-pill--red   { background: var(--color-danger);     color: var(--color-danger-fg); }
.role-pill--amber { background: var(--color-warning-bg); color: var(--color-warning);   }
.role-pill--blue  { background: var(--color-info-bg);    color: var(--color-info);      }

.status-pill {
  display: inline-flex;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-size: var(--font-size-xs);
  font-weight: 600;
}
.status-pill--active   { background: var(--color-success-bg); color: var(--color-success); }
.status-pill--inactive { background: var(--color-gray-100);   color: var(--color-gray-500); }

.row-actions { display: flex; gap: 0.35rem; justify-content: flex-end; }

.btn-ghost {
  min-height: 2.1rem;
  padding: 0.3rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-card);
  color: var(--color-gray-700);
  font-size: var(--font-size-xs);
  font-weight: 600;
  cursor: pointer;
  transition: background var(--transition-fast);
}
.btn-ghost:hover { background: var(--color-gray-50); }
.btn-ghost--warn { color: var(--color-danger); border-color: color-mix(in srgb, var(--color-danger) 30%, var(--color-border)); }
.btn-ghost--ok   { color: var(--color-success); border-color: color-mix(in srgb, var(--color-success) 30%, var(--color-border)); }

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  min-height: 2.25rem;
  padding: 0.4rem 1rem;
  background: var(--color-primary);
  color: var(--color-primary-foreground);
  border: none;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  font-weight: 600;
  cursor: pointer;
}
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-primary:not(:disabled):hover { opacity: 0.88; }

.btn-danger {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  min-height: 2.25rem;
  padding: 0.4rem 1rem;
  background: var(--color-danger);
  color: var(--color-primary-foreground);
  border: none;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  font-weight: 600;
  cursor: pointer;
}
.btn-danger:disabled { opacity: 0.6; cursor: not-allowed; }

.loading-state {
  display: flex; flex-direction: column; align-items: center;
  gap: 1rem; padding: 3rem; color: var(--color-gray-500);
}

.add-form { display: flex; flex-direction: column; gap: 1rem; }

.add-intro {
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
  line-height: 1.55;
  margin: 0;
}

.user-form { display: flex; flex-direction: column; gap: 1rem; }

.form-group { display: flex; flex-direction: column; gap: 0.3rem; }
.form-group label {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--color-gray-800);
}
.form-group input,
.form-group select {
  min-height: 2.5rem;
  padding: 0 0.75rem;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  background: var(--color-surface-muted);
  color: var(--color-foreground);
  box-sizing: border-box;
  width: 100%;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}
.form-group input:focus,
.form-group select:focus { outline: none; border-color: var(--color-focus); background: var(--color-surface-raised); box-shadow: var(--shadow-focus); }
.form-group select[multiple] { min-height: 6rem; padding: 0.5rem; }

.add-error {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: var(--color-danger-bg);
  color: var(--color-danger);
  border: 1px solid color-mix(in srgb, var(--color-danger) 28%, var(--color-border));
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  line-height: 1.5;
}
.add-error svg { flex-shrink: 0; margin-top: 0.1rem; }

@media (max-width: 48rem) {
  .page-header { flex-direction: column; align-items: flex-start; }
  .add-btn { width: 100%; justify-content: center; }
  th:nth-child(5), td:nth-child(5) { display: none; }  /* hide phone col */
  .td-email { max-width: 8rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
}
</style>