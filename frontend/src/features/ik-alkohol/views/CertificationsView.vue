<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  certificateStatusForDate,
  formatDateValue,
  type CertificationType,
  type CertificationRecord,
} from '@/features/ik-alkohol/composables/useAlkoholData'
import type { CertificateStatus } from '@/features/ik-alkohol/types'
import { useCertifications } from '@/features/ik-alkohol/composables/useCertifications'
import { getCertificationCatalog, type CertificationCatalogItem } from '@/features/ik-alkohol/api/certificationCatalog'
import { getUsers, type UserResponse } from '@/features/ik-alkohol/api/users'
import { useAuthStore } from '@/stores/auth'
import { storeToRefs } from 'pinia'

const ALCOHOL_TYPES = new Set<CertificationType>([
  'RESPONSIBLE_ALCOHOL_SERVICE',
  'AGE_VERIFICATION',
])

const {
  items: certifications,
  isLoading,
  isSubmitting,
  error: certError,
  loadItems,
  addItem,
  editItem,
  removeItem,
} = useCertifications()

const authStore = useAuthStore()
const { currentOrg } = storeToRefs(authStore)

const hasLoadedOnce = ref(false)
const hasLoadedUsers = ref(false)

const users = ref<UserResponse[]>([])
const certificationCatalog = ref<CertificationCatalogItem[]>([])
const isLoadingUsers = ref(false)

const canManageCertifications = computed(() => authStore.hasRole('ADMIN', 'MANAGER'))

const activeFilter = ref<'ALL' | CertificationType>('ALL')
const sortDir = ref<'asc' | 'desc'>('asc')

const feedback = ref<{ type: 'success' | 'error'; msg: string } | null>(null)

const modalOpen = ref(false)
const modalMode = ref<'add' | 'edit'>('add')
const editingCertId = ref<number | null>(null)
const modalError = ref<string | null>(null)

const confirmId = ref<number | null>(null)

const formState = ref({
  userId: null as number | null,
  trainingType: '' as CertificationType | '',
  title: '',
  completedAtDate: '',
  completedAtTime: '',
  expiresAtDate: '',
  expiresAtTime: '',
  notes: '',
})

const availableCertTypes = computed(() =>
    certificationCatalog.value
        .filter((item) => ALCOHOL_TYPES.has(item.trainingType as CertificationType))
        .map((item) => ({
          value: item.trainingType as CertificationType,
          label: item.displayName,
        })),
)

const certificationTypeLabels = computed<Record<string, string>>(() =>
    Object.fromEntries(certificationCatalog.value.map((item) => [item.trainingType, item.displayName])),
)

const userMap = computed(() => new Map(users.value.map((u) => [u.userId, u])))

type TableRow = {
  id: number
  employee: string
  email: string | null
  title: string
  trainingType: CertificationType
  completedAt: string
  expiresAt: string
  sortExpiresAt: string | null
  status: CertificateStatus | 'Mangler'
  notes: string | null
  raw: CertificationRecord
}

const alcoholCertifications = computed(() =>
    certifications.value.filter((cert) => ALCOHOL_TYPES.has(cert.trainingType as CertificationType)),
)

const totalCertificates = computed(() => alcoholCertifications.value.length)

const statusCount = computed(() => {
  const counts: Record<CertificateStatus, number> = {
    Gyldig: 0,
    'Utløper snart': 0,
    Utgått: 0,
  }

  alcoholCertifications.value.forEach((cert) => {
    const status: CertificateStatus | null = cert.expiresAt
      ? certificateStatusForDate(cert.expiresAt.slice(0, 10))
      : cert.status === 'COMPLETED'
        ? 'Gyldig'
        : null

    if (status) {
      counts[status] = (counts[status] ?? 0) + 1
    }
  })

  return {
    Gyldig: counts.Gyldig,
    UtløperSnart: counts['Utløper snart'],
    Utgått: counts.Utgått,
  }
})

const expiringSoon = computed(() => statusCount.value.UtløperSnart)
const expiredCount = computed(() => statusCount.value.Utgått)

const flatRows = computed<TableRow[]>(() => {
  let rows = alcoholCertifications.value.map((cert) => {
    const user = cert.user?.userId ? userMap.value.get(cert.user.userId) : null
    const status: CertificateStatus | 'Mangler' = cert.expiresAt
      ? certificateStatusForDate(cert.expiresAt.slice(0, 10))
      : cert.status === 'COMPLETED'
        ? 'Gyldig'
        : 'Mangler'

    return {
      id: cert.trainingRecordId,
      employee: user?.displayName ?? cert.user?.displayName ?? '—',
      email: user?.email ?? cert.user?.email ?? null,
      title: cert.title,
      trainingType: cert.trainingType as CertificationType,
      completedAt: cert.completedAt ? formatDateValue(cert.completedAt.slice(0, 10)) : '—',
      expiresAt: cert.expiresAt ? formatDateValue(cert.expiresAt.slice(0, 10)) : '—',
      sortExpiresAt: cert.expiresAt ? cert.expiresAt.slice(0, 10) : null,
      status,
      notes: cert.notes ?? null,
      raw: cert,
    }
  })

  if (activeFilter.value !== 'ALL') {
    rows = rows.filter((row) => row.trainingType === activeFilter.value)
  }

  rows.sort((a, b) => {
    const da = a.sortExpiresAt ?? '9999-12-31'
    const db = b.sortExpiresAt ?? '9999-12-31'
    return sortDir.value === 'asc' ? da.localeCompare(db) : db.localeCompare(da)
  })

  return rows
})

const loadUsersForOrg = async (force = false) => {
  const orgNumber = currentOrg.value?.orgNumber
  if (!orgNumber || !canManageCertifications.value || (hasLoadedUsers.value && !force)) return

  isLoadingUsers.value = true
  try {
    const result = await getUsers(orgNumber)
    if (result.ok) {
      users.value = result.data.filter((u) => u.isActive)
      hasLoadedUsers.value = true
    } else {
      users.value = []
    }
  } catch {
    users.value = []
  } finally {
    isLoadingUsers.value = false
  }
}

const loadCertificationCatalog = async () => {
  const orgNumber = currentOrg.value?.orgNumber
  if (!orgNumber) return

  const result = await getCertificationCatalog(orgNumber)
  certificationCatalog.value = result.ok ? result.data : []
}

const loadAll = async () => {
  const orgNumber = currentOrg.value?.orgNumber
  if (!orgNumber || hasLoadedOnce.value) return

  hasLoadedOnce.value = true
  await Promise.all([loadItems(orgNumber), loadCertificationCatalog()])
  await loadUsersForOrg()
}

const resetForm = () => {
  formState.value = {
    userId: null,
    trainingType: '',
    title: '',
    completedAtDate: '',
    completedAtTime: '',
    expiresAtDate: '',
    expiresAtTime: '',
    notes: '',
  }
}

const openAdd = () => {
  modalMode.value = 'add'
  editingCertId.value = null
  modalError.value = null
  resetForm()

  const defaultType = availableCertTypes.value[0]
  if (defaultType) {
    formState.value.trainingType = defaultType.value
    formState.value.title = certificationTypeLabels.value[formState.value.trainingType] ?? formState.value.trainingType
  }

  void loadUsersForOrg()
  modalOpen.value = true
}

const openEdit = (cert: CertificationRecord) => {
  modalMode.value = 'edit'
  editingCertId.value = cert.trainingRecordId
  modalError.value = null

  formState.value = {
    userId: cert.user?.userId ?? null,
    trainingType: cert.trainingType as CertificationType,
    title: cert.title,
    completedAtDate: cert.completedAt ? cert.completedAt.slice(0, 10) : '',
    completedAtTime: cert.completedAt ? cert.completedAt.slice(11, 16) : '',
    expiresAtDate: cert.expiresAt ? cert.expiresAt.slice(0, 10) : '',
    expiresAtTime: cert.expiresAt ? cert.expiresAt.slice(11, 16) : '',
    notes: cert.notes ?? '',
  }

  void loadUsersForOrg()
  modalOpen.value = true
}

const closeModal = () => {
  modalOpen.value = false
}

const onTypeChange = () => {
  if (modalMode.value === 'add' && formState.value.trainingType) {
    formState.value.title =
        certificationTypeLabels.value[formState.value.trainingType] ?? formState.value.trainingType
  }
}

const saveCertification = async () => {
  if (!formState.value.userId || !formState.value.trainingType || !formState.value.title.trim()) {
    modalError.value = 'Velg ansatt, type og fyll inn tittel.'
    return
  }

  const orgNumber = currentOrg.value?.orgNumber
  if (!orgNumber) {
    modalError.value = 'Fant ikke organisasjon.'
    return
  }

  modalError.value = null

  const completedAt = formState.value.completedAtDate
      ? `${formState.value.completedAtDate}T${formState.value.completedAtTime || '00:00'}:00`
      : null

  const expiresAt = formState.value.expiresAtDate
      ? `${formState.value.expiresAtDate}T${formState.value.expiresAtTime || '00:00'}:00`
      : null

  const requestData = {
    userId: formState.value.userId,
    trainingType: formState.value.trainingType,
    title: formState.value.title.trim(),
    completedAt,
    expiresAt,
    notes: formState.value.notes.trim() || undefined,
  }

  const ok =
      modalMode.value === 'add'
          ? await addItem(requestData, orgNumber)
          : await editItem(editingCertId.value!, requestData, orgNumber)

  if (ok) {
    flash('success', modalMode.value === 'add' ? 'Sertifisering lagt til.' : 'Sertifisering oppdatert.')
    closeModal()
    resetForm()
  } else {
    modalError.value = certError.value ?? 'Lagring feilet.'
  }
}

const deleteCertification = async () => {
  const orgNumber = currentOrg.value?.orgNumber
  if (!orgNumber || confirmId.value === null) return

  const id = confirmId.value
  const title = flatRows.value.find((row) => row.id === id)?.title ?? 'sertifiseringen'
  const ok = await removeItem(id, orgNumber)

  confirmId.value = null

  if (ok) flash('success', `«${title}» slettet.`)
  else flash('error', 'Sletting feilet.')
}

const flash = (type: 'success' | 'error', msg: string) => {
  feedback.value = { type, msg }
  setTimeout(() => {
    feedback.value = null
  }, 4500)
}

const statusToneClass = (status: string) => {
  switch (status) {
    case 'Utgått':
      return 'status-pill--danger'
    case 'Utløper snart':
      return 'status-pill--warn'
    case 'Mangler':
      return 'status-pill--neutral'
    default:
      return 'status-pill--ok'
  }
}

const toggleSort = () => {
  sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
}

void loadAll()

watch(
    () => currentOrg.value?.orgNumber,
    (newOrgNumber, previousOrgNumber) => {
      if (newOrgNumber !== previousOrgNumber) {
        hasLoadedOnce.value = false
        hasLoadedUsers.value = false
        users.value = []
        certificationCatalog.value = []
      }

      if (newOrgNumber && !hasLoadedOnce.value) {
        void loadAll()
      }
    },
    { immediate: true },
)
</script>

<template>
  <div class="training-view">
    <header class="page-header">
      <div>
        <h1>Sertifiseringer</h1>
        <p class="subtitle">Oversikt over kunnskapsprøver og sertifiseringer for alkoholservering</p>
      </div>
      <button v-if="canManageCertifications" class="add-btn" type="button" @click="openAdd">
        + Legg til sertifisering
      </button>
    </header>

    <div v-if="feedback" class="feedback" :class="`feedback--${feedback.type}`" role="status">
      {{ feedback.msg }}
    </div>

    <div v-if="certError" class="inline-error">{{ certError }}</div>

    <div class="stats-row">
      <div class="stat">
        <strong>{{ totalCertificates }}</strong>
        <span>Totalt</span>
      </div>
      <div class="stat stat--ok">
        <strong>{{ statusCount.Gyldig }}</strong>
        <span>Gyldige</span>
      </div>
      <div class="stat stat--warn">
        <strong>{{ expiringSoon }}</strong>
        <span>Utløper snart</span>
      </div>
      <div class="stat stat--danger">
        <strong>{{ expiredCount }}</strong>
        <span>Utgåtte</span>
      </div>
    </div>

    <div class="filter-row">
      <div class="filter-group">
        <span class="filter-label">Type</span>
        <div class="chip-row">
          <button
              class="chip"
              :class="{ 'chip--active': activeFilter === 'ALL' }"
              type="button"
              @click="activeFilter = 'ALL'"
          >
            Alle
          </button>
          <button
              v-for="type in availableCertTypes"
              :key="type.value"
              class="chip"
              :class="{ 'chip--active': activeFilter === type.value }"
              type="button"
              @click="activeFilter = type.value"
          >
            {{ type.label }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="isLoading || isLoadingUsers" class="loading">Laster sertifiseringer…</div>

    <div v-else-if="flatRows.length === 0" class="empty">
      <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <path d="M7 3h7l5 5v13a1 1 0 0 1-1 1H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z" />
        <path d="M14 3v6h6" />
        <path d="M9 13h6" />
        <path d="M9 17h4" />
      </svg>
      <p>Ingen sertifiseringer funnet.</p>
      <button v-if="canManageCertifications" class="add-btn" type="button" @click="openAdd">
        Legg til første sertifisering
      </button>
    </div>

    <div v-else class="table-card">
      <table>
        <thead>
        <tr>
          <th>Ansatt</th>
          <th>Type</th>
          <th>Tittel</th>
          <th>Fullført</th>
          <th>
            <button class="sort-btn" type="button" @click="toggleSort">
              Utløper
              <span>{{ sortDir === 'asc' ? '↑' : '↓' }}</span>
            </button>
          </th>
          <th>Status</th>
          <th class="th-actions"></th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="row in flatRows" :key="row.id">
          <td>
            <p class="cell-name">{{ row.employee }}</p>
            <p class="cell-sub">{{ row.email ?? '' }}</p>
          </td>
          <td>
              <span class="type-pill">
                {{ certificationTypeLabels[row.trainingType] ?? row.trainingType }}
              </span>
          </td>
          <td class="td-title">
            <div class="title-wrap">
              <span>{{ row.title }}</span>
              <span v-if="row.notes" class="title-note">{{ row.notes }}</span>
            </div>
          </td>
          <td class="td-meta">{{ row.completedAt }}</td>
          <td class="td-meta">{{ row.expiresAt }}</td>
          <td>
              <span class="status-pill" :class="statusToneClass(row.status)">
                {{ row.status }}
              </span>
          </td>
          <td v-if="canManageCertifications" class="td-actions">
            <button class="row-btn" type="button" @click="openEdit(row.raw)">Rediger</button>
            <button class="row-btn row-btn--danger" type="button" @click="confirmId = row.id">Slett</button>
          </td>
        </tr>
        </tbody>
      </table>
    </div>

    <div class="info-box">
      <h3>Krav til kunnskapsprøve</h3>
      <p>
        Alle som selger, skjenker eller utleverer alkohol skal ha bestått kunnskapsprøve.
        Administrer sertifiseringer via tabellen ovenfor.
      </p>
    </div>

    <Teleport to="body">
      <div v-if="modalOpen" class="overlay" @click.self="closeModal">
        <div class="modal" role="dialog" aria-modal="true">
          <header class="modal__header">
            <h2>{{ modalMode === 'add' ? 'Legg til sertifisering' : 'Rediger sertifisering' }}</h2>
            <button class="modal__close" type="button" @click="closeModal">✕</button>
          </header>

          <div class="modal__body">
            <div class="field">
              <label for="cert-user">Ansatt <span class="req">*</span></label>
              <select
                  id="cert-user"
                  v-model="formState.userId"
                  :disabled="isLoadingUsers || modalMode === 'edit'"
              >
                <option :value="null" disabled>— Velg ansatt —</option>
                <option v-for="user in users" :key="user.userId" :value="user.userId">
                  {{ user.displayName }} ({{ user.email }})
                </option>
              </select>
            </div>

            <div class="field-row">
              <div class="field">
                <label for="cert-type">Type <span class="req">*</span></label>
                <select id="cert-type" v-model="formState.trainingType" @change="onTypeChange">
                  <option value="" disabled>— Velg type —</option>
                  <option v-for="type in availableCertTypes" :key="type.value" :value="type.value">
                    {{ type.label }}
                  </option>
                </select>
              </div>

              <div class="field">
                <label for="cert-title">Tittel <span class="req">*</span></label>
                <input
                    id="cert-title"
                    v-model="formState.title"
                    type="text"
                    placeholder="f.eks. Kunnskapsprøve alkoholloven"
                    maxlength="255"
                />
              </div>
            </div>

            <div class="field-row field-row--double">
              <div class="field">
                <label for="cert-completed-date">Fullført dato</label>
                <input id="cert-completed-date" v-model="formState.completedAtDate" type="date" />
              </div>
              <div class="field">
                <label for="cert-completed-time">Fullført tid</label>
                <input id="cert-completed-time" v-model="formState.completedAtTime" type="time" />
              </div>
            </div>

            <div class="field-row field-row--double">
              <div class="field">
                <label for="cert-expires-date">Utløper dato</label>
                <input id="cert-expires-date" v-model="formState.expiresAtDate" type="date" />
              </div>
              <div class="field">
                <label for="cert-expires-time">Utløper tid</label>
                <input id="cert-expires-time" v-model="formState.expiresAtTime" type="time" />
              </div>
            </div>

            <div class="field">
              <label for="cert-notes">Notater</label>
              <textarea
                  id="cert-notes"
                  v-model="formState.notes"
                  rows="3"
                  placeholder="Valgfrie kommentarer"
                  maxlength="2000"
              />
            </div>

            <p v-if="modalError" class="form-error">{{ modalError }}</p>
          </div>

          <footer class="modal__footer">
            <button class="btn-ghost" type="button" :disabled="isSubmitting" @click="closeModal">
              Avbryt
            </button>
            <button class="btn-primary" type="button" :disabled="isSubmitting" @click="saveCertification">
              {{ isSubmitting ? 'Lagrer…' : modalMode === 'add' ? 'Legg til' : 'Lagre' }}
            </button>
          </footer>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="confirmId !== null" class="overlay" @click.self="confirmId = null">
        <div class="modal modal--sm" role="dialog" aria-modal="true">
          <header class="modal__header">
            <h2>Slett sertifisering</h2>
            <button class="modal__close" type="button" @click="confirmId = null">✕</button>
          </header>
          <div class="modal__body">
            <p class="confirm-msg">
              Er du sikker på at du vil slette
              <strong>«{{ flatRows.find((row) => row.id === confirmId)?.title }}»</strong>?
            </p>
          </div>
          <footer class="modal__footer">
            <button class="btn-ghost" type="button" @click="confirmId = null">Avbryt</button>
            <button class="btn-danger" type="button" :disabled="isSubmitting" @click="deleteCertification">
              {{ isSubmitting ? 'Sletter…' : 'Slett' }}
            </button>
          </footer>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>

.training-view {
  display: grid;
  gap: 1rem;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.page-header h1 {
  margin: 0;
}

.subtitle {
  margin: 0.35rem 0 0;
  color: var(--color-gray-600);
}

.add-btn {
  min-height: 2.5rem;
  padding: 0.4rem 0.9rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: var(--color-primary-foreground);
  cursor: pointer;
}

.feedback,
.inline-error {
  padding: 0.65rem 0.8rem;
  border-radius: var(--radius-md);
}

.feedback--success {
  background: var(--color-success-bg);
  color: var(--color-success);
}

.feedback--error,
.inline-error {
  background: var(--color-danger-bg);
  color: var(--color-danger);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(9rem, 1fr));
  gap: 0.75rem;
}

.stat {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.75rem;
  background: var(--color-card);
  display: grid;
  gap: 0.2rem;
}

.stat strong {
  font-size: 1.3rem;
}

.stat--ok { border-color: color-mix(in srgb, var(--color-success) 40%, var(--color-border)); }
.stat--warn { border-color: color-mix(in srgb, #d97706 40%, var(--color-border)); }
.stat--danger { border-color: color-mix(in srgb, var(--color-danger) 40%, var(--color-border)); }

.filter-group {
  display: grid;
  gap: 0.35rem;
}

.filter-label {
  font-size: var(--font-size-xs);
  color: var(--color-gray-600);
}

.chip-row {
  display: flex;
  gap: 0.45rem;
  flex-wrap: wrap;
}

.chip {
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: var(--color-card);
  color: var(--color-gray-700);
  padding: 0.3rem 0.75rem;
  cursor: pointer;
}

.chip--active {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: var(--color-primary-foreground);
}

.loading,
.empty {
  padding: 2rem 1rem;
  text-align: center;
  color: var(--color-gray-600);
}

.table-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: 0.6rem 0.7rem;
  border-bottom: 1px solid var(--color-border);
  text-align: left;
}

th {
  font-size: var(--font-size-xs);
  color: var(--color-gray-600);
  background: var(--color-gray-50);
}

.sort-btn {
  border: 0;
  background: transparent;
  font: inherit;
  color: inherit;
  cursor: pointer;
}

.cell-name {
  margin: 0;
  font-weight: 600;
}

.cell-sub {
  margin: 0.1rem 0 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-xs);
}

.type-pill,
.status-pill {
  display: inline-flex;
  align-items: center;
  padding: 0.15rem 0.55rem;
  border-radius: 999px;
  font-size: var(--font-size-xs);
}

.type-pill {
  background: var(--color-gray-100);
  color: var(--color-gray-700);
}

.title-wrap {
  display: grid;
  gap: 0.2rem;
}

.title-note {
  color: var(--color-gray-600);
  font-size: var(--font-size-xs);
}

.td-meta {
  color: var(--color-gray-700);
}

.status-pill--ok { background: var(--color-success-bg); color: var(--color-success); }
.status-pill--warn { background: #fef3c7; color: #92400e; }
.status-pill--danger { background: var(--color-danger-bg); color: var(--color-danger); }
.status-pill--neutral { background: var(--color-gray-100); color: var(--color-gray-600); }

.th-actions { width: 1px; }
.td-actions { white-space: nowrap; }

.row-btn {
  min-height: 2rem;
  padding: 0.25rem 0.7rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-card);
  cursor: pointer;
}

.row-btn + .row-btn {
  margin-left: 0.35rem;
}

.row-btn--danger {
  color: var(--color-danger);
}

.info-box {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  padding: 0.9rem;
}

.info-box h3,
.info-box p {
  margin: 0;
}

.info-box p {
  margin-top: 0.4rem;
  color: var(--color-gray-700);
}

.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.42);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 1rem;
}

.modal {
  width: min(100%, 38rem);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.modal--sm {
  width: min(100%, 30rem);
}

.modal__header,
.modal__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.8rem 1rem;
  border-bottom: 1px solid var(--color-border);
}

.modal__footer {
  justify-content: flex-end;
  gap: 0.5rem;
  border-top: 1px solid var(--color-border);
  border-bottom: 0;
}

.modal__header h2 {
  margin: 0;
  font-size: var(--font-size-lg);
}

.modal__close {
  border: 0;
  background: transparent;
  cursor: pointer;
}

.modal__body {
  display: grid;
  gap: 0.7rem;
  padding: 1rem;
}

.field {
  display: grid;
  gap: 0.25rem;
}

.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.7rem;
}

.field input,
.field select,
.field textarea {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0.45rem 0.6rem;
  font: inherit;
}

.req {
  color: var(--color-danger);
}

.form-error {
  margin: 0;
  border: 1px solid color-mix(in srgb, var(--color-danger) 30%, var(--color-border));
  border-radius: var(--radius-sm);
  background: var(--color-danger-bg);
  color: var(--color-danger);
  padding: 0.5rem 0.6rem;
}

.confirm-msg {
  margin: 0;
}

.btn-ghost,
.btn-primary,
.btn-danger {
  min-height: 2.2rem;
  padding: 0.35rem 0.9rem;
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.btn-ghost {
  border: 1px solid var(--color-border);
  background: var(--color-card);
}

.btn-primary {
  border: 1px solid var(--color-primary);
  background: var(--color-primary);
  color: var(--color-primary-foreground);
}

.btn-danger {
  border: 1px solid #b91c1c;
  background: #dc2626;
  color: #fff;
}

@media (max-width: 48rem) {
  .field-row {
    grid-template-columns: 1fr;
  }

  .page-header {
    align-items: stretch;
  }

  .add-btn {
    width: 100%;
  }
}

</style>