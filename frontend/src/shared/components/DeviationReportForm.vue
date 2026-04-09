<!--
  DeviationReportForm – Gjenbrukbar komponent for å melde et avvik

  Brukes overalt der brukere kan opprette en avviksrapport.

  Props:
    open        – boolean, om skjemaet vises (som modal-innhold)

  Emits:
    submit(payload)  – DeviationReportCreateRequest klar for POST
    cancel           – bruker avbrøt

  Eksempel:
    <DeviationReportForm :open="showForm" @submit="createReport" @cancel="showForm = false" />
-->
<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { client } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import { getUsers, type User } from '@/features/admin/api/users'

type ReportType = 'INCIDENT' | 'DISCREPANCY'
type Severity = 'MINOR' | 'MAJOR' | 'CRITICAL'

interface Location {
  locationId: number
  name: string
  description?: string
}

interface DeviationReportCreateRequest {
  reportType: ReportType
  severity: Severity
  title: string
  description: string
  locationId?: number
  occurredDate?: string
  occurredTime?: string
  discoveredByUserId?: number
  discoveredByName?: string
  reportedToUserId?: number
  reportedToName?: string
}

const props = defineProps<{ open: boolean }>()

const emit = defineEmits<{
  submit: [payload: DeviationReportCreateRequest]
  cancel: []
}>()

const authStore = useAuthStore()

const locations = ref<Location[]>([])
const locationsLoading = ref(false)
const locationsError = ref(false)

const employees = ref<User[]>([])
const employeesLoading = ref(false)
const employeesFallback = ref(false)
const employeesError = ref(false)

async function fetchLocations() {
  const orgNumber = authStore.currentOrg?.orgNumber
  if (!orgNumber) return
  locationsLoading.value = true
  locationsError.value = false
  try {
    const { data } = await client.get<Location[]>('/locations', { params: { orgNumber } })
    locations.value = data.filter((l) => l.name)
  } catch {
    locationsError.value = true
  } finally {
    locationsLoading.value = false
  }
}

async function fetchEmployees() {
  const orgNumber = authStore.currentOrg?.orgNumber
  if (!orgNumber) return
  employeesLoading.value = true
  employeesFallback.value = false
  employeesError.value = false
  try {
    const data = await getUsers(orgNumber)
    employees.value = data.filter((u) => u.isActive)
  } catch (err: unknown) {
    const status = (err as { response?: { status?: number } })?.response?.status
    if (status === 403) {
      employeesFallback.value = true
    } else {
      employeesError.value = true
    }
  } finally {
    employeesLoading.value = false
  }
}

watch(() => props.open, (isOpen) => {
  if (isOpen) {
    if (locations.value.length === 0) fetchLocations()
    if (employees.value.length === 0 && !employeesFallback.value) fetchEmployees()
  }
})

const form = ref({
  reportType: '' as ReportType | '',
  severity: '' as Severity | '',
  title: '',
  description: '',
  locationId: null as number | null,
  occurredDate: '',
  occurredTime: '',
  discoveredByUserId: null as number | null,
  discoveredByName: '',
  reportedToUserId: null as number | null,
  reportedToName: '',
})

const attempted = ref(false)

const errors = computed(() => {
  const e: Partial<Record<keyof typeof form.value, string>> = {}
  if (!form.value.reportType) e.reportType = 'Velg avvikstype'
  if (!form.value.severity) e.severity = 'Velg alvorlighetsgrad'
  if (!form.value.title.trim()) e.title = 'Tittel er påkrevd'
  if (!form.value.description.trim()) e.description = 'Beskrivelse er påkrevd'
  return e
})

const hasErrors = computed(() => Object.keys(errors.value).length > 0)

function handleSubmit() {
  attempted.value = true
  if (hasErrors.value) return

  const payload: DeviationReportCreateRequest = {
    reportType: form.value.reportType as ReportType,
    severity: form.value.severity as Severity,
    title: form.value.title.trim(),
    description: form.value.description.trim(),
  }

  if (form.value.locationId) payload.locationId = form.value.locationId
  if (form.value.occurredDate) payload.occurredDate = form.value.occurredDate
  if (form.value.occurredTime) payload.occurredTime = form.value.occurredTime

  if (employeesFallback.value) {
    if (form.value.discoveredByName.trim()) payload.discoveredByName = form.value.discoveredByName.trim()
    if (form.value.reportedToName.trim()) payload.reportedToName = form.value.reportedToName.trim()
  } else {
    if (form.value.discoveredByUserId) payload.discoveredByUserId = form.value.discoveredByUserId
    if (form.value.reportedToUserId) payload.reportedToUserId = form.value.reportedToUserId
  }

  emit('submit', payload)
  resetForm()
}

function handleCancel() {
  resetForm()
  emit('cancel')
}

function resetForm() {
  attempted.value = false
  form.value = {
    reportType: '',
    severity: '',
    title: '',
    description: '',
    locationId: null,
    occurredDate: '',
    occurredTime: '',
    discoveredByUserId: null,
    discoveredByName: '',
    reportedToUserId: null,
    reportedToName: '',
  }
}
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="overlay" @click.self="handleCancel">
      <div class="dialog" role="dialog" aria-modal="true" aria-labelledby="deviation-dialog-title">

        <div class="dialog__header">
          <div>
            <h2 id="deviation-dialog-title" class="dialog__title">Meld avvik</h2>
            <p class="dialog__subtitle">Felt merket * er påkrevd. Resterende opplysninger fylles ut av ansvarlig under behandling.</p>
          </div>
          <button class="dialog__close" type="button" aria-label="Lukk" @click="handleCancel">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <div class="dialog__body">

          <div class="field-row">
            <div class="field">
              <label class="label" for="dev-type">Type <span class="req" aria-hidden="true">*</span></label>
              <div class="segment" role="group" aria-labelledby="dev-type-label">
                <button
                    type="button"
                    class="segment__btn"
                    :class="{ 'segment__btn--active': form.reportType === 'INCIDENT' }"
                    @click="form.reportType = 'INCIDENT'"
                >
                  Hendelse
                </button>
                <button
                    type="button"
                    class="segment__btn"
                    :class="{ 'segment__btn--active': form.reportType === 'DISCREPANCY' }"
                    @click="form.reportType = 'DISCREPANCY'"
                >
                  Avvik
                </button>
              </div>
              <p v-if="attempted && errors.reportType" class="field__error">{{ errors.reportType }}</p>
            </div>

            <div class="field">
              <label class="label">Alvorlighetsgrad <span class="req" aria-hidden="true">*</span></label>
              <div class="severity-row" role="group">
                <button
                    type="button"
                    class="severity-btn severity-btn--minor"
                    :class="{ 'severity-btn--active': form.severity === 'MINOR' }"
                    @click="form.severity = 'MINOR'"
                >
                  Lav
                </button>
                <button
                    type="button"
                    class="severity-btn severity-btn--major"
                    :class="{ 'severity-btn--active': form.severity === 'MAJOR' }"
                    @click="form.severity = 'MAJOR'"
                >
                  Moderat
                </button>
                <button
                    type="button"
                    class="severity-btn severity-btn--critical"
                    :class="{ 'severity-btn--active': form.severity === 'CRITICAL' }"
                    @click="form.severity = 'CRITICAL'"
                >
                  Kritisk
                </button>
              </div>
              <p v-if="attempted && errors.severity" class="field__error">{{ errors.severity }}</p>
            </div>
          </div>

          <div class="field">
            <label class="label" for="dev-title">Tittel <span class="req" aria-hidden="true">*</span></label>
            <input
                id="dev-title"
                v-model="form.title"
                type="text"
                class="input"
                :class="{ 'input--error': attempted && errors.title }"
                placeholder="Kort, beskrivende tittel på avviket"
                maxlength="255"
            />
            <p v-if="attempted && errors.title" class="field__error">{{ errors.title }}</p>
          </div>

          <div class="field">
            <label class="label" for="dev-description">Beskrivelse <span class="req" aria-hidden="true">*</span></label>
            <textarea
                id="dev-description"
                v-model="form.description"
                class="input input--textarea"
                :class="{ 'input--error': attempted && errors.description }"
                rows="4"
                placeholder="Beskriv hva som skjedde, hva ble observert og eventuelle umiddelbare konsekvenser"
                maxlength="10000"
            />
            <p v-if="attempted && errors.description" class="field__error">{{ errors.description }}</p>
          </div>

          <div class="divider" />

          <p class="optional-heading">Valgfrie opplysninger</p>

          <div class="field-row">
            <div class="field">
              <label class="label" for="dev-date">Hendelsesdato</label>
              <input id="dev-date" v-model="form.occurredDate" type="date" class="input" />
            </div>
            <div class="field">
              <label class="label" for="dev-time">Tidspunkt</label>
              <input id="dev-time" v-model="form.occurredTime" type="time" class="input" />
            </div>
          </div>

          <div class="field">
            <label class="label" for="dev-location">Sted / lokasjon</label>
            <select
                id="dev-location"
                v-model="form.locationId"
                class="input"
                :disabled="locationsLoading"
            >
              <option :value="null">
                {{ locationsLoading ? 'Henter lokasjoner…' : '- Ingen lokasjon valgt -' }}
              </option>
              <option v-for="loc in locations" :key="loc.locationId" :value="loc.locationId">
                {{ loc.name }}
              </option>
            </select>
            <p v-if="locationsError" class="field__error">Kunne ikke hente lokasjoner. Prøv igjen.</p>
          </div>

          <div class="field-row">
            <div class="field">
              <label class="label" for="dev-discovered">Oppdaget av</label>
              <select
                  v-if="!employeesFallback"
                  id="dev-discovered"
                  v-model="form.discoveredByUserId"
                  class="input"
                  :disabled="employeesLoading"
              >
                <option :value="null">
                  {{ employeesLoading ? 'Henter ansatte…' : '- Ikke angitt -' }}
                </option>
                <option v-for="emp in employees" :key="emp.userId" :value="emp.userId">
                  {{ emp.displayName }}
                </option>
              </select>
              <input
                  v-else
                  id="dev-discovered"
                  v-model="form.discoveredByName"
                  type="text"
                  class="input"
                  placeholder="Navn på person som oppdaget avviket"
                  maxlength="255"
              />
            </div>
            <div class="field">
              <label class="label" for="dev-reported-to">Rapportert til</label>
              <select
                  v-if="!employeesFallback"
                  id="dev-reported-to"
                  v-model="form.reportedToUserId"
                  class="input"
                  :disabled="employeesLoading"
              >
                <option :value="null">
                  {{ employeesLoading ? 'Henter ansatte…' : '- Ikke angitt -' }}
                </option>
                <option v-for="emp in employees" :key="emp.userId" :value="emp.userId">
                  {{ emp.displayName }}
                </option>
              </select>
              <input
                  v-else
                  id="dev-reported-to"
                  v-model="form.reportedToName"
                  type="text"
                  class="input"
                  placeholder="Navn på ansvarlig leder"
                  maxlength="255"
              />
            </div>
          </div>

        </div>

        <div class="dialog__footer">
          <p v-if="attempted && hasErrors" class="footer__error">Fyll ut alle påkrevde felt for å fortsette.</p>
          <button type="button" class="btn btn--ghost" @click="handleCancel">Avbryt</button>
          <button type="button" class="btn btn--primary" @click="handleSubmit">Send inn avvik</button>
        </div>

      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 1rem;
}

.dialog {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  width: 100%;
  max-width: 560px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.dialog__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  padding: 1.1rem 1.25rem 0.9rem;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.dialog__title {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-foreground);
}

.dialog__subtitle {
  margin: 0.2rem 0 0;
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
}

.dialog__close {
  border: none;
  background: none;
  color: var(--color-gray-400);
  cursor: pointer;
  padding: 0.25rem;
  border-radius: var(--radius-sm);
  display: flex;
  flex-shrink: 0;
}

.dialog__close:hover {
  color: var(--color-foreground);
  background: var(--color-accent);
}

.dialog__body {
  padding: 1.1rem 1.25rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  flex: 1;
}

.dialog__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.6rem;
  padding: 0.9rem 1.25rem;
  border-top: 1px solid var(--color-border);
  background: var(--color-gray-50);
  flex-shrink: 0;
}

.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-gray-700);
}

.req {
  color: var(--color-danger);
}

.input {
  min-height: 2.5rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-family: inherit;
  color: var(--color-foreground);
  background: var(--color-card);
  transition: border-color var(--transition-fast);
}

.input:focus {
  outline: none;
  border-color: var(--color-focus);
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.12);
}

.input--error {
  border-color: var(--color-danger);
}

.input--textarea {
  min-height: 6rem;
  resize: vertical;
  line-height: 1.5;
}

select.input {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.6rem center;
  padding-right: 2rem;
  cursor: pointer;
}

select.input:disabled {
  color: var(--color-gray-400);
  cursor: not-allowed;
}

.field__error {
  margin: 0;
  font-size: var(--font-size-xs);
  color: var(--color-danger);
}

.segment {
  display: inline-flex;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.segment__btn {
  flex: 1;
  min-height: 2.5rem;
  padding: 0 1rem;
  border: none;
  border-right: 1px solid var(--color-border);
  background: var(--color-card);
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
  cursor: pointer;
  transition: background var(--transition-fast), color var(--transition-fast);
  white-space: nowrap;
}

.segment__btn:last-child {
  border-right: none;
}

.segment__btn--active {
  background: var(--color-foreground);
  color: var(--color-primary-foreground);
}

.severity-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.4rem;
}

.severity-btn {
  min-height: 2.5rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.severity-btn--minor { color: var(--color-success); }
.severity-btn--major { color: var(--color-warning); }
.severity-btn--critical { color: var(--color-danger); }

.severity-btn--minor.severity-btn--active {
  background: var(--color-success-bg);
  border-color: color-mix(in srgb, var(--color-success) 40%, var(--color-border));
}

.severity-btn--major.severity-btn--active {
  background: var(--color-warning-bg);
  border-color: color-mix(in srgb, var(--color-warning) 40%, var(--color-border));
}

.severity-btn--critical.severity-btn--active {
  background: var(--color-danger-bg);
  border-color: color-mix(in srgb, var(--color-danger) 40%, var(--color-border));
}

.divider {
  border: none;
  border-top: 1px solid var(--color-border);
  margin: 0.1rem 0;
}

.optional-heading {
  margin: 0;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-gray-400);
}

.footer__error {
  flex: 1;
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-danger);
}

.btn {
  min-height: 2.5rem;
  padding: 0 1.1rem;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  cursor: pointer;
  border: none;
  transition: opacity var(--transition-fast), background var(--transition-fast);
  white-space: nowrap;
}

.btn--primary {
  background: var(--color-primary);
  color: var(--color-primary-foreground);
}

.btn--primary:hover {
  opacity: 0.88;
}

.btn--ghost {
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-gray-700);
}

.btn--ghost:hover {
  background: var(--color-accent);
}

@media (max-width: 36rem) {
  .field-row {
    grid-template-columns: 1fr;
  }

  .dialog__footer {
    flex-wrap: wrap;
  }

  .btn {
    flex: 1;
    text-align: center;
  }

  .footer__error {
    width: 100%;
    flex: none;
  }
}
</style>