<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import BaseModal from '@/shared/components/BaseModal.vue'
import { useAuthStore } from '@/stores/auth'
import { useIkMatData } from '../composables/useIkMatData'
import { getOrganizationTempDefaults } from '@/shared/utils/orgSettings'
import { getUsers } from '@/features/admin/api/users'
import type { TemperatureRecord } from '../types/domain'
import DeviationReportForm from '@/shared/components/DeviationReportForm.vue'
import { client } from '@/api/client'

//  composable
const {
  temperatureRecords,
  temperaturePoints,
  locations,
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

const router = useRouter()
const authStore = useAuthStore()

// Fetch employees directly - orgUsers in useIkMatData is never populated
interface Employee { id: number; label: string; email: string }
const employees = ref<Employee[]>([])

onMounted(async () => {
  const orgNumber = authStore.currentOrg?.orgNumber
  if (!orgNumber) return
  try {
    const users = await getUsers(orgNumber)
    employees.value = users
        .filter((u) => u.isActive)
        .map((u) => ({ id: u.userId, label: u.displayName || u.email, email: u.email }))
  } catch {
    // non-critical - dropdown just stays empty
  }
})

//  tabs
type Tab = 'locations' | 'logs'
const activeTab = ref<Tab>('locations')

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

// Options modal state
const pointOptionsModalOpen = ref(false)
const selectedPointForOptionsId = ref<number | null>(null)

//  derived data
const canManage = computed(() => authStore.hasRole('ADMIN', 'MANAGER'))
const isMobile = ref(false)

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

const activePoints = computed(() =>
    temperaturePoints.filter((p) => p.isActive !== false)
)

const locationById = computed(() =>
    new Map(locations.map((l) => [l.locationId, l]))
)

// Latest record per log point
const latestByPoint = computed(() => {
  const map = new Map<number, TemperatureRecord>()
  for (const r of temperatureRecords) {
    if (!map.has(r.log_point_id)) map.set(r.log_point_id, r)
  }
  return map
})

// Location rows: one row per active point, showing latest log
interface LocationRow {
  pointId: number
  pointName: string
  locationId: number
  locationName: string
  minTemp: number | null
  maxTemp: number | null
  latest: TemperatureRecord | null
  isAlert: boolean
}

const locationRows = computed<LocationRow[]>(() =>
    activePoints.value.map((point) => {
      const loc  = locationById.value.get(point.locationId)
      const latest = latestByPoint.value.get(point.logPointId) ?? null
      const isAlert = latest ? !isTemperatureInRange(latest) : false
      return {
        pointId:      point.logPointId,
        pointName:    point.name,
        locationId:   point.locationId,
        locationName: point.locationName ?? loc?.name ?? '-',
        minTemp:      loc?.tempMinC ?? null,
        maxTemp:      loc?.tempMaxC ?? null,
        latest,
        isAlert,
      }
    }).sort((a, b) => {
      if (a.isAlert !== b.isAlert) return a.isAlert ? -1 : 1
      return a.locationName.localeCompare(b.locationName, 'nb')
    })
)

// All logs sorted newest first
const allLogs = computed<TemperatureRecord[]>(() =>
    [...temperatureRecords].sort((a, b) => {
      const da = `${a.recorded_date}T${a.recorded_time}`
      const db = `${b.recorded_date}T${b.recorded_time}`
      return db.localeCompare(da)
    })
)

const alerts = computed(() => locationRows.value.filter((r) => r.isAlert))
const okCount = computed(() => locationRows.value.filter((r) => !r.isAlert && r.latest).length)

const employeeOptions = computed(() => employees.value)

// Match logged-in user by email against fetched employees
const selfEmployeeId = computed(() => {
  const me = authStore.email?.toLowerCase()
  return employees.value.find((e) => e.email.toLowerCase() === me)?.id ?? null
})

const pointOptions = computed(() =>
    activePoints.value.map((p) => {
      const locName = p.locationName ?? locationById.value.get(p.locationId)?.name
      return {
        id: p.logPointId,
        label: locName ? `${p.name} - ${locName}` : p.name,
      }
    })
)

const resetPointForm = () => {
  const defaults = getOrganizationTempDefaults(authStore.currentOrg?.orgNumber)
  pointForm.name = ''
  pointForm.locationName = ''
  pointForm.minTempC = defaults.min != null ? String(defaults.min) : ''
  pointForm.maxTempC = defaults.max != null ? String(defaults.max) : ''
  editingLocationId.value = null
  originalRange.value = { min: defaults.min, max: defaults.max }
}

const instances = computed<TemperatureInstance[]>(() => {
  return locationRows.value.map((row) => ({
    pointId: row.pointId,
    title: row.pointName,
    locationId: row.locationId,
    locationName: row.locationName,
    minTemp: row.minTemp ?? 0,
    maxTemp: row.maxTemp ?? 4,
    latestEntryId: row.latest?.id ?? null,
    latestTemp: row.latest?.temperature_c ?? null,
    latestRecordedBy: row.latest?.recorded_by ?? '-',
    latestDate: row.latest?.recorded_date ?? '-',
    latestTime: row.latest?.recorded_time ?? '-',
    isAlert: row.isAlert,
  }))
})

const selectedLocationLabel = computed(() => {
  if (!pointForm.locationName.trim()) {
    return null
  }
  return `${pointForm.locationName.trim()} (${pointForm.minTempC || '-'} til ${pointForm.maxTempC || '-'}°C)`
})
//  log modal
const modalOpen   = ref(false)
const isSubmitting = ref(false)
const actionError  = ref<string | null>(null)
const editingEntryId = ref<number | null>(null)

const measurementModalOpen = modalOpen
const editingMeasurementEntryId = editingEntryId
const measurementForm = reactive({
  get temperatureC() {
    return form.temperatureC
  },
  set temperatureC(value: string) {
    form.temperatureC = value
  },
  get measuredDate() {
    return form.date
  },
  set measuredDate(value: string) {
    form.date = value
  },
  get measuredTime() {
    return form.time
  },
  set measuredTime(value: string) {
    form.time = value
  },
  get employeeId() {
    return form.employeeId
  },
  set employeeId(value: number | null) {
    form.employeeId = value
  },
  get employeeName() {
    const selected = employeeOptions.value.find((employee) => employee.id === form.employeeId)
    return selected?.label ?? ''
  },
  set employeeName(value: string) {
    const selected = employeeOptions.value.find((employee) => employee.label === value)
    form.employeeId = selected?.id ?? null
  },
})

const form = reactive({
  pointId:     null as number | null,
  temperatureC: '',
  useNow:      true,
  date:        todayStr(),
  time:        nowTimeStr(),
  employeeId:  null as number | null,
})

function todayStr() {
  return new Date().toISOString().slice(0, 10)
}
function nowTimeStr() {
  return new Date().toTimeString().slice(0, 5)
}

function openModal(pointId?: number, existing?: TemperatureRecord | null) {
  actionError.value = null
  editingEntryId.value = existing?.id ?? null

  form.pointId      = pointId ?? null
  form.temperatureC = existing ? String(existing.temperature_c) : ''
  form.useNow       = !existing
  form.date         = existing?.recorded_date ?? todayStr()
  form.time         = existing?.recorded_time?.slice(0, 5) ?? nowTimeStr()

  // Employee: when editing keep whoever recorded it, otherwise default to self
  if (existing?.recorded_by) {
    const match = employeeOptions.value.find((e) => e.label === existing.recorded_by)
    form.employeeId = match?.id ?? selfEmployeeId.value
  } else {
    form.employeeId = selfEmployeeId.value
  }

  modalOpen.value = true
}

function closeModal() {
  modalOpen.value = false
}

const openMeasurementModal = (pointId: number) => {
  openModal(pointId)
  desktopMenuPointId.value = null
}

const closeMeasurementModal = () => {
  closeModal()
  selectedMobileCardId.value = null
}

const submitMeasurement = submit

const currentMeasurementPoint = computed(() => {
  return instances.value.find((item) => item.pointId === form.pointId) ?? null
})

function measuredAt(): string {
  if (form.useNow) return new Date().toISOString()
  return new Date(`${form.date}T${form.time}:00`).toISOString()
}

const currentRange = computed(() => {
  if (form.pointId === null) return null
  const point = activePoints.value.find((p) => p.logPointId === form.pointId)
  if (!point) return null
  const loc = locationById.value.get(point.locationId)
  if (!loc) return null
  return { min: loc.tempMinC, max: loc.tempMaxC, name: point.name }
})

const isValidTemp = computed(() => {
  const s = form.temperatureC.trim()
  if (!s) return false
  const t = Number(s)
  return Number.isFinite(t)
})

const tempOutOfRange = computed(() => {
  if (!isValidTemp.value || !currentRange.value) return false
  const t = Number(form.temperatureC.trim())
  const { min, max } = currentRange.value
  return (min != null && t < min) || (max != null && t > max)
})

function onPointChange(e: Event) {
  const val = (e.target as HTMLSelectElement).value
  form.pointId = val ? Number(val) : null
}

function onEmployeeChange(e: Event) {
  const val = (e.target as HTMLSelectElement).value
  form.employeeId = val ? Number(val) : null
}

async function submit() {
  if (form.pointId === null || !isValidTemp.value) return
  const t = Number(form.temperatureC.trim())

  isSubmitting.value = true
  actionError.value  = null

  try {
    const emp = employeeOptions.value.find((e) => e.id === form.employeeId)
    const payload = {
      logPointId:        form.pointId,
      temperatureC:      t,
      measuredAt:        measuredAt(),
      noteText:          emp ? `Malt av: ${emp.label}` : undefined,
      recordedByUserId:  form.employeeId ?? undefined,
    }

    if (editingEntryId.value) {
      await updateTemperatureMeasurement(editingEntryId.value, payload)
    } else {
      await createTemperatureMeasurement(payload)
    }
    modalOpen.value = false
  } catch {
    actionError.value = 'Kunne ikke registrere temperaturmåling. Prøv igjen.'
  } finally {
    isSubmitting.value = false
  }
}

//  deviation flow
// ─── deviation modal ──────────────────────────────────────────────────────────
const deviationOpen = ref(false)
const deviationPrefill = ref<{
  reportType?: 'INCIDENT' | 'DISCREPANCY'
  severity?: 'MINOR' | 'MAJOR' | 'CRITICAL'
  title?: string
  description?: string
  locationId?: number
  occurredDate?: string
  occurredTime?: string
  discoveredByUserId?: number
} | undefined>(undefined)
const deviationSubmitting = ref(false)

function openDeviationModal(row: LocationRow) {
  const temp = row.latest ? `${row.latest.temperature_c}°C` : 'ukjent temperatur'
  const range = `${row.minTemp ?? '?'}°C til ${row.maxTemp ?? '?'}°C`

  deviationPrefill.value = {
    reportType:   'INCIDENT',
    severity:     'MAJOR',
    title:        `Temperaturavvik – ${row.locationName}`,
    description:  `Temperatur ved ${row.locationName} ble målt til ${temp}. Gyldig område: ${range}.`,
    locationId:   row.locationId ?? undefined,
    occurredDate: row.latest?.recorded_date ?? undefined,
    occurredTime: row.latest?.recorded_time?.slice(0, 5) ?? undefined,
    discoveredByUserId: selfEmployeeId.value ?? undefined,
  }
  deviationOpen.value = true
}

async function submitDeviation(payload: { reportType: string; severity: string; title: string; description: string; [key: string]: unknown }) {
  const orgNumber = authStore.currentOrg?.orgNumber
  if (!orgNumber) return
  deviationSubmitting.value = true
  try {
    await client.post('/deviations', { ...payload, orgNumber })
    deviationOpen.value = false
  } catch {
    // DeviationReportForm handles its own error display; re-throw so it knows
  } finally {
    deviationSubmitting.value = false
  }
}

//  helpers
function fmtTemp(t: number | null | undefined) {
  return t == null ? '-' : `${t}°C`
}
function fmtRange(min: number | null, max: number | null) {
  if (min == null && max == null) return '-'
  return `${min ?? '?'}°C - ${max ?? '?'}°C`
}
function fmtDateTime(date: string, time: string) {
  if (!date || date === '-') return '-'
  return time && time !== '-' ? `${date} ${time.slice(0, 5)}` : date
}
function statusTone(row: LocationRow) {
  if (!row.latest) return 'none'
  return row.isAlert ? 'danger' : 'ok'
}

const openDeviationFlow = (instance: TemperatureInstance) => {
  const measuredLabel = instance.latestTemp === null ? 'ukjent temperatur' : `${instance.latestTemp}°C`
  const description = `Temperatur ved ${instance.title} (${instance.locationName}) ble målt til ${measuredLabel}. Gyldig område er ${instance.minTemp}°C til ${instance.maxTemp}°C.`

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
  selectedPointForOptionsId.value = pointId
  pointOptionsModalOpen.value = true
}

const toggleMobileActions = (pointId: number) => {
  selectedMobileCardId.value = selectedMobileCardId.value === pointId ? null : pointId
}

const openAddPointModal = () => {
  pointModalMode.value = 'add'
  editingPointId.value = null
  resetPointForm()
  actionError.value = null
  pointModalOpen.value = true
  pointOptionsModalOpen.value = false
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
  pointOptionsModalOpen.value = false
  desktopMenuPointId.value = null
  selectedMobileCardId.value = null
}

const closePointModal = () => {
  pointModalOpen.value = false
  pointOptionsModalOpen.value = false
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
    actionError.value = 'Kunne ikke lagre temperaturpunkt. Prøv igjen.'
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
    pointOptionsModalOpen.value = false
    desktopMenuPointId.value = null
    selectedMobileCardId.value = null
  } catch {
    actionError.value = 'Kunne ikke slette temperaturpunkt. Prøv igjen.'
  } finally {
    isSubmitting.value = false
  }
}

const updateViewport = () => {
  if (typeof window === 'undefined') {
    return
  }

  isMobile.value = window.matchMedia('(max-width: 47.99rem)').matches
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

    <!-- Header -->
    <header class="page-header">
      <div>
        <h1>Temperaturkontroll</h1>
        <p class="subtitle">Kontinuerlig overvåking av kjøl, frys og varmholding</p>
      </div>
      <button class="log-btn" type="button" @click="openModal()">
        + Registrer måling
      </button>
    </header>

    <!-- Alert banner -->
    <div v-if="alerts.length > 0" class="alert-banner" role="alert">
      <span><strong>{{ alerts.length }}</strong> lokasjon{{ alerts.length > 1 ? 'er' : '' }} har temperaturavvik.</span>
      <router-link :to="{ name: 'Deviations' }">Se avvik →</router-link>
    </div>

    <!-- Stats -->
    <div class="stats-row">
      <div class="stat stat--primary">
        <strong>{{ locationRows.length }}</strong>
        <span>Punkter</span>
      </div>
      <div class="stat stat--ok">
        <strong>{{ okCount }}</strong>
        <span>OK</span>
      </div>
      <div class="stat stat--warn">
        <strong>{{ alerts.length }}</strong>
        <span>Avvik</span>
      </div>
    </div>

    <section v-if="error" class="inline-error">{{ error }}</section>
    <section v-else-if="actionError" class="inline-error">{{ actionError }}</section>

    <section v-if="!isMobile" class="desktop-list" aria-label="Temperaturpunkter">
      <div class="desktop-list__scroller">
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

          <span
            class="status-pill"
            :class="instance.isAlert ? 'status-pill--danger' : instance.latestTemp === null ? 'status-pill--idle' : 'status-pill--good'"
          >
            {{ instance.isAlert ? 'Avvik' : instance.latestTemp === null ? 'Ikke malt' : 'OK' }}
          </span>

          <div class="desktop-actions">
            <button
              type="button"
              class="action-btn"
              :class="instance.isAlert ? 'action-btn--danger' : ''"
              @click="instance.isAlert ? openDeviationFlow(instance) : openMeasurementModal(instance.pointId)"
            >
              {{ instance.isAlert ? 'Registrer avvik' : instance.latestEntryId ? 'Rediger måling' : 'Registrer måling' }}
            </button>

            <div v-if="canManage" class="options-menu">
              <button
                class="options-menu__trigger"
                type="button"
                aria-label="Apne handlinger"
                @click="toggleDesktopMenu(instance.pointId)"
              >
                <span class="dot" />
                <span class="dot" />
                <span class="dot" />
              </button>
            </div>
          </div>
        </article>

        <p v-if="instances.length === 0 && !isLoading" class="empty-state">Ingen temperaturpunkter registrert.</p>
      </div>
    </section>

    <div v-if="canManage && !isMobile" class="desktop-list__actions">
      <button type="button" class="add-item-btn" @click="openAddPointModal">
        + Legg til temperaturpunkt
      </button>
    </div>

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
          <span class="status-pill" :class="instance.isAlert ? 'status-pill--danger' : instance.latestTemp === null ? 'status-pill--idle' : 'status-pill--good'">
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
          {{ instance.isAlert ? 'Registrer avvik' : instance.latestEntryId ? 'Rediger måling' : 'Registrer måling' }}
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
        <button type="button" class="btn-ghost" @click="closePointModal">Avbryt</button>
        <button type="button" class="btn-primary" :disabled="isSubmitting || Number(pointForm.minTempC) > Number(pointForm.maxTempC)" @click="savePoint">Lagre</button>
      </template>
    </BaseModal>

    <BaseModal
      :open="measurementModalOpen"
      title="Registrer temperaturmåling"
      @close="closeMeasurementModal"
    >
      <form class="modal-form" @submit.prevent="submitMeasurement">
        <p v-if="currentMeasurementPoint" class="modal-form__hint">
          {{ currentMeasurementPoint.title }}: {{ currentMeasurementPoint.minTemp }}°C til {{ currentMeasurementPoint.maxTemp }}°C
        </p>

        <label>
          Målt temperatur (°C)
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
        <button type="button" class="btn-ghost" @click="closeMeasurementModal">Avbryt</button>
        <button type="button" class="btn-primary" :disabled="isSubmitting" @click="submitMeasurement">{{ editingMeasurementEntryId ? 'Oppdater' : 'Send inn' }}</button>
      </template>
    </BaseModal>

    <BaseModal
      :open="pointOptionsModalOpen"
      title="Alternativer"
      @close="() => { pointOptionsModalOpen = false }"
    >
      <div class="modal-options">
        <p class="modal-options__hint">Velg en handling for dette temperaturpunktet.</p>
        <div class="modal-options__actions">
          <button
            type="button"
            class="btn-ghost"
            @click="selectedPointForOptionsId ? openEditPointModal(selectedPointForOptionsId) : null"
          >
            Rediger
          </button>
          <button
            type="button"
            class="btn-ghost btn-ghost--danger"
            @click="selectedPointForOptionsId ? removePoint(selectedPointForOptionsId) : null"
          >
            Slett
          </button>
        </div>
      </div>

      <template #footer>
        <button type="button" class="btn-ghost" @click="pointOptionsModalOpen = false">Lukk</button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.temperature-page {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

/*  Header  */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 1.5rem;
  margin-bottom: 0.5rem;
}

.page-header h1 {
  margin: 0;
  font-family: var(--font-family-montserrat, 'Montserrat', sans-serif);
  font-size: clamp(1.5rem, 2.4vw, 2rem);
  font-weight: 700;
  letter-spacing: -0.015em;
  color: var(--color-foreground);
}

.subtitle {
  margin: 0.5rem 0 0;
  font-family: var(--font-family-hind, 'Hind', sans-serif);
  color: var(--color-gray-500);
  font-size: var(--font-size-sm);
  font-weight: 400;
  line-height: 1.5;
}

.alert-banner {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border: 1px solid color-mix(in srgb, var(--color-danger) 70%, var(--color-border));
  border-radius: var(--radius-md);
  background: var(--color-danger);
  color: var(--color-primary-foreground);
  font-family: var(--font-family-hind, 'Hind', sans-serif);
  font-size: var(--font-size-sm);
  font-weight: 400;
  white-space: nowrap;
  flex-shrink: 0;
}

.alert-banner strong {
  font-weight: 600;
}

.alert-banner a {
  color: inherit;
  text-decoration: underline;
  text-decoration-thickness: 2px;
  text-underline-offset: 3px;
  font-weight: 600;
  white-space: nowrap;
  transition: opacity var(--transition-fast);
}

.alert-banner a:hover {
  opacity: 0.9;
}

.alert-banner a:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.stat {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  border: 1px solid var(--color-border);
  border-left: 4px solid var(--color-primary);
  border-radius: var(--radius-md);
  padding: 1rem;
  background: color-mix(in srgb, var(--color-primary) 6%, var(--color-card));
  box-shadow: 0 1px 3px rgba(0, 39, 43, 0.08), 0 1px 2px rgba(0, 39, 43, 0.04);
  transition: box-shadow var(--transition-fast), border-color var(--transition-fast);
}

.stat p {
  margin: 0;
  font-family: var(--font-family-montserrat, 'Montserrat', sans-serif);
  color: var(--color-gray-600);
  font-size: 0.6875rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.stat strong {
  display: block;
  margin-top: 0.5rem;
  font-family: var(--font-family-montserrat, 'Montserrat', sans-serif);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-primary);
}

.stat span {
  display: block;
  margin-top: 0.25rem;
  font-family: var(--font-family-hind, 'Hind', sans-serif);
  font-size: var(--font-size-xs);
  color: var(--color-gray-700);
  font-weight: 400;
}

.stat--primary {
  border-left-color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 6%, var(--color-card));
}

.stat--primary strong {
  color: var(--color-primary);
}

.stat--ok {
  border-left-color: var(--color-cta);
  background: var(--color-cta);
  border-color: color-mix(in srgb, var(--color-cta) 75%, var(--color-border));
}

.stat--warn {
  border-left-color: var(--color-secondary);
  background: var(--color-secondary);
  border-color: color-mix(in srgb, var(--color-secondary) 75%, var(--color-border));
}

.stat--ok p,
.stat--ok strong,
.stat--ok span {
  color: var(--color-cta-foreground);
}

.stat--warn p,
.stat--warn strong,
.stat--warn span {
  color: var(--color-secondary-foreground);
}

/*  Error  */
.inline-error {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: var(--color-danger);
  color: var(--color-primary-foreground);
  border: 1px solid color-mix(in srgb, var(--color-danger) 70%, var(--color-border));
  border-radius: var(--radius-md);
  font-family: var(--font-family-hind, 'Hind', sans-serif);
  font-size: var(--font-size-sm);
  font-weight: 400;
  margin-bottom: 1rem;
}


.form-error {
  margin: 0;
  padding: 0.75rem 1rem;
  background: var(--color-danger-bg);
  color: var(--color-danger);
  border: 1px solid var(--color-danger-border);
  border-radius: var(--radius-md);
  font-family: var(--font-family-hind, 'Hind', sans-serif);
  font-size: var(--font-size-sm);
  font-weight: 400;
}

.desktop-list {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  padding: 0;
  box-shadow: 0 1px 3px rgba(0, 39, 43, 0.08), 0 1px 2px rgba(0, 39, 43, 0.04);
  overflow: hidden;
}

.desktop-list__scroller {
  overflow-x: auto;
}

.desktop-list__actions {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 1.5rem;
}

.desktop-list__header,
.desktop-row {
  display: grid;
  grid-template-columns: minmax(10rem, 1.5fr) minmax(8rem, 1fr) minmax(8rem, 1fr) minmax(6rem, auto) minmax(12rem, 1fr);
  gap: 0.75rem;
  align-items: center;
  padding: 1.5rem;
  min-width: 58rem;
}

.desktop-list__header {
  border-bottom: 1px solid var(--color-border);
  background: var(--color-gray-50);
  font-family: var(--font-family-montserrat, 'Montserrat', sans-serif);
  font-size: 0.6875rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--color-foreground);
}

.desktop-row {
  border-bottom: 1px solid var(--color-border);
}

.desktop-row:last-of-type {
  border-bottom: none;
}

.desktop-row--alert {
  background: color-mix(in srgb, var(--color-danger) 15%, var(--color-card));
}

.desktop-row__title {
  margin: 0;
  font-family: var(--font-family-hind, 'Hind', sans-serif);
  color: var(--color-foreground);
  font-size: var(--font-size-sm);
  font-weight: 500;
}

.desktop-row__meta {
  margin: 0.25rem 0 0;
  font-family: var(--font-family-hind, 'Hind', sans-serif);
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
  font-weight: 400;
}

.desktop-row__value {
  margin: 0;
  font-family: var(--font-family-hind, 'Hind', sans-serif);
  color: var(--color-foreground);
  font-size: var(--font-size-sm);
  font-weight: 400;
}

th, td {
  text-align: left;
  padding: 0.75rem 1rem;
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

.tr--alert { background: color-mix(in srgb, var(--color-danger-bg) 60%, var(--color-card)); }

.cell-title { font-size: var(--font-size-sm); font-weight: 500; color: var(--color-foreground); margin: 0; }
.cell-sub   { font-size: var(--font-size-xs); color: var(--color-gray-500); margin: 0.1rem 0 0; }

.td-meta    { font-size: var(--font-size-sm); color: var(--color-gray-600); }
.td-actions { white-space: nowrap; text-align: right; }

.temp-val { font-size: var(--font-size-base); font-weight: 600; }
.temp-val--ok     { color: var(--color-success); }
.temp-val--danger { color: var(--color-danger); }

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid transparent;
  border-radius: 999px;
  padding: 0.35rem 0.75rem;
  min-width: fit-content;
  min-height: 2rem;
  font-family: var(--font-family-montserrat, 'Montserrat', sans-serif);
  font-size: 0.6875rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  white-space: nowrap;
}

.status-pill--good {
  background: var(--color-cta);
  color: var(--color-cta-foreground);
  border-color: var(--color-cta);
}

.status-pill--danger {
  background: var(--color-danger);
  color: var(--color-danger-fg);
  border-color: var(--color-danger);
}

.status-pill--idle {
  background: var(--color-gray-100);
  color: var(--color-gray-500);
  border-color: var(--color-border);
}

.row-btn {
  min-height: 2rem;
  padding: 0.35rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-card);
  font-family: var(--font-family-hind, 'Hind', sans-serif);
  font-size: var(--font-size-xs);
  font-weight: 600;
  color: var(--color-gray-700);
  cursor: pointer;
  transition: background-color var(--transition-fast), border-color var(--transition-fast);
  margin-left: 0.5rem;
}

.row-btn:hover {
  background: var(--color-gray-50);
  border-color: var(--color-gray-400);
}

.row-btn:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.desktop-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.desktop-actions > .action-btn {
  width: fit-content;
  min-height: 2.25rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 0.4rem 1.25rem;
  border: none;
  border-radius: var(--radius-md);
  font-family: var(--font-family-montserrat, 'Montserrat', sans-serif);
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color var(--transition-fast), opacity var(--transition-fast);
}

.action-btn {
  background: var(--color-primary);
  color: var(--color-primary-foreground);
}

.action-btn:hover:not(:disabled) {
  opacity: 0.88;
}

.action-btn:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-btn--danger {
  background: var(--color-danger);
  color: var(--color-danger-fg);
}

.options-menu {
  position: relative;
}

.options-menu__trigger {
  aspect-ratio: 1 / 1;
  width: 2.5rem;
  height: 2.5rem;
  min-width: 2.5rem;
  min-height: 2.5rem;
  border: 1px solid var(--color-border);
  background: var(--color-card);
  border-radius: var(--radius-sm);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.15rem;
  cursor: pointer;
  padding: 0;
  transition: background-color var(--transition-fast), border-color var(--transition-fast);
}

.options-menu__trigger:hover {
  background: var(--color-gray-50);
  border-color: var(--color-gray-400);
}

.options-menu__trigger:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.dot {
  width: 0.2rem;
  height: 0.2rem;
  background: var(--color-gray-600);
  border-radius: 100%;
}

.modal {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
  width: 100%;
  max-width: 26rem;
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  overflow-y: auto;
}

/*  Mobile Cards  */
.mobile-cards {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.mobile-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-left: 4px solid var(--color-focus);
  border-radius: var(--radius-md);
  padding: 1rem;
  box-shadow: 0 1px 3px rgba(0, 39, 43, 0.08), 0 1px 2px rgba(0, 39, 43, 0.04);
  cursor: pointer;
  transition: box-shadow var(--transition-fast), border-left-color var(--transition-fast);
}

.mobile-card:hover {
  box-shadow: 0 2px 4px rgba(0, 39, 43, 0.12);
  border-left-color: var(--color-focus);
}

.mobile-card--alert {
  border-left-color: var(--color-danger);
  background: color-mix(in srgb, var(--color-danger) 5%, var(--color-card));
}

.mobile-card--alert:hover {
  border-left-color: var(--color-danger);
}

.mobile-card--active {
  border-left-color: var(--color-focus);
  box-shadow: 0 4px 12px rgba(0, 39, 43, 0.15);
}

.mobile-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1rem;
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 0.75rem;
}

.mobile-card__header h2 {
  margin: 0;
  font-family: var(--font-family-montserrat, 'Montserrat', sans-serif);
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-foreground);
  flex-shrink: 1;
  min-width: 0;
}

.mobile-card__meta {
  margin: 0.5rem 0 0;
  font-family: var(--font-family-hind, 'Hind', sans-serif);
  font-size: var(--font-size-xs);
  font-weight: 300;
  color: var(--color-gray-600);
  line-height: 1.5;
}

.mobile-card__temperature {
  margin: 0.75rem 0 0;
  font-family: var(--font-family-hind, 'Hind', sans-serif);
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--color-foreground);
}

.mobile-card__action {
  width: 100%;
  margin-top: 1rem;
  min-height: 2.5rem;
}

.mobile-card__manage {
  display: grid;
  gap: 0.45rem;
  width: 100%;
  margin-bottom: 0.7rem;
}

.mobile-card__manage-btn {
  min-height: 2rem;
  padding: 0.35rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-card);
  font-family: var(--font-family-hind, 'Hind', sans-serif);
  font-size: var(--font-size-xs);
  font-weight: 600;
  color: var(--color-gray-700);
  cursor: pointer;
  transition: background-color var(--transition-fast), border-color var(--transition-fast);
}

.mobile-card__manage-btn:hover {
  background: var(--color-gray-50);
  border-color: var(--color-gray-400);
}

.mobile-card__manage-btn:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.mobile-card__manage-btn--danger {
  border-color: var(--color-danger-border);
  background: var(--color-danger-bg);
  color: var(--color-danger);
}

.mobile-card__manage-btn--danger:hover {
  background: color-mix(in srgb, var(--color-danger-bg) 80%, var(--color-danger) 20%);
  border-color: var(--color-danger);
}

.modal__header h2 {
  margin: 0;
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--color-foreground);
  padding: 0.35rem 0.55rem;
  font-size: var(--font-size-xs);
  width: 100%;
}

.modal__close:hover { background: var(--color-gray-100); color: var(--color-foreground); }
.modal__close:hover { background: var(--color-gray-100); color: var(--color-foreground); }

.modal__body {
  padding: 1rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.modal-options {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1rem 0;
}

.modal-options__hint {
  margin: 0;
  font-family: var(--font-family);
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
  font-weight: 400;
}

.modal-options__actions {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.modal-form {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

.modal-form > label {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.375rem;
  width: 100%;
  font-family: var(--font-family-display);
  font-size: 0.8125rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-gray-600);
}

.modal-form input,
.modal-form select {
  width: 100%;
  min-height: 2.5rem;
  padding: 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-family: var(--font-family);
  font-size: var(--font-size-sm);
  background: var(--color-card);
  color: var(--color-foreground);
  box-sizing: border-box;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast), background-color var(--transition-fast);
}

.modal-form input:focus,
.modal-form select:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-info-bg);
}

.modal-form input:focus-visible,
.modal-form select:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.modal-form select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%236B7280' stroke-width='2'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
  padding-right: 2rem;
  cursor: pointer;
}

.modal-form__row {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

.modal-form__row > label {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.375rem;
  width: 100%;
  font-family: var(--font-family-display);
  font-size: 0.8125rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-gray-600);
}

.modal-form__hint {
  margin: 0;
  font-family: var(--font-family-hind, 'Hind', sans-serif);
  font-size: var(--font-size-xs);
  color: var(--color-gray-600);
  line-height: 1.5;
}

.modal-form__error {
  margin: 0;
  padding: 0.75rem 1rem;
  border: 1px solid var(--color-danger-border);
  border-radius: var(--radius-md);
  background: var(--color-danger-bg);
  color: var(--color-danger);
  font-family: var(--font-family-hind, 'Hind', sans-serif);
  font-size: var(--font-size-sm);
  line-height: 1.4;
}

.modal__footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  border-top: 1px solid var(--color-border);
}

/*  Form fields  */
.field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.field label {
  font-family: var(--font-family-display);
  font-size: 0.8125rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-gray-600);
}

.field select,
.field input {
  min-height: 2.5rem;
  padding: 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-family: var(--font-family);
  font-size: var(--font-size-sm);
  background: var(--color-card);
  color: var(--color-foreground);
  box-sizing: border-box;
  width: 100%;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast), background-color var(--transition-fast);
}

.field select:focus,
.field input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-info-bg);
  background: var(--color-card);
}

.field select:focus-visible,
.field input:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
  box-shadow: 0 0 0 3px var(--color-info-bg);
}

.field select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%236B7280' stroke-width='2'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
  padding-right: 2rem;
  cursor: pointer;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid transparent;
  border-radius: 999px;
  padding: 0.35rem 0.75rem;
  min-width: fit-content;
  min-height: 2rem;
  font-family: var(--font-family-montserrat, 'Montserrat', sans-serif);
  font-size: 0.6875rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  white-space: nowrap;
}

.status-pill--good {
  background: var(--color-cta);
  color: var(--color-cta-foreground);
  border-color: var(--color-cta);
}

.status-pill--danger {
  background: var(--color-danger);
  color: var(--color-danger-fg);
  border-color: var(--color-danger);
}

.status-pill--idle {
  background: var(--color-gray-100);
  color: var(--color-gray-500);
  border-color: var(--color-border);
}

.add-item-btn {
  min-height: 2.5rem;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  padding: 0.5rem 1.25rem;
  margin-top: 1rem;
  border: 1.5px solid var(--color-primary);
  background: transparent;
  color: var(--color-primary);
  font-family: var(--font-family-display);
  font-size: var(--font-size-sm);
  font-weight: 600;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background-color var(--transition-fast), color var(--transition-fast), border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.add-item-btn:hover {
  background: color-mix(in srgb, var(--color-primary) 8%, var(--color-card));
  border-color: var(--color-primary);
  color: var(--color-primary);
  box-shadow: 0 1px 3px rgba(0, 39, 43, 0.08);
}

.add-item-btn:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.add-item-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.range-hint {
  padding: 0.75rem 1rem;
  background: var(--color-gray-50);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-family: var(--font-family-hind, 'Hind', sans-serif);
  font-size: var(--font-size-xs);
  color: var(--color-gray-600);
  line-height: 1.5;
  font-weight: 400;
}

.form-error {
  margin: 0;
  padding: 0.75rem 1rem;
  background: var(--color-danger-bg);
  color: var(--color-danger);
  border: 1px solid var(--color-danger-border);
  border-radius: var(--radius-md);
  font-family: var(--font-family);
  font-size: var(--font-size-sm);
  font-weight: 400;
}

/*  Layout & Responsive  */
@media (max-width: 48rem) {
  .page-header { flex-direction: column; align-items: flex-start; }
  .log-btn { width: 100%; text-align: center; }
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  th:nth-child(3), td:nth-child(3),
  th:nth-child(4), td:nth-child(4) { display: none; }
}
</style>
