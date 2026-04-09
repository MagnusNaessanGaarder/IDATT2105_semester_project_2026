<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import {
  type AuditLogEntry,
  type SettingItem,
  type SettingsState,
  useAdminData,
} from '../composables/useAdminData'
import { useAuthStore } from '@/stores/auth'
import type { BackendSettings } from '../api/settingsApi'

const authStore = useAuthStore()
const data = useAdminData()

// Get current organization from auth store
const currentOrgNumber = computed(() => {
  return authStore.currentOrg?.orgNumber || 0
})

const settingsState = ref<SettingsState>({
  system: JSON.parse(JSON.stringify(data.settings.system)),
  notification_preferences: JSON.parse(JSON.stringify(data.settings.notification_preferences)),
  security: JSON.parse(JSON.stringify(data.settings.security)),
  backup: JSON.parse(JSON.stringify(data.settings.backup)),
})

const backendSettings = ref<BackendSettings | null>(null)
const hasChanges = ref(false)
const showSuccessMessage = ref(false)
const successMessage = ref('')
const successTimeoutId = ref<number | null>(null)

const query = ref('')

const sections = computed(() => [
  data.settings.system,
  data.settings.notification_preferences,
  data.settings.security,
  data.settings.backup,
])

const persistenceLabel = (item: SettingItem): string => {
  if (item.persistence === 'backend') return 'Delt (database)'
  if (item.persistence === 'local') return 'Lokal (denne nettleseren)'
  return 'Skrivebeskyttet'
}

const filteredAuditLog = computed(() => {
  const search = query.value.trim().toLowerCase()
  return data.sortedAuditLog.filter((entry) => {
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

const updateSetting = (sectionIndex: number, itemId: string, nextValue: unknown) => {
  const section = sections.value[sectionIndex]
  if (!section) {
    return
  }

  const item = section.items.find((entry) => entry.id === itemId)
  if (!item || item.persistence === 'readonly') {
    return
  }

  item.current_value = nextValue
  hasChanges.value = true
}

const isSettingActive = (item: SettingItem) => item.active !== false

const asDateTime = (entry: AuditLogEntry): string => data.formatDateTime(entry.timestamp)

/**
 * Load settings from backend on mount
 */
const loadBackendSettings = async () => {
  if (!currentOrgNumber.value) {
    return
  }

  const settings = await data.fetchSettingsFromBackend(currentOrgNumber.value)
  if (settings) {
    backendSettings.value = settings
    const mappedSettings = data.applyLocalSettings(
      data.mapBackendSettingsToFrontend(settings),
      currentOrgNumber.value
    )
    settingsState.value = {
      system: { ...mappedSettings.system },
      notification_preferences: { ...mappedSettings.notification_preferences },
      security: { ...mappedSettings.security },
      backup: { ...mappedSettings.backup },
    }
    hasChanges.value = false
  }
}

/**
 * Save settings to backend and localStorage
 */
const handleSave = async () => {
  if (!currentOrgNumber.value || !backendSettings.value) {
    return
  }

  const updatedBackendSettings = await data.saveSettings(
    settingsState.value,
    backendSettings.value,
    currentOrgNumber.value
  )

  if (updatedBackendSettings) {
    backendSettings.value = updatedBackendSettings
    hasChanges.value = false
    successMessage.value = 'Innstillinger lagret'
    showSuccessMessage.value = true
    if (successTimeoutId.value !== null) {
      window.clearTimeout(successTimeoutId.value)
    }
    successTimeoutId.value = window.setTimeout(() => {
      showSuccessMessage.value = false
      successTimeoutId.value = null
    }, 3000)
  }
}

/**
 * Export settings as JSON
 */
const handleExport = () => {
  if (!currentOrgNumber.value || !backendSettings.value) {
    return
  }

  data.exportSettings(
    settingsState.value,
    backendSettings.value,
    currentOrgNumber.value
  )
}

onMounted(() => {
  loadBackendSettings()
})

onUnmounted(() => {
  if (successTimeoutId.value !== null) {
    window.clearTimeout(successTimeoutId.value)
  }
})
</script>

<template>
  <div class="view-page settings-view">
    <header class="page-header">
      <div>
        <h1>Innstillinger</h1>
        <p class="subtitle">Konfigurer system, varslinger, sikkerhet og sikkerhetskopi</p>
      </div>
      <div class="header-actions">
        <button
          class="btn btn--secondary"
          type="button"
          :disabled="data.isLoading.value || !backendSettings"
          @click="handleExport"
        >
          Eksporter data
        </button>
        <button
          class="btn btn--primary"
          type="button"
          :disabled="!hasChanges || data.isLoading.value"
          @click="handleSave"
        >
          {{ data.isLoading.value ? 'Lagrer...' : 'Lagre endringer' }}
        </button>
      </div>
    </header>

    <!-- Success message -->
    <div v-if="showSuccessMessage" class="success-message">
      ✓ {{ successMessage }}
    </div>

    <!-- Error message -->
    <div v-if="data.error.value" class="error-message">
      ⚠ {{ data.error.value }}
    </div>

    <!-- Loading state -->
    <div v-if="data.isLoading.value && !backendSettings" class="loading-state">
      <p>Laster innstillinger fra server...</p>
    </div>

    <!-- Settings content (hidden while loading) -->
    <template v-if="backendSettings">
      <section class="settings-summary" aria-label="Systemoversikt">
        <article class="summary-card">
          <strong>{{ sections.length }}</strong>
          <span>Konfigurasjonsseksjoner</span>
        </article>
        <article class="summary-card">
          <strong>{{ sections.flatMap((section) => section.items).filter((item) => item.type === 'toggle').length }}</strong>
          <span>Brytere</span>
        </article>
        <article class="summary-card">
          <strong>{{ sections.flatMap((section) => section.items).filter((item) => item.type === 'number').length }}</strong>
          <span>Numeriske felt</span>
        </article>
        <article class="summary-card">
          <strong>{{ data.auditLog.length }}</strong>
          <span>Revisjonshendelser</span>
        </article>
      </section>

      <section class="settings-grid" aria-label="Konfigurasjonspanel">
        <article v-for="(section, sectionIndex) in sections" :key="section.section_title" class="settings-section">
          <h2 class="settings-title">{{ section.section_title }}</h2>
          <div class="settings-items">
            <div v-for="item in section.items" :key="item.id" class="setting-item">
              <div class="setting-header">
                <label :for="item.id" class="setting-label">{{ item.label }}</label>
                <span class="setting-persistence">{{ persistenceLabel(item) }}</span>
                <p v-if="item.description" class="setting-description">{{ item.description }}</p>
              </div>

              <div class="setting-control">
                <select
                  v-if="item.type === 'select'"
                  :id="item.id"
                  class="setting-select"
                  :value="String(item.current_value)"
                  :disabled="item.persistence === 'readonly' || data.isLoading.value"
                  @change="updateSetting(sectionIndex, item.id, ($event.target as HTMLSelectElement).value)"
                >
                  <option v-for="option in item.options" :key="option">{{ option }}</option>
                </select>

                <label v-else-if="item.type === 'toggle'" class="toggle-wrap">
                  <input
                    :id="item.id"
                    type="checkbox"
                    :checked="Boolean(item.current_value)"
                    :disabled="item.persistence === 'readonly' || data.isLoading.value"
                    @change="updateSetting(sectionIndex, item.id, ($event.target as HTMLInputElement).checked)"
                  >
                  <span>{{ Boolean(item.current_value) ? 'På' : 'Av' }}</span>
                </label>

                <input
                  v-else-if="item.type === 'number'"
                  :id="item.id"
                  class="setting-input"
                  type="number"
                  :value="Number(item.current_value)"
                  :min="item.min"
                  :max="item.max"
                  :disabled="item.persistence === 'readonly' || data.isLoading.value"
                  @change="updateSetting(sectionIndex, item.id, Number(($event.target as HTMLInputElement).value))"
                >

                <span v-else class="setting-info">{{ String(item.current_value) }}</span>
              </div>
            </div>
          </div>
        </article>
      </section>

      <section class="audit-section">
        <header class="audit-header">
          <h2>Revisjonslogg</h2>
          <input v-model="query" class="audit-search" type="search" placeholder="Søk i hendelser" />
        </header>

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
              <tr v-for="entry in filteredAuditLog" :key="entry.id">
                <td>{{ asDateTime(entry) }}</td>
                <td>{{ entry.user }}</td>
                <td>{{ entry.action }}</td>
                <td>{{ entry.resource }}</td>
                <td>{{ entry.details }}</td>
                <td>
                  <span class="result-pill" :class="{ 'result-pill--ok': entry.result === 'SUCCESS' }">
                    {{ entry.result }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.settings-view {
  display: grid;
  gap: 1rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 1rem;
}

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

.warning-state {
  border: 1px solid color-mix(in srgb, var(--color-warning) 35%, var(--color-border));
  background: var(--color-warning-bg);
  border-radius: var(--radius-md);
  color: var(--color-warning);
  padding: 0.8rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.7rem;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.settings-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.75rem;
}

.summary-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  text-align: center;
  padding: 0.85rem;
}

.summary-card strong {
  color: var(--color-gray-900);
  font-size: var(--font-size-xl);
}

.summary-card span {
  display: block;
  margin-top: 0.2rem;
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.settings-section {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.9rem;
}

.settings-title {
  margin: 0 0 0.75rem;
  font-size: var(--font-size-lg);
  font-weight: 600;
}

.settings-items {
  display: grid;
  gap: 0.8rem;
}

.setting-item {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 0.8rem;
  align-items: center;
  padding: 0.6rem 0;
  border-bottom: 1px solid var(--color-border);
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-item--inactive {
  opacity: 0.6;
}

.setting-header {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.setting-label {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--color-foreground);
}

.setting-description {
  margin: 0;
  font-size: var(--font-size-xs);
  color: var(--color-gray-600);
}

.setting-persistence {
  width: fit-content;
  margin-top: 0.1rem;
  padding: 0.1rem 0.4rem;
  border-radius: 999px;
  font-size: 0.68rem;
  font-weight: 600;
  color: var(--color-gray-600);
  background: var(--color-gray-100);
}

.setting-control {
  display: flex;
  align-items: center;
}

.setting-select,
.setting-input {
  min-height: 2.3rem;
  padding: 0 0.7rem;
  background: var(--color-gray-50);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  color: var(--color-foreground);
}

.toggle-wrap {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  font-size: var(--font-size-xs);
  color: var(--color-gray-600);
}

.toggle-wrap input {
  width: 1.1rem;
  height: 1.1rem;
}

.setting-info {
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
  padding: 0.4rem 0.65rem;
  background: var(--color-gray-100);
  border-radius: var(--radius-sm);
}

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
}

.audit-header h2 {
  font-size: var(--font-size-lg);
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

.btn {
  min-height: 2.7rem;
  padding: 0.45rem 0.95rem;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: 600;
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

.success-message {
  padding: 0.75rem 0.95rem;
  border-radius: var(--radius-md);
  background: var(--color-success-bg);
  color: var(--color-success);
  border: 1px solid var(--color-success);
  font-size: var(--font-size-sm);
  font-weight: 600;
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

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 20rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-gray-50);
  color: var(--color-gray-600);
}

@media (max-width: 48rem) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
  }

  .btn {
    flex: 1;
  }

  .settings-summary,
  .settings-grid {
    grid-template-columns: 1fr;
  }

  .setting-item {
    grid-template-columns: 1fr;
  }

  .audit-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .audit-search {
    width: 100%;
    min-width: 0;
  }
}
</style>
