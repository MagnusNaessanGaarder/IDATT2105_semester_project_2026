<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import {
  type SettingItem,
  type SettingsState,
  useAdminData,
} from '../composables/useAdminData'
import useSettingsValidation from '../composables/useSettingsValidation'
import { getUsers } from '../api/users'
import { useAuthStore } from '@/stores/auth'
import type { BackendSettings } from '../api/settingsApi'
import { cacheOrganizationSettings } from '@/shared/utils/orgSettings'
import AuditLogSection from '../components/AuditLogSection.vue'

const authStore = useAuthStore()
const data = useAdminData()
const validation = useSettingsValidation()

// Get current user's email for auto-fill
const currentUserEmail = computed(() => authStore.user?.email ?? '')

const currentOrgNumber = computed(() => authStore.currentOrg?.orgNumber || 0)
const backendSettings = ref<BackendSettings | null>(null)
const originalSettings = ref<SettingsState | null>(null)
const cloneSettingsState = (state: SettingsState): SettingsState =>
  JSON.parse(JSON.stringify(state)) as SettingsState
const templateDefaults = ref<SettingsState>(cloneSettingsState(data.settings))
const settingsState = ref<SettingsState>(data.mapBackendSettingsToFrontend({
  orgNumber: 0,
  timezoneName: 'Europe/Oslo',
  localeCode: 'nb-NO',
  enableFoodModule: true,
  enableAlcoholModule: true,
  defaultTempMinC: null,
  defaultTempMaxC: null,
  reminderEmailEnabled: true,
  notificationEmail: null,
  displayName: null,
  legalName: null,
  contactEmail: null,
  contactPhone: null,
  retentionUserMonths: 12,
  retentionAuditMonths: 12,
  createdAt: '',
  updatedAt: '',
}))

const hasChanges = ref(false)
const showResetConfirm = ref(false)
const showDefaultsConfirm = ref(false)
const showUnsavedDialog = ref(false)
const pendingNavigation = ref<(() => void) | null>(null)

// Track original values for visual indicators
const originalValues = ref<Record<string, unknown>>({})

const sections = computed(() => [
  settingsState.value.profile,
  settingsState.value.organization,
  settingsState.value.modules,
  settingsState.value.temperature,
  settingsState.value.alerts_retention,
])

const modulesDisabledWarning = computed(() => {
  const moduleItems = settingsState.value.modules.items
  const foodEnabled = moduleItems.find((item) => item.id === 'enable_food_module')?.current_value === true
  const alcoholEnabled = moduleItems.find((item) => item.id === 'enable_alcohol_module')?.current_value === true
  return !foodEnabled && !alcoholEnabled
})

const updatedAtLabel = computed(() => {
  if (!backendSettings.value?.updatedAt) {
    return null
  }
  return data.formatDateTime(backendSettings.value.updatedAt)
})

const isAtTemplateDefaults = computed(() => {
  return JSON.stringify(settingsState.value) === JSON.stringify(templateDefaults.value)
})

const canResetToDefaults = computed(() => {
  return !data.isLoading.value && !!backendSettings.value && !isAtTemplateDefaults.value
})

const canResetChanges = computed(() => {
  return !data.isLoading.value && !!backendSettings.value && hasChanges.value
})

const canSaveChanges = computed(() => {
  return !data.isLoading.value && hasChanges.value
})

// Check if a field has been modified
const isFieldModified = (sectionId: string, itemId: string): boolean => {
  const key = `${sectionId}.${itemId}`
  const item = findItemById(sectionId, itemId)
  if (!item) return false
  return originalValues.value[key] !== item.current_value
}

const findItemById = (sectionId: string, itemId: string): SettingItem | undefined => {
  const section = sections.value.find((s) => s.id === sectionId)
  return section?.items.find((item) => item.id === itemId)
}

const storeOriginalValues = () => {
  originalValues.value = {}
  sections.value.forEach((section) => {
    section.items.forEach((item) => {
      originalValues.value[`${section.id}.${item.id}`] = item.current_value
    })
  })
}

const updateSetting = (sectionId: string, itemId: string, nextValue: unknown) => {
  const item = findItemById(sectionId, itemId)
  if (!item) return

  item.current_value = nextValue
  hasChanges.value = true

  // Real-time validation - validate immediately as user types/changes
  validation.validateField(itemId, nextValue, settingsState.value)
}

const invalidInputs = ref<Set<string>>(new Set())

const tempRangeError = ref<string | null>(null)

// Real-time validation for number inputs
const validateInputLive = (sectionId: string, itemId: string, input: HTMLInputElement, integerOnly = false) => {
  if (input.validity.badInput) {
    invalidInputs.value.add(itemId)
    return
  }
  const numValue = parseFloat(input.value)
  if (integerOnly && !Number.isInteger(numValue) && input.value !== '') {
    invalidInputs.value.add(itemId)
    return
  }
  invalidInputs.value.delete(itemId)
  if (!isNaN(numValue)) {
    updateSetting(sectionId, itemId, numValue)
  } else {
    validation.clearError(itemId)
  }
}

const validateAndUpdateNumber = (
  sectionId: string,
  itemId: string,
  input: HTMLInputElement,
  min?: number,
  max?: number
) => {
  const value = parseFloat(input.value)

  // Check if valid number
  if (isNaN(value)) {
    // Mark as invalid
    invalidInputs.value.add(itemId)
    return
  }

  // Valid number - remove invalid state
  invalidInputs.value.delete(itemId)

  // Apply min/max constraints
  let constrainedValue = value
  if (min !== undefined && value < min) {
    constrainedValue = min
  }
  if (max !== undefined && value > max) {
    constrainedValue = max
  }

  // Round to reasonable precision
  if (itemId.includes('temp')) {
    constrainedValue = Math.round(constrainedValue * 10) / 10 // 1 decimal for temp
  } else {
    constrainedValue = Math.round(constrainedValue) // Integer for months
  }

  // Update the value first
  updateSetting(sectionId, itemId, constrainedValue)

  // Check temperature cross-validation immediately
  if (itemId === 'default_temp_min_c' || itemId === 'default_temp_max_c') {
    validation.validateField(itemId, constrainedValue, settingsState.value)

    // Also validate the other temperature field
    const otherItemId = itemId === 'default_temp_min_c' ? 'default_temp_max_c' : 'default_temp_min_c'
    const otherItem = findItemById('temperature', otherItemId)
    if (otherItem) {
      validation.validateField(otherItemId, otherItem.current_value, settingsState.value)
    }
  }

  // Update input display if value was constrained
  if (constrainedValue !== value) {
    input.value = String(constrainedValue)
  }
}

const isInvalidInput = (itemId: string): boolean => {
  return invalidInputs.value.has(itemId)
}

const auditLogSectionRef = ref<InstanceType<typeof AuditLogSection> | null>(null)

const loadAuditLog = async (orgNumber: number) => {
  // Refresh the audit log section if available
  if (auditLogSectionRef.value) {
    await auditLogSectionRef.value.refresh()
  }
}

const applyProfileDefaults = async (targetState: SettingsState) => {
  const alertsSection = targetState.alerts_retention
  const notificationEmailItem = alertsSection.items.find(item => item.id === 'notification_email')
  const currentOrg = authStore.currentOrg

  let contactEmailItem: SettingItem | undefined
  let contactPhoneItem: SettingItem | undefined

  if (currentOrg) {
    const profileSection = targetState.profile
    const displayNameItem = profileSection.items.find(item => item.id === 'display_name')
    const legalNameItem = profileSection.items.find(item => item.id === 'legal_name')
    contactEmailItem = profileSection.items.find(item => item.id === 'contact_email')
    contactPhoneItem = profileSection.items.find(item => item.id === 'contact_phone')

    if (displayNameItem && (!displayNameItem.current_value || displayNameItem.current_value === '')) {
      displayNameItem.current_value = currentOrg.orgName
    }
    if (legalNameItem && (!legalNameItem.current_value || legalNameItem.current_value === '')) {
      legalNameItem.current_value = currentOrg.orgName
    }
    if (contactEmailItem && (!contactEmailItem.current_value || contactEmailItem.current_value === '')) {
      contactEmailItem.current_value = currentOrg.contactEmail ?? currentUserEmail.value
    }
    if (contactPhoneItem && (!contactPhoneItem.current_value || contactPhoneItem.current_value === '')) {
      contactPhoneItem.current_value = currentOrg.contactPhone ?? ''
    }
  }

  if (notificationEmailItem && (!notificationEmailItem.current_value || notificationEmailItem.current_value === '')) {
    notificationEmailItem.current_value = currentOrg?.contactEmail ?? currentUserEmail.value
  }

  const needsContactFallback =
    ((!contactEmailItem?.current_value || contactEmailItem.current_value === '') ||
      (!contactPhoneItem?.current_value || contactPhoneItem.current_value === '') ||
      (!notificationEmailItem?.current_value || notificationEmailItem.current_value === '')) &&
    currentUserEmail.value.length > 0

  if (needsContactFallback) {
    try {
      const users = await getUsers(currentOrgNumber.value)
      const currentUser = users.find((user) => user.email === currentUserEmail.value)
      if (currentUser) {
        if (contactEmailItem && (!contactEmailItem.current_value || contactEmailItem.current_value === '')) {
          contactEmailItem.current_value = currentUser.email
        }
        if (contactPhoneItem && (!contactPhoneItem.current_value || contactPhoneItem.current_value === '')) {
          contactPhoneItem.current_value = currentUser.phone ?? ''
        }
        if (notificationEmailItem && (!notificationEmailItem.current_value || notificationEmailItem.current_value === '')) {
          notificationEmailItem.current_value = currentUser.email
        }
      }
    } catch {
      // Keep existing values when user lookup fails.
    }
  }
}

const loadBackendSettings = async () => {
  if (!currentOrgNumber.value) return

  const settings = await data.fetchSettingsFromBackend(currentOrgNumber.value)
  if (settings) {
    backendSettings.value = settings
    cacheOrganizationSettings(settings)
    settingsState.value = data.mapBackendSettingsToFrontend(settings)
    await applyProfileDefaults(settingsState.value)

    templateDefaults.value = cloneSettingsState(data.settings)
    await applyProfileDefaults(templateDefaults.value)

    originalSettings.value = cloneSettingsState(settingsState.value)
    storeOriginalValues()
    hasChanges.value = false
    validation.clearAllErrors()
  }
}

const handleSave = async () => {
  if (!currentOrgNumber.value || !backendSettings.value) return
  if (!validation.validateSettings(settingsState.value)) return

  const updatedBackendSettings = await data.saveSettings(settingsState.value, currentOrgNumber.value)
  if (!updatedBackendSettings) return

  backendSettings.value = updatedBackendSettings
  cacheOrganizationSettings(updatedBackendSettings)
  settingsState.value = data.mapBackendSettingsToFrontend(updatedBackendSettings)
  await applyProfileDefaults(settingsState.value)
  originalSettings.value = cloneSettingsState(settingsState.value)
  storeOriginalValues()
  hasChanges.value = false
  validation.clearAllErrors()

  // Refresh audit log immediately to show the save action
  await loadAuditLog(currentOrgNumber.value)
}

const confirmReset = () => {
  showResetConfirm.value = true
}

const handleReset = async () => {
  showResetConfirm.value = false
  await loadBackendSettings()
}

const cancelReset = () => {
  showResetConfirm.value = false
}

const confirmResetToDefaults = () => {
  showDefaultsConfirm.value = true
}

const handleResetToDefaults = () => {
  showDefaultsConfirm.value = false
  settingsState.value = cloneSettingsState(templateDefaults.value)
  hasChanges.value = true
  validation.clearAllErrors()
}

const cancelResetToDefaults = () => {
  showDefaultsConfirm.value = false
}

// Keyboard shortcut for save
const handleKeydown = (e: KeyboardEvent) => {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    if (hasChanges.value && !data.isLoading.value) {
      handleSave()
    }
  }
}

// Navigation guard for unsaved changes
onBeforeRouteLeave((to, from, next) => {
  if (hasChanges.value) {
    showUnsavedDialog.value = true
    pendingNavigation.value = () => next()
    return false
  }
  next()
})

const confirmUnsavedNavigation = () => {
  showUnsavedDialog.value = false
  hasChanges.value = false // Prevent further prompts
  if (pendingNavigation.value) {
    pendingNavigation.value()
    pendingNavigation.value = null
  }
}

const cancelUnsavedNavigation = () => {
  showUnsavedDialog.value = false
  pendingNavigation.value = null
}

onMounted(() => {
  loadBackendSettings()
})

watch(currentOrgNumber, () => {
  loadBackendSettings()
})
</script>

<template>
  <div class="view-page settings-view" role="main" aria-labelledby="settings-heading">
    <!-- SettingsView bruker .view-page fra components.css for konsistent layout -->
    <header class="page-header">
      <div>
        <h1 id="settings-heading">Innstillinger</h1>
        <p class="subtitle">Konfigurer organisasjonsinnstillinger og revisjonsspor</p>
      </div>
      <div class="header-actions">
        <button
          class="btn btn--secondary"
          type="button"
          :disabled="!canResetToDefaults"
          @click="confirmResetToDefaults"
          aria-label="Tilbakestill alle innstillinger til systemstandardverdier"
        >
          Tilbakestill til standard
        </button>
        <button
          class="btn btn--secondary"
          type="button"
          :disabled="!canResetChanges"
          @click="confirmReset"
          aria-label="Forkast ulagrede endringer og last inn lagrede innstillinger"
        >
          Tilbakestill endringer
        </button>
        <button
          class="btn btn--primary"
          type="button"
          :disabled="!canSaveChanges"
          @click="handleSave"
          aria-label="Lagre endringer i organisasjonsinnstillinger"
        >
          {{ data.isLoading.value ? 'Lagrer...' : 'Lagre endringer' }}
        </button>
      </div>
    </header>

    <div v-if="data.error.value" class="error-message" role="alert" aria-live="assertive">
      ⚠ {{ data.error.value }}
    </div>
    <div v-if="modulesDisabledWarning" class="warning-message" role="alert" aria-live="polite">
      ⚠ Minst én modul må være aktivert.
    </div>

    <div v-if="data.isLoading.value && !backendSettings" class="loading-state">
      <p>Laster innstillinger fra server...</p>
    </div>

    <template v-if="backendSettings">
      <section class="settings-status-bar" aria-label="Lagringsstatus">
        <div class="status-indicator" :class="{ 'status-indicator--unsaved': hasChanges }">
          <span class="status-dot"></span>
          <span class="status-text">{{ hasChanges ? 'Ulagrede endringer' : 'Alle endringer lagret' }}</span>
        </div>
        <p v-if="updatedAtLabel" class="last-saved">Sist lagret: {{ updatedAtLabel }}</p>
      </section>

      <section class="settings-grid" aria-label="Konfigurasjonspanel">
        <!-- Left column: Profile + Alerts (bigger cards) -->
        <div class="settings-column">
          <article class="settings-section">
            <h2 class="settings-title">{{ settingsState.profile.section_title }}</h2>
            <div class="settings-items">
              <div
                v-for="item in settingsState.profile.items"
                :key="item.id"
                class="setting-item"
                :class="{ 'setting-item--modified': isFieldModified('profile', item.id) }"
              >
                <div class="setting-header">
                  <div class="setting-label-wrap">
                    <label :for="item.id" class="setting-label">{{ item.label }}</label>
                    <div v-if="item.description" class="help-tooltip-container">
                      <span class="help-tooltip">?</span>
                      <div class="help-tooltip-text">{{ item.description }}</div>
                    </div>
                  </div>
                </div>
                <div class="setting-control">
                  <input
                    :id="item.id"
                    class="setting-input"
                    :class="{ 'setting-input--error': validation.getError(item.id) }"
                    :type="item.type"
                    :value="String(item.current_value ?? '')"
                    :placeholder="item.placeholder || ''"
                    :disabled="data.isLoading.value"
                    @input="updateSetting('profile', item.id, ($event.target as HTMLInputElement).value)"
                  >
                  <p v-if="validation.getError(item.id)" class="field-error">{{ validation.getError(item.id) }}</p>
                </div>
              </div>
            </div>
          </article>

          <article class="settings-section">
            <h2 class="settings-title">{{ settingsState.alerts_retention.section_title }}</h2>
            <div class="settings-items">
              <div
                v-for="item in settingsState.alerts_retention.items"
                :key="item.id"
                class="setting-item"
                :class="{ 'setting-item--modified': isFieldModified('alerts_retention', item.id) }"
              >
                <div class="setting-header">
                  <div class="setting-label-wrap">
                    <label :for="item.id" class="setting-label">{{ item.label }}</label>
                    <div v-if="item.description" class="help-tooltip-container">
                      <span class="help-tooltip">?</span>
                      <div class="help-tooltip-text">{{ item.description }}</div>
                    </div>
                  </div>
                </div>
                <div class="setting-control">
                  <label v-if="item.type === 'toggle'" class="toggle-wrap" :class="{ 'toggle-wrap--error': validation.getError(item.id) }">
                    <input
                      :id="item.id"
                      type="checkbox"
                      :checked="Boolean(item.current_value)"
                      :disabled="data.isLoading.value"
                      @change="updateSetting('alerts_retention', item.id, ($event.target as HTMLInputElement).checked)"
                    >
                    <span>{{ Boolean(item.current_value) ? 'På' : 'Av' }}</span>
                  </label>
                  <input
                    v-else-if="item.type === 'number'"
                    :id="item.id"
                    class="setting-input"
                    :class="{ 'setting-input--error': isInvalidInput(item.id) || validation.getError(item.id) }"
                    type="number"
                    :value="item.current_value ?? ''"
                    :min="item.min"
                    :max="item.max"
                    :step="item.step"
                    :placeholder="item.placeholder || ''"
                    :disabled="data.isLoading.value"
                    @keydown="(e) => ['e','E','+','.'].includes(e.key) && e.preventDefault()"
                    @input="validateInputLive('alerts_retention', item.id, $event.target as HTMLInputElement, true)"
                    @blur="validateAndUpdateNumber('alerts_retention', item.id, ($event.target as HTMLInputElement), item.min, item.max)"
                  >
                  <input
                    v-else
                    :id="item.id"
                    class="setting-input"
                    :class="{ 'setting-input--error': validation.getError(item.id) }"
                    :type="item.type"
                    :value="String(item.current_value ?? '')"
                    :placeholder="item.placeholder || ''"
                    :disabled="data.isLoading.value"
                    @input="updateSetting('alerts_retention', item.id, ($event.target as HTMLInputElement).value)"
                  >
                  <p v-if="isInvalidInput(item.id)" class="field-error">Må være et heltall</p>
                  <p v-else-if="validation.getError(item.id)" class="field-error">{{ validation.getError(item.id) }}</p>
                </div>
              </div>
            </div>
          </article>
        </div>

        <!-- Right column: Organization + Modules + Temperature (smaller cards) -->
        <div class="settings-column">
          <article class="settings-section">
            <h2 class="settings-title">{{ settingsState.organization.section_title }}</h2>
            <div class="settings-items">
              <div
                v-for="item in settingsState.organization.items"
                :key="item.id"
                class="setting-item"
                :class="{ 'setting-item--modified': isFieldModified('organization', item.id) }"
              >
                <div class="setting-header">
                  <div class="setting-label-wrap">
                    <label :for="item.id" class="setting-label">{{ item.label }}</label>
                    <div v-if="item.description" class="help-tooltip-container">
                      <span class="help-tooltip">?</span>
                      <div class="help-tooltip-text">{{ item.description }}</div>
                    </div>
                  </div>
                </div>
                <div class="setting-control">
                  <select
                    v-if="item.type === 'select'"
                    :id="item.id"
                    class="setting-select"
                    :class="{ 'setting-select--error': validation.getError(item.id) }"
                    :value="String(item.current_value)"
                    :disabled="data.isLoading.value"
                    @change="updateSetting('organization', item.id, ($event.target as HTMLSelectElement).value)"
                  >
                    <option v-for="option in item.options" :key="option" :value="option">
                      {{ option }}
                    </option>
                  </select>
                  <input
                    v-else
                    :id="item.id"
                    class="setting-input"
                    :class="{ 'setting-input--error': validation.getError(item.id) }"
                    :type="item.type"
                    :value="String(item.current_value ?? '')"
                    :placeholder="item.placeholder || ''"
                    :disabled="data.isLoading.value"
                    @input="updateSetting('organization', item.id, ($event.target as HTMLInputElement).value)"
                  >
                  <p v-if="validation.getError(item.id)" class="field-error">{{ validation.getError(item.id) }}</p>
                </div>
              </div>
            </div>
          </article>

          <article class="settings-section">
            <h2 class="settings-title">{{ settingsState.modules.section_title }}</h2>
            <div class="settings-items">
              <div
                v-for="item in settingsState.modules.items"
                :key="item.id"
                class="setting-item"
                :class="{ 'setting-item--modified': isFieldModified('modules', item.id) }"
              >
                <div class="setting-header">
                  <div class="setting-label-wrap">
                    <label :for="item.id" class="setting-label">{{ item.label }}</label>
                    <div v-if="item.description" class="help-tooltip-container">
                      <span class="help-tooltip">?</span>
                      <div class="help-tooltip-text">{{ item.description }}</div>
                    </div>
                  </div>
                </div>
                <div class="setting-control">
                  <label class="toggle-wrap" :class="{ 'toggle-wrap--error': validation.getError(item.id) }">
                    <input
                      :id="item.id"
                      type="checkbox"
                      :checked="Boolean(item.current_value)"
                      :disabled="data.isLoading.value"
                      @change="updateSetting('modules', item.id, ($event.target as HTMLInputElement).checked)"
                    >
                    <span>{{ Boolean(item.current_value) ? 'På' : 'Av' }}</span>
                  </label>
                  <p v-if="validation.getError(item.id)" class="field-error">{{ validation.getError(item.id) }}</p>
                </div>
              </div>
            </div>
          </article>

          <article class="settings-section">
            <h2 class="settings-title">{{ settingsState.temperature.section_title }}</h2>
            <p v-if="tempRangeError" class="section-error">{{ tempRangeError }}</p>
            <div class="settings-items">
              <div
                v-for="item in settingsState.temperature.items"
                :key="item.id"
                class="setting-item"
                :class="{ 'setting-item--modified': isFieldModified('temperature', item.id) }"
              >
                <div class="setting-header">
                  <div class="setting-label-wrap">
                    <label :for="item.id" class="setting-label">{{ item.label }}</label>
                    <div v-if="item.description" class="help-tooltip-container">
                      <span class="help-tooltip">?</span>
                      <div class="help-tooltip-text">{{ item.description }}</div>
                    </div>
                  </div>
                </div>
                <div class="setting-control">
                  <input
                    :id="item.id"
                    class="setting-input"
                    :class="{ 'setting-input--error': isInvalidInput(item.id) || validation.getError(item.id) }"
                    type="number"
                    :value="item.current_value ?? ''"
                    :min="item.min"
                    :max="item.max"
                    :step="item.step"
                    :disabled="data.isLoading.value"
                    @keydown="(e) => ['e','E','+'].includes(e.key) && e.preventDefault()"
                    @input="validateInputLive('temperature', item.id, $event.target as HTMLInputElement)"
                    @blur="validateAndUpdateNumber('temperature', item.id, ($event.target as HTMLInputElement), item.min, item.max)"
                  >
                  <p v-if="isInvalidInput(item.id)" class="field-error">Må være et tall, f.eks. -18 eller 4</p>
                  <p v-else-if="validation.getError(item.id)" class="field-error">{{ validation.getError(item.id) }}</p>
                </div>
              </div>
            </div>
          </article>
        </div>
      </section>

      <AuditLogSection
        ref="auditLogSectionRef"
        :org-number="currentOrgNumber"
      />
    </template>

    <!-- Reset Confirmation Dialog -->
    <div v-if="showResetConfirm" class="modal-overlay" role="dialog" aria-modal="true" aria-labelledby="reset-title" @click.self="cancelReset">
      <div class="modal" role="document">
        <h3 id="reset-title">Bekreft tilbakestilling</h3>
        <p>Er du sikker på at du vil forkaste alle endringer og laste inn lagrede innstillinger på nytt?</p>
        <div class="modal-actions">
          <button class="btn btn--secondary" @click="cancelReset" aria-label="Avbryt tilbakestilling">Avbryt</button>
          <button class="btn btn--danger" @click="handleReset" aria-label="Bekreft tilbakestilling av endringer">Tilbakestill</button>
        </div>
      </div>
    </div>

    <!-- Reset to Defaults Confirmation Dialog -->
    <div v-if="showDefaultsConfirm" class="modal-overlay" role="dialog" aria-modal="true" aria-labelledby="defaults-title" @click.self="cancelResetToDefaults">
      <div class="modal" role="document">
        <h3 id="defaults-title">Bekreft tilbakestilling til standardverdier</h3>
        <p>Dette vil tilbakestille alle innstillinger til systemstandardverdier. Lagrede endringer vil ikke bli påvirket før du klikker "Lagre endringer".</p>
        <div class="modal-actions">
          <button class="btn btn--secondary" @click="cancelResetToDefaults" aria-label="Avbryt tilbakestilling til standardverdier">Avbryt</button>
          <button class="btn btn--danger" @click="handleResetToDefaults" aria-label="Bekreft tilbakestilling til standardverdier">Tilbakestill til standard</button>
        </div>
      </div>
    </div>

    <!-- Unsaved Changes Dialog -->
    <div v-if="showUnsavedDialog" class="modal-overlay" role="dialog" aria-modal="true" aria-labelledby="unsaved-title" @click.self="cancelUnsavedNavigation">
      <div class="modal" role="document">
        <h3 id="unsaved-title">Ulagrede endringer</h3>
        <p>Du har ulagrede endringer. Vil du forlate siden uten å lagre?</p>
        <div class="modal-actions">
          <button class="btn btn--secondary" @click="cancelUnsavedNavigation" aria-label="Bli på siden og fortsett redigering">Bli på siden</button>
          <button class="btn btn--danger" @click="confirmUnsavedNavigation" aria-label="Forlat siden uten å lagre endringer">Forlat uten å lagre</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Bruker delte klasser fra components.css:
   - .view-page (wrapper)
   - .page-header (header layout)
   - .btn, .btn--primary, .btn--secondary, .btn--danger (knapper)
   Se components.css for detaljer.
*/

.settings-view {
  /* Tilleggsspissifikk styling for innstillinger */
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

.meta-line {
  margin: 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
}

.header-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.settings-status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  background: var(--color-gray-50);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.status-dot {
  width: 0.5rem;
  height: 0.5rem;
  border-radius: 50%;
  background: var(--color-success);
}

.status-indicator--unsaved .status-dot {
  background: var(--color-warning);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.status-text {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--color-gray-700);
}

.status-indicator--unsaved .status-text {
  color: var(--color-warning);
  font-weight: 600;
}

.last-saved {
  margin: 0;
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
}

.settings-grid {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.settings-section {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.75rem;
}

/* Tablet: 2 independent columns */
@media (min-width: 768px) {
  .settings-grid {
    flex-direction: row;
    align-items: flex-start;
    gap: 0.75rem;
  }

  .settings-column {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    flex: 1;
    min-width: 0;
  }
}

.settings-section {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.75rem;
}

/* Tablet: 2 columns */
@media (min-width: 768px) {
  .settings-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    grid-auto-rows: min-content; /* Cards sized by content, no extra space */
    gap: 0.5rem;
    align-items: start; /* Each card independent at top */
  }
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
  align-items: start;
  padding: 0.6rem 0;
  border-bottom: 1px solid var(--color-border);
  transition: background-color 0.2s;
  min-height: 3.5rem;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-item--modified {
  position: relative;
}

.setting-item--modified::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(251, 191, 36, 0.12); /* Subtle yellow tint */
  pointer-events: none;
  z-index: 0;
}

.setting-item--modified > * {
  position: relative;
  z-index: 1;
}

.setting-header {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.setting-label-wrap {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.setting-label {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--color-foreground);
}

.help-tooltip-container {
  position: relative;
  display: inline-flex;
}

.help-tooltip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.1rem;
  height: 1.1rem;
  border-radius: 50%;
  background: var(--color-gray-200);
  color: var(--color-gray-600);
  font-size: 0.7rem;
  font-weight: 700;
  cursor: help;
}

.help-tooltip:hover {
  background: var(--color-gray-300);
}

.help-tooltip-text {
  position: absolute;
  bottom: 125%;
  left: 50%;
  transform: translateX(-50%);
  width: 220px;
  padding: 0.6rem 0.8rem;
  background: var(--color-gray-800);
  color: white;
  font-size: var(--font-size-xs);
  font-weight: 400;
  border-radius: var(--radius-md);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.2s, visibility 0.2s;
  z-index: 100;
  pointer-events: none;
  line-height: 1.4;
}

.help-tooltip-text::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border: 5px solid transparent;
  border-top-color: var(--color-gray-800);
}

.help-tooltip-container:hover .help-tooltip-text {
  opacity: 1;
  visibility: visible;
}

.setting-description {
  margin: 0;
  font-size: var(--font-size-xs);
  color: var(--color-gray-600);
}

.setting-control {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  position: relative;
  padding-bottom: 0.9rem; /* Space for error message */
}

.setting-select,
.setting-input {
  height: 2.75rem;
  min-width: 14rem;
  padding: 0 0.7rem;
  background: var(--color-gray-50);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  color: var(--color-foreground);
  box-sizing: border-box;
}

.toggle-wrap {
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  font-size: var(--font-size-sm);
  color: var(--color-gray-700);
  font-weight: 500;
  cursor: pointer;
  padding: 0.25rem 0;
}

.toggle-wrap input {
  width: 1.5rem;
  height: 1.5rem;
  cursor: pointer;
  accent-color: var(--color-foreground);
}

.field-error {
  position: absolute;
  right: 100%;
  top: 50%;
  transform: translateY(-50%);
  padding-right: 0.5rem;
  color: var(--color-danger);
  font-size: 0.7rem;
  white-space: nowrap;
  margin: 0;
}

.section-error {
  margin: 0 0 0.25rem;
  padding: 0;
  color: var(--color-danger);
  font-size: var(--font-size-xs);
  font-weight: 500;
  background: none;
  border: none;
  min-height: 1rem;
}

.number-input-wrap {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.setting-input--error {
  border-color: var(--color-danger) !important;
  background: var(--color-danger-bg) !important;
}

.setting-select--error {
  border-color: var(--color-danger) !important;
  background: var(--color-danger-bg) !important;
}

.toggle-wrap--error {
  color: var(--color-danger);
}

.input-error-msg {
  font-size: var(--font-size-xs);
  color: var(--color-danger);
  font-weight: 600;
}

.warning-message {
  padding: 0.75rem 0.95rem;
  border-radius: var(--radius-md);
  background: var(--color-warning-bg);
  color: var(--color-warning);
  border: 1px solid var(--color-warning);
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

/* Modal Styles */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal {
  background: var(--color-card);
  border-radius: var(--radius-md);
  padding: 1.5rem;
  max-width: 28rem;
  width: 100%;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.modal h3 {
  margin: 0 0 0.75rem;
  font-size: var(--font-size-lg);
}

.modal p {
  margin: 0 0 1.25rem;
  color: var(--color-gray-600);
  line-height: 1.5;
}

.modal-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
}

/* Button Styles - WCAG-forbedret med tydeligere kanter */
.btn {
  min-height: 2.7rem;
  padding: 0.45rem 0.95rem;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: 600;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.15s ease;
}

/* Lagre endringer - Primærknapp med forsterket synlighet */
.btn--primary {
  background: var(--color-foreground);
  color: var(--color-background);
  border-color: var(--color-foreground);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.btn--primary:hover {
  background: var(--color-gray-900);
  border-color: var(--color-gray-900);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
  transform: translateY(-1px);
}

.btn--primary:focus-visible {
  outline: 3px solid var(--color-focus);
  outline-offset: 2px;
}

/* Tilbake til standard / Tilbakestill endringer - Sekundærknapp med tydelig kant */
.btn--secondary {
  background: var(--color-card);
  color: var(--color-gray-700);
  border: 2px solid var(--color-border);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.btn--secondary:hover {
  background: var(--color-gray-50);
  border-color: var(--color-gray-400);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.btn--secondary:focus-visible {
  outline: 3px solid var(--color-focus);
  outline-offset: 2px;
  border-color: var(--color-gray-500);
}

.btn--danger {
  background: var(--color-danger);
  color: white;
  border-color: var(--color-danger);
}

.btn--danger:hover {
  background: var(--color-danger-hover, #dc2626);
  border-color: var(--color-danger-hover, #dc2626);
}

.btn--danger:focus-visible {
  outline: 3px solid var(--color-focus);
  outline-offset: 2px;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  border-color: var(--color-gray-300);
  transform: none;
  box-shadow: none;
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

/* Mobile-first: Default styles are for mobile */
.setting-item {
  grid-template-columns: 1fr;
  gap: 0.5rem;
}

.setting-input,
.setting-select {
  min-width: 0;
  width: 100%;
}

.modal-actions {
  flex-direction: column;
}

.modal-actions .btn {
  width: 100%;
}

/* Desktop: 768px+ */
@media (min-width: 768px) {
  .setting-item {
    grid-template-columns: 1fr auto;
    gap: 0.8rem;
  }

  .modal-actions {
    flex-direction: row;
  }

  .modal-actions .btn {
    width: auto;
  }
}
</style>
