<script setup lang="ts">
import { computed, nextTick, onMounted, ref, reactive } from 'vue'
import { client } from '@/api/client'
import { useAuthStore } from '@/stores/auth'

//  types 
interface Location {
  locationId: number
  name: string
  description: string | null
  locationType: string
  tempMinC: number | null
  tempMaxC: number | null
  isActive: boolean
}

type FormData = {
  name: string
  description: string
  locationType: string
  tempMinC: string
  tempMaxC: string
  isActive: boolean
}

//  state 
const authStore  = useAuthStore()
const orgNumber  = computed(() => authStore.currentOrg?.orgNumber)

const locations  = ref<Location[]>([])
const loading    = ref(false)
const error      = ref<string | null>(null)
const feedback   = ref<{ type: 'success' | 'error'; msg: string } | null>(null)

// modal
const modalOpen    = ref(false)
const modalMode    = ref<'add' | 'edit'>('add')
const editingId    = ref<number | null>(null)
const saving       = ref(false)
const modalError   = ref<string | null>(null)

const form = reactive<FormData>({
  name:         '',
  description:  '',
  locationType: 'OTHER',
  tempMinC:     '',
  tempMaxC:     '',
  isActive:     true,
})

// confirm delete
const confirmId   = ref<number | null>(null)
const deleting    = ref(false)

//  location types 
const locationTypes = [
  { value: 'KITCHEN',      label: 'Kjøkken'          },
  { value: 'BAR',          label: 'Bar'               },
  { value: 'FREEZER',      label: 'Fryser'            },
  { value: 'FRIDGE',       label: 'Kjølerom'          },
  { value: 'STORAGE',      label: 'Lager'             },
  { value: 'SERVING_AREA', label: 'Serveringsområde'  },
  { value: 'HOT_FOOD',     label: 'Varmmat'           },
  { value: 'OTHER',        label: 'Annet'             },
]

const typeLabel = (v: string) => locationTypes.find((t) => t.value === v)?.label ?? v

// Suggested names per type for datalist
const nameSuggestions: Record<string, string[]> = {
  KITCHEN:      ['Kjøkken', 'Kjøkken 1', 'Kjøkken 2'],
  BAR:          ['Bar', 'Bar 1', 'Bar 2', 'Uteservering'],
  FREEZER:      ['Fryser', 'Fryser 1', 'Fryser 2', 'Fryseboks'],
  FRIDGE:       ['Kjølerom', 'Kjøleskap 1', 'Kjøleskap 2', 'Kjøleskap 3'],
  STORAGE:      ['Lager', 'Tørrlager', 'Kjølelager'],
  SERVING_AREA: ['Serveringsområde', 'Buffet', 'Bardisk'],
  HOT_FOOD:     ['Varmmat', 'Varmholding 1', 'Varmholding 2'],
  OTHER:        ['Annet'],
}

const currentSuggestions = computed(() => nameSuggestions[form.locationType] ?? [])

const isValidTemp = (v: string) => v.trim() === '' || v.trim() === '-' || /^-?\d+([.,]\d+)?$/.test(v.trim())

const tempMinError = computed(() => {
  if (!showTemp.value || form.tempMinC.trim() === '') return null
  if (!isValidTemp(form.tempMinC)) return 'Må være et tall, f.eks. -18 eller 2.5'
  return null
})

const tempMaxError = computed(() => {
  if (!showTemp.value || form.tempMaxC.trim() === '') return null
  if (!isValidTemp(form.tempMaxC)) return 'Må være et tall, f.eks. 4 eller 65'
  const min = parseFloat(form.tempMinC.replace(',', '.'))
  const max = parseFloat(form.tempMaxC.replace(',', '.'))
  if (!isNaN(min) && !isNaN(max) && max <= min) return 'Maks må være høyere enn min'
  return null
})

const tempInputsValid = computed(() => !tempMinError.value && !tempMaxError.value)

function onTypeChange() {
  if (modalMode.value !== 'add') return
  if (form.locationType === 'OTHER') {
    form.name = ''
    nextTick(() => {
      document.getElementById('loc-name')?.focus()
    })
  } else if (!form.name.trim()) {
    form.name = typeLabel(form.locationType)
  }
}

// Temperature-bearing types (show temp range fields)
const tempTypes = new Set(['FREEZER', 'FRIDGE', 'HOT_FOOD', 'KITCHEN'])
const showTemp = computed(() => tempTypes.has(form.locationType))

//  derived 
const active   = computed(() => locations.value.filter((l) => l.isActive))
const inactive = computed(() => locations.value.filter((l) => !l.isActive))

//  API 
async function fetchLocations() {
  if (!orgNumber.value) return
  loading.value = true
  error.value   = null
  try {
    const { data } = await client.get<Location[]>('/locations', {
      params: { orgNumber: orgNumber.value },
    })
    locations.value = data.sort((a, b) => a.name.localeCompare(b.name, 'nb'))
  } catch {
    error.value = 'Kunne ikke hente lokasjoner.'
  } finally {
    loading.value = false
  }
}

function showFeedback(type: 'success' | 'error', msg: string) {
  feedback.value = { type, msg }
  setTimeout(() => { feedback.value = null }, 4000)
}

//  modal helpers 
function openAdd() {
  modalMode.value   = 'add'
  editingId.value   = null
  modalError.value  = null
  form.name         = ''
  form.description  = ''
  form.locationType = 'OTHER'
  form.tempMinC     = ''
  form.tempMaxC     = ''
  form.isActive     = true
  modalOpen.value   = true
}

function openEdit(loc: Location) {
  modalMode.value   = 'edit'
  editingId.value   = loc.locationId
  modalError.value  = null
  form.name         = loc.name
  form.description  = loc.description ?? ''
  form.locationType = loc.locationType
  form.tempMinC     = loc.tempMinC != null ? String(loc.tempMinC) : ''
  form.tempMaxC     = loc.tempMaxC != null ? String(loc.tempMaxC) : ''
  form.isActive     = loc.isActive
  modalOpen.value   = true
}

function closeModal() {
  modalOpen.value = false
}

function buildPayload() {
  const min = form.tempMinC.trim() !== '' ? Number(form.tempMinC) : null
  const max = form.tempMaxC.trim() !== '' ? Number(form.tempMaxC) : null
  return {
    name:         form.name.trim(),
    description:  form.description.trim() || null,
    locationType: form.locationType,
    tempMinC:     showTemp.value ? min : null,
    tempMaxC:     showTemp.value ? max : null,
    isActive:     form.isActive,
  }
}

async function save() {
  if (!orgNumber.value || !form.name.trim()) return
  saving.value      = true
  modalError.value  = null
  try {
    const payload = buildPayload()
    if (modalMode.value === 'add') {
      const { data } = await client.post<Location>('/locations', payload, {
        params: { orgNumber: orgNumber.value },
      })
      locations.value = [...locations.value, data].sort((a, b) => a.name.localeCompare(b.name, 'nb'))
      showFeedback('success', `«${data.name}» er lagt til.`)
    } else {
      const { data } = await client.put<Location>(`/locations/${editingId.value}`, payload, {
        params: { orgNumber: orgNumber.value },
      })
      locations.value = locations.value
          .map((l) => l.locationId === data.locationId ? data : l)
          .sort((a, b) => a.name.localeCompare(b.name, 'nb'))
      showFeedback('success', `«${data.name}» er oppdatert.`)
    }
    closeModal()
  } catch (err: unknown) {
    const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
    modalError.value = msg ?? 'Lagring feilet. Prøv igjen.'
  } finally {
    saving.value = false
  }
}

async function deleteLocation() {
  if (!orgNumber.value || confirmId.value === null) return
  deleting.value = true
  const id = confirmId.value
  try {
    await client.delete(`/locations/${id}`, {
      params: { orgNumber: orgNumber.value },
    })
    const removed = locations.value.find((l) => l.locationId === id)
    locations.value = locations.value.filter((l) => l.locationId !== id)
    confirmId.value = null
    showFeedback('success', `«${removed?.name ?? 'Lokasjon'}» er slettet.`)
  } catch {
    showFeedback('error', 'Sletting feilet. Prøv igjen.')
    confirmId.value = null
  } finally {
    deleting.value = false
  }
}

onMounted(fetchLocations)
</script>

<template>
  <div class="view-page locations-view">

    <!-- Header -->
    <header class="page-header">
      <div>
        <h1>Lokasjoner</h1>
        <p class="subtitle">Administrer lokasjoner og temperaturområder i organisasjonen</p>
      </div>
      <button class="btn btn--primary" type="button" @click="openAdd">
        + Ny lokasjon
      </button>
    </header>

    <!-- Error -->
    <div v-if="error" class="inline-error">{{ error }}</div>

    <!-- Loading -->
    <div v-if="loading" class="loading">Laster lokasjoner…</div>

    <template v-else>
      <!-- Stats -->
      <div class="stats-row">
        <div class="stat app-surface">
          <strong>{{ locations.length }}</strong>
          <span>Totalt</span>
        </div>
        <div class="stat app-surface stat--ok">
          <strong>{{ active.length }}</strong>
          <span>Aktive</span>
        </div>
        <div class="stat app-surface stat--muted">
          <strong>{{ inactive.length }}</strong>
          <span>Inaktive</span>
        </div>
      </div>

      <!-- Empty -->
      <div v-if="locations.length === 0" class="empty">
        <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/>
        </svg>
        <p>Ingen lokasjoner registrert ennå.</p>
        <button class="btn btn--primary" type="button" @click="openAdd">Legg til første lokasjon</button>
      </div>

      <!-- Table -->
      <div v-else class="table-card app-surface">
        <table>
          <thead>
          <tr>
            <th>Navn</th>
            <th>Type</th>
            <th>Temperaturområde</th>
            <th>Status</th>
            <th class="th-actions"></th>
          </tr>
          </thead>
          <tbody>
          <tr
              v-for="loc in locations"
              :key="loc.locationId"
              :class="{ 'tr--inactive': !loc.isActive }"
          >
            <td>
              <p class="cell-name">{{ loc.name }}</p>
              <p v-if="loc.description" class="cell-sub">{{ loc.description }}</p>
            </td>
            <td>
              <span class="type-pill">{{ typeLabel(loc.locationType) }}</span>
            </td>
            <td class="td-range">
                <span v-if="loc.tempMinC != null || loc.tempMaxC != null">
                  {{ loc.tempMinC ?? '?' }}°C – {{ loc.tempMaxC ?? '?' }}°C
                </span>
              <span v-else class="td-none">—</span>
            </td>
            <td>
                <span class="status-pill" :class="loc.isActive ? 'status-pill--ok' : 'status-pill--off'">
                  {{ loc.isActive ? 'Aktiv' : 'Inaktiv' }}
                </span>
            </td>
            <td class="td-actions">
              <button class="row-btn" type="button" @click="openEdit(loc)">Rediger</button>
              <button class="row-btn row-btn--danger" type="button" @click="confirmId = loc.locationId">Slett</button>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </template>

    <!--  Add / Edit modal  -->
    <Teleport to="body">
      <div v-if="modalOpen" class="overlay" @click.self="closeModal">
        <div class="modal" role="dialog" aria-modal="true">

          <header class="modal__header">
            <h2>{{ modalMode === 'add' ? 'Ny lokasjon' : 'Rediger lokasjon' }}</h2>
            <button class="modal__close" type="button" aria-label="Lukk" @click="closeModal">✕</button>
          </header>

          <div class="modal__body">

            <div class="field-row">
              <div class="field">
                <label for="loc-type">Type <span class="req">*</span></label>
                <select id="loc-type" v-model="form.locationType" @change="onTypeChange">
                  <option v-for="t in locationTypes" :key="t.value" :value="t.value">
                    {{ t.label }}
                  </option>
                </select>
              </div>

              <div class="field">
                <label for="loc-active">Status</label>
                <select id="loc-active" v-model="form.isActive">
                  <option :value="true">Aktiv</option>
                  <option :value="false">Inaktiv</option>
                </select>
              </div>
            </div>

            <div class="field">
              <label for="loc-name">Navn <span class="req">*</span></label>
              <input
                  id="loc-name"
                  v-model="form.name"
                  type="text"
                  list="loc-name-suggestions"
                  :placeholder="form.locationType === 'OTHER' ? 'Skriv inn egendefinert navn…' : 'f.eks. Kjøleskap 1'"
                  maxlength="100"
                  required
              />
              <datalist id="loc-name-suggestions">
                <option v-for="s in currentSuggestions" :key="s" :value="s" />
              </datalist>
              <p v-if="form.locationType === 'OTHER'" class="field-hint">Gi lokasjonen et beskrivende navn, f.eks. «Personalrom» eller «Ventilasjonsrom».</p>
            </div>

            <div class="field">
              <label for="loc-desc">Beskrivelse</label>
              <input
                  id="loc-desc"
                  v-model="form.description"
                  type="text"
                  placeholder="Valgfri beskrivelse"
                  maxlength="255"
              />
            </div>

            <div v-if="showTemp" class="field-row">
              <div class="field">
                <label for="loc-min">Min. temperatur (°C)</label>
                <input
                    id="loc-min"
                    v-model="form.tempMinC"
                    type="text"
                    inputmode="decimal"
                    placeholder="f.eks. 0"
                    :class="{ 'input--error': tempMinError }"
                />
                <p v-if="tempMinError" class="field-error">{{ tempMinError }}</p>
              </div>
              <div class="field">
                <label for="loc-max">Maks. temperatur (°C)</label>
                <input
                    id="loc-max"
                    v-model="form.tempMaxC"
                    type="text"
                    inputmode="decimal"
                    placeholder="f.eks. 4"
                    :class="{ 'input--error': tempMaxError }"
                />
                <p v-if="tempMaxError" class="field-error">{{ tempMaxError }}</p>
              </div>
            </div>

            <p v-if="modalError" class="form-error">{{ modalError }}</p>

          </div>

          <footer class="modal__footer">
            <button class="btn btn--ghost" type="button" @click="closeModal">Avbryt</button>
            <button
                class="btn btn--primary"
                type="button"
                :disabled="saving || !form.name.trim() || !tempInputsValid"
                @click="save"
            >
              {{ saving ? 'Lagrer…' : modalMode === 'add' ? 'Opprett' : 'Lagre' }}
            </button>
          </footer>

        </div>
      </div>
    </Teleport>

    <!--  Toast feedback  -->
    <Teleport to="body">
      <div v-if="feedback" class="toast" :class="`toast--${feedback.type}`" role="status" aria-live="polite">
        {{ feedback.msg }}
      </div>
    </Teleport>

    <!--  Confirm delete  -->
    <Teleport to="body">
      <div v-if="confirmId !== null" class="overlay" @click.self="confirmId = null">
        <div class="modal modal--sm" role="dialog" aria-modal="true">
          <header class="modal__header">
            <h2>Slett lokasjon</h2>
            <button class="modal__close" type="button" @click="confirmId = null">✕</button>
          </header>
          <div class="modal__body">
            <p class="confirm-msg">
              Er du sikker på at du vil slette
              <strong>«{{ locations.find((l) => l.locationId === confirmId)?.name }}»</strong>?
              Dette kan ikke angres.
            </p>
          </div>
          <footer class="modal__footer">
            <button class="btn btn--ghost" type="button" @click="confirmId = null">Avbryt</button>
            <button class="btn btn--danger" type="button" :disabled="deleting" @click="deleteLocation">
              {{ deleting ? 'Sletter…' : 'Slett' }}
            </button>
          </footer>
        </div>
      </div>
    </Teleport>

  </div>
</template>

<style scoped>
/* Bruker delte klasser fra components.css:
   - .view-page (wrapper)
   - .page-header (header layout)
   - .btn, .btn--primary (knapper)
   Se components.css for detaljer.
*/

.locations-view {
  /* Tilleggsspissifikk styling for lokasjoner */
}

/*  Header  */
.page-header h1 {
  font-family: var(--font-family-display);
  line-height: var(--line-height-heading);
  letter-spacing: -0.01em;
}

.subtitle {
  margin: 0.3rem 0 0;
}

/*  Toast feedback (fixed, doesn't shift layout)  */
.toast {
  position: fixed;
  bottom: 2rem;
  right: 2rem;
  z-index: 200;
  padding: 1rem 1.4rem;
  border-radius: var(--radius-lg);
  font-size: var(--font-size-base);
  font-weight: 600;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  max-width: 26rem;
  border-width: 1.5px;
  border-style: solid;
}
.toast--success {
  background: var(--color-success-bg);
  color: var(--color-success);
  border-color: var(--color-success);
}
.toast--error {
  background: var(--color-danger-bg);
  color: var(--color-danger);
  border-color: var(--color-danger);
}

.inline-error {
  padding: 0.65rem 0.9rem;
  background: var(--color-danger-bg);
  color: var(--color-danger);
  border: 1px solid color-mix(in srgb, var(--color-danger) 30%, var(--color-border));
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
}

.loading {
  padding: 3rem 1rem;
  text-align: center;
  color: var(--color-gray-400);
  font-size: var(--font-size-sm);
}

/*  Stats  */
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.75rem;
}
.stat {
  padding: 0.75rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}
.stat strong { font-size: 1.6rem; font-weight: 700; color: var(--color-foreground); }
.stat span   { font-size: var(--font-size-xs); color: var(--color-gray-500); }
.stat--ok    { border-left: 3px solid var(--color-success); }
.stat--muted { border-left: 3px solid var(--color-gray-300); }

/*  Empty state  */
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
.tr--inactive { opacity: 0.55; }

.cell-name { font-size: var(--font-size-sm); font-weight: 500; margin: 0; color: var(--color-foreground); }
.cell-sub  { font-size: var(--font-size-xs); color: var(--color-gray-500); margin: 0.1rem 0 0; }

.type-pill {
  display: inline-flex;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-size: var(--font-size-xs);
  font-weight: 500;
  background: var(--color-gray-100);
  color: var(--color-gray-700);
}

.td-range { font-size: var(--font-size-sm); color: var(--color-gray-700); font-variant-numeric: tabular-nums; }
.td-none  { color: var(--color-gray-400); }

.status-pill {
  display: inline-flex;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-size: var(--font-size-xs);
  font-weight: 600;
}
.status-pill--ok  { background: var(--color-success-bg); color: var(--color-success); }
.status-pill--off { background: var(--color-gray-100);   color: var(--color-gray-500); }

.th-actions { width: 1px; }
.td-actions { white-space: nowrap; text-align: right; }

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
  background: rgba(0, 39, 43, 0.58);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: var(--spacing-lg);
}
.modal {
  background: var(--color-surface-raised);
  border: none;
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-md);
  width: 100%;
  max-width: 26rem;
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
  padding: var(--spacing-lg);
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
  padding: var(--spacing-lg);
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}
.modal__footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-sm);
  padding: var(--spacing-lg);
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
.field select:focus {
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

.field-hint {
  margin: 0.3rem 0 0;
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
}

.field-error {
  margin: 0.3rem 0 0;
  font-size: var(--font-size-xs);
  color: var(--color-danger);
  font-weight: 500;
}

.input--error {
  border-color: var(--color-danger) !important;
  background: var(--color-danger-bg) !important;
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

/*  Buttons  
   Bruker delte klasser fra components.css:
   - .btn (base)
   - .btn--primary, .btn--ghost, .btn--danger (varianter)
   
   WCAG-forbedringer: Tydeligere kanter og kontrast for bedre synlighet
*/

.btn { 
  min-height: 2.25rem;
  border: 2px solid transparent;
}

/* + Ny lokasjon - forsterket synlighet */
.btn--primary {
  border-color: var(--color-brand-deep-forest);
  box-shadow: 0 2px 4px rgba(0, 39, 43, 0.15);
  font-weight: 700;
  letter-spacing: 0.02em;
}

.btn-danger {
  min-height: 2.25rem;
  padding: 0.4rem 1.25rem;
  border: none;
  border-radius: var(--radius-md);
  background: var(--color-danger);
  color: var(--color-danger-fg);
  font-size: var(--font-size-sm);
  font-weight: 600;
  cursor: pointer;
}
.btn-danger:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-danger:hover:not(:disabled) { background: var(--color-danger-hover); }

/*  Responsive  */
@media (max-width: 48rem) {
  .page-header { flex-direction: column; align-items: flex-start; }
  .stats-row { grid-template-columns: repeat(3, 1fr); }
  .field-row { grid-template-columns: 1fr; }
  th:nth-child(4), td:nth-child(4) { display: none; }
}
</style>