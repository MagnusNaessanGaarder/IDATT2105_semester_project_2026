<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import {
  useExport,
  exportTypeLabels,
  exportFormatLabels,
  exportStatusLabels,
  exportStatusTone,
} from '../composables/useExport.ts'
import type { ExportType, ExportFormat } from '../api.ts'

const {
  exports,
  activeJob,
  isCreating,
  isPolling,
  isLoadingList,
  error,
  stalledWarning,
  createExport,
  downloadExport,
  loadExports,
  dismissError,
  resetActiveJob,
} = useExport()

// ── Form state ───────────────────────────────────────────────────────────────
const exportType = ref<ExportType>('FULL_COMPLIANCE_REPORT')
const exportFormat = ref<ExportFormat>('PDF')
const dateFrom = ref('')
const dateTo = ref('')

const exportTypes: ExportType[] = [
  'FULL_COMPLIANCE_REPORT',
  'AUDIT_REPORT',
  'CHECKLIST_REPORT',
  'TEMPERATURE_REPORT',
  'DEVIATION_REPORT',
  'TRAINING_REPORT',
]

const exportFormats: ExportFormat[] = ['PDF', 'JSON']

async function handleSubmit() {
  resetActiveJob()
  await createExport({
    exportType: exportType.value,
    format: exportFormat.value,
    dateFrom: dateFrom.value || null,
    dateTo: dateTo.value || null,
  })
}

function formatDate(iso: string | null | undefined) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('nb-NO', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// A job is "in flight" when it's PENDING or RUNNING
function isInFlight(status: string) {
  return status === 'PENDING' || status === 'RUNNING'
}

onMounted(() => loadExports())
onBeforeUnmount(() => resetActiveJob())
</script>

<template>
  <div class="view-page export-view">
    <!-- Header -->
    <header class="page-header">
      <div>
        <h1>Eksport</h1>
        <p class="subtitle">Generer samsvarsrapporter for Mattilsynet og interne revisjoner</p>
      </div>
    </header>

    <!-- Hard error banner -->
    <div v-if="error" class="banner banner--error" role="alert">
      <span>{{ error }}</span>
      <button type="button" class="banner__dismiss" aria-label="Lukk feilmelding" @click="dismissError">✕</button>
    </div>

    <!-- Stall warning banner (softer — job may still complete) -->
    <div v-else-if="stalledWarning" class="banner banner--warning" role="status">
      <span>⏳ {{ stalledWarning }}</span>
    </div>

    <!-- Layout: form + status side-by-side -->
    <div class="export-layout">

      <!-- Left: Export form -->
      <section class="export-card" aria-label="Opprett ny eksport">
        <h2 class="card-title">Ny eksport</h2>

        <div class="field">
          <label class="label" for="export-type">Rapporttype</label>
          <select id="export-type" v-model="exportType" class="select">
            <option v-for="t in exportTypes" :key="t" :value="t">
              {{ exportTypeLabels[t] }}
            </option>
          </select>
        </div>

        <div class="field">
          <label class="label">Format</label>
          <div class="format-row">
            <button
                v-for="fmt in exportFormats"
                :key="fmt"
                type="button"
                class="format-btn"
                :class="{ 'format-btn--active': exportFormat === fmt }"
                :aria-pressed="exportFormat === fmt"
                @click="exportFormat = fmt"
            >
              <span class="format-btn__icon">{{ fmt === 'PDF' ? '📄' : '{ }' }}</span>
              {{ exportFormatLabels[fmt] }}
            </button>
          </div>
        </div>

        <div class="field-row">
          <div class="field">
            <label class="label" for="date-from">Fra dato</label>
            <input id="date-from" v-model="dateFrom" type="date" class="input" />
          </div>
          <div class="field">
            <label class="label" for="date-to">Til dato</label>
            <input id="date-to" v-model="dateTo" type="date" class="input" />
          </div>
        </div>

        <button
            type="button"
            class="submit-btn"
            :disabled="isCreating || isPolling"
            @click="handleSubmit"
        >
          <span v-if="isCreating">Oppretter eksport…</span>
          <span v-else>Generer rapport</span>
        </button>
      </section>

      <!-- Right: Active job status -->
      <section class="export-card export-card--status" aria-label="Eksportstatus">
        <h2 class="card-title">Siste jobb</h2>

        <div v-if="!activeJob" class="empty-state-small">
          <p>Ingen aktiv eksportjobb.</p>
        </div>

        <template v-else>
          <div class="status-row">
            <span class="status-label">Rapporttype</span>
            <span class="status-value">{{ exportTypeLabels[activeJob.exportType] }}</span>
          </div>
          <div class="status-row">
            <span class="status-label">Format</span>
            <span class="status-value">{{ exportFormatLabels[activeJob.format] }}</span>
          </div>
          <div class="status-row">
            <span class="status-label">Status</span>
            <span class="pill" :class="`pill--${exportStatusTone[activeJob.status]}`">
              <span v-if="isPolling && isInFlight(activeJob.status)" class="spinner" />
              {{ exportStatusLabels[activeJob.status] ?? activeJob.status }}
            </span>
          </div>
          <div v-if="activeJob.recordCount != null" class="status-row">
            <span class="status-label">Poster</span>
            <span class="status-value">{{ activeJob.recordCount }}</span>
          </div>
          <div class="status-row">
            <span class="status-label">Opprettet</span>
            <span class="status-value">{{ formatDate(activeJob.requestedAt) }}</span>
          </div>
          <div v-if="activeJob.completedAt" class="status-row">
            <span class="status-label">Ferdigstilt</span>
            <span class="status-value">{{ formatDate(activeJob.completedAt) }}</span>
          </div>
          <div v-if="activeJob.failureReason" class="status-row">
            <span class="status-label">Årsak</span>
            <span class="status-value status-value--danger">{{ activeJob.failureReason }}</span>
          </div>

          <button
              v-if="activeJob.status === 'COMPLETED'"
              type="button"
              class="download-btn"
              @click="downloadExport(activeJob.exportJobId)"
          >
            Last ned {{ exportFormatLabels[activeJob.format] }}
          </button>
        </template>
      </section>
    </div>

    <!-- Export history -->
    <section class="history-section" aria-label="Eksporthistorikk">
      <h2 class="card-title">Historikk</h2>

      <div v-if="isLoadingList" class="loading-row">Laster historikk…</div>

      <div v-else-if="exports.length === 0" class="empty-state">
        <p>Ingen tidligere eksporter funnet.</p>
      </div>

      <table v-else class="history-table">
        <thead>
        <tr>
          <th>Rapporttype</th>
          <th>Format</th>
          <th>Status</th>
          <th>Poster</th>
          <th>Opprettet</th>
          <th>Ferdig</th>
          <th></th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="job in exports" :key="job.exportJobId" class="history-row">
          <td>{{ exportTypeLabels[job.exportType] }}</td>
          <td>{{ exportFormatLabels[job.format] }}</td>
          <td>
              <span class="pill" :class="`pill--${exportStatusTone[job.status]}`">
                {{ exportStatusLabels[job.status] ?? job.status }}
              </span>
          </td>
          <td>{{ job.recordCount ?? '—' }}</td>
          <td>{{ formatDate(job.requestedAt) }}</td>
          <td>{{ formatDate(job.completedAt) }}</td>
          <td>
            <button
                v-if="job.status === 'COMPLETED'"
                type="button"
                class="action-btn"
                @click="downloadExport(job.exportJobId)"
            >
              Last ned
            </button>
          </td>
        </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<style scoped>
.export-view {
  display: grid;
  gap: 1.5rem;
}

/* Header */
.page-header h1 {
  margin: 0;
  font-size: var(--font-size-3xl);
  font-weight: 700;
  letter-spacing: -0.015em;
}

.subtitle {
  margin-top: 0.4rem;
  color: var(--color-gray-500);
}

/* Banners */
.banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1rem;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
}

.banner--error {
  background: var(--color-danger-bg);
  border: 1px solid var(--color-danger);
  color: var(--color-danger);
}

.banner--warning {
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning);
  color: var(--color-warning);
}

.banner__dismiss {
  background: none;
  border: none;
  cursor: pointer;
  color: inherit;
  font-size: var(--font-size-base);
  line-height: 1;
  padding: 0;
}

/* Two-column layout */
.export-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

/* Cards */
.export-card {
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.card-title {
  margin: 0 0 0.25rem;
  font-size: var(--font-size-lg);
  font-weight: 700;
  color: var(--color-gray-900);
}

/* Form fields */
.field {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.label {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--color-gray-700);
}

.select,
.input {
  min-height: 2.6rem;
  padding: 0 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-gray-50);
  font-size: var(--font-size-sm);
  color: var(--color-gray-900);
  width: 100%;
}

.select:focus,
.input:focus {
  outline: 2px solid var(--color-focus);
  outline-offset: 1px;
}

/* Format toggle */
.format-row {
  display: flex;
  gap: 0.5rem;
}

.format-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  min-height: 2.6rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #fff;
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--color-gray-600);
  cursor: pointer;
  transition: background var(--transition-fast), color var(--transition-fast), border-color var(--transition-fast);
}

.format-btn--active {
  background: var(--color-foreground);
  color: #fff;
  border-color: var(--color-foreground);
}

.format-btn__icon {
  font-size: var(--font-size-base);
}

/* Submit */
.submit-btn {
  min-height: 2.8rem;
  padding: 0.5rem 1rem;
  border-radius: var(--radius-md);
  background: var(--color-foreground);
  color: #fff;
  font-size: var(--font-size-sm);
  font-weight: 700;
  cursor: pointer;
  transition: opacity var(--transition-fast);
  margin-top: auto;
}

.submit-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

/* Status rows */
.status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
  padding: 0.4rem 0;
  border-bottom: 1px solid var(--color-gray-100);
}

.status-row:last-of-type {
  border-bottom: none;
}

.status-label {
  font-size: var(--font-size-sm);
  color: var(--color-gray-500);
}

.status-value {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--color-gray-900);
  text-align: right;
}

.status-value--danger {
  color: var(--color-danger);
}

/* Pills */
.pill {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: var(--font-size-xs);
  border-radius: 999px;
  padding: 0.2rem 0.55rem;
  font-weight: 600;
  background: var(--color-gray-100);
  color: var(--color-gray-700);
}

.pill--green {
  background: var(--color-success-bg);
  color: var(--color-success);
}

.pill--blue {
  background: var(--color-info-bg);
  color: var(--color-info);
}

.pill--red {
  background: var(--color-danger-bg);
  color: var(--color-danger);
}

.pill--gray {
  background: var(--color-gray-100);
  color: var(--color-gray-600);
}

/* Spinner */
.spinner {
  display: inline-block;
  width: 0.6rem;
  height: 0.6rem;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Download */
.download-btn {
  min-height: 2.6rem;
  padding: 0.45rem 0.85rem;
  border-radius: var(--radius-md);
  background: var(--color-success);
  color: #fff;
  font-size: var(--font-size-sm);
  font-weight: 700;
  cursor: pointer;
  transition: opacity var(--transition-fast);
  margin-top: 0.25rem;
}

.download-btn:hover {
  opacity: 0.88;
}

/* History */
.history-section {
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1.25rem;
}

.loading-row {
  text-align: center;
  padding: 1.5rem;
  color: var(--color-gray-500);
  font-size: var(--font-size-sm);
}

.empty-state,
.empty-state-small {
  text-align: center;
  padding: 2rem;
  color: var(--color-gray-500);
  font-size: var(--font-size-sm);
}

.empty-state-small {
  padding: 1rem;
}

.history-table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--font-size-sm);
}

.history-table th {
  text-align: left;
  padding: 0.55rem 0.75rem;
  font-size: var(--font-size-xs);
  font-weight: 600;
  color: var(--color-gray-500);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border-bottom: 1px solid var(--color-border);
}

.history-row td {
  padding: 0.65rem 0.75rem;
  border-bottom: 1px solid var(--color-gray-100);
  color: var(--color-gray-800);
  vertical-align: middle;
}

.history-row:last-child td {
  border-bottom: none;
}

.action-btn {
  min-height: 2rem;
  padding: 0.25rem 0.65rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: #fff;
  font-size: var(--font-size-xs);
  font-weight: 600;
  color: var(--color-gray-700);
  cursor: pointer;
}

.action-btn:hover {
  background: var(--color-gray-50);
}

/* Responsive */
@media (max-width: 48rem) {
  .export-layout {
    grid-template-columns: 1fr;
  }

  .field-row {
    grid-template-columns: 1fr;
  }

  .history-table {
    display: block;
    overflow-x: auto;
    white-space: nowrap;
  }
}
</style>
