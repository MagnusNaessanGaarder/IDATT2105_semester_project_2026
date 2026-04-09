<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { client } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import DeviationReportForm from '@/shared/components/DeviationReportForm.vue'

type DeviationStatus =
    | 'DRAFT'
    | 'REPORTED'
    | 'UNDER_INVESTIGATION'
    | 'CORRECTIVE_ACTION_PLANNED'
    | 'CORRECTIVE_ACTION_COMPLETED'
    | 'CLOSED'

type Severity = 'MINOR' | 'MAJOR' | 'CRITICAL'
type ReportType = 'INCIDENT' | 'DISCREPANCY'

interface DeviationReport {
  reportId: number
  orgNumber: number
  reportType: ReportType
  severity: Severity
  title: string
  description: string
  locationText: string | null
  occurredDate: string | null
  occurredTime: string | null
  reportDate: string
  status: DeviationStatus
  immediateActionText: string | null
  causeAnalysisText: string | null
  correctiveActionText: string | null
  completionText: string | null
  closedAt: string | null
  discoveredByName: string | null
  reportedToName: string | null
  assignedToName?: string | null
}

interface UserRole {
  roleId: number
  roleName: string
}

interface User {
  userId: number
  displayName: string
  email: string
  phone?: string
  isActive: boolean
  roles: UserRole[]
}

type FilterStatus = 'all' | DeviationStatus

const authStore = useAuthStore()
const orgNumber = computed(() => authStore.currentOrg?.orgNumber)

const deviations = ref<DeviationReport[]>([])
const loading = ref(false)
const refreshing = ref(false)
const error = ref<string | null>(null)

const selectedStatus = ref<FilterStatus>('all')
const selectedId = ref<number | null>(null)
const showForm = ref(false)

const actionLoading = ref(false)
const actionError = ref<string | null>(null)
const actionText = ref('')
const activeAction = ref<'immediate' | 'cause' | 'corrective' | 'complete' | null>(null)

const employees = ref<User[]>([])
const assignOpen = ref(false)
const assignUserId = ref<number | null>(null)
const assignLoading = ref(false)
const assignError = ref<string | null>(null)

async function fetchEmployees() {
  if (!orgNumber.value) return
  try {
    const response = await client.get<User[]>('/api/users', {
      params: { orgNumber: orgNumber.value },
    })
    const data = response.data
    employees.value = data.filter((u) => u.isActive)
  } catch {
    // non-critical — assign UI degrades gracefully
  }
}

async function fetchDeviations() {
  if (!orgNumber.value) return
  loading.value = true
  error.value = null
  try {
    const { data } = await client.get<DeviationReport[]>('/deviations', {
      params: { orgNumber: orgNumber.value },
    })
    deviations.value = data
    const first = data[0]
    if (selectedId.value === null && first) {
      selectedId.value = first.reportId
    }
  } catch {
    error.value = 'Kunne ikke hente avvik. Prøv igjen.'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDeviations()
  fetchEmployees()
})

const filtered = computed(() => {
  if (selectedStatus.value === 'all') return deviations.value
  return deviations.value.filter((d) => d.status === selectedStatus.value)
})

const selectedDeviation = computed(
    () => filtered.value.find((d) => d.reportId === selectedId.value) ?? filtered.value[0] ?? null
)

watch(filtered, (list) => {
  const first = list[0]
  if (!list.find((d) => d.reportId === selectedId.value) && first) {
    selectedId.value = first.reportId
  }
})

function selectDeviation(id: number) {
  selectedId.value = id
  activeAction.value = null
  actionText.value = ''
  actionError.value = null
  assignOpen.value = false
  assignUserId.value = null
  assignError.value = null
}

async function assignReport() {
  const d = selectedDeviation.value
  if (!d || !orgNumber.value || !assignUserId.value) {
    assignError.value = 'Velg en person å tildele avviket til.'
    return
  }
  assignLoading.value = true
  assignError.value = null
  try {
    await client.post(
        `/deviations/${d.reportId}/assign`,
        {},
        { params: { orgNumber: orgNumber.value, assignedToUserId: assignUserId.value } }
    )
    await refetchReport(d.reportId)
    const emp = employees.value.find((e) => e.userId === assignUserId.value)
    const updated = deviations.value.find((r) => r.reportId === d.reportId)
    if (updated && emp) updated.assignedToName = emp.displayName
    assignOpen.value = false
    assignUserId.value = null
  } catch (err: unknown) {
    const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
    assignError.value = msg ?? 'Tildeling feilet. Prøv igjen.'
  } finally {
    assignLoading.value = false
  }
}

async function createReport(payload: unknown) {
  if (!orgNumber.value) return
  try {
    const { data } = await client.post<DeviationReport>('/deviations', payload, {
      params: { orgNumber: orgNumber.value },
    })
    deviations.value.unshift(data)
    selectedId.value = data.reportId
    showForm.value = false
  } catch {
    error.value = 'Kunne ikke opprette avvik.'
  }
}

async function startInvestigation() {
  const d = selectedDeviation.value
  if (!d || !orgNumber.value) return
  actionLoading.value = true
  actionError.value = null
  try {
    await client.put<DeviationReport>(
        `/deviations/${d.reportId}/status`,
        { status: 'UNDER_INVESTIGATION' },
        { params: { orgNumber: orgNumber.value } }
    )
    await refetchReport(d.reportId)
  } catch (err: unknown) {
    const msg = (err as { response?: { data?: { message?: string }; status?: number } })?.response?.data?.message
    actionError.value = msg ? `Statusendring feilet: ${msg}` : 'Statusendring feilet.'
  } finally {
    actionLoading.value = false
  }
}

async function submitAction() {
  const d = selectedDeviation.value
  if (!d || !orgNumber.value || !activeAction.value) return
  if (!actionText.value.trim()) { actionError.value = 'Tekst er påkrevd.'; return }

  const endpoints: Record<NonNullable<typeof activeAction.value>, string> = {
    immediate: `/deviations/${d.reportId}/immediate-action`,
    cause: `/deviations/${d.reportId}/cause-analysis`,
    corrective: `/deviations/${d.reportId}/corrective-action`,
    complete: `/deviations/${d.reportId}/complete`,
  }

  actionLoading.value = true
  actionError.value = null
  try {
    await client.post<DeviationReport>(
        endpoints[activeAction.value],
        { actionText: actionText.value.trim() },
        { params: { orgNumber: orgNumber.value } }
    )
    await refetchReport(d.reportId)
    activeAction.value = null
    actionText.value = ''
  } catch (err: unknown) {
    const msg = (err as { response?: { data?: { message?: string }; status?: number } })?.response?.data?.message
    actionError.value = msg ? `Feilet: ${msg}` : 'Handlingen feilet. Prøv igjen.'
  } finally {
    actionLoading.value = false
  }
}

async function closeReport() {
  const d = selectedDeviation.value
  if (!d || !orgNumber.value) return
  actionLoading.value = true
  actionError.value = null
  try {
    await client.post<DeviationReport>(
        `/deviations/${d.reportId}/close`,
        {},
        { params: { orgNumber: orgNumber.value } }
    )
    await refetchReport(d.reportId)
  } catch (err: unknown) {
    const msg = (err as { response?: { data?: { message?: string }; status?: number } })?.response?.data?.message
    actionError.value = msg ? `Lukking feilet: ${msg}` : 'Lukking feilet.'
    console.error('closeReport error:', err)
  } finally {
    actionLoading.value = false
  }
}

async function refetchReport(reportId: number) {
  // Re-fetch the full list to guarantee we get the latest state.
  // This is safe since fetchDeviations already works correctly.
  await fetchDeviations()
  // Restore the selected item after the refresh
  selectedId.value = reportId
}

function statusLabel(status: DeviationStatus): string {
  const map: Record<DeviationStatus, string> = {
    DRAFT: 'Utkast',
    REPORTED: 'Rapportert',
    UNDER_INVESTIGATION: 'Under etterforskning',
    CORRECTIVE_ACTION_PLANNED: 'Tiltak planlagt',
    CORRECTIVE_ACTION_COMPLETED: 'Tiltak fullført',
    CLOSED: 'Lukket',
  }
  return map[status] ?? status
}

function statusTone(status: DeviationStatus): string {
  if (status === 'CLOSED' || status === 'CORRECTIVE_ACTION_COMPLETED') return 'good'
  if (status === 'REPORTED') return 'danger'
  return 'warn'
}

function severityLabel(s: Severity): string {
  return { MINOR: 'Lav', MAJOR: 'Moderat', CRITICAL: 'Kritisk' }[s] ?? s
}

function severityTone(s: Severity): string {
  return { MINOR: 'info', MAJOR: 'warn', CRITICAL: 'danger' }[s] ?? 'info'
}

function reportTypeLabel(t: ReportType): string {
  return { INCIDENT: 'Hendelse', DISCREPANCY: 'Avvik' }[t] ?? t
}

function formatDate(d: string | null): string {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('nb-NO', { day: '2-digit', month: 'short', year: 'numeric' })
}

const filters: { key: FilterStatus; label: string }[] = [
  { key: 'all', label: 'Alle' },
  { key: 'REPORTED', label: 'Rapportert' },
  { key: 'UNDER_INVESTIGATION', label: 'Etterforskning' },
  { key: 'CORRECTIVE_ACTION_PLANNED', label: 'Tiltak planlagt' },
  { key: 'CORRECTIVE_ACTION_COMPLETED', label: 'Tiltak fullført' },
  { key: 'CLOSED', label: 'Lukket' },
]

const actionLabels: Record<NonNullable<typeof activeAction.value>, string> = {
  immediate: 'Umiddelbar handling',
  cause: 'Årsaksanalyse',
  corrective: 'Korrigerende tiltak',
  complete: 'Fullføring',
}
</script>

<template>
  <div class="deviations-page">
    
    <!-- Page Header -->
    <header class="page-header">
      <div>
        <h1 class="page-title">Avvik</h1>
        <p class="page-subtitle">Registrer, prioriter og lukk avvik i en samlet arbeidsflate</p>
      </div>
      <button class="primary-btn" type="button" @click="showForm = true">Meld avvik</button>
    </header>

    <div class="filter-row" role="tablist">
      <button
          v-for="f in filters"
          :key="f.key"
          class="filter-chip"
          :class="{ 'filter-chip--active': selectedStatus === f.key }"
          @click="selectedStatus = f.key"
      >
        {{ f.label }}
      </button>
    </div>

    <div v-if="loading" class="state-msg">Laster avvik…</div>
    <div v-else-if="error" class="state-msg state-msg--error">{{ error }}</div>

    <section v-else class="deviations-layout">
      <aside class="panel panel--list" aria-label="Avviksliste">
        <div v-if="filtered.length === 0" class="empty-list">Ingen avvik matcher valgt filter.</div>
        <button
            v-for="item in filtered"
            :key="item.reportId"
            class="list-item"
            :class="{ 'list-item--active': selectedDeviation?.reportId === item.reportId }"
            @click="selectDeviation(item.reportId)"
        >
          <p class="list-item__title">{{ item.title }}</p>
          <p class="list-item__meta">{{ item.locationText ?? '—' }} · {{ formatDate(item.reportDate) }}</p>
          <div class="list-item__chips">
            <span class="chip" :class="`chip--${statusTone(item.status)}`">{{ statusLabel(item.status) }}</span>
            <span class="chip" :class="`chip--${severityTone(item.severity)}`">{{ severityLabel(item.severity) }}</span>
          </div>
        </button>
      </aside>

      <article v-if="selectedDeviation" class="panel panel--detail">
        <header class="detail-header">
          <div>
            <h2 class="detail-title">{{ selectedDeviation.title }}</h2>
            <div class="detail-header__chips">
              <span class="chip" :class="`chip--${statusTone(selectedDeviation.status)}`">{{ statusLabel(selectedDeviation.status) }}</span>
              <span class="chip chip--neutral">{{ reportTypeLabel(selectedDeviation.reportType) }}</span>
              <span class="chip" :class="`chip--${severityTone(selectedDeviation.severity)}`">{{ severityLabel(selectedDeviation.severity) }}</span>
              <span v-if="refreshing" class="chip chip--neutral">Oppdaterer…</span>
            </div>
          </div>
        </header>

        <p class="detail-description">{{ selectedDeviation.description }}</p>

        <!-- Deviation Details -->
        <div class="detail-grid">
          <div class="detail-field">
            <p class="detail-label">Rapportert</p>
            <p class="detail-value">{{ formatDate(selectedDeviation.reportDate) }}</p>
          </div>
          <div class="detail-field">
            <p class="detail-label">Hendelsesdato</p>
            <p class="detail-value">
              {{ formatDate(selectedDeviation.occurredDate) }}{{ selectedDeviation.occurredTime ? ` kl. ${selectedDeviation.occurredTime}` : '' }}
            </p>
          </div>
          <div class="detail-field">
            <p class="detail-label">Lokasjon</p>
            <p class="detail-value">{{ selectedDeviation.locationText ?? '—' }}</p>
          </div>
          <div class="detail-field">
            <p class="detail-label">Oppdaget av</p>
            <p class="detail-value">{{ selectedDeviation.discoveredByName ?? '—' }}</p>
          </div>
          <div class="detail-field">
            <p class="detail-label">Rapportert til</p>
            <p class="detail-value">{{ selectedDeviation.reportedToName ?? '—' }}</p>
          </div>
          <div class="detail-field">
            <p class="detail-label">Tildelt</p>
            <p class="detail-value">{{ selectedDeviation.assignedToName ?? '—' }}</p>
          </div>
        </div>

        <div class="workflow">
          <div class="workflow__step" :class="{ 'workflow__step--done': selectedDeviation.immediateActionText }">
            <p class="workflow__label">Umiddelbar handling</p>
            <p v-if="selectedDeviation.immediateActionText" class="workflow__text">{{ selectedDeviation.immediateActionText }}</p>
            <p v-else class="workflow__empty">Ikke registrert</p>
          </div>
          <div class="workflow__step" :class="{ 'workflow__step--done': selectedDeviation.causeAnalysisText }">
            <p class="workflow__label">Årsaksanalyse</p>
            <p v-if="selectedDeviation.causeAnalysisText" class="workflow__text">{{ selectedDeviation.causeAnalysisText }}</p>
            <p v-else class="workflow__empty">Ikke registrert</p>
          </div>
          <div class="workflow__step" :class="{ 'workflow__step--done': selectedDeviation.correctiveActionText }">
            <p class="workflow__label">Korrigerende tiltak</p>
            <p v-if="selectedDeviation.correctiveActionText" class="workflow__text">{{ selectedDeviation.correctiveActionText }}</p>
            <p v-else class="workflow__empty">Ikke registrert</p>
          </div>
          <div class="workflow__step" :class="{ 'workflow__step--done': selectedDeviation.completionText }">
            <p class="workflow__label">Fullføring</p>
            <p v-if="selectedDeviation.completionText" class="workflow__text">{{ selectedDeviation.completionText }}</p>
            <p v-else class="workflow__empty">Ikke registrert</p>
          </div>
        </div>

        <div v-if="selectedDeviation.status !== 'CLOSED'" class="actions">
          <p class="actions__heading">Handlinger</p>

          <div class="actions__btns">
            <button
                v-if="selectedDeviation.status === 'REPORTED'"
                class="action-btn action-btn--primary"
                :disabled="actionLoading"
                @click="startInvestigation"
            >
              {{ actionLoading ? 'Starter…' : 'Start etterforskning' }}
            </button>

            <template v-if="selectedDeviation.status === 'UNDER_INVESTIGATION'">
              <button
                  v-if="!selectedDeviation.immediateActionText"
                  class="action-btn"
                  :class="{ 'action-btn--active': activeAction === 'immediate' }"
                  @click="activeAction = activeAction === 'immediate' ? null : 'immediate'; actionText = ''"
              >
                Legg til umiddelbar handling
              </button>
              <button
                  v-if="!selectedDeviation.causeAnalysisText"
                  class="action-btn"
                  :class="{ 'action-btn--active': activeAction === 'cause' }"
                  @click="activeAction = activeAction === 'cause' ? null : 'cause'; actionText = ''"
              >
                Legg til årsaksanalyse
              </button>
            </template>

            <button
                v-if="(selectedDeviation.status === 'CORRECTIVE_ACTION_PLANNED' || (selectedDeviation.status === 'UNDER_INVESTIGATION' && selectedDeviation.immediateActionText && selectedDeviation.causeAnalysisText)) && !selectedDeviation.correctiveActionText"
                class="action-btn"
                :class="{ 'action-btn--active': activeAction === 'corrective' }"
                @click="activeAction = activeAction === 'corrective' ? null : 'corrective'; actionText = ''"
            >
              Legg til korrigerende tiltak
            </button>

            <button
                v-if="selectedDeviation.status === 'CORRECTIVE_ACTION_PLANNED'"
                class="action-btn"
                :class="{ 'action-btn--active': activeAction === 'complete' }"
                @click="activeAction = activeAction === 'complete' ? null : 'complete'; actionText = ''"
            >
              Fullfør avvik
            </button>

            <button
                v-if="selectedDeviation.status === 'CORRECTIVE_ACTION_COMPLETED'"
                class="action-btn action-btn--close"
                :disabled="actionLoading"
                @click="closeReport"
            >
              Lukk avvik
            </button>

            <button
                class="action-btn"
                :class="{ 'action-btn--active': assignOpen }"
                :disabled="actionLoading || assignLoading"
                @click="assignOpen = !assignOpen; assignError = null"
            >
              {{ selectedDeviation.assignedToName ? 'Endre tildeling' : 'Tildel avvik' }}
            </button>
          </div>

          <div v-if="actionError && !activeAction" class="action-error">{{ actionError }}</div>

          <div v-if="assignOpen" class="action-form">
            <label class="action-form__label" for="assign-select">Tildel til</label>
            <select id="assign-select" v-model="assignUserId" class="action-form__select">
              <option :value="null">— Velg ansatt —</option>
              <option v-for="emp in employees" :key="emp.userId" :value="emp.userId">
                {{ emp.displayName }}
              </option>
            </select>
            <div class="action-form__footer">
              <p v-if="assignError" class="action-form__error">{{ assignError }}</p>
              <button class="action-btn action-btn--ghost" @click="assignOpen = false; assignError = null">Avbryt</button>
              <button class="action-btn action-btn--primary" :disabled="assignLoading || !assignUserId" @click="assignReport">
                {{ assignLoading ? 'Lagrer…' : 'Lagre' }}
              </button>
            </div>
          </div>

          <div v-if="activeAction" class="action-form">
            <label class="action-form__label" :for="`action-${selectedDeviation.reportId}`">
              {{ actionLabels[activeAction] }}
            </label>
            <textarea
                :id="`action-${selectedDeviation.reportId}`"
                v-model="actionText"
                class="action-form__textarea"
                rows="3"
                :placeholder="`Beskriv ${actionLabels[activeAction].toLowerCase()}…`"
            />
            <div class="action-form__footer">
              <p v-if="actionError" class="action-form__error">{{ actionError }}</p>
              <button class="action-btn action-btn--ghost" @click="activeAction = null; actionText = ''; actionError = null">Avbryt</button>
              <button class="action-btn action-btn--primary" :disabled="actionLoading" @click="submitAction">
                {{ actionLoading ? 'Lagrer…' : 'Lagre' }}
              </button>
            </div>
          </div>
        </div>

        <div v-if="selectedDeviation.status === 'CLOSED'" class="closed-banner">
          Avvik lukket{{ selectedDeviation.closedAt ? ` ${formatDate(selectedDeviation.closedAt)}` : '' }}
        </div>
      </article>

      <div v-else class="panel panel--detail panel--empty">
        <p>Velg et avvik fra listen.</p>
      </div>
    </section>

    <DeviationReportForm :open="showForm" @submit="createReport" @cancel="showForm = false" />
  </div>
</template>

<style scoped>
.deviations-page {
  max-width: 76rem;
  margin: 0 auto;
  display: grid;
  gap: 1.25rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 1rem;
}

.page-title {
  margin: 0;
  font-size: var(--font-size-3xl);
  font-weight: 700;
  letter-spacing: -0.015em;
}

.page-subtitle {
  margin-top: 0.5rem;
  color: var(--color-gray-500);
  font-size: var(--font-size-sm);
}

.primary-btn {
  flex-shrink: 0;
  min-height: 2.5rem;
  padding: 0 1rem;
  border: none;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: var(--color-primary-foreground);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  cursor: pointer;
}

.primary-btn:hover {
  opacity: 0.88;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.filter-chip {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.4rem 0.8rem;
  background: var(--color-card);
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
  cursor: pointer;
}

.filter-chip--active {
  border-color: var(--color-foreground);
  background: var(--color-foreground);
  color: var(--color-primary-foreground);
}

.state-msg {
  padding: 1.25rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-card);
  color: var(--color-gray-500);
  font-size: var(--font-size-sm);
  text-align: center;
}

.state-msg--error {
  color: var(--color-danger);
  background: var(--color-danger-bg);
  border-color: color-mix(in srgb, var(--color-danger) 30%, var(--color-border));
}

.deviations-layout {
  display: grid;
  grid-template-columns: minmax(16rem, 22rem) 1fr;
  gap: 1rem;
  align-items: start;
}

.panel {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.panel--list {
  padding: 0.5rem;
  display: grid;
  gap: 0.45rem;
  align-content: start;
}

.panel--detail {
  padding: 1.1rem;
  display: grid;
  gap: 1rem;
}

.panel--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 12rem;
  color: var(--color-gray-400);
  font-size: var(--font-size-sm);
}

.empty-list {
  padding: 1rem 0.5rem;
  color: var(--color-gray-400);
  font-size: var(--font-size-sm);
  text-align: center;
}

.list-item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  text-align: left;
  padding: 0.7rem 0.75rem;
  cursor: pointer;
  transition: background var(--transition-fast), border-color var(--transition-fast);
  width: 100%;
}

.list-item:hover {
  background: var(--color-gray-50);
}

.list-item--active {
  border-color: var(--color-foreground);
  background: var(--color-gray-50);
}

.list-item__title {
  margin: 0;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-foreground);
}

.list-item__meta {
  margin: 0.2rem 0 0;
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
}

.list-item__chips {
  margin-top: 0.45rem;
  display: flex;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.chip {
  display: inline-flex;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  padding: 0.15rem 0.45rem;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
}

.chip--good    { background: var(--color-success-bg); color: var(--color-success); border-color: color-mix(in srgb, var(--color-success) 30%, var(--color-border)); }
.chip--warn    { background: var(--color-warning-bg); color: var(--color-warning); border-color: color-mix(in srgb, var(--color-warning) 30%, var(--color-border)); }
.chip--danger  { background: var(--color-danger-bg);  color: var(--color-danger);  border-color: color-mix(in srgb, var(--color-danger)  30%, var(--color-border)); }
.chip--info    { background: var(--color-info-bg);    color: var(--color-info);    border-color: color-mix(in srgb, var(--color-info)    30%, var(--color-border)); }
.chip--neutral { background: var(--color-gray-100);   color: var(--color-gray-600); border-color: var(--color-border); }

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
}

.detail-header__chips {
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
  margin-top: 0.5rem;
}

.detail-title {
  margin: 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-foreground);
}

.detail-description {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
  line-height: 1.6;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
  padding: 0.85rem;
  background: var(--color-gray-50);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.detail-label {
  margin: 0;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  color: var(--color-gray-500);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.detail-value {
  margin: 0.2rem 0 0;
  font-size: var(--font-size-sm);
  color: var(--color-foreground);
}

.workflow {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.5rem;
}

.workflow__step {
  padding: 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
}

.workflow__step--done {
  border-color: color-mix(in srgb, var(--color-success) 40%, var(--color-border));
  background: var(--color-success-bg);
}

.workflow__label {
  margin: 0 0 0.35rem;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-gray-500);
}

.workflow__step--done .workflow__label {
  color: var(--color-success);
}

.workflow__text {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-foreground);
  line-height: 1.5;
}

.workflow__empty {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-gray-400);
  font-style: italic;
}

.actions {
  border-top: 1px solid var(--color-border);
  padding-top: 1rem;
  display: grid;
  gap: 0.75rem;
}

.actions__heading {
  margin: 0;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-gray-400);
}

.actions__btns {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.action-btn {
  min-height: 2.25rem;
  padding: 0 0.85rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  color: var(--color-foreground);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  cursor: pointer;
  transition: background var(--transition-fast), border-color var(--transition-fast);
}

.action-btn:hover:not(:disabled) {
  background: var(--color-gray-100);
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-btn--active {
  border-color: var(--color-foreground);
  background: var(--color-foreground);
  color: var(--color-primary-foreground);
}

.action-btn--primary {
  border-color: var(--color-foreground);
  background: var(--color-foreground);
  color: var(--color-primary-foreground);
}

.action-btn--primary:hover:not(:disabled) {
  opacity: 0.88;
  background: var(--color-foreground);
}

.action-btn--close {
  border-color: color-mix(in srgb, var(--color-success) 50%, var(--color-border));
  background: var(--color-success-bg);
  color: var(--color-success);
}

.action-btn--ghost {
  background: transparent;
  border-color: var(--color-border);
  color: var(--color-gray-600);
}

.action-error {
  font-size: var(--font-size-sm);
  color: var(--color-danger);
}

.action-form {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-gray-50);
  padding: 0.85rem;
  display: grid;
  gap: 0.5rem;
}

.action-form__label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-gray-700);
}

.action-form__textarea {
  width: 100%;
  min-height: 5rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-family: inherit;
  resize: vertical;
  background: var(--color-card);
  box-sizing: border-box;
}

.action-form__textarea:focus {
  outline: none;
  border-color: var(--color-focus);
}

.action-form__select {
  width: 100%;
  min-height: 2.5rem;
  padding: 0.5rem 2rem 0.5rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-family: inherit;
  background: var(--color-card);
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.6rem center;
  cursor: pointer;
  box-sizing: border-box;
}

.action-form__select:focus {
  outline: none;
  border-color: var(--color-focus);
}

.action-form__footer {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  justify-content: flex-end;
}

.action-form__error {
  flex: 1;
  margin: 0;
  font-size: var(--font-size-xs);
  color: var(--color-danger);
}

.closed-banner {
  padding: 0.75rem 1rem;
  border-radius: var(--radius-md);
  background: var(--color-success-bg);
  border: 1px solid color-mix(in srgb, var(--color-success) 30%, var(--color-border));
  color: var(--color-success);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  text-align: center;
}

@media (max-width: 64rem) {
  .deviations-layout {
    grid-template-columns: 1fr;
  }

  .workflow {
    grid-template-columns: repeat(2, 1fr);
  }

  .detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 40rem) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .primary-btn {
    width: 100%;
  }

  .workflow {
    grid-template-columns: 1fr;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>