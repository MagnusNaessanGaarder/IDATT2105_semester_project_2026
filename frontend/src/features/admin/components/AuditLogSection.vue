<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { type AuditLogEntry, useAuditLog } from '../composables/useAuditLog'
import { formatDateTimeForOrganization } from '@/shared/utils/orgSettings'

const props = defineProps<{
  orgNumber: number
}>()

const auditData = useAuditLog()
const query = ref('')
const dateRangePreset = ref('')
const expandedEntryId = ref<number | null>(null)

const emit = defineEmits<{
  load: [orgNumber: number]
}>()

const toggleEntryDetails = (entryId: number) => {
  expandedEntryId.value = expandedEntryId.value === entryId ? null : entryId
}

const hasActiveFilters = computed(() => {
  return auditData.filters.value.actionType !== 'ALL' ||
    auditData.filters.value.fromDate !== '' ||
    auditData.filters.value.toDate !== '' ||
    query.value !== ''
})

const dateRangeError = computed(() => {
  const validation = auditData.validateDateRange()
  return validation.valid ? null : validation.error
})

const canExportCsv = computed(() => auditData.totalEntries.value > 0)

const applyDatePreset = () => {
  const today = new Date()
  const formatDate = (d: Date): string => {
    const parts = d.toISOString().split('T')
    return parts[0] ?? ''
  }

  switch (dateRangePreset.value) {
    case 'today':
      auditData.filters.value.fromDate = formatDate(today)
      auditData.filters.value.toDate = formatDate(today)
      break
    case 'week': {
      const weekAgo = new Date(today)
      weekAgo.setDate(weekAgo.getDate() - 7)
      auditData.filters.value.fromDate = formatDate(weekAgo)
      auditData.filters.value.toDate = formatDate(today)
      break
    }
    case 'month': {
      const monthAgo = new Date(today)
      monthAgo.setDate(monthAgo.getDate() - 30)
      auditData.filters.value.fromDate = formatDate(monthAgo)
      auditData.filters.value.toDate = formatDate(today)
      break
    }
    case 'custom':
      // Keep existing dates or set to today
      if (!auditData.filters.value.fromDate) {
        auditData.filters.value.fromDate = formatDate(today)
      }
      if (!auditData.filters.value.toDate) {
        auditData.filters.value.toDate = formatDate(today)
      }
      break
    default:
      auditData.filters.value.fromDate = ''
      auditData.filters.value.toDate = ''
  }

  if (dateRangePreset.value !== 'custom') {
    applyFilters()
  }
}

const filteredAuditLog = computed(() => {
  const search = query.value.trim().toLowerCase()
  return auditData.paginatedAuditLog.value.filter((entry) => {
    if (search.length === 0) {
      return true
    }

    return (
      entry.user.toLowerCase().includes(search) ||
      entry.action.toLowerCase().includes(search) ||
      entry.details.toLowerCase().includes(search) ||
      entry.resource.toLowerCase().includes(search)
    )
  })
})

const formatDateTime = (timestamp: string): string => {
  return formatDateTimeForOrganization(timestamp, props.orgNumber, {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const formatFieldName = (key: string): string => {
  const fieldNames: Record<string, string> = {
    localeCode: 'Locale',
    timezoneName: 'Tidssone',
    enableFoodModule: 'IK-Mat modul',
    enableAlcoholModule: 'IK-Alkohol modul',
    defaultTempMinC: 'Min temperatur',
    defaultTempMaxC: 'Maks temperatur',
    reminderEmailEnabled: 'E-postpåminnelser',
    notificationEmail: 'Varslings-e-post',
    retentionUserMonths: 'Brukerdata lagring',
    retentionAuditMonths: 'Revisjonsdata lagring',
    displayName: 'Visningsnavn',
    legalName: 'Juridisk navn',
    contactEmail: 'Kontakt e-post',
    contactPhone: 'Kontakt telefon',
    display_name: 'Visningsnavn',
    legal_name: 'Juridisk navn',
    contact_email: 'Kontakt e-post',
    contact_phone: 'Kontakt telefon',
  }
  return fieldNames[key] || key
}

const formatValue = (value: unknown): string => {
  if (value === null || value === undefined) return '(tom)'
  if (typeof value === 'boolean') return value ? 'På' : 'Av'
  if (typeof value === 'string') return value
  return String(value)
}

const getChangedFields = (entry: AuditLogEntry): Array<{ field: string; oldVal: string; newVal: string }> => {
  const changes: Array<{ field: string; oldVal: string; newVal: string }> = []
  const oldValues = entry.oldValues || {}
  const newValues = entry.newValues || {}
  const allKeys = new Set([...Object.keys(oldValues), ...Object.keys(newValues)])

  for (const key of allKeys) {
    if (key === 'createdAt' || key === 'updatedAt' || key === 'orgNumber') continue
    const oldVal = formatValue(oldValues[key])
    const newVal = formatValue(newValues[key])
    if (oldVal !== newVal) {
      changes.push({ field: formatFieldName(key), oldVal, newVal })
    }
  }
  return changes
}

const applyFilters = async () => {
  const validation = auditData.validateDateRange()
  if (!validation.valid) {
    return
  }
  auditData.resetPagination()
  await auditData.fetchAuditLog(props.orgNumber)
  emit('load', props.orgNumber)
}

const resetFilters = async () => {
  auditData.filters.value.actionType = 'ALL'
  auditData.filters.value.fromDate = ''
  auditData.filters.value.toDate = ''
  auditData.filters.value.entityType = ''
  auditData.filters.value.entityId = ''
  query.value = ''
  dateRangePreset.value = ''
  auditData.resetPagination()
  await auditData.fetchAuditLog(props.orgNumber)
  emit('load', props.orgNumber)
}

const handleExport = () => {
  auditData.exportToCsv()
}

onMounted(async () => {
  if (props.orgNumber) {
    await auditData.fetchAuditLog(props.orgNumber)
  }
})

watch(() => props.orgNumber, async (next) => {
  if (next) {
    await auditData.fetchAuditLog(next)
  }
})

// Expose refresh method for parent component
const refresh = async () => {
  if (props.orgNumber) {
    await auditData.fetchAuditLog(props.orgNumber)
  }
}

defineExpose({
  refresh,
})
</script>

<template>
  <section class="audit-section">
    <header class="audit-header">
      <div class="audit-header__left">
        <h2>Revisjonslogg</h2>
        <span class="audit-count">
          Viser {{ auditData.paginationInfo.value.start }}–{{ auditData.paginationInfo.value.end }} av {{ auditData.paginationInfo.value.total }} hendelser
        </span>
      </div>
      <div class="audit-header__right">
        <input v-model="query" class="audit-search" type="search" placeholder="Søk i hendelser" />
        <button
          v-if="canExportCsv"
          class="btn btn--secondary btn--sm"
          type="button"
          @click="handleExport"
        >
          Eksporter CSV
        </button>
      </div>
    </header>

    <div class="audit-filters">
      <div class="filter-group">
        <label class="filter-label">Handling</label>
        <select v-model="auditData.filters.value.actionType" class="audit-filter-control" @change="applyFilters">
          <option value="ALL">Alle handlinger</option>
          <option value="CREATE">La til</option>
          <option value="UPDATE">Endret</option>
          <option value="DELETE">Slettet</option>
          <option value="LOGIN">Innlogging</option>
          <option value="LOGOUT">Utlogging</option>
        </select>
      </div>

      <div class="filter-group">
        <label class="filter-label">Periode</label>
        <select v-model="dateRangePreset" class="audit-filter-control" @change="applyDatePreset">
          <option value="">Alle datoer</option>
          <option value="today">I dag</option>
          <option value="week">Siste 7 dager</option>
          <option value="month">Siste 30 dager</option>
          <option value="custom">Egne datoer...</option>
        </select>
      </div>

      <div v-if="dateRangePreset === 'custom'" class="filter-group filter-group--dates">
        <label class="filter-label">Fra dato</label>
        <input
          v-model="auditData.filters.value.fromDate"
          class="audit-filter-control"
          :class="{ 'audit-filter-control--error': dateRangeError }"
          type="date"
        >
      </div>

      <div v-if="dateRangePreset === 'custom'" class="filter-group filter-group--dates">
        <label class="filter-label">Til dato</label>
        <input
          v-model="auditData.filters.value.toDate"
          class="audit-filter-control"
          :class="{ 'audit-filter-control--error': dateRangeError }"
          type="date"
        >
      </div>

      <button v-if="hasActiveFilters" class="btn btn--secondary btn--sm filter-reset" type="button" @click="resetFilters">
        Nullstill filtre
      </button>
    </div>

    <p v-if="dateRangeError" class="date-error">{{ dateRangeError }}</p>

    <div v-if="auditData.isLoading.value" class="audit-empty-state">
      <p>Laster revisjonslogg...</p>
    </div>
    <div v-else-if="auditData.error.value" class="error-message">
      ⚠ {{ auditData.error.value }}
    </div>
    <div v-else-if="filteredAuditLog.length === 0" class="audit-empty-state">
      <p>Ingen revisjonshendelser funnet</p>
    </div>

    <template v-else>
      <div class="audit-table-wrap">
        <table>
          <thead>
            <tr>
              <th>Tid</th>
              <th>Bruker</th>
              <th>Handling</th>
              <th>Ressurs</th>
              <th>Detaljer</th>
              <th>Resultat</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="entry in filteredAuditLog" :key="entry.id">
              <tr
                class="audit-row"
                :class="{ 'audit-row--expanded': expandedEntryId === entry.id }"
                @click="toggleEntryDetails(entry.id)"
              >
                <td>{{ formatDateTime(entry.timestamp) }}</td>
                <td>{{ entry.user }}</td>
                <td>
                  <span class="action-badge" :class="`action-badge--${entry.action.toLowerCase().replace(/\s+/g, '-')}`">
                    {{ entry.action }}
                  </span>
                </td>
                <td>{{ entry.resource }}</td>
                <td class="details-cell">
                  <span class="details-preview">{{ entry.details }}</span>
                  <span v-if="entry.details.length > 50" class="expand-hint">(klikk for mer)</span>
                </td>
                <td>
                  <span class="result-pill" :class="{ 'result-pill--ok': entry.result === 'SUCCESS' }">
                    {{ entry.result }}
                  </span>
                </td>
              </tr>
              <tr v-if="expandedEntryId === entry.id" class="audit-row-details">
                <td colspan="6">
                  <div class="audit-details-panel">
                    <h4>Detaljer om hendelsen</h4>

                    <!-- Changed Fields Table -->
                    <div v-if="getChangedFields(entry).length > 0" class="changes-section">
                      <h5>Endrede felt</h5>
                      <table class="changes-table">
                        <thead>
                          <tr>
                            <th>Felt</th>
                            <th>Gammel verdi</th>
                            <th>Ny verdi</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="change in getChangedFields(entry)" :key="change.field">
                            <td class="field-name">{{ change.field }}</td>
                            <td class="old-value">{{ change.oldVal }}</td>
                            <td class="new-value">{{ change.newVal }}</td>
                          </tr>
                        </tbody>
                      </table>
                    </div>

                    <!-- Basic Info -->
                    <div class="detail-grid">
                      <div class="detail-item">
                        <span class="detail-label">Tidspunkt:</span>
                        <span class="detail-value">{{ formatDateTime(entry.timestamp) }}</span>
                      </div>
                      <div class="detail-item">
                        <span class="detail-label">Bruker:</span>
                        <span class="detail-value">{{ entry.user }}</span>
                      </div>
                      <div class="detail-item">
                        <span class="detail-label">Handling:</span>
                        <span class="detail-value">{{ entry.action }}</span>
                      </div>
                      <div class="detail-item">
                        <span class="detail-label">Ressurs:</span>
                        <span class="detail-value">{{ entry.resource }}</span>
                      </div>
                      <div class="detail-item">
                        <span class="detail-label">Resultat:</span>
                        <span class="detail-value" :class="{ 'text-success': entry.result === 'SUCCESS', 'text-error': entry.result === 'FAILED' }">
                          {{ entry.result }}
                        </span>
                      </div>
                    </div>
                    <button class="btn btn--secondary btn--sm close-details" @click.stop="toggleEntryDetails(entry.id)">
                      Lukk detaljer
                    </button>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div class="pagination">
        <div class="pagination__size">
          <label>Vis:</label>
          <select v-model="auditData.pageSize.value" @change="(e) => auditData.setPageSize(Number((e.target as HTMLSelectElement).value))">
            <option v-for="size in auditData.pageSizeOptions" :key="size" :value="size">{{ size }}</option>
          </select>
          <span>per side</span>
        </div>

        <div class="pagination__controls">
          <button
            class="btn btn--secondary btn--sm"
            :disabled="auditData.currentPage.value === 1"
            @click="auditData.goToFirstPage()"
          >
            « Første
          </button>
          <button
            class="btn btn--secondary btn--sm"
            :disabled="auditData.currentPage.value === 1"
            @click="auditData.goToPreviousPage()"
          >
            ‹ Forrige
          </button>

          <span class="pagination__info">
            Side {{ auditData.currentPage.value }} av {{ auditData.totalPages.value }}
          </span>

          <button
            class="btn btn--secondary btn--sm"
            :disabled="auditData.currentPage.value === auditData.totalPages.value"
            @click="auditData.goToNextPage()"
          >
            Neste ›
          </button>
          <button
            class="btn btn--secondary btn--sm"
            :disabled="auditData.currentPage.value === auditData.totalPages.value"
            @click="auditData.goToLastPage()"
          >
            Siste »
          </button>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.audit-section {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  padding: 0.75rem;
}

.audit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.7rem;
  margin-bottom: 0.75rem;
  flex-wrap: wrap;
}

.audit-header__left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.audit-header__right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.audit-header h2 {
  font-size: var(--font-size-lg);
  margin: 0;
}

.audit-count {
  font-size: var(--font-size-sm);
  color: var(--color-gray-500);
}

.audit-search {
  min-height: 2.4rem;
  min-width: 14rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-gray-50);
  padding: 0 0.75rem;
}

.audit-table-wrap {
  overflow-x: auto;
}

.audit-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
  padding: 0.75rem;
  background: var(--color-gray-50);
  border-radius: var(--radius-md);
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.filter-group--dates {
  min-width: 140px;
}

.filter-label {
  font-size: var(--font-size-xs);
  font-weight: 600;
  color: var(--color-gray-600);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.audit-filter-control {
  min-height: 2.3rem;
  padding: 0 0.7rem;
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  color: var(--color-foreground);
  min-width: 160px;
}

.audit-filter-control--error {
  border-color: var(--color-danger);
  background: var(--color-danger-bg);
}

.date-error {
  color: var(--color-danger);
  font-size: var(--font-size-sm);
  margin: -0.5rem 0 0.75rem 0.75rem;
}

.filter-reset {
  margin-left: auto;
}

.audit-empty-state {
  padding: 1rem;
  color: var(--color-gray-600);
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  text-align: left;
  padding: 0.65rem;
  border-bottom: 1px solid var(--color-gray-100);
  vertical-align: top;
}

th {
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  background: var(--color-gray-50);
}

td {
  font-size: var(--font-size-sm);
  color: var(--color-gray-700);
}

.result-pill {
  display: inline-flex;
  border-radius: 999px;
  padding: 0.2rem 0.5rem;
  font-size: var(--font-size-xs);
  font-weight: 600;
  background: var(--color-danger-bg);
  color: var(--color-danger);
}

.result-pill--ok {
  background: var(--color-success-bg);
  color: var(--color-success);
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border);
  flex-wrap: wrap;
  gap: 0.75rem;
}

.pagination__size {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
}

.pagination__size select {
  min-height: 2rem;
  padding: 0 0.5rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-gray-50);
}

.pagination__controls {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.pagination__info {
  padding: 0 0.75rem;
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
}

.btn {
  min-height: 2.7rem;
  padding: 0.45rem 0.95rem;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.btn--sm {
  min-height: 2.2rem;
  padding: 0.35rem 0.75rem;
  font-size: var(--font-size-xs);
}

.btn--primary {
  background: var(--color-foreground);
  color: var(--color-background);
}

.btn--primary:hover {
  background: var(--color-gray-900);
}

.btn--secondary {
  background: var(--color-card);
  color: var(--color-gray-700);
  border: 1px solid var(--color-border);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.error-message {
  padding: 0.75rem 0.95rem;
  border-radius: var(--radius-md);
  background: var(--color-danger-bg);
  color: var(--color-danger);
  border: 1px solid var(--color-danger);
  font-size: var(--font-size-sm);
  font-weight: 600;
}

@media (max-width: 48rem) {
  .audit-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .audit-search {
    width: 100%;
    min-width: 0;
  }

  .audit-filters {
    grid-template-columns: 1fr;
  }

  .pagination {
    flex-direction: column;
    align-items: stretch;
  }

  .pagination__controls {
    justify-content: center;
  }
}

/* Expandable audit row styles */
.audit-row {
  cursor: pointer;
  transition: background-color 0.2s;
}

.audit-row:hover {
  background: var(--color-gray-50);
}

.audit-row--expanded {
  background: var(--color-gray-100);
}

.details-cell {
  max-width: 300px;
}

.details-preview {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.expand-hint {
  font-size: var(--font-size-xs);
  color: var(--color-gray-400);
  font-style: italic;
  margin-left: 0.5rem;
}

.action-badge {
  display: inline-flex;
  padding: 0.2rem 0.5rem;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: 600;
}

.action-badge--la-til,
.action-badge--create {
  background: var(--color-success-bg);
  color: var(--color-success);
}

.action-badge--endret,
.action-badge--update {
  background: var(--color-warning-bg);
  color: var(--color-warning);
}

.action-badge--slettet,
.action-badge--delete {
  background: var(--color-danger-bg);
  color: var(--color-danger);
}

.audit-row-details {
  background: var(--color-gray-50);
}

.audit-row-details td {
  padding: 0;
  border-bottom: none;
}

.audit-details-panel {
  padding: 1rem 1.5rem;
}

.audit-details-panel h4 {
  margin: 0 0 1rem;
  font-size: var(--font-size-md);
  color: var(--color-gray-800);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.75rem 1.5rem;
  margin-bottom: 1rem;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.detail-item--full {
  grid-column: 1 / -1;
}

.detail-label {
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.detail-value {
  font-size: var(--font-size-sm);
  color: var(--color-gray-800);
  font-weight: 500;
}

.detail-value--long {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.5;
  padding: 0.5rem;
  background: white;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
}

.text-success {
  color: var(--color-success);
  font-weight: 600;
}

.text-error {
  color: var(--color-danger);
  font-weight: 600;
}

.close-details {
  margin-top: 0.5rem;
}

/* Changes table styles */
.changes-section {
  margin-bottom: 1.5rem;
  background: white;
  border-radius: var(--radius-md);
  padding: 1rem;
  border: 1px solid var(--color-border);
}

.changes-section h5 {
  margin: 0 0 0.75rem;
  font-size: var(--font-size-sm);
  color: var(--color-gray-700);
  font-weight: 600;
}

.changes-table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--font-size-sm);
}

.changes-table th {
  text-align: left;
  padding: 0.5rem;
  background: var(--color-gray-100);
  color: var(--color-gray-600);
  font-weight: 600;
  font-size: var(--font-size-xs);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border-bottom: 1px solid var(--color-border);
}

.changes-table td {
  padding: 0.5rem;
  border-bottom: 1px solid var(--color-gray-100);
}

.changes-table .field-name {
  font-weight: 500;
  color: var(--color-gray-800);
}

.changes-table .old-value {
  color: var(--color-gray-500);
  text-decoration: line-through;
  background: var(--color-gray-50);
  padding: 0.25rem 0.5rem;
  border-radius: var(--radius-sm);
}

.changes-table .new-value {
  color: var(--color-success);
  font-weight: 500;
  background: var(--color-success-bg);
  padding: 0.25rem 0.5rem;
  border-radius: var(--radius-sm);
}

@media (max-width: 48rem) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .changes-table {
    font-size: var(--font-size-xs);
  }

  .changes-table th,
  .changes-table td {
    padding: 0.35rem;
  }
}
</style>
