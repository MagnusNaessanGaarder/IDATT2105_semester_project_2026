<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useIkMatData } from '../composables/useIkMatData'
import { getUsers } from '@/features/admin/api/users'
import type { TemperatureRecord } from '../types/domain'

//  composable
const {
  temperatureRecords,
  temperaturePoints,
  locations,
  isTemperatureInRange,
  createTemperatureMeasurement,
  updateTemperatureMeasurement,
  isLoading,
  error,
} = useIkMatData()

const router    = useRouter()
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

//  derived data
const canManage = computed(() => authStore.hasRole('ADMIN', 'MANAGER'))

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

//  log modal 
const modalOpen   = ref(false)
const isSubmitting = ref(false)
const actionError  = ref<string | null>(null)
const editingEntryId = ref<number | null>(null)

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

function measuredAt(): string {
  if (form.useNow) return new Date().toISOString()
  return new Date(`${form.date}T${form.time}:00`).toISOString()
}

const currentRange = computed(() => {
  if (!form.pointId) return null
  const point = activePoints.value.find((p) => p.logPointId === form.pointId)
  if (!point) return null
  const loc = locationById.value.get(point.locationId)
  if (!loc) return null
  return { min: loc.tempMinC, max: loc.tempMaxC, name: point.name }
})

const tempOutOfRange = computed(() => {
  const t = Number(form.temperatureC)
  if (!Number.isFinite(t) || !currentRange.value) return false
  const { min, max } = currentRange.value
  return (min != null && t < min) || (max != null && t > max)
})

async function submit() {
  if (!form.pointId || !form.temperatureC.trim()) return
  const t = Number(form.temperatureC)
  if (!Number.isFinite(t)) return

  isSubmitting.value = true
  actionError.value  = null

  try {
    const emp = employeeOptions.value.find((e) => e.id === form.employeeId)
    const payload = {
      logPointId:        form.pointId,
      temperatureC:      t,
      measuredAt:        measuredAt(),
      noteText:          emp ? `Målt av: ${emp.label}` : undefined,
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
function goToDeviation(row: LocationRow) {
  const temp = row.latest ? `${row.latest.temperature_c}°C` : 'ukjent'
  const range = `${row.minTemp ?? '?'}°C til ${row.maxTemp ?? '?'}°C`
  void router.push({
    name: 'Deviations',
    query: {
      openCreate: '1',
      source: 'temperature',
      title: `Temperaturavvik - ${row.locationName}`,
      location: row.locationName,
      description: `Temperatur ved ${row.locationName} ble målt til ${temp}. Gyldig område: ${range}.`,
      severity: 'MAJOR',
    },
  })
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
      <div class="stat">
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
      <div class="stat">
        <strong>{{ allLogs.length }}</strong>
        <span>Målinger</span>
      </div>
    </div>

    <!-- Error -->
    <div v-if="error" class="inline-error">{{ error }}</div>

    <!-- Tabs -->
    <div class="tabs">
      <button
          class="tab"
          :class="{ 'tab--active': activeTab === 'locations' }"
          @click="activeTab = 'locations'"
      >
        Lokasjoner
      </button>
      <button
          class="tab"
          :class="{ 'tab--active': activeTab === 'logs' }"
          @click="activeTab = 'logs'"
      >
        Alle målinger
      </button>
    </div>

    <!--  Locations tab  -->
    <div v-if="activeTab === 'locations'" class="tab-panel">
      <div v-if="isLoading" class="loading">Laster…</div>
      <div v-else-if="locationRows.length === 0" class="empty">
        Ingen aktive temperaturpunkter.
      </div>
      <div v-else class="table-wrap">
        <table>
          <thead>
          <tr>
            <th>Lokasjon / Punkt</th>
            <th>Siste måling</th>
            <th>Tidspunkt</th>
            <th>Gyldig område</th>
            <th>Status</th>
            <th></th>
          </tr>
          </thead>
          <tbody>
          <tr
              v-for="row in locationRows"
              :key="row.pointId"
              :class="{ 'tr--alert': row.isAlert }"
          >
            <td>
              <p class="cell-title">{{ row.locationName }}</p>
              <p class="cell-sub">{{ row.pointName }}</p>
            </td>
            <td>
                <span
                    class="temp-val"
                    :class="{
                    'temp-val--ok':     statusTone(row) === 'ok',
                    'temp-val--danger': statusTone(row) === 'danger',
                  }"
                >
                  {{ fmtTemp(row.latest?.temperature_c) }}
                </span>
            </td>
            <td class="td-meta">
              {{ row.latest ? fmtDateTime(row.latest.recorded_date, row.latest.recorded_time) : '-' }}
            </td>
            <td class="td-meta">{{ fmtRange(row.minTemp, row.maxTemp) }}</td>
            <td>
                <span
                    v-if="row.latest"
                    class="status-pill"
                    :class="row.isAlert ? 'status-pill--danger' : 'status-pill--ok'"
                >
                  {{ row.isAlert ? 'Avvik' : 'OK' }}
                </span>
              <span v-else class="status-pill status-pill--none">Ikke målt</span>
            </td>
            <td class="td-actions">
              <button
                  v-if="row.isAlert"
                  class="row-btn row-btn--danger"
                  type="button"
                  @click="goToDeviation(row)"
              >
                Meld avvik
              </button>
              <button
                  class="row-btn"
                  type="button"
                  @click="openModal(row.pointId)"
              >
                Registrer
              </button>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!--  Logs tab  -->
    <div v-else class="tab-panel">
      <div v-if="isLoading" class="loading">Laster…</div>
      <div v-else-if="allLogs.length === 0" class="empty">
        Ingen målinger registrert ennå.
      </div>
      <div v-else class="table-wrap">
        <table>
          <thead>
          <tr>
            <th>Lokasjon / Punkt</th>
            <th>Temperatur</th>
            <th>Tidspunkt</th>
            <th>Målt av</th>
            <th>Status</th>
            <th v-if="canManage"></th>
          </tr>
          </thead>
          <tbody>
          <tr
              v-for="log in allLogs"
              :key="log.id"
              :class="{ 'tr--alert': log.status !== 'ok' }"
          >
            <td>
              <p class="cell-title">{{ log.location || '-' }}</p>
              <p class="cell-sub">{{ log.log_point_name }}</p>
            </td>
            <td>
                <span
                    class="temp-val"
                    :class="{
                    'temp-val--ok':     log.status === 'ok',
                    'temp-val--danger': log.status !== 'ok',
                  }"
                >
                  {{ fmtTemp(log.temperature_c) }}
                </span>
            </td>
            <td class="td-meta">{{ fmtDateTime(log.recorded_date, log.recorded_time) }}</td>
            <td class="td-meta">{{ log.recorded_by || '-' }}</td>
            <td>
                <span
                    class="status-pill"
                    :class="log.status === 'ok' ? 'status-pill--ok' : 'status-pill--danger'"
                >
                  {{ log.status === 'ok' ? 'OK' : 'Avvik' }}
                </span>
            </td>
            <td v-if="canManage" class="td-actions">
              <button
                  class="row-btn"
                  type="button"
                  @click="openModal(log.log_point_id, log)"
              >
                Rediger
              </button>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!--  Log measurement modal  -->
    <Teleport to="body">
      <div v-if="modalOpen" class="overlay" @click.self="closeModal">
        <div class="modal" role="dialog" aria-modal="true" aria-label="Registrer temperaturmåling">

          <header class="modal__header">
            <h2>{{ editingEntryId ? 'Rediger måling' : 'Registrer måling' }}</h2>
            <button class="modal__close" type="button" aria-label="Lukk" @click="closeModal">✕</button>
          </header>

          <div class="modal__body">

            <!-- Point selector -->
            <div class="field">
              <label for="m-point">Målested</label>
              <select id="m-point" v-model.number="form.pointId" required>
                <option :value="null" disabled>Velg punkt</option>
                <option v-for="opt in pointOptions" :key="opt.id" :value="opt.id">
                  {{ opt.label }}
                </option>
              </select>
            </div>

            <!-- Range hint -->
            <p v-if="currentRange" class="range-hint">
              Gyldig område for <strong>{{ currentRange.name }}</strong>:
              {{ fmtRange(currentRange.min, currentRange.max) }}
            </p>

            <!-- Temperature -->
            <div class="field">
              <label for="m-temp">Temperatur (°C)</label>
              <input
                  id="m-temp"
                  v-model="form.temperatureC"
                  type="number"
                  step="0.1"
                  placeholder="f.eks. 3.5"
                  required
                  :class="{ 'input--warn': tempOutOfRange }"
              />
              <p v-if="tempOutOfRange" class="field-warn">⚠ Utenfor gyldig område</p>
            </div>

            <!-- Time -->
            <div class="field">
              <label>Tidspunkt</label>
              <div class="time-row">
                <button
                    class="now-btn"
                    type="button"
                    :class="{ 'now-btn--active': form.useNow }"
                    @click="form.useNow = true"
                >
                  Nå
                </button>
                <button
                    class="now-btn"
                    type="button"
                    :class="{ 'now-btn--active': !form.useNow }"
                    @click="form.useNow = false"
                >
                  Velg tid
                </button>
              </div>
              <div v-if="!form.useNow" class="time-inputs">
                <input v-model="form.date" type="date" required />
                <input v-model="form.time" type="time" required />
              </div>
              <p v-else class="now-label">Registreres med nåværende tidspunkt</p>
            </div>

            <!-- Employee -->
            <div class="field">
              <label for="m-emp">Målt av</label>
              <select id="m-emp" v-model.number="form.employeeId">
                <option :value="null">- Velg ansatt -</option>
                <option v-for="e in employeeOptions" :key="e.id" :value="e.id">
                  {{ e.label }}
                </option>
              </select>
            </div>

            <p v-if="actionError" class="form-error">{{ actionError }}</p>

          </div>

          <footer class="modal__footer">
            <button class="btn-ghost" type="button" @click="closeModal">Avbryt</button>
            <button
                class="btn-primary"
                type="button"
                :disabled="isSubmitting || !form.pointId || !form.temperatureC.trim()"
                @click="submit"
            >
              {{ isSubmitting ? 'Lagrer…' : editingEntryId ? 'Oppdater' : 'Lagre' }}
            </button>
          </footer>

        </div>
      </div>
    </Teleport>

  </div>
</template>

<style scoped>
.temperature-page {
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
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
  font-size: clamp(1.5rem, 2.4vw, var(--font-size-2xl));
  font-weight: 700;
  letter-spacing: -0.015em;
}

.subtitle {
  margin: 0.3rem 0 0;
  color: var(--color-gray-500);
  font-size: var(--font-size-sm);
}

.log-btn {
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
.log-btn:hover { opacity: 0.88; }

/*  Alert banner  */
.alert-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 0.7rem 1rem;
  background: var(--color-danger-bg);
  border: 1px solid color-mix(in srgb, var(--color-danger) 30%, var(--color-border));
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  color: var(--color-danger);
}
.alert-banner a { color: inherit; font-weight: 600; text-decoration: none; }

/*  Stats  */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.75rem;
}

.stat {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  padding: 0.75rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}
.stat strong { font-size: 1.6rem; font-weight: 700; color: var(--color-foreground); }
.stat span   { font-size: var(--font-size-xs); color: var(--color-gray-500); }
.stat--ok    { border-left: 3px solid var(--color-success); }
.stat--warn  { border-left: 3px solid var(--color-danger); }

/*  Error  */
.inline-error {
  padding: 0.65rem 0.9rem;
  background: var(--color-danger-bg);
  color: var(--color-danger);
  border: 1px solid color-mix(in srgb, var(--color-danger) 30%, var(--color-border));
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
}

/*  Tabs  */
.tabs {
  display: flex;
  gap: 0;
  border-bottom: 2px solid var(--color-border);
}

.tab {
  padding: 0.55rem 1.1rem;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  background: transparent;
  color: var(--color-gray-500);
  font-size: var(--font-size-sm);
  font-weight: 500;
  cursor: pointer;
  transition: color var(--transition-fast), border-color var(--transition-fast);
}
.tab:hover { color: var(--color-foreground); }
.tab--active {
  color: var(--color-foreground);
  border-bottom-color: var(--color-foreground);
  font-weight: 600;
}

/*  Table  */
.tab-panel { min-height: 8rem; }

.loading, .empty {
  padding: 3rem 1rem;
  text-align: center;
  color: var(--color-gray-400);
  font-size: var(--font-size-sm);
}

.table-wrap {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-card);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

table { width: 100%; border-collapse: collapse; }

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
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  font-size: var(--font-size-xs);
  font-weight: 600;
}
.status-pill--ok     { background: var(--color-success-bg); color: var(--color-success); }
.status-pill--danger { background: var(--color-danger-bg);  color: var(--color-danger);  }
.status-pill--none   { background: var(--color-gray-100);   color: var(--color-gray-500); }

.row-btn {
  min-height: 2rem;
  padding: 0.25rem 0.65rem;
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
.row-btn--danger {
  color: var(--color-danger);
  border-color: color-mix(in srgb, var(--color-danger) 30%, var(--color-border));
}

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

.modal__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.1rem 1.25rem 0.75rem;
  border-bottom: 1px solid var(--color-border);
}
.modal__header h2 { margin: 0; font-size: var(--font-size-lg); font-weight: 600; }
.modal__close {
  width: 2rem; height: 2rem;
  border: none; background: none;
  color: var(--color-gray-500);
  border-radius: var(--radius-sm);
  cursor: pointer; font-size: 1rem;
}
.modal__close:hover { background: var(--color-gray-100); color: var(--color-foreground); }

.modal__body {
  padding: 1.1rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.modal__footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem 1.1rem;
  border-top: 1px solid var(--color-border);
}

/*  Form fields  */
.field { display: flex; flex-direction: column; gap: 0.3rem; }
.field label {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--color-gray-700);
}
.field select,
.field input[type="number"],
.field input[type="date"],
.field input[type="time"] {
  min-height: 2.5rem;
  padding: 0 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  background: var(--color-card);
  color: var(--color-foreground);
  box-sizing: border-box;
  width: 100%;
}
.field select:focus,
.field input:focus {
  outline: none;
  border-color: var(--color-focus);
  box-shadow: var(--shadow-focus);
}
.input--warn { border-color: var(--color-warning) !important; }
.field-warn { margin: 0; font-size: var(--font-size-xs); color: var(--color-warning); }

.range-hint {
  margin: -0.25rem 0 0;
  padding: 0.5rem 0.75rem;
  background: var(--color-gray-50);
  border-radius: var(--radius-md);
  font-size: var(--font-size-xs);
  color: var(--color-gray-600);
}

/*  Time toggle  */
.time-row {
  display: flex;
  gap: 0.4rem;
}
.now-btn {
  flex: 1;
  min-height: 2.5rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}
.now-btn--active {
  border-color: var(--color-foreground);
  background: var(--color-foreground);
  color: var(--color-primary-foreground);
  font-weight: 600;
}
.now-label {
  margin: 0;
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
  font-style: italic;
}
.time-inputs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
  margin-top: 0.4rem;
}

.form-error {
  margin: 0;
  padding: 0.6rem 0.75rem;
  background: var(--color-danger-bg);
  color: var(--color-danger);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
}

/*  Buttons  */
.btn-ghost {
  min-height: 2.25rem;
  padding: 0.4rem 0.9rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-card);
  color: var(--color-gray-700);
  font-size: var(--font-size-sm);
  font-weight: 600;
  cursor: pointer;
}
.btn-ghost:hover { background: var(--color-gray-50); }

.btn-primary {
  min-height: 2.25rem;
  padding: 0.4rem 1.1rem;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--color-primary);
  color: var(--color-primary-foreground);
  font-size: var(--font-size-sm);
  font-weight: 600;
  cursor: pointer;
}
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-primary:not(:disabled):hover { opacity: 0.88; }

/*  Responsive  */
@media (max-width: 48rem) {
  .page-header { flex-direction: column; align-items: flex-start; }
  .log-btn { width: 100%; text-align: center; }
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  th:nth-child(3), td:nth-child(3),
  th:nth-child(4), td:nth-child(4) { display: none; }
}
</style>