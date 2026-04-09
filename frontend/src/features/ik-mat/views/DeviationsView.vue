<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseModal from '@/shared/components/BaseModal.vue'
import { useAuthStore } from '@/stores/auth'
import { useIkMatData } from '../composables/useIkMatData'
import DeviationCard from '../components/DeviationCard.vue'

const route = useRoute()
const router = useRouter()

const authStore = useAuthStore()
const { deviations, formatDate, createDeviation, updateDeviation, deleteDeviation, resolveDeviation, startDeviationHandling } = useIkMatData()

const selectedStatus = ref<'all' | 'open' | 'in-progress' | 'resolved'>('all')
const selectedId = ref<number | null>(deviations[0]?.id ?? null)
const createModalOpen = ref(false)
const editModalOpen = ref(false)
const createError = ref<string | null>(null)
const editError = ref<string | null>(null)
const createInFlight = ref(false)
const editInFlight = ref(false)

const createForm = reactive({
  title: '',
  description: '',
  locationText: '',
  severity: 'MAJOR' as 'MINOR' | 'MAJOR' | 'CRITICAL',
  discoveredByName: '',
  occurredDate: '',
  occurredTime: '',
  sourceTemperatureEntryId: null as number | null,
})

const editForm = reactive({
  id: null as number | null,
  title: '',
  description: '',
  locationText: '',
  severity: 'MAJOR' as 'MINOR' | 'MAJOR' | 'CRITICAL',
  discoveredByName: '',
  occurredDate: '',
  occurredTime: '',
})

const canAdmin = computed(() => authStore.hasRole('ADMIN'))
const canManageDeviation = computed(() => authStore.hasRole('ADMIN', 'MANAGER'))
const canEditSelected = computed(() => {
  return Boolean(canAdmin.value && selectedDeviation.value && selectedDeviation.value.status !== 'resolved')
})
const canResolveSelected = computed(() => {
  return Boolean(canManageDeviation.value && selectedDeviation.value && selectedDeviation.value.status === 'in-progress')
})

const filtered = computed(() => {
  if (selectedStatus.value === 'all') {
    return deviations
  }

  return deviations.filter((item) => item.status === selectedStatus.value)
})

const selectedDeviation = computed(() => {
  return filtered.value.find((item) => item.id === selectedId.value) ?? filtered.value[0] ?? null
})

const statusLabel = (status: 'open' | 'in-progress' | 'resolved') => {
  if (status === 'open') {
    return 'Åpen'
  }

  if (status === 'in-progress') {
    return 'Pågår'
  }

  return 'Løst'
}

const severityLabel = (severity: 'low' | 'medium' | 'high') => {
  if (severity === 'high') {
    return 'Høy'
  }

  if (severity === 'medium') {
    return 'Medium'
  }

  return 'Lav'
}

const resetCreateForm = () => {
  createForm.title = ''
  createForm.description = ''
  createForm.locationText = ''
  createForm.severity = 'MAJOR'
  createForm.discoveredByName = ''
  createForm.occurredDate = ''
  createForm.occurredTime = ''
  createForm.sourceTemperatureEntryId = null
}

const clearCreateQuery = () => {
  const nextQuery = { ...route.query }
  delete nextQuery.openCreate
  delete nextQuery.source
  delete nextQuery.title
  delete nextQuery.description
  delete nextQuery.location
  delete nextQuery.severity
  delete nextQuery.discoverer
  delete nextQuery.sourceEntryId

  void router.replace({ query: nextQuery })
}

const openCreateModal = () => {
  createError.value = null
  createModalOpen.value = true
}

const closeCreateModal = () => {
  createModalOpen.value = false
  createError.value = null
  clearCreateQuery()
}

const openEditModal = () => {
  if (!selectedDeviation.value || !canAdmin.value) return
  if (selectedDeviation.value.status === 'resolved') {
    editError.value = 'Lukkede avvik kan ikke redigeres.'
    return
  }
  editError.value = null
  editForm.id = selectedDeviation.value.id
  editForm.title = selectedDeviation.value.title
  editForm.description = selectedDeviation.value.description
  editForm.locationText = selectedDeviation.value.location
  editForm.severity = selectedDeviation.value.severity === 'high' ? 'CRITICAL' : selectedDeviation.value.severity === 'medium' ? 'MAJOR' : 'MINOR'
  editForm.discoveredByName = selectedDeviation.value.reported_by
  editForm.occurredDate = selectedDeviation.value.reported_date || ''
  editForm.occurredTime = selectedDeviation.value.reported_time || ''
  editModalOpen.value = true
}

const closeEditModal = () => {
  editModalOpen.value = false
  editError.value = null
}

const startSelectedDeviationHandling = async () => {
  if (!selectedDeviation.value || !canManageDeviation.value) {
    return
  }

  if (selectedDeviation.value.status !== 'open') {
    editError.value = 'Avviket ma vaere apent for a starte behandling.'
    return
  }

  editError.value = null
  try {
    await startDeviationHandling(selectedDeviation.value.id)
  } catch (err: unknown) {
    const apiError = err as { response?: { data?: { message?: string } } }
    editError.value = apiError?.response?.data?.message ?? 'Kunne ikke starte behandling av avvik. Prov igjen.'
  }
}

const submitDeviation = async () => {
  if (!createForm.title.trim() || !createForm.description.trim()) {
    return
  }

  createError.value = null
  createInFlight.value = true

  try {
    const created = await createDeviation({
      reportType: 'INCIDENT',
      severity: createForm.severity,
      title: createForm.title.trim(),
      description: createForm.description.trim(),
      locationText: createForm.locationText.trim() || undefined,
      discoveredByName: createForm.discoveredByName.trim() || undefined,
      occurredDate: createForm.occurredDate || undefined,
      occurredTime: createForm.occurredTime || undefined,
      sourceTemperatureEntryId: createForm.sourceTemperatureEntryId ?? undefined,
    })

    selectedStatus.value = 'all'
    selectedId.value = created.reportId
    createModalOpen.value = false
    clearCreateQuery()
    resetCreateForm()
  } catch {
    createError.value = 'Kunne ikke opprette avvik. Prov igjen.'
  } finally {
    createInFlight.value = false
  }
}

const submitEditDeviation = async () => {
  if (!editForm.id || !editForm.title.trim() || !editForm.description.trim()) {
    return
  }

  editError.value = null
  editInFlight.value = true

  try {
    await updateDeviation(editForm.id, {
      reportType: 'INCIDENT',
      severity: editForm.severity,
      title: editForm.title.trim(),
      description: editForm.description.trim(),
      locationText: editForm.locationText.trim() || undefined,
      discoveredByName: editForm.discoveredByName.trim() || undefined,
      occurredDate: editForm.occurredDate || undefined,
      occurredTime: editForm.occurredTime || undefined,
    })
    closeEditModal()
  } catch (err: unknown) {
    const apiError = err as { response?: { data?: { message?: string } } }
    if (apiError?.response?.data?.message) {
      editError.value = apiError.response.data.message
    } else {
      editError.value = 'Kunne ikke oppdatere avvik. Prov igjen.'
    }
  } finally {
    editInFlight.value = false
  }
}

const removeSelectedDeviation = async () => {
  if (!selectedDeviation.value || !canAdmin.value) {
    return
  }

  const shouldDelete = window.confirm('Slette dette avviket?')
  if (!shouldDelete) {
    return
  }

  try {
    await deleteDeviation(selectedDeviation.value.id)
    selectedId.value = deviations[0]?.id ?? null
  } catch {
    editError.value = 'Kunne ikke slette avvik. Prov igjen.'
  }
}

const resolveSelectedDeviation = async () => {
  if (!selectedDeviation.value || !canManageDeviation.value) {
    return
  }

  if (selectedDeviation.value.status !== 'in-progress') {
    editError.value = 'Avviket ma vaere under behandling for a kunne loses.'
    return
  }

  try {
    await resolveDeviation(selectedDeviation.value.id)
  } catch (err: unknown) {
    const apiError = err as { response?: { data?: { message?: string } } }
    editError.value = apiError?.response?.data?.message ?? 'Kunne ikke løse avvik. Prov igjen.'
  }
}

watch(
  () => route.query,
  (query) => {
    if (query.openCreate !== '1') {
      return
    }

    createForm.title = typeof query.title === 'string' ? query.title : ''
    createForm.description = typeof query.description === 'string' ? query.description : ''
    createForm.locationText = typeof query.location === 'string' ? query.location : ''
    createForm.severity = query.severity === 'CRITICAL' ? 'CRITICAL' : query.severity === 'MINOR' ? 'MINOR' : 'MAJOR'
    createForm.discoveredByName = typeof query.discoverer === 'string' ? query.discoverer : ''
    createForm.sourceTemperatureEntryId = typeof query.sourceEntryId === 'string' ? Number(query.sourceEntryId) : null
    createForm.occurredDate = new Date().toISOString().slice(0, 10)
    createForm.occurredTime = new Date().toISOString().slice(11, 16)
    openCreateModal()
  },
  { immediate: true },
)
</script>

<template>
  <div class="deviations-page">
    <header class="page-header">
      <h1>Avvik</h1>
      <p class="subtitle">Registrer, prioriter og lukk avvik i en samlet arbeidsflate</p>
    </header>

    <div class="filter-row" role="tablist" aria-label="Filtrer avvik">
      <button class="filter-chip" :class="{ 'filter-chip--active': selectedStatus === 'all' }" @click="selectedStatus = 'all'">Alle</button>
      <button class="filter-chip" :class="{ 'filter-chip--active': selectedStatus === 'open' }" @click="selectedStatus = 'open'">Åpne</button>
      <button class="filter-chip" :class="{ 'filter-chip--active': selectedStatus === 'in-progress' }" @click="selectedStatus = 'in-progress'">Pågår</button>
      <button class="filter-chip" :class="{ 'filter-chip--active': selectedStatus === 'resolved' }" @click="selectedStatus = 'resolved'">Løste</button>
      <button class="create-btn" type="button" @click="openCreateModal">+ Registrer avvik</button>
    </div>

    <section class="deviations-layout">
      <aside class="deviation-list" aria-label="Avviksliste">
        <DeviationCard
          v-for="item in filtered"
          :key="item.id"
          :deviation="item"
          @view="selectedId = item.id"
        />
        <div v-if="filtered.length === 0" class="empty-state-message" role="status">
          <h3>Ingen avvik igjen</h3>
          <p>Flott arbeid. Det finnes ingen avvik som trenger oppfolging akkurat na.</p>
        </div>
      </aside>

      <article class="deviation-detail" v-if="selectedDeviation">
        <header class="deviation-detail__header">
          <h2>{{ selectedDeviation.title }}</h2>
          <span class="status-chip" :class="selectedDeviation.status === 'resolved' ? 'status-chip--good' : selectedDeviation.status === 'in-progress' ? 'status-chip--warn' : 'status-chip--danger'">
            {{ statusLabel(selectedDeviation.status) }}
          </span>
        </header>

        <div v-if="canManageDeviation" class="admin-actions">
          <button type="button" class="mini-btn" :disabled="selectedDeviation.status !== 'open'" @click="startSelectedDeviationHandling">Start behandling</button>
          <button type="button" class="mini-btn" :disabled="!canEditSelected" @click="openEditModal">Rediger</button>
          <button type="button" class="mini-btn" :disabled="!canResolveSelected" @click="resolveSelectedDeviation">Løs</button>
          <button v-if="canAdmin" type="button" class="mini-btn mini-btn--danger" @click="removeSelectedDeviation">Slett</button>
        </div>

        <p class="deviation-detail__description">{{ selectedDeviation.description }}</p>

        <div class="detail-grid">
          <div>
            <p class="detail-label">Meldt av</p>
            <p>{{ selectedDeviation.reported_by }}</p>
          </div>
          <div>
            <p class="detail-label">Tidspunkt</p>
            <p>{{ formatDate(selectedDeviation.reported_date) }} kl. {{ selectedDeviation.reported_time }}</p>
          </div>
          <div>
            <p class="detail-label">Umiddelbar handling</p>
            <p>{{ selectedDeviation.immediate_action }}</p>
          </div>
          <div>
            <p class="detail-label">Korrigerende tiltak</p>
            <p>{{ selectedDeviation.corrective_action }}</p>
          </div>
        </div>
      </article>

      <article v-else class="deviation-detail deviation-detail--empty" role="status">
        <h2>Ingen avvik gjenstar</h2>
        <p>Det er ingen registrerte avvik igjen i denne visningen. Alt ser bra ut.</p>
      </article>
    </section>

    <BaseModal :open="createModalOpen" title="Registrer nytt avvik" @close="closeCreateModal">
      <form class="deviation-form" @submit.prevent="submitDeviation">
        <label>
          Tittel
          <input v-model="createForm.title" type="text" required />
        </label>

        <label>
          Beskrivelse
          <textarea v-model="createForm.description" rows="4" required />
        </label>

        <label>
          Lokasjon
          <input v-model="createForm.locationText" type="text" />
        </label>

        <label>
          Alvorlighetsgrad
          <select v-model="createForm.severity">
            <option value="MINOR">Lav</option>
            <option value="MAJOR">Medium</option>
            <option value="CRITICAL">Høy</option>
          </select>
        </label>

        <label>
          Oppdaget av
          <input v-model="createForm.discoveredByName" type="text" />
        </label>

        <div class="deviation-form__row">
          <label>
            Dato
            <input v-model="createForm.occurredDate" type="date" />
          </label>
          <label>
            Tid
            <input v-model="createForm.occurredTime" type="time" />
          </label>
        </div>

        <p v-if="createError" class="deviation-form__error">{{ createError }}</p>
      </form>

      <template #footer>
        <button type="button" class="modal-btn modal-btn--ghost" @click="closeCreateModal">Avbryt</button>
        <button type="button" class="modal-btn" :disabled="createInFlight" @click="submitDeviation">Opprett avvik</button>
      </template>
    </BaseModal>

    <BaseModal :open="editModalOpen" title="Rediger avvik" @close="closeEditModal">
      <form class="deviation-form" @submit.prevent="submitEditDeviation">
        <label>
          Tittel
          <input v-model="editForm.title" type="text" required />
        </label>

        <label>
          Beskrivelse
          <textarea v-model="editForm.description" rows="4" required />
        </label>

        <label>
          Lokasjon
          <input v-model="editForm.locationText" type="text" />
        </label>

        <label>
          Alvorlighetsgrad
          <select v-model="editForm.severity">
            <option value="MINOR">Lav</option>
            <option value="MAJOR">Medium</option>
            <option value="CRITICAL">Høy</option>
          </select>
        </label>

        <div class="deviation-form__row">
          <label>
            Dato
            <input v-model="editForm.occurredDate" type="date" />
          </label>
          <label>
            Tid
            <input v-model="editForm.occurredTime" type="time" />
          </label>
        </div>

        <p v-if="editError" class="deviation-form__error">{{ editError }}</p>
      </form>

      <template #footer>
        <button type="button" class="modal-btn modal-btn--ghost" @click="closeEditModal">Avbryt</button>
        <button type="button" class="modal-btn" :disabled="editInFlight" @click="submitEditDeviation">Lagre</button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.deviations-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 1.25rem;
}

.page-header h1 {
  margin: 0;
  font-size: var(--font-size-2xl);
  color: var(--ik-mat-primary);
}

.subtitle {
  margin: 0.35rem 0 0;
  color: var(--color-gray-500);
  font-size: var(--font-size-sm);
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.create-btn {
  margin-left: auto;
  border: 1px solid var(--ik-mat-primary);
  border-radius: var(--radius-md);
  padding: 0.4rem 0.8rem;
  background: color-mix(in srgb, var(--ik-mat-primary) 10%, var(--color-card));
  color: var(--ik-mat-primary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
}

.filter-chip {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.4rem 0.8rem;
  background: var(--color-card);
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
}

.filter-chip--active {
  border-color: var(--ik-mat-primary);
  background: var(--ik-mat-primary);
  color: var(--color-primary-foreground);
}

.deviations-layout {
  display: grid;
  grid-template-columns: minmax(16rem, 22rem) 1fr;
  gap: 0.85rem;
}

.deviation-list {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  padding: 0.5rem;
  display: grid;
  gap: 0.45rem;
  align-content: start;
}

.empty-state-message {
  border: 1px dashed color-mix(in srgb, var(--ik-mat-primary) 45%, var(--color-border));
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--ik-mat-bg) 45%, var(--color-card));
  padding: 0.75rem;
}

.empty-state-message h3 {
  margin: 0;
  color: var(--ik-mat-primary);
  font-size: var(--font-size-sm);
}

.empty-state-message p {
  margin: 0.35rem 0 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-xs);
}

.deviation-list__item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-card);
  text-align: left;
  padding: 0.65rem;
}

.deviation-list__item--active {
  border-color: color-mix(in srgb, var(--ik-mat-primary) 45%, var(--color-border));
  background: color-mix(in srgb, var(--ik-mat-bg) 40%, var(--color-card));
}

.deviation-list__title {
  margin: 0;
  color: var(--color-foreground);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
}

.deviation-list__meta {
  margin: 0.2rem 0 0;
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
}

.deviation-list__chips {
  margin-top: 0.45rem;
  display: flex;
  gap: 0.4rem;
}

.deviation-detail {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  padding: 0.9rem;
}

.deviation-detail--empty {
  display: grid;
  align-content: center;
  gap: 0.5rem;
  min-height: 14rem;
}

.deviation-detail--empty h2 {
  margin: 0;
  color: var(--ik-mat-primary);
  font-size: var(--font-size-lg);
}

.deviation-detail--empty p {
  margin: 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
}

.deviation-detail__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
  margin-bottom: 0.65rem;
}

.deviation-detail__header h2 {
  margin: 0;
  font-size: var(--font-size-lg);
  color: var(--color-foreground);
}

.deviation-detail__description {
  margin: 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
}

.admin-actions {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.mini-btn {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-card);
  color: var(--color-foreground);
  padding: 0.35rem 0.6rem;
  font-size: var(--font-size-xs);
}

.mini-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.mini-btn--danger {
  color: var(--color-danger);
  border-color: color-mix(in srgb, var(--color-danger) 35%, var(--color-border));
}

.detail-grid {
  margin-top: 0.8rem;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.7rem;
}

.detail-label {
  margin: 0;
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.detail-grid p {
  margin: 0.2rem 0 0;
  color: var(--color-foreground);
  font-size: var(--font-size-sm);
}

.status-chip {
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  padding: 0.2rem 0.45rem;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
}

.status-chip--good {
  color: var(--color-success);
  background: var(--color-success-bg);
  border-color: color-mix(in srgb, var(--color-success) 35%, var(--color-border));
}

.status-chip--warn {
  color: var(--color-warning);
  background: var(--color-warning-bg);
  border-color: color-mix(in srgb, var(--color-warning) 35%, var(--color-border));
}

.status-chip--danger {
  color: var(--color-danger);
  background: var(--color-danger-bg);
  border-color: color-mix(in srgb, var(--color-danger) 35%, var(--color-border));
}

.status-chip--info {
  color: var(--color-info);
  background: var(--color-info-bg);
  border-color: color-mix(in srgb, var(--color-info) 35%, var(--color-border));
}

.deviation-form {
  display: grid;
  gap: 0.8rem;
}

.deviation-form label {
  display: grid;
  gap: 0.3rem;
  color: var(--color-gray-700);
  font-size: var(--font-size-sm);
}

.deviation-form input,
.deviation-form textarea,
.deviation-form select {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-card);
  padding: 0.55rem 0.7rem;
}

.deviation-form__row {
  display: grid;
  gap: 0.8rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.deviation-form__error {
  margin: 0;
  color: var(--color-danger);
  font-size: var(--font-size-xs);
}

.modal-btn {
  border: 1px solid var(--ik-mat-primary);
  background: var(--ik-mat-primary);
  color: #fff;
  border-radius: var(--radius-sm);
  padding: 0.45rem 0.8rem;
}

.modal-btn--ghost {
  border-color: var(--color-border);
  background: transparent;
  color: var(--color-foreground);
}

@media (max-width: 62rem) {
  .deviations-layout {
    grid-template-columns: 1fr;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .create-btn {
    margin-left: 0;
  }

  .deviation-form__row {
    grid-template-columns: 1fr;
  }
}
</style>
