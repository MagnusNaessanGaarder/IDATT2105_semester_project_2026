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
    // RFC 5322 compliant email regex
    const emailRegex = /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/
    return emailRegex.test(email)
  }

  const getEmailError = (email: string): string | null => {
    if (email.length === 0) {
      return null // Empty is allowed (optional fields)
    }

    // Check for common typos
    const commonTypos = [
      { pattern: /@gmil\.com$/i, correct: '@gmail.com' },
      { pattern: /@gmal\.com$/i, correct: '@gmail.com' },
      { pattern: /@gamil\.com$/i, correct: '@gmail.com' },
      { pattern: /@hotmial\.com$/i, correct: '@hotmail.com' },
      { pattern: /@hotmal\.com$/i, correct: '@hotmail.com' },
      { pattern: /@outlok\.com$/i, correct: '@outlook.com' },
      { pattern: /@outook\.com$/i, correct: '@outlook.com' },
      { pattern: /@yaho\.com$/i, correct: '@yahoo.com' },
    ]

    for (const typo of commonTypos) {
      if (typo.pattern.test(email)) {
        return `Mulig skrivefeil: Mente du ${typo.correct}?`
      }
    }

    // Check basic format
    if (!email.includes('@')) {
      return 'E-post må inneholde @'
    }

    const parts = email.split('@')
    if (parts.length !== 2) {
      return 'Ugyldig e-postformat'
    }

    const localPart = parts[0] ?? ''
    const domain = parts[1] ?? ''

    if (localPart.length === 0) {
      return 'Mangler navn før @'
    }

    if (domain.length === 0) {
      return 'Mangler domene etter @'
    }

    if (!domain.includes('.')) {
      return 'Ugyldig domene (mangler .com, .no, etc.)'
    }

    const domainParts = domain.split('.')
    const tld = domainParts[domainParts.length - 1]
    if (!tld || tld.length < 2) {
      return 'Ugyldig toppdomene (må være minst 2 tegn)'
    }

    // Full RFC validation
    if (!validateEmail(email)) {
      return 'Ugyldig e-postformat'
    }

    return null
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

  const setError = (itemId: string, message: string) => {
    validationErrors.value[itemId] = message
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
      case 'contact_phone':
        return validatePhone(value)
      case 'display_name':
        return validateRequiredText(itemId, value, 100)
      case 'legal_name':
        return validateRequiredText(itemId, value, 200)
      case 'default_temp_min_c':
      case 'default_temp_max_c':
        return validateTemperature(itemId, value, settingsState)
      case 'retention_user_months':
        return validateBoundedInteger(itemId, value, 1, 120, 'Må være mellom 1 og 120 måneder')
      case 'retention_audit_months':
        return validateBoundedInteger(itemId, value, 12, 120, 'Må være mellom 12 og 120 måneder (GDPR min. 5 år)')
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
    }

    const error = getEmailError(email)
    if (error) {
      validationErrors.value.notification_email = error
      return false
    }
    return true
  }

  const validateContactEmail = (value: unknown): boolean => {
    const email = String(value ?? '').trim()
    const error = getEmailError(email)
    if (error) {
      validationErrors.value.contact_email = error
      return false
    }
    return true
  }

  const validatePhone = (value: unknown): boolean => {
    const phone = String(value ?? '').trim()
    if (phone.length === 0) return true
    if (phone.length > 20) {
      validationErrors.value.contact_phone = 'Telefonnummer kan ikke være lengre enn 20 tegn.'
      return false
    }
    // Allow valid phone format (+ and numbers)
    if (!/^[+]?[\d\s-()]+$/.test(phone)) {
      validationErrors.value.contact_phone = 'Ugyldig telefonnummer. Bruk kun tall, +, og mellomrom.'
      return false
    }
    return true
  }

  const validateRequiredText = (itemId: string, value: unknown, maxLength = 255): boolean => {
    const text = String(value ?? '').trim()
    if (text.length === 0) {
      validationErrors.value[itemId] = 'Dette feltet er påkrevd.'
      return false
    }
    if (text.length > maxLength) {
      validationErrors.value[itemId] = `Maks ${maxLength} tegn.`
      return false
    }
    return true
  }

  const validateTemperature = (itemId: string, value: unknown, settingsState: SettingsState): boolean => {
    const num = toNumber(value)
    if (num !== null) {
      if (num < -25) {
        validationErrors.value[itemId] = 'Minimum er -25°C'
        return false
      }
      if (num > 25) {
        validationErrors.value[itemId] = 'Maksimum er 25°C'
        return false
      }
    }

    const minTemp = toNumber(
      settingsState.temperature.items.find((item) => item.id === 'default_temp_min_c')?.current_value
    )
    const maxTemp = toNumber(
      settingsState.temperature.items.find((item) => item.id === 'default_temp_max_c')?.current_value
    )

    if (minTemp !== null && maxTemp !== null && minTemp > maxTemp) {
      if (itemId === 'default_temp_min_c') {
        validationErrors.value.default_temp_min_c = 'Min må være lavere enn maks'
      } else {
        validationErrors.value.default_temp_max_c = 'Maks må være høyere enn min'
      }
      return false
    }
    return true
  }

  const validateBoundedInteger = (itemId: string, value: unknown, min: number, max: number, message: string): boolean => {
    const num = toNumber(value)
    if (num === null) return true
    if (num < min || num > max) {
      validationErrors.value[itemId] = message
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
