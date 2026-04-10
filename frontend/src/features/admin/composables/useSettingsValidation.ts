import { ref } from 'vue'
import type { SettingsState } from './useAdminData'

export interface ValidationErrors {
  [key: string]: string
}

const useSettingsValidation = () => {
  const validationErrors = ref<ValidationErrors>({})

  const toNumber = (value: unknown): number | null => {
    if (value === null || value === undefined || value === '') {
      return null
    }
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : null
  }

  const validateEmail = (email: string): boolean => {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
  }

  const validateSettings = (settingsState: SettingsState): boolean => {
    const errors: ValidationErrors = {}

    // Email validation
    const reminderEnabled = settingsState.alerts_retention.items.find(
      (item) => item.id === 'reminder_email_enabled'
    )?.current_value === true
    const notificationEmail = String(
      settingsState.alerts_retention.items.find((item) => item.id === 'notification_email')?.current_value ?? ''
    ).trim()

    if (reminderEnabled && notificationEmail.length === 0) {
      errors.notification_email = 'E-post er påkrevd når e-postpåminnelser er aktivert.'
    } else if (notificationEmail.length > 0 && !validateEmail(notificationEmail)) {
      errors.notification_email = 'Ugyldig e-postformat.'
    }

    // Temperature validation
    const minTemp = toNumber(
      settingsState.temperature.items.find((item) => item.id === 'default_temp_min_c')?.current_value
    )
    const maxTemp = toNumber(
      settingsState.temperature.items.find((item) => item.id === 'default_temp_max_c')?.current_value
    )
    if (minTemp !== null && maxTemp !== null && minTemp > maxTemp) {
      errors.default_temp_min_c = 'Min temperatur må være mindre enn eller lik maks temperatur.'
      errors.default_temp_max_c = 'Maks temperatur må være større enn eller lik min temperatur.'
    }

    // Module validation
    const foodEnabled = settingsState.modules.items.find((item) => item.id === 'enable_food_module')?.current_value === true
    const alcoholEnabled = settingsState.modules.items.find((item) => item.id === 'enable_alcohol_module')?.current_value === true
    if (!foodEnabled && !alcoholEnabled) {
      errors.enable_alcohol_module = 'Minst én modul må være aktivert.'
    }

    validationErrors.value = errors
    return Object.keys(errors).length === 0
  }

  const clearError = (itemId: string) => {
    if (validationErrors.value[itemId]) {
      delete validationErrors.value[itemId]
    }
  }

  const getError = (itemId: string): string | null => {
    return validationErrors.value[itemId] ?? null
  }

  const clearAllErrors = () => {
    validationErrors.value = {}
  }

  // Validate a single field in real-time
  const validateField = (itemId: string, value: unknown, settingsState: SettingsState): boolean => {
    // Clear previous error for this field
    clearError(itemId)

    switch (itemId) {
      case 'notification_email':
        return validateNotificationEmail(value, settingsState)
      case 'contact_email':
        return validateContactEmail(value)
      case 'default_temp_min_c':
      case 'default_temp_max_c':
        return validateTemperature(itemId, value, settingsState)
      case 'enable_food_module':
      case 'enable_alcohol_module':
        return validateModules(settingsState)
      default:
        return true
    }
  }

  const validateNotificationEmail = (value: unknown, settingsState: SettingsState): boolean => {
    const reminderEnabled = settingsState.alerts_retention.items.find(
      (item) => item.id === 'reminder_email_enabled'
    )?.current_value === true
    const email = String(value ?? '').trim()

    if (reminderEnabled && email.length === 0) {
      validationErrors.value.notification_email = 'E-post er påkrevd når e-postpåminnelser er aktivert.'
      return false
    } else if (email.length > 0 && !validateEmail(email)) {
      validationErrors.value.notification_email = 'Ugyldig e-postformat.'
      return false
    }
    return true
  }

  const validateContactEmail = (value: unknown): boolean => {
    const email = String(value ?? '').trim()
    if (email.length > 0 && !validateEmail(email)) {
      validationErrors.value.contact_email = 'Ugyldig e-postformat.'
      return false
    }
    return true
  }

  const validateTemperature = (itemId: string, value: unknown, settingsState: SettingsState): boolean => {
    const minTemp = toNumber(
      settingsState.temperature.items.find((item) => item.id === 'default_temp_min_c')?.current_value
    )
    const maxTemp = toNumber(
      settingsState.temperature.items.find((item) => item.id === 'default_temp_max_c')?.current_value
    )

    if (minTemp !== null && maxTemp !== null && minTemp > maxTemp) {
      if (itemId === 'default_temp_min_c') {
        validationErrors.value.default_temp_min_c = 'Min temperatur må være mindre enn eller lik maks temperatur.'
      } else {
        validationErrors.value.default_temp_max_c = 'Maks temperatur må være større enn eller lik min temperatur.'
      }
      return false
    }
    return true
  }

  const validateModules = (settingsState: SettingsState): boolean => {
    const foodEnabled = settingsState.modules.items.find((item) => item.id === 'enable_food_module')?.current_value === true
    const alcoholEnabled = settingsState.modules.items.find((item) => item.id === 'enable_alcohol_module')?.current_value === true

    if (!foodEnabled && !alcoholEnabled) {
      validationErrors.value.enable_alcohol_module = 'Minst én modul må være aktivert.'
      return false
    }
    return true
  }

  return {
    validationErrors,
    validateSettings,
    validateField,
    clearError,
    getError,
    clearAllErrors,
  }
}

export { useSettingsValidation }
export default useSettingsValidation
