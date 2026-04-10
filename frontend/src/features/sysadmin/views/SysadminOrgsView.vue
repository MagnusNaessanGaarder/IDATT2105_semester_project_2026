<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { client } from '@/api/client'

//  types 
interface Organization {
  orgNumber: number
  legalName: string
  displayName: string | null
  contactEmail: string | null
  contactPhone: string | null
  isActive: boolean
  createdAt?: string
}

//  state 
const orgs      = ref<Organization[]>([])
const loading   = ref(false)
const error     = ref<string | null>(null)
const feedback  = ref<{ type: 'success' | 'error'; msg: string } | null>(null)
const search    = ref('')

// modal
const modalOpen  = ref(false)
const modalMode  = ref<'add' | 'edit'>('add')
const saving     = ref(false)
const modalError = ref<string | null>(null)

const form = reactive({
  orgNumber:    '' as string | number,
  legalName:    '',
  displayName:  '',
  contactEmail: '',
  contactPhone: '',
  isActive:     true,
})

// confirm deactivate
const confirmOrg  = ref<Organization | null>(null)
const confirming  = ref(false)

//  derived 
const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return orgs.value
  return orgs.value.filter((o) =>
      String(o.orgNumber).includes(q) ||
      o.legalName.toLowerCase().includes(q) ||
      (o.displayName ?? '').toLowerCase().includes(q) ||
      (o.contactEmail ?? '').toLowerCase().includes(q)
  )
})

const activeCount   = computed(() => orgs.value.filter((o) => o.isActive).length)
const inactiveCount = computed(() => orgs.value.filter((o) => !o.isActive).length)

//  API 
async function fetchOrgs() {
  loading.value = true
  error.value   = null
  try {
    const { data } = await client.get<Organization[]>('/sysadmin/organizations')
    orgs.value = data.sort((a, b) => a.legalName.localeCompare(b.legalName, 'nb'))
  } catch {
    error.value = 'Kunne ikke hente organisasjoner.'
  } finally {
    loading.value = false
  }
}

function flash(type: 'success' | 'error', msg: string) {
  feedback.value = { type, msg }
  setTimeout(() => { feedback.value = null }, 5000)
}

//  modal 
function openAdd() {
  modalMode.value   = 'add'
  modalError.value  = null
  form.orgNumber    = ''
  form.legalName    = ''
  form.displayName  = ''
  form.contactEmail = ''
  form.contactPhone = ''
  form.isActive     = true
  modalOpen.value   = true
}

function openEdit(org: Organization) {
  modalMode.value   = 'edit'
  modalError.value  = null
  form.orgNumber    = org.orgNumber
  form.legalName    = org.legalName
  form.displayName  = org.displayName ?? ''
  form.contactEmail = org.contactEmail ?? ''
  form.contactPhone = org.contactPhone ?? ''
  form.isActive     = org.isActive
  modalOpen.value   = true
}

function closeModal() { modalOpen.value = false }

async function save() {
  if (!form.legalName.trim()) { modalError.value = 'Juridisk navn er påkrevd.'; return }
  if (modalMode.value === 'add' && !String(form.orgNumber).trim()) {
    modalError.value = 'Organisasjonsnummer er påkrevd.'
    return
  }

  saving.value     = true
  modalError.value = null

  try {
    const payload = {
      orgNumber:    Number(form.orgNumber),
      legalName:    form.legalName.trim(),
      displayName:  form.displayName.trim() || form.legalName.trim(),
      contactEmail: form.contactEmail.trim() || null,
      contactPhone: form.contactPhone.trim() || null,
      isActive:     form.isActive,
    }

    if (modalMode.value === 'add') {
      const { data } = await client.post<Organization>('/sysadmin/organizations', payload)
      orgs.value = [...orgs.value, data].sort((a, b) => a.legalName.localeCompare(b.legalName, 'nb'))
      flash('success', `«${data.legalName}» (${data.orgNumber}) er registrert.`)
    } else {
      const { data } = await client.put<Organization>(
          `/sysadmin/organizations/${form.orgNumber}`, payload
      )
      orgs.value = orgs.value
          .map((o) => o.orgNumber === data.orgNumber ? data : o)
          .sort((a, b) => a.legalName.localeCompare(b.legalName, 'nb'))
      flash('success', `«${data.legalName}» er oppdatert.`)
    }
    closeModal()
  } catch (err: unknown) {
    const status = (err as { response?: { status?: number } })?.response?.status
    const msg    = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
    if (status === 409) {
      modalError.value = 'Et organisasjonsnummer med dette nummeret finnes allerede.'
    } else {
      modalError.value = msg ?? 'Lagring feilet. Prøv igjen.'
    }
  } finally {
    saving.value = false
  }
}

//  deactivate 
function askDeactivate(org: Organization) { confirmOrg.value = org }

async function confirmDeactivate() {
  if (!confirmOrg.value) return
  confirming.value = true
  const org = confirmOrg.value
  try {
    await client.delete(`/sysadmin/organizations/${org.orgNumber}`)
    orgs.value = orgs.value.map((o) =>
        o.orgNumber === org.orgNumber ? { ...o, isActive: false } : o
    )
    flash('success', `«${org.legalName}» er deaktivert.`)
    confirmOrg.value = null
  } catch {
    flash('error', 'Deaktivering feilet. Prøv igjen.')
    confirmOrg.value = null
  } finally {
    confirming.value = false
  }
}

function fmtDate(iso?: string) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('nb-NO', { day: '2-digit', month: 'short', year: 'numeric' })
}

onMounted(fetchOrgs)
</script>

<template>
  <div class="sysadmin-orgs">

    <!-- Header -->
    <header class="page-header">
      <div>
        <h1>Organisasjoner</h1>
        <p class="subtitle">Registrer, rediger og deaktiver organisasjoner på plattformen</p>
      </div>
      <button class="add-btn" type="button" @click="openAdd">
        + Ny organisasjon
      </button>
    </header>

    <!-- Feedback -->
    <div v-if="feedback" class="feedback" :class="`feedback--${feedback.type}`" role="status">
      {{ feedback.msg }}
    </div>

    <!-- Error -->
    <div v-if="error" class="inline-error">{{ error }}</div>

    <!-- Stats -->
    <div class="stats-row">
      <div class="stat">
        <strong>{{ orgs.length }}</strong>
        <span>Totalt</span>
      </div>
      <div class="stat stat--ok">
        <strong>{{ activeCount }}</strong>
        <span>Aktive</span>
      </div>
      <div class="stat stat--off">
        <strong>{{ inactiveCount }}</strong>
        <span>Inaktive</span>
      </div>
    </div>

    <!-- Search -->
    <div class="search-wrap">
      <svg class="search-icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
      </svg>
      <input v-model="search" type="search" class="search-input" placeholder="Søk på navn, nummer eller e-post…" />
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading">Laster organisasjoner…</div>

    <!-- Empty -->
    <div v-else-if="orgs.length === 0" class="empty">
      <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
        <polyline points="9 22 9 12 15 12 15 22"/>
      </svg>
      <p>Ingen organisasjoner registrert ennå.</p>
      <button class="add-btn" type="button" @click="openAdd">Registrer første organisasjon</button>
    </div>

    <!-- No results -->
    <div v-else-if="filtered.length === 0" class="empty">
      <p>Ingen treff for «{{ search }}».</p>
    </div>

    <!-- Table -->
    <div v-else class="table-card">
      <table>
        <thead>
        <tr>
          <th>Org.nr</th>
          <th>Juridisk navn</th>
          <th>Visningsnavn</th>
          <th>Kontakt</th>
          <th>Registrert</th>
          <th>Status</th>
          <th class="th-actions"></th>
        </tr>
        </thead>
        <tbody>
        <tr
            v-for="org in filtered"
            :key="org.orgNumber"
            :class="{ 'tr--inactive': !org.isActive }"
        >
          <td class="td-orgnr">{{ org.orgNumber }}</td>
          <td>
            <p class="cell-name">{{ org.legalName }}</p>
          </td>
          <td class="td-secondary">{{ org.displayName || '—' }}</td>
          <td class="td-secondary">
            <p v-if="org.contactEmail">{{ org.contactEmail }}</p>
            <p v-if="org.contactPhone" class="td-phone">{{ org.contactPhone }}</p>
            <span v-if="!org.contactEmail && !org.contactPhone">—</span>
          </td>
          <td class="td-secondary">{{ fmtDate(org.createdAt) }}</td>
          <td>
              <span class="status-pill" :class="org.isActive ? 'status-pill--ok' : 'status-pill--off'">
                {{ org.isActive ? 'Aktiv' : 'Inaktiv' }}
              </span>
          </td>
          <td class="td-actions">
            <button class="row-btn" type="button" @click="openEdit(org)">Rediger</button>
            <button
                v-if="org.isActive"
                class="row-btn row-btn--danger"
                type="button"
                @click="askDeactivate(org)"
            >
              Deaktiver
            </button>
          </td>
        </tr>
        </tbody>
      </table>
    </div>

    <!--  Add / Edit modal  -->
    <Teleport to="body">
      <div v-if="modalOpen" class="overlay" @click.self="closeModal">
        <div class="modal" role="dialog" aria-modal="true">
          <header class="modal__header">
            <h2>{{ modalMode === 'add' ? 'Registrer organisasjon' : 'Rediger organisasjon' }}</h2>
            <button class="modal__close" type="button" @click="closeModal">✕</button>
          </header>

          <div class="modal__body">
            <div class="field">
              <label for="f-orgnr">
                Organisasjonsnummer <span class="req">*</span>
              </label>
              <input
                  id="f-orgnr"
                  v-model="form.orgNumber"
                  type="text"
                  inputmode="numeric"
                  placeholder="9 siffer, f.eks. 123456789"
                  :disabled="modalMode === 'edit'"
                  maxlength="9"
              />
            </div>

            <div class="field">
              <label for="f-legal">Juridisk navn <span class="req">*</span></label>
              <input
                  id="f-legal"
                  v-model="form.legalName"
                  type="text"
                  placeholder="Foretakets fulle juridiske navn"
                  maxlength="255"
              />
            </div>

            <div class="field">
              <label for="f-display">Visningsnavn</label>
              <input
                  id="f-display"
                  v-model="form.displayName"
                  type="text"
                  placeholder="Kort navn vist i appen (valgfritt)"
                  maxlength="255"
              />
            </div>

            <div class="field-row">
              <div class="field">
                <label for="f-email">Kontakt-e-post</label>
                <input
                    id="f-email"
                    v-model="form.contactEmail"
                    type="email"
                    placeholder="post@virksomhet.no"
                />
              </div>
              <div class="field">
                <label for="f-phone">Kontakttelefon</label>
                <input
                    id="f-phone"
                    v-model="form.contactPhone"
                    type="tel"
                    placeholder="+47 000 00 000"
                />
              </div>
            </div>

            <div v-if="modalMode === 'edit'" class="field">
              <label for="f-active">Status</label>
              <select id="f-active" v-model="form.isActive">
                <option :value="true">Aktiv</option>
                <option :value="false">Inaktiv</option>
              </select>
            </div>

            <p v-if="modalError" class="form-error">{{ modalError }}</p>
          </div>

          <footer class="modal__footer">
            <button class="btn-ghost" type="button" @click="closeModal">Avbryt</button>
            <button
                class="btn-primary"
                type="button"
                :disabled="saving"
                @click="save"
            >
              {{ saving ? 'Lagrer…' : modalMode === 'add' ? 'Registrer' : 'Lagre' }}
            </button>
          </footer>
        </div>
      </div>
    </Teleport>

    <!--  Confirm deactivate  -->
    <Teleport to="body">
      <div v-if="confirmOrg" class="overlay" @click.self="confirmOrg = null">
        <div class="modal modal--sm" role="dialog" aria-modal="true">
          <header class="modal__header">
            <h2>Deaktiver organisasjon</h2>
            <button class="modal__close" type="button" @click="confirmOrg = null">✕</button>
          </header>
          <div class="modal__body">
            <p class="confirm-msg">
              Er du sikker på at du vil deaktivere
              <strong>«{{ confirmOrg.legalName }}»</strong> ({{ confirmOrg.orgNumber }})?
              Brukerne deres mister tilgang til systemet.
            </p>
          </div>
          <footer class="modal__footer">
            <button class="btn-ghost" type="button" @click="confirmOrg = null">Avbryt</button>
            <button class="btn-danger" type="button" :disabled="confirming" @click="confirmDeactivate">
              {{ confirming ? 'Deaktiverer…' : 'Deaktiver' }}
            </button>
          </footer>
        </div>
      </div>
    </Teleport>

  </div>
</template>

<style scoped>
.sysadmin-orgs {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/*  Header  */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 1rem;
}
.page-header h1 {
  margin: 0;
  font-size: clamp(1.6rem, 2.4vw, var(--font-size-3xl));
  font-weight: 700;
  letter-spacing: -0.015em;
}
.subtitle {
  margin: 0.3rem 0 0;
  color: var(--color-gray-500);
  font-size: var(--font-size-sm);
}
.add-btn {
  min-height: var(--touch-target);
  padding: var(--button-padding-md);
  background: var(--color-primary);
  color: var(--color-primary-foreground);
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
}
.add-btn:hover { opacity: 0.88; }

/*  Feedback  */
.feedback {
  padding: 0.7rem 1rem;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: 600;
}
.feedback--success { background: var(--color-success-bg); color: var(--color-success); border: 1px solid var(--color-success); }
.feedback--error   { background: var(--color-danger-bg);  color: var(--color-danger);  border: 1px solid var(--color-danger); }
.inline-error {
  padding: 0.65rem 0.9rem;
  background: var(--color-danger-bg);
  color: var(--color-danger);
  border: 1px solid color-mix(in srgb, var(--color-danger) 30%, var(--color-border));
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
}

/*  Stats  */
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.75rem;
}
.stat {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  padding: 0.85rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}
.stat strong { font-size: 1.7rem; font-weight: 700; color: var(--color-foreground); }
.stat span   { font-size: var(--font-size-xs); color: var(--color-gray-500); }
.stat--ok    { border-left: 3px solid var(--color-success); }
.stat--off   { border-left: 3px solid var(--color-gray-300); }

/*  Search  */
.search-wrap {
  position: relative;
  max-width: 28rem;
}
.search-icon {
  position: absolute;
  left: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--color-gray-400);
  pointer-events: none;
}
.search-input {
  width: 100%;
  min-height: var(--touch-target);
  padding: 0 0.75rem 0 2.25rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  font-size: var(--font-size-sm);
  color: var(--color-foreground);
  box-sizing: border-box;
}
.search-input:focus { outline: none; border-color: var(--color-focus); box-shadow: var(--shadow-focus); }

/*  Loading / empty  */
.loading {
  padding: 3rem 1rem;
  text-align: center;
  color: var(--color-gray-400);
  font-size: var(--font-size-sm);
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 4rem 1rem;
  color: var(--color-gray-400);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-lg);
}
.empty p { margin: 0; font-size: var(--font-size-sm); }

/*  Table  */
.table-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-card);
  box-shadow: var(--shadow-sm);
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
tr:last-child td { border-bottom: none; }
.tr--inactive { opacity: 0.5; }

.td-orgnr   { font-size: var(--font-size-sm); font-weight: 600; color: var(--color-gray-700); font-variant-numeric: tabular-nums; white-space: nowrap; }
.cell-name  { font-size: var(--font-size-sm); font-weight: 500; margin: 0; }
.td-secondary { font-size: var(--font-size-sm); color: var(--color-gray-600); }
.td-phone   { color: var(--color-gray-500); font-size: var(--font-size-xs); margin: 0.1rem 0 0; }
.th-actions { width: 1px; }
.td-actions { white-space: nowrap; text-align: right; }

.status-pill {
  display: inline-flex;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-size: var(--font-size-xs);
  font-weight: 600;
}
.status-pill--ok  { background: var(--color-success-bg); color: var(--color-success); }
.status-pill--off { background: var(--color-gray-100);   color: var(--color-gray-500); }

.row-btn {
  min-height: 2rem;
  padding: 0.25rem 0.7rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-card);
  color: var(--color-gray-700);
  font-size: var(--font-size-xs);
  font-weight: 600;
  cursor: pointer;
  margin-left: 0.3rem;
}
.row-btn:hover { background: var(--color-gray-50); }
.row-btn--danger { color: var(--color-danger); border-color: color-mix(in srgb, var(--color-danger) 30%, var(--color-border)); }
.row-btn--danger:hover { background: var(--color-danger-bg); }

/*  Modal  */
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 1rem;
}
.modal {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
  width: 100%;
  max-width: 28rem;
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  overflow-y: auto;
}
.modal--sm { max-width: 22rem; }

.modal__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.25rem 0.85rem;
  border-bottom: 1px solid var(--color-border);
}
.modal__header h2 { margin: 0; font-size: var(--font-size-base); font-weight: 600; }
.modal__close {
  width: 1.75rem; height: 1.75rem;
  border: none; background: none;
  color: var(--color-gray-400);
  border-radius: var(--radius-sm);
  cursor: pointer; font-size: 0.9rem;
  display: flex; align-items: center; justify-content: center;
}
.modal__close:hover { background: var(--color-gray-100); color: var(--color-foreground); }

.modal__body {
  padding: 1rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}
.modal__footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  border-top: 1px solid var(--color-border);
}

/*  Form  */
.field { display: flex; flex-direction: column; gap: 0.3rem; }
.field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
.field label {
  font-size: var(--font-size-xs);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-gray-500);
}
.req { color: var(--color-danger); }
.field input,
.field select {
  min-height: 2.5rem;
  padding: 0 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-family: inherit;
  background: var(--color-background);
  color: var(--color-foreground);
  box-sizing: border-box;
  width: 100%;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
}
.field input:focus,
.field select:focus { outline: none; border-color: var(--color-focus); box-shadow: var(--shadow-focus); background: var(--color-card); }
.field input:disabled { opacity: 0.55; cursor: not-allowed; }
.field select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
  padding-right: 2rem;
  cursor: pointer;
}
.form-error {
  padding: 0.55rem 0.75rem;
  background: var(--color-danger-bg);
  color: var(--color-danger);
  border: 1px solid color-mix(in srgb, var(--color-danger) 25%, var(--color-border));
  border-radius: var(--radius-md);
  font-size: var(--font-size-xs);
  font-weight: 500;
  margin: 0;
}
.confirm-msg {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-gray-700);
  line-height: 1.55;
}

/*  Buttons  */
.btn-ghost {
  min-height: 2.25rem;
  padding: 0.4rem 1rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  color: var(--color-gray-700);
  font-size: var(--font-size-sm);
  font-weight: 500;
  cursor: pointer;
}
.btn-ghost:hover { background: var(--color-gray-50); }
.btn-primary {
  min-height: 2.25rem;
  padding: 0.4rem 1.25rem;
  border: none;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: var(--color-primary-foreground);
  font-size: var(--font-size-sm);
  font-weight: 600;
  cursor: pointer;
}
.btn-primary:disabled { opacity: 0.45; cursor: not-allowed; }
.btn-primary:not(:disabled):hover { opacity: 0.88; }
.btn-danger {
  min-height: 2.25rem;
  padding: 0.4rem 1.25rem;
  border: none;
  border-radius: var(--radius-md);
  background: #dc2626;
  color: #fff;
  font-size: var(--font-size-sm);
  font-weight: 600;
  cursor: pointer;
}
.btn-danger:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-danger:not(:disabled):hover { background: #b91c1c; }

/*  Responsive  */
@media (max-width: 48rem) {
  .page-header { flex-direction: column; align-items: flex-start; }
  .add-btn { width: 100%; text-align: center; }
  .field-row { grid-template-columns: 1fr; }
  th:nth-child(3), td:nth-child(3),
  th:nth-child(5), td:nth-child(5) { display: none; }
}
</style>