<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import BaseModal from '@/shared/components/BaseModal.vue'
import { useAuthStore } from '@/stores/auth'
import { useIkMatData } from '../composables/useIkMatData'

type PointModalMode = 'add' | 'edit'

interface TemperatureInstance {
  pointId: number
  title: string
  locationId: number
  locationName: string
  minTemp: number
  maxTemp: number
  latestEntryId: number | null
  latestTemp: number | null
  latestRecordedBy: string
  latestDate: string
  latestTime: string
  isAlert: boolean
}

const router = useRouter()
const authStore = useAuthStore()

const {
  temperatureRecords,
  temperaturePoints,
  locations,
  orgUsers,
  isTemperatureInRange,
  createTemperaturePointWithLocation,
  updateTemperaturePointAndLocation,
  deleteTemperaturePoint,
  clearTemperatureMeasurementsForPoint,
  createTemperatureMeasurement,
  updateTemperatureMeasurement,
  isLoading,
  error,
} = useIkMatData()

const isMobile = ref(false)
const actionError = ref<string | null>(null)
const isSubmitting = ref(false)

const desktopMenuPointId = ref<number | null>(null)
const selectedMobileCardId = ref<number | null>(null)

const pointModalOpen = ref(false)
const pointModalMode = ref<PointModalMode>('add')
const editingPointId = ref<number | null>(null)
const editingLocationId = ref<number | null>(null)
const originalRange = ref<{ min: number | null; max: number | null }>({ min: null, max: null })
const pointForm = reactive({
  name: '',
  locationName: '',
  minTempC: '',
  maxTempC: '',
})

const measurementModalOpen = ref(false)
const measurementPointId = ref<number | null>(null)
const editingMeasurementEntryId = ref<number | null>(null)
const measurementForm = reactive({
  temperatureC: '',
  measuredDate: '',
  measuredTime: '',
  employeeId: null as number | null,
  employeeName: '',
})

const canManage = computed(() => authStore.hasRole('ADMIN', 'MANAGER'))

const alerts = computed(() => instances.value.filter((item) => item.isAlert))
const okCount = computed(() => instances.value.filter((item) => !item.isAlert && item.latestTemp !== null).length)

const locationById = computed(() => {
  return new Map(locations.map((location) => [location.locationId, location]))
})

const latestRecordByPoint = computed(() => {
  const map = new Map<number, (typeof temperatureRecords)[number]>()

  for (const record of temperatureRecords) {
    if (!map.has(record.log_point_id)) {
      map.set(record.log_point_id, record)
    }
  }

  return map
})

const instances = computed<TemperatureInstance[]>(() => {
  return temperaturePoints
    .filter((point) => point.isActive !== false)
    .map((point) => {
      const latest = latestRecordByPoint.value.get(point.logPointId)
      const location = locationById.value.get(point.locationId)

      const minTemp = Number(location?.tempMinC ?? 0)
      const maxTemp = Number(location?.tempMaxC ?? 4)
      const isAlert = latest ? !isTemperatureInRange(latest) : false

      return {
        pointId: point.logPointId,
        title: point.name,
        locationId: point.locationId,
        locationName: point.locationName ?? location?.name ?? 'Ukjent lokasjon',
        minTemp,
        maxTemp,
        latestEntryId: latest?.id ?? null,
        latestTemp: latest ? latest.temperature_c : null,
        latestRecordedBy: latest?.recorded_by ?? '-',
        latestDate: latest?.recorded_date ?? '-',
        latestTime: latest?.recorded_time ?? '-',
        isAlert,
      }
    })
    .sort((a, b) => {
      if (a.isAlert !== b.isAlert) {
        return a.isAlert ? -1 : 1
      }
      return a.title.localeCompare(b.title, 'nb')
    })
})

const currentMeasurementPoint = computed(() => {
  return instances.value.find((item) => item.pointId === measurementPointId.value) ?? null
})

const selectedLocationLabel = computed(() => {
  if (!pointForm.locationName.trim()) {
    return null
  }
  return `${pointForm.locationName.trim()} (${pointForm.minTempC || '-'} til ${pointForm.maxTempC || '-'}°C)`
})

const employeeOptions = computed(() => {
  return orgUsers
    .filter((user) => user.isActive)
    .map((user) => ({
      id: user.userId,
      label: user.displayName || user.email,
    }))
})

const updateViewport = () => {
  if (typeof window === 'undefined') {
    return
  }

  isMobile.value = window.matchMedia('(max-width: 47.99rem)').matches
}

const resetPointForm = () => {
  pointForm.name = ''
  pointForm.locationName = ''
  pointForm.minTempC = ''
  pointForm.maxTempC = ''
  editingLocationId.value = null
  originalRange.value = { min: null, max: null }
}

const openAddPointModal = () => {
  pointModalMode.value = 'add'
  editingPointId.value = null
  resetPointForm()
  actionError.value = null
  pointModalOpen.value = true
}

const openEditPointModal = (pointId: number) => {
  const point = temperaturePoints.find((item) => item.logPointId === pointId)
  if (!point) {
    return
  }
  const location = locationById.value.get(point.locationId)

  pointModalMode.value = 'edit'
  editingPointId.value = pointId
  pointForm.name = point.name
  pointForm.locationName = location?.name ?? point.locationName ?? ''
  pointForm.minTempC = location?.tempMinC != null ? String(location.tempMinC) : ''
  pointForm.maxTempC = location?.tempMaxC != null ? String(location.tempMaxC) : ''
  originalRange.value = {
    min: location?.tempMinC != null ? Number(location.tempMinC) : null,
    max: location?.tempMaxC != null ? Number(location.tempMaxC) : null,
  }
  editingLocationId.value = point.locationId
  actionError.value = null
  pointModalOpen.value = true
  desktopMenuPointId.value = null
  selectedMobileCardId.value = null
}

const closePointModal = () => {
  pointModalOpen.value = false
}

const savePoint = async () => {
  const minTemp = Number(pointForm.minTempC)
  const maxTemp = Number(pointForm.maxTempC)

  if (!canManage.value || !pointForm.name.trim() || !pointForm.locationName.trim()) {
    return
  }

  if (!Number.isFinite(minTemp) || !Number.isFinite(maxTemp)) {
    return
  }

  actionError.value = null
  isSubmitting.value = true

  try {
    const locationPayload = {
      name: pointForm.locationName.trim(),
      locationType: 'OTHER' as const,
      tempMinC: minTemp,
      tempMaxC: maxTemp,
      isActive: true,
    }

    const pointPayload = {
      name: pointForm.name.trim(),
      isActive: true,
    }

    if (pointModalMode.value === 'add') {
      await createTemperaturePointWithLocation(locationPayload, pointPayload)
    } else if (editingPointId.value !== null && editingLocationId.value !== null) {
      const didChangeRange = originalRange.value.min !== minTemp || originalRange.value.max !== maxTemp

      await updateTemperaturePointAndLocation(editingPointId.value, editingLocationId.value, locationPayload, {
        ...pointPayload,
        locationId: editingLocationId.value,
      })

      if (didChangeRange) {
        await clearTemperatureMeasurementsForPoint(editingPointId.value)
      }
    }

    pointModalOpen.value = false
  } catch {
    actionError.value = 'Kunne ikke lagre temperaturpunkt. Prov igjen.'
  } finally {
    isSubmitting.value = false
  }
}

const removePoint = async (pointId: number) => {
  if (!canManage.value) {
    return
  }

  const shouldDelete = window.confirm('Slette temperaturpunktet?')
  if (!shouldDelete) {
    return
  }

  actionError.value = null
  isSubmitting.value = true

  try {
    await deleteTemperaturePoint(pointId)
    desktopMenuPointId.value = null
    selectedMobileCardId.value = null
  } catch {
    actionError.value = 'Kunne ikke slette temperaturpunkt. Prov igjen.'
  } finally {
    isSubmitting.value = false
  }
}

const openMeasurementModal = (pointId: number) => {
  measurementPointId.value = pointId
  const instance = instances.value.find((item) => item.pointId === pointId)
  const now = new Date()
  const defaultDate = now.toISOString().slice(0, 10)
  const defaultTime = now.toISOString().slice(11, 16)

  measurementForm.temperatureC = instance?.latestTemp != null ? String(instance.latestTemp) : ''
  measurementForm.measuredDate = instance?.latestDate && instance.latestDate !== '-' ? instance.latestDate : defaultDate
  measurementForm.measuredTime = instance?.latestTime && instance.latestTime !== '-' ? instance.latestTime : defaultTime
  measurementForm.employeeName = instance?.latestRecordedBy && instance.latestRecordedBy !== '-' ? instance.latestRecordedBy : (authStore.user?.name ?? '')

  const matchedEmployee = employeeOptions.value.find((employee) => employee.label === measurementForm.employeeName)
  measurementForm.employeeId = matchedEmployee?.id ?? null
  editingMeasurementEntryId.value = instance?.latestEntryId ?? null

  actionError.value = null
  measurementModalOpen.value = true
  desktopMenuPointId.value = null
}

const closeMeasurementModal = () => {
  measurementModalOpen.value = false
  editingMeasurementEntryId.value = null
}

const combinedMeasuredAt = () => {
  if (!measurementForm.measuredDate || !measurementForm.measuredTime) {
    return new Date().toISOString()
  }
  return new Date(`${measurementForm.measuredDate}T${measurementForm.measuredTime}:00`).toISOString()
}

const submitMeasurement = async () => {
  if (measurementPointId.value === null) {
    return
  }

  const numericTemp = Number(measurementForm.temperatureC)
  if (!Number.isFinite(numericTemp)) {
    return
  }

  actionError.value = null
  isSubmitting.value = true

  try {
    const selectedEmployee = measurementForm.employeeId != null
      ? employeeOptions.value.find((employee) => employee.id === measurementForm.employeeId)
      : null
    const measuredBy = selectedEmployee?.label ?? measurementForm.employeeName.trim()
    const payload = {
      logPointId: measurementPointId.value,
      temperatureC: numericTemp,
      measuredAt: combinedMeasuredAt(),
      noteText: measuredBy ? `Malt av: ${measuredBy}` : undefined,
      recordedByUserId: measurementForm.employeeId ?? undefined,
    }

    if (editingMeasurementEntryId.value) {
      await updateTemperatureMeasurement(editingMeasurementEntryId.value, payload)
    } else {
      await createTemperatureMeasurement(payload)
    }

    measurementModalOpen.value = false
    selectedMobileCardId.value = null
  } catch {
    actionError.value = 'Kunne ikke registrere temperaturmaling. Prov igjen.'
  } finally {
    isSubmitting.value = false
  }
}

const openDeviationFlow = (instance: TemperatureInstance) => {
  const measuredLabel = instance.latestTemp === null ? 'ukjent temperatur' : `${instance.latestTemp}°C`
  const description = `Temperatur ved ${instance.title} (${instance.locationName}) ble malt til ${measuredLabel}. Gyldig omrade er ${instance.minTemp}°C til ${instance.maxTemp}°C.`

  void router.push({
    name: 'Deviations',
    query: {
      openCreate: '1',
      source: 'temperature',
      title: `Temperaturavvik - ${instance.title}`,
      location: instance.locationName,
      description,
      severity: 'MAJOR',
      discoverer: authStore.user?.name ?? '',
      sourceEntryId: instance.latestEntryId != null ? String(instance.latestEntryId) : undefined,
    },
  })
}

const toggleDesktopMenu = (pointId: number) => {
  desktopMenuPointId.value = desktopMenuPointId.value === pointId ? null : pointId
}

const toggleMobileActions = (pointId: number) => {
  selectedMobileCardId.value = selectedMobileCardId.value === pointId ? null : pointId
}

onMounted(() => {
  updateViewport()
  window.addEventListener('resize', updateViewport)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateViewport)
})
</script>

<template>
  <div class="temperature-page">
    <header class="page-header">
      <h1>Temperaturkontroll</h1>
      <p class="subtitle">Kontinuerlig overvaking av kjol, frys og varmholding</p>
    </header>

    <section v-if="alerts.length > 0" class="alert-banner" role="alert">
      <p><strong>{{ alerts.length }}</strong> malepunkter har temperaturavvik og trenger oppfolging.</p>
      <router-link :to="{ name: 'Deviations' }">Se avvik</router-link>
    </section>

    <section class="summary-grid" aria-label="Temperaturstatus">
      <article class="summary-card">
        <p>Temperaturpunkter</p>
        <strong>{{ instances.length }}</strong>
      </article>
      <article class="summary-card summary-card--good">
        <p>Innenfor grense</p>
        <strong>{{ okCount }}</strong>
      </article>
      <article class="summary-card summary-card--warn">
        <p>Avvik</p>
        <strong>{{ alerts.length }}</strong>
      </article>
    </section>

    <section v-if="error" class="inline-error">{{ error }}</section>
    <section v-else-if="actionError" class="inline-error">{{ actionError }}</section>

    <section v-if="!isMobile" class="desktop-list" aria-label="Temperaturpunkter">
      <header class="desktop-list__header">
        <span>Tittel</span>
        <span>Temperatur</span>
        <span>Gyldig omrade</span>
        <span>Status</span>
        <span>Handlinger</span>
      </header>

      <article
        v-for="instance in instances"
        :key="instance.pointId"
        class="desktop-row"
        :class="{ 'desktop-row--alert': instance.isAlert }"
      >
        <div>
          <p class="desktop-row__title">{{ instance.title }}</p>
          <p class="desktop-row__meta">{{ instance.locationName }}</p>
        </div>

        <div>
          <p class="desktop-row__value">{{ instance.latestTemp === null ? '-' : `${instance.latestTemp}°C` }}</p>
          <p class="desktop-row__meta">{{ instance.latestDate }} {{ instance.latestTime === '-' ? '' : `kl. ${instance.latestTime}` }}</p>
        </div>

        <p class="desktop-row__value">{{ instance.minTemp }}°C til {{ instance.maxTemp }}°C</p>

        <span class="status-pill" :class="instance.isAlert ? 'status-pill--danger' : 'status-pill--good'">
          {{ instance.isAlert ? 'Avvik' : instance.latestTemp === null ? 'Ikke malt' : 'OK' }}
        </span>

        <div class="desktop-actions">
          <button
            type="button"
            class="action-btn"
            :class="instance.isAlert ? 'action-btn--danger' : ''"
            @click="instance.isAlert ? openDeviationFlow(instance) : openMeasurementModal(instance.pointId)"
          >
            {{ instance.isAlert ? 'Registrer avvik' : instance.latestEntryId ? 'Rediger maling' : 'Registrer maling' }}
          </button>

          <div v-if="canManage" class="options-menu">
            <button
              class="options-menu__trigger"
              type="button"
              aria-label="Apne handlinger"
              :aria-expanded="desktopMenuPointId === instance.pointId"
              @click="toggleDesktopMenu(instance.pointId)"
            >
              <span class="dot" />
              <span class="dot" />
              <span class="dot" />
            </button>

            <div v-if="desktopMenuPointId === instance.pointId" class="options-menu__list" role="menu">
              <button type="button" role="menuitem" class="options-menu__item" @click="openEditPointModal(instance.pointId)">Rediger</button>
              <button type="button" role="menuitem" class="options-menu__item options-menu__item--danger" @click="removePoint(instance.pointId)">Slett</button>
            </div>
          </div>
        </div>
      </article>

      <p v-if="instances.length === 0 && !isLoading" class="empty-state">Ingen temperaturpunkter registrert.</p>

      <button v-if="canManage" type="button" class="add-item-btn" @click="openAddPointModal">
        + Legg til temperaturpunkt
      </button>
    </section>

    <section v-else class="mobile-cards" aria-label="Temperaturkort">
      <article
        v-for="instance in instances"
        :key="instance.pointId"
        class="mobile-card"
        :class="{ 'mobile-card--alert': instance.isAlert, 'mobile-card--active': selectedMobileCardId === instance.pointId }"
        @click="toggleMobileActions(instance.pointId)"
      >
        <div v-if="canManage && selectedMobileCardId === instance.pointId" class="mobile-card__manage" @click.stop>
          <button type="button" class="mobile-card__manage-btn" @click="openEditPointModal(instance.pointId)">Rediger</button>
          <button type="button" class="mobile-card__manage-btn mobile-card__manage-btn--danger" @click="removePoint(instance.pointId)">Slett</button>
        </div>

        <header class="mobile-card__header">
          <h2>{{ instance.title }}</h2>
          <span class="status-pill" :class="instance.isAlert ? 'status-pill--danger' : 'status-pill--good'">
            {{ instance.isAlert ? 'Avvik' : instance.latestTemp === null ? 'Ikke malt' : 'OK' }}
          </span>
        </header>

        <p class="mobile-card__meta">{{ instance.locationName }}</p>
        <p class="mobile-card__temperature">{{ instance.latestTemp === null ? '-' : `${instance.latestTemp}°C` }}</p>
        <p class="mobile-card__meta">Gyldig: {{ instance.minTemp }}°C til {{ instance.maxTemp }}°C</p>
        <p class="mobile-card__meta">{{ instance.latestDate }} {{ instance.latestTime === '-' ? '' : `kl. ${instance.latestTime}` }}</p>
        <p class="mobile-card__meta">Malt av: {{ instance.latestRecordedBy }}</p>

        <button
          type="button"
          class="action-btn mobile-card__action"
          :class="instance.isAlert ? 'action-btn--danger' : ''"
          @click.stop="instance.isAlert ? openDeviationFlow(instance) : openMeasurementModal(instance.pointId)"
        >
          {{ instance.isAlert ? 'Registrer avvik' : instance.latestEntryId ? 'Rediger maling' : 'Registrer maling' }}
        </button>
      </article>

      <p v-if="instances.length === 0 && !isLoading" class="empty-state">Ingen temperaturpunkter registrert.</p>

      <button v-if="canManage" type="button" class="add-item-btn" @click="openAddPointModal">
        + Legg til temperaturpunkt
      </button>
    </section>

    <BaseModal
      :open="pointModalOpen"
      :title="pointModalMode === 'add' ? 'Legg til temperaturpunkt' : 'Rediger temperaturpunkt'"
      @close="closePointModal"
    >
      <form class="modal-form" @submit.prevent="savePoint">
        <label>
          Kontrollpunkt
          <input v-model="pointForm.name" type="text" required />
        </label>

        <label>
          Lokasjonsnavn
          <input v-model="pointForm.locationName" type="text" required />
        </label>

        <div class="modal-form__row">
          <label>
            Min temperatur (°C)
            <input v-model="pointForm.minTempC" type="number" step="0.1" required />
          </label>
          <label>
            Maks temperatur (°C)
            <input v-model="pointForm.maxTempC" type="number" step="0.1" required />
          </label>
        </div>

        <p v-if="Number(pointForm.minTempC) > Number(pointForm.maxTempC)" class="modal-form__error">
          Min temperatur kan ikke være høyere enn maks temperatur.
        </p>

        <label>
          Gyldig temperaturintervall
          <input :value="`${pointForm.minTempC || '-'}°C til ${pointForm.maxTempC || '-'}°C`" type="text" disabled />
        </label>

        <p v-if="selectedLocationLabel" class="modal-form__hint">Gyldig temperatur: {{ selectedLocationLabel }}</p>
      </form>

      <template #footer>
        <button type="button" class="modal-btn modal-btn--ghost" @click="closePointModal">Avbryt</button>
        <button type="button" class="modal-btn" :disabled="isSubmitting || Number(pointForm.minTempC) > Number(pointForm.maxTempC)" @click="savePoint">Lagre</button>
      </template>
    </BaseModal>

    <BaseModal
      :open="measurementModalOpen"
      title="Registrer temperaturmaling"
      @close="closeMeasurementModal"
    >
      <form class="modal-form" @submit.prevent="submitMeasurement">
        <p v-if="currentMeasurementPoint" class="modal-form__hint">
          {{ currentMeasurementPoint.title }}: {{ currentMeasurementPoint.minTemp }}°C til {{ currentMeasurementPoint.maxTemp }}°C
        </p>

        <label>
          Malt temperatur (°C)
          <input v-model="measurementForm.temperatureC" type="number" step="0.1" required />
        </label>

        <label>
          Dato
          <input v-model="measurementForm.measuredDate" type="date" required />
        </label>

        <label>
          Tid
          <input v-model="measurementForm.measuredTime" type="time" required />
        </label>

        <label>
          Ansatt
          <select v-if="employeeOptions.length > 0" v-model.number="measurementForm.employeeId">
            <option :value="null">Velg ansatt</option>
            <option v-for="employee in employeeOptions" :key="employee.id" :value="employee.id">{{ employee.label }}</option>
          </select>
          <input v-model="measurementForm.employeeName" type="text" :required="employeeOptions.length === 0" placeholder="Navn på ansatt" />
        </label>
      </form>

      <template #footer>
        <button type="button" class="modal-btn modal-btn--ghost" @click="closeMeasurementModal">Avbryt</button>
        <button type="button" class="modal-btn" :disabled="isSubmitting" @click="submitMeasurement">{{ editingMeasurementEntryId ? 'Oppdater' : 'Send inn' }}</button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.temperature-page {
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

.alert-banner {
  border: 1px solid color-mix(in srgb, var(--color-danger) 35%, var(--color-border));
  background: var(--color-danger-bg);
  color: color-mix(in srgb, var(--color-danger) 70%, var(--color-foreground));
  border-radius: var(--radius-md);
  padding: 0.75rem 0.9rem;
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.95rem;
}

.alert-banner p {
  margin: 0;
  font-size: var(--font-size-sm);
}

.alert-banner a {
  color: inherit;
  text-decoration: underline;
  font-size: var(--font-size-sm);
  white-space: nowrap;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr));
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.summary-card {
  border: 1px solid var(--color-border);
  background: var(--color-card);
  border-radius: var(--radius-md);
  padding: 0.8rem;
}

.summary-card p {
  margin: 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-xs);
}

.summary-card strong {
  display: block;
  margin-top: 0.35rem;
  color: var(--color-foreground);
  font-size: 1.45rem;
}

.summary-card--good {
  border-left: 0.25rem solid var(--color-success);
}

.summary-card--warn {
  border-left: 0.25rem solid var(--color-warning);
}

.inline-error {
  margin-bottom: 0.85rem;
  border: 1px solid color-mix(in srgb, var(--color-danger) 40%, var(--color-border));
  background: var(--color-danger-bg);
  color: var(--color-danger);
  border-radius: var(--radius-md);
  padding: 0.65rem 0.75rem;
  font-size: var(--font-size-sm);
}

.desktop-list {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--color-card);
}

.desktop-list__header,
.desktop-row {
  display: grid;
  grid-template-columns: minmax(10rem, 1.5fr) minmax(8rem, 1fr) minmax(8rem, 1fr) minmax(6rem, auto) minmax(12rem, 1fr);
  gap: 0.75rem;
  align-items: center;
  padding: 0.75rem 0.85rem;
}

.desktop-list__header {
  background: color-mix(in srgb, var(--ik-mat-bg) 55%, var(--color-card));
  border-bottom: 1px solid var(--color-border);
  color: var(--color-gray-600);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.desktop-row {
  border-bottom: 1px solid var(--color-border);
}

.desktop-row:last-of-type {
  border-bottom: none;
}

.desktop-row--alert {
  background: color-mix(in srgb, var(--color-danger-bg) 70%, var(--color-card));
}

.desktop-row__title {
  margin: 0;
  color: var(--color-foreground);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
}

.desktop-row__meta {
  margin: 0.2rem 0 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-xs);
}

.desktop-row__value {
  margin: 0;
  color: var(--color-foreground);
  font-size: var(--font-size-sm);
}

.desktop-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 0.5rem;
  position: relative;
}

.action-btn {
  border: 1px solid var(--ik-mat-primary);
  background: color-mix(in srgb, var(--ik-mat-primary) 10%, var(--color-card));
  color: var(--ik-mat-primary);
  border-radius: var(--radius-sm);
  padding: 0.35rem 0.6rem;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
}

.action-btn--danger {
  border-color: var(--color-danger);
  background: var(--color-danger-bg);
  color: var(--color-danger);
}

.options-menu {
  position: relative;
}

.options-menu__trigger {
  aspect-ratio: 1 / 1;
  border: 1px solid var(--color-border);
  background: var(--color-card);
  border-radius: var(--radius-sm);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.15rem;
  cursor: pointer;
  width: 2rem;
}

.dot {
  width: 0.2rem;
  height: 0.2rem;
  background: var(--color-gray-600);
  border-radius: 100%;
}

.options-menu__list {
  position: absolute;
  top: calc(100% + 0.25rem);
  right: 0;
  z-index: 20;
  min-width: 9rem;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.options-menu__item {
  width: 100%;
  border: 0;
  background: transparent;
  padding: 0.6rem 0.8rem;
  text-align: left;
  color: var(--color-foreground);
  font-size: var(--font-size-sm);
}

.options-menu__item:hover {
  background: var(--color-accent);
}

.options-menu__item--danger {
  color: var(--color-danger);
}

.mobile-cards {
  display: grid;
  gap: 0.85rem;
}

.mobile-card {
  position: relative;
  border: 1px solid var(--color-border);
  background: var(--color-card);
  border-radius: var(--radius-md);
  padding: 0.85rem;
}

.mobile-card--alert {
  border-left: 0.25rem solid var(--color-danger);
  background: var(--color-danger-bg);
}

.mobile-card--active {
  box-shadow: var(--shadow-md);
}

.mobile-card__manage {
  display: flex;
  gap: 0.45rem;
  margin-bottom: 0.7rem;
}

.mobile-card__manage-btn {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-card);
  color: var(--color-foreground);
  padding: 0.35rem 0.55rem;
  font-size: var(--font-size-xs);
}

.mobile-card__manage-btn--danger {
  border-color: color-mix(in srgb, var(--color-danger) 40%, var(--color-border));
  color: var(--color-danger);
}

.mobile-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.6rem;
}

.mobile-card__header h2 {
  margin: 0;
  color: var(--color-foreground);
  font-size: var(--font-size-base);
}

.mobile-card__temperature {
  margin: 0.4rem 0;
  color: var(--color-foreground);
  font-size: 1.7rem;
  font-weight: var(--font-weight-bold);
}

.mobile-card__meta {
  margin: 0.25rem 0 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-xs);
}

.mobile-card__action {
  margin-top: 0.65rem;
  width: 100%;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  padding: 0.2rem 0.45rem;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
}

.status-pill--good {
  color: var(--color-success);
  background: var(--color-success-bg);
  border-color: color-mix(in srgb, var(--color-success) 35%, var(--color-border));
}

.status-pill--danger {
  color: var(--color-danger);
  background: var(--color-danger-bg);
  border-color: color-mix(in srgb, var(--color-danger) 35%, var(--color-border));
}

.add-item-btn {
  margin-top: 1rem;
  padding: 0.65rem 1rem;
  border: 1px solid var(--ik-mat-primary);
  background: color-mix(in srgb, var(--ik-mat-primary) 7%, var(--color-card));
  color: var(--ik-mat-primary);
  font-weight: var(--font-weight-semibold);
  border-radius: var(--radius-md);
}

.empty-state {
  margin-top: 0.9rem;
  text-align: center;
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
}

.modal-form {
  display: grid;
  gap: 0.8rem;
}

.modal-form label {
  display: grid;
  gap: 0.3rem;
  font-size: var(--font-size-sm);
  color: var(--color-gray-700);
}

.modal-form input,
.modal-form select {
  border: 1px solid var(--color-border);
  padding: 0.55rem 0.7rem;
  border-radius: var(--radius-sm);
  background: var(--color-card);
}

.modal-form__hint {
  margin: 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-xs);
}

.modal-form__row {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.modal-form__error {
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

@media (max-width: 47.99rem) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .modal-form__row {
    grid-template-columns: 1fr;
  }
}
</style>