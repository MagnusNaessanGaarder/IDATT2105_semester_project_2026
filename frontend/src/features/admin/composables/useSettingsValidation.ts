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

  return {
    validationErrors,
    validateSettings,
    clearError,
    getError,
    clearAllErrors,
  }
}

export { useSettingsValidation }
export default useSettingsValidation
