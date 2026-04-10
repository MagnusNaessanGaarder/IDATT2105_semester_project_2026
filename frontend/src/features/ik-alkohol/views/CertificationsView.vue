<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  CERTIFICATION_TYPE_LABELS,
  certificateStatusForDate,
  formatDateValue,
  type CertificationType,
  type CertificateStatus,
} from '@/features/ik-alkohol/composables/useAlkoholData'
import { useCertifications } from '@/features/ik-alkohol/composables/useCertifications'
import { getUsers, type UserResponse } from '@/features/ik-alkohol/api/users'
import { useAuthStore } from '@/stores/auth'
import { storeToRefs } from 'pinia'
import BaseModal from '@/shared/components/BaseModal.vue'
import BaseSpinner from '@/shared/components/BaseSpinner.vue'
import type { CertificationRecord } from '../composables/useAlkoholData'

interface UserCertificationGroup {
  userId: number | null
  employee: string
  email: string | null
  certifications: Array<{
    id: number
    title: string
    trainingType: CertificationType
    status: CertificateStatus | 'Mangler'
    expires: string
    completedAt: string
    notes: string | null
    raw: CertificationRecord
  }>
}

const {
  items: certifications,
  isLoading: isLoadingCerts,
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
const isLoadingUsers = ref(false)
const canManageCertifications = computed(() => authStore.hasRole('ADMIN', 'MANAGER'))

const showFormModal = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const editingCertId = ref<number | null>(null)

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

const availableCertTypes = computed(() => {
  return Object.entries(CERTIFICATION_TYPE_LABELS).map(([value, label]) => ({
    value: value as CertificationType,
    label,
  }))
})

const certificationTypes = computed(() => availableCertTypes.value.map((type) => type.label))

const totalCertificates = computed(() => certifications.value.length)

const statusCount = computed(() => {
  const counts: Record<CertificateStatus, number> = {
    Gyldig: 0,
    'Utløper snart': 0,
    Utgått: 0,
  }

  certifications.value.forEach((cert) => {
    const status = cert.expiresAt
      ? certificateStatusForDate(cert.expiresAt.slice(0, 10))
      : cert.status === 'COMPLETED'
        ? 'Gyldig'
        : null

    if (status) {
      counts[status] += 1
    }
  })

  return {
    Gyldig: counts.Gyldig,
    UtløperSnart: counts['Utløper snart'],
    Utgått: counts.Utgått,
  }
})

const userGroups = computed<UserCertificationGroup[]>(() => {
  const certsByUser = new Map<number, UserCertificationGroup['certifications']>()

  certifications.value.forEach((cert) => {
    const userId = cert.user?.userId
    if (!userId) {
      return
    }

    const list = certsByUser.get(userId) ?? []
    list.push({
      id: cert.trainingRecordId,
      title: cert.title,
      trainingType: cert.trainingType,
      status: cert.expiresAt
        ? certificateStatusForDate(cert.expiresAt.slice(0, 10))
        : cert.status === 'COMPLETED'
          ? 'Gyldig'
          : 'Mangler',
      expires: cert.expiresAt ? formatDateValue(cert.expiresAt.slice(0, 10)) : '-',
      completedAt: cert.completedAt ? formatDateValue(cert.completedAt.slice(0, 10)) : '-',
      notes: cert.notes,
      raw: cert,
    })
    certsByUser.set(userId, list)
  })

  const knownUsers = users.value.length > 0
    ? users.value
    : certifications.value
      .map((cert) => cert.user)
      .filter((user): user is NonNullable<CertificationRecord['user']> => user !== null)
      .map((user) => ({
        userId: user.userId,
        displayName: user.displayName,
        email: user.email,
        isActive: true,
        roles: [],
      }))

  return knownUsers.map((user) => ({
    userId: user.userId,
    employee: user.displayName,
    email: user.email ?? null,
    certifications: (certsByUser.get(user.userId) ?? []).slice().sort((a, b) => a.title.localeCompare(b.title)),
  }))
})

const loadUsers = async (force = false) => {
  const orgNumber = currentOrg.value?.orgNumber
  if (!orgNumber || !canManageCertifications.value || (hasLoadedUsers.value && !force)) return

  isLoadingUsers.value = true
  try {
    const result = await getUsers(orgNumber)
    if (result.ok) {
      users.value = result.data.filter((u) => u.isActive)
      hasLoadedUsers.value = true
    }
  } catch {
    users.value = []
  } finally {
    isLoadingUsers.value = false
  }
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

const openAddModal = () => {
  formMode.value = 'add'
  editingCertId.value = null
  resetForm()
  void loadUsers()
  showFormModal.value = true
}

const openAddModalForType = (trainingType: CertificationType) => {
  openAddModal()
  formState.value.trainingType = trainingType
  formState.value.title = CERTIFICATION_TYPE_LABELS[trainingType]
}

const openAddModalForUser = (userId: number | null) => {
  openAddModal()
  formState.value.userId = userId
}

const openEditModal = (cert: CertificationRecord) => {
  formMode.value = 'edit'
  editingCertId.value = cert.trainingRecordId

  const completedDate = cert.completedAt ? cert.completedAt.slice(0, 10) : ''
  const completedTime = cert.completedAt ? cert.completedAt.slice(11, 16) : ''
  const expiresDate = cert.expiresAt ? cert.expiresAt.slice(0, 10) : ''
  const expiresTime = cert.expiresAt ? cert.expiresAt.slice(11, 16) : ''

  formState.value = {
    userId: cert.user?.userId ?? null,
    trainingType: cert.trainingType,
    title: cert.title,
    completedAtDate: completedDate,
    completedAtTime: completedTime,
    expiresAtDate: expiresDate,
    expiresAtTime: expiresTime,
    notes: cert.notes ?? '',
  }

  void loadUsers()
  showFormModal.value = true
}

const closeFormModal = () => {
  showFormModal.value = false
}

const saveCertification = async () => {
  if (!formState.value.userId || !formState.value.trainingType || !formState.value.title.trim()) {
    return
  }

  const orgNumber = currentOrg.value?.orgNumber
  if (!orgNumber) return

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

  if (formMode.value === 'add') {
    const success = await addItem(requestData, orgNumber)
    if (success) {
      showFormModal.value = false
      resetForm()
    }
  }

  if (formMode.value === 'edit' && editingCertId.value !== null) {
    const success = await editItem(editingCertId.value, requestData, orgNumber)
    if (success) {
      showFormModal.value = false
      resetForm()
    }
  }
}

const deleteCertification = async (id: number) => {
  const orgNumber = currentOrg.value?.orgNumber
  if (!orgNumber) return

  const confirmed = confirm('Er du sikker på at du vil slette denne sertifiseringen?')
  if (!confirmed) return

  await removeItem(id, orgNumber)
}

const getStatusClass = (status: string): string => {
  switch (status) {
    case 'Utløper snart':
      return 'status-pill--soon'
    case 'Utgått':
      return 'status-pill--expired'
    case 'Mangler':
      return 'status-pill--missing'
    default:
      return ''
  }
}

const loadCertifications = async () => {
  const orgNumber = currentOrg.value?.orgNumber
  if (orgNumber && !hasLoadedOnce.value) {
    hasLoadedOnce.value = true
    await loadItems(orgNumber)
    await loadUsers()
  }
}

// Load immediately if org is available
void loadCertifications()

// Watch for when currentOrg becomes available (handles page reload timing)
watch(() => currentOrg.value?.orgNumber, (newOrgNumber, previousOrgNumber) => {
  if (newOrgNumber !== previousOrgNumber) {
    hasLoadedOnce.value = false
    hasLoadedUsers.value = false
    users.value = []
  }

  if (newOrgNumber && !hasLoadedOnce.value) {
    void loadCertifications()
  }
}, { immediate: true })
</script>

<template>
  <div class="certifications-page">
    <header class="page-header">
      <h1>Sertifiseringer</h1>
      <p class="subtitle">Oversikt over kunnskapsprover og sertifiseringer for alkoholservering</p>
    </header>

    <section class="status-summary" aria-label="Sertifikatstatus oversikt">
      <article class="status-box status-box--valid">
        <p class="status-box__label">Gyldig</p>
        <p class="status-box__value">{{ statusCount.Gyldig }}</p>
        <p class="status-box__meta">Av totalt {{ totalCertificates }} sertifikater</p>
      </article>
      <article class="status-box status-box--soon">
        <p class="status-box__label">Utløper snart</p>
        <p class="status-box__value">{{ statusCount.UtløperSnart }}</p>
        <p class="status-box__meta">Bør planlegges fornyet</p>
      </article>
      <article class="status-box status-box--expired">
        <p class="status-box__label">Utgått</p>
        <p class="status-box__value">{{ statusCount.Utgått }}</p>
        <p class="status-box__meta">Krever oppfølging umiddelbart</p>
      </article>
    </section>

    <section class="type-strip" aria-label="Sertifiseringstyper">
      <p class="type-strip__label">Typer i systemet:</p>
      <ul>
        <li v-for="type in certificationTypes" :key="type">{{ type }}</li>
      </ul>
    </section>

    <section class="matrix-section" aria-label="Personellsertifiseringer">
      <div class="section-header">
        <h2 class="matrix-title">Personell sertifiseringer</h2>
        <button v-if="canManageCertifications" type="button" class="add-btn" @click="openAddModal">
          + Legg til sertifisering
        </button>
      </div>

      <div v-if="certError" class="error-alert" role="alert">
        {{ certError }}
      </div>

      <div class="user-cert-list">
        <article v-for="group in userGroups" :key="group.userId ?? group.employee" class="user-cert-card">
          <div class="user-cert-card__header">
            <div>
              <h3 class="user-cert-card__title">{{ group.employee }}</h3>
              <p v-if="group.email" class="user-cert-card__meta">{{ group.email }}</p>
            </div>
            <button
              v-if="canManageCertifications"
              type="button"
              class="action-btn action-btn--edit"
              @click="openAddModalForUser(group.userId)"
            >
              + Nytt sertifikat
            </button>
          </div>

          <div v-if="group.certifications.length > 0" class="user-cert-card__items">
            <article
              v-for="certification in group.certifications"
              :key="certification.id"
              class="user-cert-item"
            >
              <div class="user-cert-item__content">
                <div>
                  <p class="user-cert-item__title">{{ certification.title }}</p>
                  <div class="user-cert-item__meta">
                    <span class="type-badge">{{ CERTIFICATION_TYPE_LABELS[certification.trainingType] }}</span>
                    <span>Gyldig til {{ certification.expires }}</span>
                    <span>Fullført {{ certification.completedAt }}</span>
                  </div>
                  <p v-if="certification.notes" class="user-cert-item__notes">{{ certification.notes }}</p>
                </div>
                <span class="status-pill" :class="getStatusClass(certification.status)">
                  {{ certification.status }}
                </span>
              </div>

              <div v-if="canManageCertifications" class="action-buttons">
                <button
                  type="button"
                  class="action-btn action-btn--edit"
                  @click="openEditModal(certification.raw)"
                >
                  Rediger
                </button>
                <button
                  type="button"
                  class="action-btn action-btn--delete"
                  @click="deleteCertification(certification.id)"
                >
                  Slett
                </button>
              </div>
            </article>
          </div>

          <div v-else class="empty-state empty-state--compact">
            <p>Ingen sertifikater registrert for denne brukeren.</p>
          </div>
        </article>
      </div>

      <div v-if="isLoadingCerts || isLoadingUsers" class="loading-indicator">
        <BaseSpinner size="sm" />
        <span>Laster...</span>
      </div>

      <div v-else-if="userGroups.length === 0" class="empty-state">
        <p>Ingen sertifiseringer funnet.</p>
        <p v-if="canManageCertifications" class="empty-hint">Klikk "Legg til sertifisering" for å opprette den første.</p>
      </div>

      <div class="info-box">
        <h3>Krav til kunnskapsprøve</h3>
        <p>
          Alle som selger, skjenker eller utleverer alkohol skal ha bestått kunnskapsprøve.
          Administrer sertifiseringer via tabellen ovenfor.
        </p>
      </div>
    </section>

    <section class="catalog-section" aria-label="Mulige sertifikater">
      <div class="section-header">
        <h2 class="matrix-title">Mulige sertifikater</h2>
        <button v-if="canManageCertifications" type="button" class="add-btn" @click="openAddModal">
          + Opprett nytt sertifikat
        </button>
      </div>

      <div class="catalog-grid">
        <article v-for="type in availableCertTypes" :key="type.value" class="catalog-card">
          <div>
            <p class="catalog-card__title">{{ type.label }}</p>
            <p class="catalog-card__meta">{{ type.value }}</p>
          </div>
        </article>
      </div>
    </section>

    <BaseModal
      :open="showFormModal"
      :title="formMode === 'add' ? 'Legg til sertifisering' : 'Rediger sertifisering'"
      @close="closeFormModal"
    >
      <form class="cert-form" @submit.prevent="saveCertification">
        <label>
          Ansatt *
          <select v-model="formState.userId" required :disabled="isLoadingUsers">
            <option :value="null">Velg ansatt</option>
            <option v-for="user in users" :key="user.userId" :value="user.userId">
              {{ user.displayName }} ({{ user.email }})
            </option>
          </select>
          <span v-if="isLoadingUsers" class="field-hint">Laster brukere...</span>
        </label>

        <label>
          Sertifiseringstype *
          <select v-model="formState.trainingType" required>
            <option value="">Velg type</option>
            <option v-for="type in availableCertTypes" :key="type.value" :value="type.value">
              {{ type.label }}
            </option>
          </select>
        </label>

        <label>
          Tittel *
          <input v-model="formState.title" type="text" placeholder="f.eks. Kunnskapsprøve alkoholloven" required />
        </label>

        <div class="form-row">
          <label>
            Fullført dato
            <input v-model="formState.completedAtDate" type="date" />
          </label>
          <label>
            Fullført tid
            <input v-model="formState.completedAtTime" type="time" />
          </label>
        </div>

        <div class="form-row">
          <label>
            Utløper dato
            <input v-model="formState.expiresAtDate" type="date" />
          </label>
          <label>
            Utløper tid
            <input v-model="formState.expiresAtTime" type="time" />
          </label>
        </div>

        <label>
          Notater
          <textarea v-model="formState.notes" rows="3" placeholder="Valgfrie notater om sertifiseringen" />
        </label>
      </form>

      <template #footer>
        <button
          type="button"
          class="modal-btn modal-btn--ghost"
          @click="closeFormModal"
          :disabled="isSubmitting"
        >
          Avbryt
        </button>
        <button type="button" class="modal-btn" @click="saveCertification" :disabled="isSubmitting">
          <BaseSpinner v-if="isSubmitting" size="sm" />
          <span v-else>Lagre</span>
        </button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.certifications-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--ik-alkohol-primary);
  margin-bottom: 8px;
}

.subtitle {
  font-size: var(--font-size-sm);
  color: var(--color-gray-500);
}

.status-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr));
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.status-box {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.85rem;
  background: var(--color-card);
}

.status-box__label {
  margin: 0;
  font-size: var(--font-size-xs);
  color: var(--color-gray-600);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.status-box__value {
  margin: 0.35rem 0 0;
  font-size: 1.625rem;
  font-weight: 700;
}

.status-box__meta {
  margin: 0.35rem 0 0;
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
}

.status-box--valid {
  border-left: 0.25rem solid var(--color-success);
}

.status-box--soon {
  border-left: 0.25rem solid var(--color-warning);
}

.status-box--expired {
  border-left: 0.25rem solid var(--color-danger);
}

.type-strip {
  margin-bottom: 12px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.type-strip__label {
  margin: 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
}

.type-strip ul {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  list-style: none;
  margin: 0;
  padding: 0;
}

.type-strip li {
  padding: 4px 8px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: #f8fafc;
  font-size: var(--font-size-xs);
  color: var(--color-gray-600);
}

.matrix-section {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1.25rem;
}

.catalog-section {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1.25rem;
}

.catalog-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 0.75rem;
}

.catalog-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--color-card) 94%, #f8fafc);
  padding: 0.9rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.catalog-card__title {
  margin: 0;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-foreground);
}

.catalog-card__meta {
  margin: 0.35rem 0 0;
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.matrix-title {
  margin: 0;
  font-size: var(--font-size-lg);
}

.add-btn {
  padding: 0.5rem 1rem;
  border: 1px solid var(--ik-alkohol-primary);
  background: color-mix(in srgb, var(--ik-alkohol-primary) 7%, var(--color-card));
  color: var(--ik-alkohol-primary);
  font-weight: var(--font-weight-semibold);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: var(--font-size-sm);
}

.add-btn:hover {
  background: color-mix(in srgb, var(--ik-alkohol-primary) 12%, var(--color-card));
}

.user-cert-list {
  display: grid;
  gap: 1rem;
  margin-bottom: 1rem;
}

.user-cert-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1rem;
  background: color-mix(in srgb, var(--color-card) 94%, #f8fafc);
}

.user-cert-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
  margin-bottom: 0.85rem;
}

.user-cert-card__title {
  margin: 0;
  font-size: var(--font-size-base);
}

.user-cert-card__meta {
  margin: 0.2rem 0 0;
  font-size: var(--font-size-sm);
  color: var(--color-gray-500);
}

.user-cert-card__items {
  display: grid;
  gap: 0.75rem;
}

.user-cert-item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-card);
  padding: 0.85rem;
  display: grid;
  gap: 0.7rem;
}

.user-cert-item__content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
}

.user-cert-item__title {
  margin: 0;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-foreground);
}

.user-cert-item__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
  margin-top: 0.35rem;
  font-size: var(--font-size-xs);
  color: var(--color-gray-600);
}

.user-cert-item__notes {
  margin: 0.45rem 0 0;
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
}

.type-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  background: #e0f2fe;
  color: #0369a1;
  font-size: var(--font-size-xs);
  font-weight: 500;
}

.status-pill {
  display: inline-block;
  padding: 0.2rem 0.45rem;
  border-radius: var(--radius-sm);
  color: var(--color-success);
  background: var(--color-success-bg);
  border: 1px solid color-mix(in srgb, var(--color-success) 30%, var(--color-border));
  font-size: var(--font-size-xs);
  font-weight: 600;
}

.status-pill--soon {
  color: var(--color-warning);
  background: var(--color-warning-bg);
  border-color: color-mix(in srgb, var(--color-warning) 35%, var(--color-border));
}

.status-pill--expired {
  color: var(--color-danger);
  background: var(--color-danger-bg);
  border-color: color-mix(in srgb, var(--color-danger) 35%, var(--color-border));
}

.status-pill--missing {
  color: var(--color-gray-600);
  background: var(--color-gray-100);
  border-color: var(--color-border);
}

.action-buttons {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.action-btn {
  padding: 0.25rem 0.5rem;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  cursor: pointer;
  border: 1px solid var(--color-border);
  background: var(--color-card);
  color: var(--color-gray-700);
}

.action-btn--edit {
  background: #e0f2fe;
  color: #0369a1;
  border-color: #bae6fd;
}

.action-btn--edit:hover {
  background: #bae6fd;
}

.action-btn--delete {
  background: #fee2e2;
  color: #dc2626;
  border-color: #fecaca;
}

.action-btn--delete:hover {
  background: #fecaca;
}

.text-muted {
  color: var(--color-gray-400);
}

.loading-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1rem;
  color: var(--color-gray-500);
  font-size: var(--font-size-sm);
}

.error-alert {
  margin-bottom: 1rem;
  padding: 0.75rem 1rem;
  background: var(--color-error-bg, #fef2f2);
  color: var(--color-error, #dc2626);
  border: 1px solid var(--color-error-border, #fecaca);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
}

.info-box {
  padding: 14px;
  border-radius: var(--radius-sm);
  border: 1px solid #bfdbfe;
  background: #eff6ff;
}

.info-box h3 {
  margin: 0 0 4px;
  font-size: var(--font-size-base);
  color: #1e3a8a;
}

.info-box p {
  margin: 0;
  color: #1e3a8a;
  font-size: var(--font-size-sm);
}

.cert-form {
  display: grid;
  gap: 0.8rem;
}

.cert-form label {
  display: grid;
  gap: 0.3rem;
  font-size: var(--font-size-sm);
  color: var(--color-gray-700);
}

.cert-form input,
.cert-form select,
.cert-form textarea {
  border: 1px solid var(--color-border);
  padding: 0.55rem 0.7rem;
  border-radius: var(--radius-sm);
  background: var(--color-card);
  font-size: var(--font-size-sm);
}

.cert-form input:focus,
.cert-form select:focus,
.cert-form textarea:focus {
  outline: none;
  border-color: var(--ik-alkohol-primary);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.field-hint {
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
  font-style: italic;
}

.modal-btn {
  border: 1px solid var(--ik-alkohol-primary);
  background: var(--ik-alkohol-primary);
  color: #fff;
  border-radius: var(--radius-sm);
  padding: 0.45rem 0.8rem;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  font-size: var(--font-size-sm);
}

.modal-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.modal-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.modal-btn--ghost {
  background: var(--color-card);
  color: var(--color-gray-700);
  border-color: var(--color-border);
}

.debug-info {
  background: #f0f0f0;
  padding: 10px;
  margin: 10px 0;
  border: 1px solid #ccc;
  border-radius: var(--radius-sm);
}

.debug-info p {
  margin: 4px 0;
  font-size: var(--font-size-xs);
}

.empty-state {
  text-align: center;
  padding: 2rem;
  color: var(--color-gray-600);
  background: var(--color-gray-50);
  border-radius: var(--radius-md);
  margin: 1rem 0;
}

.empty-state--compact {
  margin: 0;
  padding: 1rem;
}

.empty-state p {
  margin: 0.5rem 0;
}

.empty-hint {
  font-size: var(--font-size-sm);
  color: var(--color-gray-500);
}

@media (max-width: 48rem) {
  .status-summary {
    grid-template-columns: 1fr;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .user-cert-card__header,
  .user-cert-item__content {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
