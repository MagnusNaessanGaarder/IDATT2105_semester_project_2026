<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useCertifications } from '@/features/ik-alkohol/composables/useCertifications'
import { getCertificationCatalog, type CertificationCatalogItem } from '@/features/ik-alkohol/api/certificationCatalog'
import { getUsers } from '@/features/admin/api/users'
import type { CertificationRecord } from '@/features/ik-alkohol/composables/useAlkoholData'
import { useAuthStore } from '@/stores/auth'

// Extended type: backend now serializes userId directly alongside the user object
type TrainingRecordWithUserId = CertificationRecord & { userId?: number | null }

const {
  items: records,
  isLoading,
  isSubmitting,
  error: certError,
  loadItems,
  addItem,
  editItem,
  removeItem,
} = useCertifications()

const authStore = useAuthStore()
const orgNumber = computed(() => authStore.currentOrg?.orgNumber)

const users     = ref<Array<{ userId: number; displayName: string; email: string }>>([])
const catalog   = ref<CertificationCatalogItem[]>([])
const feedback  = ref<{ type: 'success' | 'error'; msg: string } | null>(null)

const typeFilter   = ref('ALL')
const statusFilter = ref('ALL')
const sortDir      = ref<'asc' | 'desc'>('asc')

const modalOpen  = ref(false)
const modalMode  = ref<'add' | 'edit'>('add')
const editingId  = ref<number | null>(null)
const modalError = ref<string | null>(null)

const form = reactive({
  userId:           null as number | null,
  trainingType:     '',
  title:            '',
  completedAtDate:  '',
  expiresAtDate:    '',
  notes:            '',
})

const confirmId = ref<number | null>(null)

// Fallback labels when catalog endpoint is unavailable (e.g. alcohol module disabled)
const FALLBACK_TYPE_LABELS: Record<string, string> = {
  FOOD_HYGIENE:                'Mathygiene',
  ALLERGEN_HANDLING:           'Allergenhåndtering',
  TEMPERATURE_CONTROL:         'Temperaturkontroll',
  CLEANING_ROUTINES:           'Rengjøringsrutiner',
  RESPONSIBLE_ALCOHOL_SERVICE: 'Ansvarlig alkoholservering',
  AGE_VERIFICATION:            'Alderskontroll',
  OTHER:                       'Annet',
}

const typeLabels = computed<Record<string, string>>(() => {
  if (catalog.value.length > 0)
    return Object.fromEntries(catalog.value.map((c) => [c.trainingType, c.displayName]))
  return FALLBACK_TYPE_LABELS
})

// Map userId → displayName from the separately fetched user list
const userMap = computed(() =>
    new Map(users.value.map((u) => [u.userId, u.displayName]))
)

// Resolve a record's display name: prefer the loaded users list, fall back to r.user
function resolveUserName(r: CertificationRecord): string {
  // Use direct userId field (serialized from FK) if the lazy user object is null
  const rec = r as TrainingRecordWithUserId
  const id = rec.userId ?? r.user?.userId
  if (id && userMap.value.has(id)) return userMap.value.get(id)!
  return r.user?.displayName ?? '—'
}

const filtered = computed(() => {
  let result = [...records.value]
  if (typeFilter.value !== 'ALL') result = result.filter((r) => r.trainingType === typeFilter.value)
  if (statusFilter.value !== 'ALL') result = result.filter((r) => r.status === statusFilter.value)
  result.sort((a, b) => {
    const da = a.expiresAt ?? '9999'
    const db = b.expiresAt ?? '9999'
    return sortDir.value === 'asc' ? da.localeCompare(db) : db.localeCompare(da)
  })
  return result
})

const expiredCount = computed(() => records.value.filter((r) => r.status === 'EXPIRED').length)
const expiringSoon = computed(() => {
  const cutoff = new Date()
  cutoff.setDate(cutoff.getDate() + 30)
  return records.value.filter((r) => {
    if (!r.expiresAt || r.status === 'EXPIRED') return false
    return new Date(r.expiresAt) <= cutoff && new Date(r.expiresAt) > new Date()
  }).length
})

async function loadAll() {
  if (!orgNumber.value) return
  await Promise.all([loadItems(orgNumber.value), loadCatalog(), loadUsers()])
}

async function loadCatalog() {
  if (!orgNumber.value) return
  const result = await getCertificationCatalog(orgNumber.value)
  catalog.value = result.ok ? result.data : []
}

async function loadUsers() {
  if (!orgNumber.value) return
  try {
    const data = await getUsers(orgNumber.value)
    users.value = data.filter((u) => u.isActive)
  } catch {
    users.value = []
  }
}

function openAdd() {
  modalMode.value = 'add'; editingId.value = null; modalError.value = null
  form.userId = null; form.trainingType = catalog.value[0]?.trainingType ?? ''
  form.title = typeLabels.value[form.trainingType] ?? ''
  form.completedAtDate = ''; form.expiresAtDate = ''; form.notes = ''
  modalOpen.value = true
}

function openEdit(r: CertificationRecord) {
  modalMode.value = 'edit'; editingId.value = r.trainingRecordId; modalError.value = null
  form.userId = r.user?.userId ?? null; form.trainingType = r.trainingType
  form.title = r.title
  form.completedAtDate = r.completedAt ? r.completedAt.slice(0, 10) : ''
  form.expiresAtDate   = r.expiresAt   ? r.expiresAt.slice(0, 10)   : ''
  form.notes = r.notes ?? ''
  modalOpen.value = true
}

function closeModal() { modalOpen.value = false }

function onTypeChange() {
  if (modalMode.value === 'add') form.title = typeLabels.value[form.trainingType] ?? ''
}

async function save() {
  if (!orgNumber.value || !form.userId || !form.trainingType || !form.title.trim()) {
    modalError.value = 'Velg ansatt, type og fyll inn tittel.'; return
  }
  modalError.value = null
  const payload = {
    userId: form.userId, trainingType: form.trainingType, title: form.title.trim(),
    completedAt: form.completedAtDate ? `${form.completedAtDate}T00:00:00` : null,
    expiresAt:   form.expiresAtDate   ? `${form.expiresAtDate}T00:00:00`   : null,
    notes: form.notes.trim() || undefined,
  }
  const ok = modalMode.value === 'add'
      ? await addItem(payload, orgNumber.value)
      : await editItem(editingId.value!, payload, orgNumber.value)
  if (ok) { flash('success', modalMode.value === 'add' ? 'Sertifikat tildelt.' : 'Registrering oppdatert.'); closeModal() }
  else    { modalError.value = certError.value ?? 'Lagring feilet.' }
}

async function deleteRecord() {
  if (!orgNumber.value || confirmId.value === null) return
  const id = confirmId.value
  const title = records.value.find((r) => r.trainingRecordId === id)?.title
  const ok = await removeItem(id, orgNumber.value)
  confirmId.value = null
  if (ok) flash('success', `«${title}» slettet.`)
  else    flash('error', 'Sletting feilet.')
}

function flash(type: 'success' | 'error', msg: string) {
  feedback.value = { type, msg }
  setTimeout(() => { feedback.value = null }, 4500)
}

function fmtDate(iso: string | null) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('nb-NO', { day: '2-digit', month: 'short', year: 'numeric' })
}

function statusTone(s: string) { return s === 'COMPLETED' ? 'ok' : s === 'EXPIRED' ? 'danger' : 'neutral' }
function statusLabel(s: string) { return s === 'COMPLETED' ? 'Fullført' : s === 'EXPIRED' ? 'Utløpt' : 'Tildelt' }

function expiryTone(iso: string | null) {
  if (!iso) return ''
  const diff = (new Date(iso).getTime() - Date.now()) / 86_400_000
  return diff < 0 ? 'expired' : diff < 30 ? 'soon' : ''
}

function toggleSort() { sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc' }

onMounted(loadAll)
</script>

<template>
  <div class="training-view">
    <header class="page-header">
      <div>
        <h1>Opplæring og sertifikater</h1>
        <p class="subtitle">Administrer sertifikater og opplæringsregistreringer for organisasjonen</p>
      </div>
      <button class="add-btn" type="button" @click="openAdd">+ Tildel sertifikat</button>
    </header>

    <div v-if="feedback" class="feedback" :class="`feedback--${feedback.type}`" role="status">{{ feedback.msg }}</div>
    <div v-if="certError" class="inline-error">{{ certError }}</div>

    <div class="stats-row">
      <div class="stat"><strong>{{ records.length }}</strong><span>Totalt</span></div>
      <div class="stat stat--ok"><strong>{{ records.filter(r => r.status === 'COMPLETED').length }}</strong><span>Fullførte</span></div>
      <div class="stat stat--warn"><strong>{{ expiringSoon }}</strong><span>Utløper snart</span></div>
      <div class="stat stat--danger"><strong>{{ expiredCount }}</strong><span>Utløpte</span></div>
    </div>

    <div class="filter-row">
      <div class="filter-group">
        <span class="filter-label">Type</span>
        <div class="chip-row">
          <button class="chip" :class="{ 'chip--active': typeFilter === 'ALL' }" type="button" @click="typeFilter = 'ALL'">Alle</button>
          <button v-for="(label, value) in typeLabels" :key="value" class="chip" :class="{ 'chip--active': typeFilter === value }" type="button" @click="typeFilter = value">{{ label }}</button>
        </div>
      </div>
      <div class="filter-group">
        <span class="filter-label">Status</span>
        <div class="chip-row">
          <button class="chip" :class="{ 'chip--active': statusFilter === 'ALL' }" type="button" @click="statusFilter = 'ALL'">Alle</button>
          <button class="chip" :class="{ 'chip--active': statusFilter === 'ASSIGNED' }" type="button" @click="statusFilter = 'ASSIGNED'">Tildelt</button>
          <button class="chip" :class="{ 'chip--active': statusFilter === 'COMPLETED' }" type="button" @click="statusFilter = 'COMPLETED'">Fullført</button>
          <button class="chip chip--danger" :class="{ 'chip--active': statusFilter === 'EXPIRED' }" type="button" @click="statusFilter = 'EXPIRED'">Utløpt</button>
        </div>
      </div>
    </div>

    <div v-if="isLoading" class="loading">Laster opplæringsregistreringer…</div>
    <div v-else-if="records.length === 0" class="empty">
      <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/></svg>
      <p>Ingen opplæringsregistreringer ennå.</p>
      <button class="add-btn" type="button" @click="openAdd">Tildel første sertifikat</button>
    </div>
    <div v-else-if="filtered.length === 0" class="empty"><p>Ingen registreringer matcher gjeldende filter.</p></div>

    <div v-else class="table-card">
      <table>
        <thead>
        <tr>
          <th>Ansatt</th><th>Type</th><th>Tittel</th><th>Fullført</th>
          <th><button class="sort-btn" type="button" @click="toggleSort">Utløper <span>{{ sortDir === 'asc' ? '↑' : '↓' }}</span></button></th>
          <th>Status</th><th class="th-actions"></th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="r in filtered" :key="r.trainingRecordId">
          <td><p class="cell-name">{{ resolveUserName(r) }}</p><p class="cell-sub">{{ r.user?.email ?? '' }}</p></td>
          <td><span class="type-pill">{{ typeLabels[r.trainingType] ?? r.trainingType }}</span></td>
          <td class="td-title">{{ r.title }}</td>
          <td class="td-meta">{{ fmtDate(r.completedAt) }}</td>
          <td><span class="expiry-val" :class="{ 'expiry-val--expired': expiryTone(r.expiresAt) === 'expired', 'expiry-val--soon': expiryTone(r.expiresAt) === 'soon' }">{{ fmtDate(r.expiresAt) }}</span></td>
          <td><span class="status-pill" :class="`status-pill--${statusTone(r.status)}`">{{ statusLabel(r.status) }}</span></td>
          <td class="td-actions">
            <button class="row-btn" type="button" @click="openEdit(r)">Rediger</button>
            <button class="row-btn row-btn--danger" type="button" @click="confirmId = r.trainingRecordId">Slett</button>
          </td>
        </tr>
        </tbody>
      </table>
    </div>

    <Teleport to="body">
      <div v-if="modalOpen" class="overlay" @click.self="closeModal">
        <div class="modal" role="dialog" aria-modal="true">
          <header class="modal__header">
            <h2>{{ modalMode === 'add' ? 'Tildel sertifikat' : 'Rediger registrering' }}</h2>
            <button class="modal__close" type="button" @click="closeModal">✕</button>
          </header>
          <div class="modal__body">
            <div class="field">
              <label for="tr-user">Ansatt <span class="req">*</span></label>
              <select id="tr-user" :value="form.userId ?? ''" :disabled="modalMode === 'edit'" @change="form.userId = Number(($event.target as HTMLSelectElement).value) || null">
                <option value="" disabled>— Velg ansatt —</option>
                <option v-for="u in users" :key="u.userId" :value="u.userId">{{ u.displayName }} ({{ u.email }})</option>
              </select>
            </div>
            <div class="field-row">
              <div class="field">
                <label for="tr-type">Type <span class="req">*</span></label>
                <select id="tr-type" v-model="form.trainingType" @change="onTypeChange">
                  <option value="" disabled>— Velg type —</option>
                  <option v-for="(label, value) in typeLabels" :key="value" :value="value">{{ label }}</option>
                </select>
              </div>
              <div class="field">
                <label for="tr-title">Tittel <span class="req">*</span></label>
                <input id="tr-title" v-model="form.title" type="text" placeholder="f.eks. Matkurs 2026" maxlength="255" />
              </div>
            </div>
            <div class="field-row">
              <div class="field"><label for="tr-completed">Fullføringsdato</label><input id="tr-completed" v-model="form.completedAtDate" type="date" /></div>
              <div class="field"><label for="tr-expires">Utløpsdato</label><input id="tr-expires" v-model="form.expiresAtDate" type="date" /></div>
            </div>
            <div class="field"><label for="tr-notes">Notater</label><textarea id="tr-notes" v-model="form.notes" rows="2" placeholder="Valgfrie kommentarer" maxlength="2000" /></div>
            <p v-if="modalError" class="form-error">{{ modalError }}</p>
          </div>
          <footer class="modal__footer">
            <button class="btn-ghost" type="button" @click="closeModal">Avbryt</button>
            <button class="btn-primary" type="button" :disabled="isSubmitting" @click="save">{{ isSubmitting ? 'Lagrer…' : modalMode === 'add' ? 'Tildel' : 'Lagre' }}</button>
          </footer>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="confirmId !== null" class="overlay" @click.self="confirmId = null">
        <div class="modal modal--sm" role="dialog" aria-modal="true">
          <header class="modal__header">
            <h2>Slett registrering</h2>
            <button class="modal__close" type="button" @click="confirmId = null">✕</button>
          </header>
          <div class="modal__body">
            <p class="confirm-msg">Er du sikker på at du vil slette <strong>«{{ records.find(r => r.trainingRecordId === confirmId)?.title }}»</strong>?</p>
          </div>
          <footer class="modal__footer">
            <button class="btn-ghost" type="button" @click="confirmId = null">Avbryt</button>
            <button class="btn-danger" type="button" :disabled="isSubmitting" @click="deleteRecord">{{ isSubmitting ? 'Sletter…' : 'Slett' }}</button>
          </footer>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.training-view { display: flex; flex-direction: column; gap: 1.1rem; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; gap: 1rem; }
.page-header h1 { margin: 0; font-size: clamp(1.5rem, 2.4vw, var(--font-size-3xl)); font-weight: 700; letter-spacing: -0.015em; }
.subtitle { margin: 0.3rem 0 0; color: var(--color-gray-500); font-size: var(--font-size-sm); }
.add-btn { min-height: var(--touch-target); padding: var(--button-padding-md); background: var(--color-primary); color: var(--color-primary-foreground); border: none; border-radius: var(--radius-md); font-size: var(--font-size-sm); font-weight: 600; cursor: pointer; white-space: nowrap; flex-shrink: 0; }
.add-btn:hover { opacity: 0.88; }
.feedback { padding: 0.7rem 1rem; border-radius: var(--radius-md); font-size: var(--font-size-sm); font-weight: 600; }
.feedback--success { background: var(--color-success-bg); color: var(--color-success); border: 1px solid var(--color-success); }
.feedback--error   { background: var(--color-danger-bg);  color: var(--color-danger);  border: 1px solid var(--color-danger); }
.inline-error { padding: 0.65rem 0.9rem; background: var(--color-danger-bg); color: var(--color-danger); border: 1px solid color-mix(in srgb, var(--color-danger) 30%, var(--color-border)); border-radius: var(--radius-md); font-size: var(--font-size-sm); }
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 0.75rem; }
.stat { border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-card); padding: 0.75rem 1rem; display: flex; flex-direction: column; gap: 0.2rem; }
.stat strong { font-size: 1.6rem; font-weight: 700; color: var(--color-foreground); }
.stat span   { font-size: var(--font-size-xs); color: var(--color-gray-500); }
.stat--ok     { border-left: 3px solid var(--color-success); }
.stat--warn   { border-left: 3px solid var(--color-warning); }
.stat--danger { border-left: 3px solid var(--color-danger); }
.filter-row { display: flex; flex-direction: column; gap: 0.6rem; padding: 0.85rem 1rem; border: 1px solid var(--color-border); border-radius: var(--radius-lg); background: var(--color-card); }
.filter-group { display: flex; align-items: center; gap: 0.75rem; flex-wrap: wrap; }
.filter-label { font-size: var(--font-size-xs); font-weight: 600; text-transform: uppercase; letter-spacing: 0.06em; color: var(--color-gray-500); white-space: nowrap; min-width: 3.5rem; }
.chip-row { display: flex; gap: 0.35rem; flex-wrap: wrap; }
.chip { padding: 0.2rem 0.7rem; border: 1px solid var(--color-border); border-radius: 999px; background: var(--color-background); color: var(--color-gray-600); font-size: var(--font-size-xs); font-weight: 500; cursor: pointer; white-space: nowrap; transition: all var(--transition-fast); }
.chip:hover { border-color: var(--color-gray-400); color: var(--color-foreground); }
.chip--active { background: var(--color-foreground); border-color: var(--color-foreground); color: var(--color-primary-foreground); font-weight: 600; }
.chip--danger.chip--active { background: var(--color-danger); border-color: var(--color-danger); }
.loading { padding: 3rem 1rem; text-align: center; color: var(--color-gray-400); font-size: var(--font-size-sm); }
.empty { display: flex; flex-direction: column; align-items: center; gap: 0.75rem; padding: 4rem 1rem; color: var(--color-gray-400); border: 1px dashed var(--color-border); border-radius: var(--radius-lg); }
.empty p { margin: 0; font-size: var(--font-size-sm); }
.table-card { border: 1px solid var(--color-border); border-radius: var(--radius-lg); background: var(--color-card); box-shadow: var(--shadow-sm); overflow: hidden; }
table { width: 100%; border-collapse: collapse; }
th, td { text-align: left; padding: 0.8rem 1rem; border-bottom: 1px solid var(--color-gray-100); vertical-align: middle; }
th { font-size: var(--font-size-xs); font-weight: 600; text-transform: uppercase; letter-spacing: 0.06em; color: var(--color-gray-500); background: var(--color-gray-50); }
tr:last-child td { border-bottom: none; }
.sort-btn { display: inline-flex; align-items: center; gap: 0.3rem; border: none; background: none; font-size: var(--font-size-xs); font-weight: 600; text-transform: uppercase; letter-spacing: 0.06em; color: var(--color-gray-500); cursor: pointer; padding: 0; }
.sort-btn:hover { color: var(--color-foreground); }
.cell-name { font-size: var(--font-size-sm); font-weight: 500; margin: 0; }
.cell-sub  { font-size: var(--font-size-xs); color: var(--color-gray-500); margin: 0.1rem 0 0; }
.td-title  { font-size: var(--font-size-sm); }
.td-meta   { font-size: var(--font-size-sm); color: var(--color-gray-600); white-space: nowrap; }
.th-actions { width: 1px; }
.td-actions { white-space: nowrap; text-align: right; }
.type-pill { display: inline-flex; padding: 0.2rem 0.6rem; border-radius: 999px; font-size: var(--font-size-xs); font-weight: 500; background: var(--color-gray-100); color: var(--color-gray-700); white-space: nowrap; }
.expiry-val { font-size: var(--font-size-sm); color: var(--color-gray-600); white-space: nowrap; }
.expiry-val--expired { color: var(--color-danger); font-weight: 600; }
.expiry-val--soon    { color: var(--color-warning); font-weight: 600; }
.status-pill { display: inline-flex; padding: 0.2rem 0.6rem; border-radius: 999px; font-size: var(--font-size-xs); font-weight: 600; white-space: nowrap; }
.status-pill--ok      { background: var(--color-success-bg); color: var(--color-success); }
.status-pill--danger  { background: var(--color-danger-bg);  color: var(--color-danger); }
.status-pill--neutral { background: var(--color-gray-100);   color: var(--color-gray-600); }
.row-btn { min-height: 2rem; padding: 0.25rem 0.7rem; border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-card); color: var(--color-gray-700); font-size: var(--font-size-xs); font-weight: 600; cursor: pointer; margin-left: 0.3rem; }
.row-btn:hover { background: var(--color-gray-50); }
.row-btn--danger { color: var(--color-danger); border-color: color-mix(in srgb, var(--color-danger) 30%, var(--color-border)); }
.row-btn--danger:hover { background: var(--color-danger-bg); }
.overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 100; padding: 1rem; }
.modal { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-xl); box-shadow: var(--shadow-md); width: 100%; max-width: 30rem; display: flex; flex-direction: column; max-height: 90vh; overflow-y: auto; }
.modal--sm { max-width: 22rem; }
.modal__header { display: flex; justify-content: space-between; align-items: center; padding: 1rem 1.25rem 0.85rem; border-bottom: 1px solid var(--color-border); }
.modal__header h2 { margin: 0; font-size: var(--font-size-base); font-weight: 600; }
.modal__close { width: 1.75rem; height: 1.75rem; border: none; background: none; color: var(--color-gray-400); border-radius: var(--radius-sm); cursor: pointer; font-size: 0.9rem; display: flex; align-items: center; justify-content: center; }
.modal__close:hover { background: var(--color-gray-100); color: var(--color-foreground); }
.modal__body { padding: 1rem 1.25rem; display: flex; flex-direction: column; gap: 0.85rem; }
.modal__footer { display: flex; justify-content: flex-end; gap: 0.5rem; padding: 0.75rem 1.25rem; border-top: 1px solid var(--color-border); }
.field { display: flex; flex-direction: column; gap: 0.3rem; }
.field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
.field label { font-size: var(--font-size-xs); font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em; color: var(--color-gray-500); }
.req { color: var(--color-danger); }
.field input, .field select, .field textarea { min-height: 2.5rem; padding: 0.4rem 0.75rem; border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: var(--font-size-sm); font-family: inherit; background: var(--color-background); color: var(--color-foreground); box-sizing: border-box; width: 100%; transition: border-color var(--transition-fast), box-shadow var(--transition-fast); }
.field input:focus, .field select:focus, .field textarea:focus { outline: none; border-color: var(--color-focus); box-shadow: var(--shadow-focus); background: var(--color-card); }
.field select { appearance: none; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E"); background-repeat: no-repeat; background-position: right 0.75rem center; padding-right: 2rem; cursor: pointer; }
.field select:disabled { opacity: 0.55; cursor: not-allowed; }
.field textarea { min-height: 5rem; resize: vertical; }
.form-error { padding: 0.55rem 0.75rem; background: var(--color-danger-bg); color: var(--color-danger); border: 1px solid color-mix(in srgb, var(--color-danger) 25%, var(--color-border)); border-radius: var(--radius-md); font-size: var(--font-size-xs); font-weight: 500; margin: 0; }
.confirm-msg { margin: 0; font-size: var(--font-size-sm); color: var(--color-gray-700); line-height: 1.55; }
.btn-ghost { min-height: 2.25rem; padding: 0.4rem 1rem; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-card); color: var(--color-gray-700); font-size: var(--font-size-sm); font-weight: 500; cursor: pointer; }
.btn-ghost:hover { background: var(--color-gray-50); }
.btn-primary { min-height: 2.25rem; padding: 0.4rem 1.25rem; border: none; border-radius: var(--radius-md); background: var(--color-primary); color: var(--color-primary-foreground); font-size: var(--font-size-sm); font-weight: 600; cursor: pointer; }
.btn-primary:disabled { opacity: 0.45; cursor: not-allowed; }
.btn-primary:not(:disabled):hover { opacity: 0.88; }
.btn-danger { min-height: 2.25rem; padding: 0.4rem 1.25rem; border: none; border-radius: var(--radius-md); background: #dc2626; color: #fff; font-size: var(--font-size-sm); font-weight: 600; cursor: pointer; }
.btn-danger:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-danger:not(:disabled):hover { background: #b91c1c; }
@media (max-width: 48rem) {
  .page-header { flex-direction: column; align-items: flex-start; }
  .add-btn { width: 100%; text-align: center; }
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .field-row { grid-template-columns: 1fr; }
  th:nth-child(4), td:nth-child(4) { display: none; }
}
</style>