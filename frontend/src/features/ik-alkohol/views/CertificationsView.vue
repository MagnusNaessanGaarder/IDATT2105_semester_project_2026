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
.certifications-page {
  width: min(100%, 1200px);
  margin: 0 auto;
  display: grid;
  gap: var(--spacing-lg);
}

.page-header {
  display: grid;
  gap: var(--spacing-xs);
}

.page-header h1 {
  font-size: clamp(2rem, 2.5vw, var(--font-size-3xl));
  font-weight: var(--font-weight-bold);
  color: var(--color-brand-medium-violet);
  margin: 0;
}

.subtitle {
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr));
  gap: var(--spacing-md);
}

.status-box {
  border: 1px solid color-mix(in srgb, var(--color-brand-soft-violet) 20%, var(--color-border));
  border-radius: var(--radius-lg);
  padding: var(--spacing-md);
  background: var(--color-card);
  box-shadow: var(--shadow-sm);
}

.status-box__label {
  margin: 0;
  font-size: 0.6875rem;
  color: var(--color-brand-medium-violet);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.status-box__value {
  margin: 0.35rem 0 0;
  font-size: 1.625rem;
  font-weight: 700;
  color: var(--color-foreground);
}

.stat span {
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
}

.status-box--valid {
  border-left: 4px solid var(--color-cta);
}

.status-box--soon {
  border-left: 4px solid var(--color-brand-soft-violet);
}

.status-box--expired {
  border-left: 4px solid var(--color-danger);
}

.type-strip {
  display: flex;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.chip {
  padding: 0.2rem 0.7rem;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: var(--color-background);
  color: var(--color-gray-600);
  font-size: var(--font-size-xs);
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: all var(--transition-fast);
}

.filter-btn--active {
  border-color: var(--ik-alkohol-primary);
  background: color-mix(in srgb, var(--ik-alkohol-primary) 10%, white);
  color: var(--ik-alkohol-primary);
  font-weight: var(--font-weight-semibold);
}

.type-strip li {
  padding: 4px 8px;
  border: 1px solid color-mix(in srgb, var(--color-brand-soft-violet) 18%, var(--color-border));
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-brand-pale-lavender) 40%, var(--color-card));
  font-size: var(--font-size-xs);
  color: var(--color-brand-deep-violet);
}

.catalog-section {
  background: var(--color-card);
  border: 1px solid color-mix(in srgb, var(--color-brand-soft-violet) 20%, var(--color-border));
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-sm);
}

.loading {
  padding: 3rem 1rem;
  text-align: center;
  color: var(--color-gray-400);
  font-size: var(--font-size-sm);
}

.catalog-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--color-card) 94%, var(--color-background-soft));
  padding: 0.9rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 4rem 1rem;
  color: var(--color-gray-400);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-lg);
}

.empty p {
  margin: 0;
  font-size: var(--font-size-sm);
}

.table-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-card);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
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

.table-wrap th {
  text-align: left;
  padding: 12px 8px;
  border-bottom: 1px solid var(--color-border);
  color: var(--color-brand-deep-violet);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
}

.td-meta {
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
  white-space: nowrap;
}

.type-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  background: var(--color-info-bg);
  color: var(--color-info);
  font-size: var(--font-size-xs);
  font-weight: 500;
  background: var(--color-gray-100);
  color: var(--color-gray-700);
  white-space: nowrap;
}

.status-pill {
  display: inline-flex;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-size: var(--font-size-xs);
  font-weight: 600;
  white-space: nowrap;
}

.status-pill--soon {
  color: var(--color-brand-deep-violet);
  background: var(--color-brand-soft-violet);
  border-color: color-mix(in srgb, var(--color-brand-soft-violet) 40%, var(--color-border));
}

.status-pill--danger {
  background: var(--color-danger-bg);
  color: var(--color-danger);
}

.status-pill--neutral {
  background: var(--color-gray-100);
  color: var(--color-gray-600);
}

.th-actions { width: 1px; }

.td-actions {
  white-space: nowrap;
  text-align: right;
}

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

.action-btn--edit {
  background: var(--color-info-bg);
  color: var(--color-info);
  border-color: color-mix(in srgb, var(--color-info) 22%, var(--color-border));
}

.action-btn--edit:hover {
  background: color-mix(in srgb, var(--color-info-bg) 78%, var(--color-info) 22%);
}

.action-btn--delete {
  background: var(--color-danger-bg);
  color: var(--color-danger);
  border-color: var(--color-danger-border);
}

.action-btn--delete:hover {
  background: color-mix(in srgb, var(--color-danger-bg) 80%, var(--color-danger) 20%);
}

.info-box p {
  margin: 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
  line-height: 1.55;
}

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

.error-alert {
  margin-bottom: 1rem;
  padding: 0.75rem 1rem;
  background: var(--color-danger-bg);
  color: var(--color-danger);
  border: 1px solid var(--color-danger-border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
}

.info-box {
  padding: 14px;
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--color-brand-soft-violet) 18%, var(--color-border));
  background: color-mix(in srgb, var(--color-brand-pale-lavender) 46%, var(--color-card));
}

.modal__header h2 {
  margin: 0;
  font-size: var(--font-size-base);
  color: var(--color-brand-deep-violet);
}

.info-box p {
  margin: 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
}

.modal__close:hover {
  background: var(--color-gray-100);
  color: var(--color-foreground);
}

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

.field {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.field label {
  font-size: var(--font-size-xs);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-gray-500);
}

.modal-btn {
  border: 1px solid var(--ik-alkohol-primary);
  background: var(--ik-alkohol-primary);
  color: var(--color-secondary-foreground);
  border-radius: var(--radius-sm);
  padding: 0.45rem 0.8rem;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  font-size: var(--font-size-sm);
  font-family: inherit;
  background: var(--color-background);
  color: var(--color-foreground);
  box-sizing: border-box;
  width: 100%;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.field input:focus,
.field select:focus,
.field textarea:focus {
  outline: none;
  border-color: var(--color-focus);
  box-shadow: var(--shadow-focus);
  background: var(--color-card);
}

.field select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
  padding-right: 2rem;
  cursor: pointer;
}

.field select:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.debug-info {
  background: var(--color-gray-100);
  padding: 10px;
  margin: 10px 0;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
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

.btn-primary:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

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

.btn-danger:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-danger:not(:disabled):hover { background: #b91c1c; }

@media (max-width: 48rem) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .add-btn {
    width: 100%;
    text-align: center;
  }

  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .field-row {
    grid-template-columns: 1fr;
  }

  th:nth-child(4),
  td:nth-child(4) {
    display: none;
  }
}
</style>