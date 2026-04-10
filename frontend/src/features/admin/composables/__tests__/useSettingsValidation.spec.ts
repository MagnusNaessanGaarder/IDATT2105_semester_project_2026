import { beforeEach, describe, expect, it } from 'vitest'
import { useSettingsValidation } from '../useSettingsValidation'
import type { SettingsState } from '../useAdminData'

const createMockSettings = (): SettingsState => ({
  profile: {
    id: 'profile',
    section_title: 'Organisasjonsprofil',
    items: [
      { id: 'display_name', label: 'Visningsnavn', type: 'text', current_value: 'Test Org' },
      { id: 'legal_name', label: 'Juridisk navn', type: 'text', current_value: 'Test Org AS' },
      { id: 'contact_email', label: 'Kontakt e-post', type: 'email', current_value: 'test@example.com' },
      { id: 'contact_phone', label: 'Kontakt telefon', type: 'tel', current_value: '+47 123 45 678' },
    ],
  },
  organization: {
    id: 'organization',
    section_title: 'Organisasjon',
    items: [
      { id: 'locale_code', label: 'Språk', type: 'select', current_value: 'nb-NO', options: ['nb-NO', 'en-US'] },
      { id: 'timezone_name', label: 'Tidssone', type: 'select', current_value: 'Europe/Oslo', options: ['Europe/Oslo', 'UTC'] },
    ],
  },
  modules: {
    id: 'modules',
    section_title: 'Moduler',
    items: [
      { id: 'enable_food_module', label: 'IK-Mat', type: 'toggle', current_value: true },
      { id: 'enable_alcohol_module', label: 'IK-Alkohol', type: 'toggle', current_value: true },
    ],
  },
  temperature: {
    id: 'temperature',
    section_title: 'Temperatur',
    items: [
      { id: 'default_temp_min_c', label: 'Min', type: 'select', current_value: '2', options: ['0', '2', '4'] },
      { id: 'default_temp_max_c', label: 'Maks', type: 'select', current_value: '8', options: ['5', '8', '10'] },
    ],
  },
  alerts_retention: {
    id: 'alerts_retention',
    section_title: 'Varsling',
    items: [
      { id: 'reminder_email_enabled', label: 'E-post', type: 'toggle', current_value: false },
      { id: 'notification_email', label: 'E-postadresse', type: 'email', current_value: '' },
      { id: 'reminder_recipient_scope', label: 'Mottakere', type: 'select', current_value: 'ADMIN_MANAGER', options: ['ADMIN_MANAGER', 'ADMIN_MANAGER_AND_ASSIGNED'] },
      { id: 'reminder_lead_hours', label: 'Lead', type: 'number', current_value: 0 },
      { id: 'reminder_repeat_hours', label: 'Repeat', type: 'number', current_value: 24 },
      { id: 'retention_user_months', label: 'Brukerdata', type: 'select', current_value: '12', options: ['6', '12', '24'] },
      { id: 'retention_audit_months', label: 'Revisjonsdata', type: 'select', current_value: '36', options: ['12', '36', '60'] },
    ],
  },
})

describe('useSettingsValidation', () => {
  beforeEach(() => {
    const validation = useSettingsValidation()
    validation.clearAllErrors()
  })

  it('validates email when reminders are enabled', () => {
    const validation = useSettingsValidation()
    const settings = createMockSettings()

    settings.alerts_retention.items.find((i) => i.id === 'reminder_email_enabled')!.current_value = true
    settings.alerts_retention.items.find((i) => i.id === 'notification_email')!.current_value = ''

    const isValid = validation.validateSettings(settings)

    expect(isValid).toBe(false)
    expect(validation.getError('notification_email')).toBe('E-post er påkrevd når e-postpåminnelser er aktivert.')
  })

  it('validates email format', () => {
    const validation = useSettingsValidation()
    const settings = createMockSettings()

    settings.alerts_retention.items.find((i) => i.id === 'notification_email')!.current_value = 'invalid-email'

    const isValid = validation.validateSettings(settings)

    expect(isValid).toBe(false)
    expect(validation.getError('notification_email')).toBe('Ugyldig e-postformat.')
  })

  it('accepts valid email', () => {
    const validation = useSettingsValidation()
    const settings = createMockSettings()

    settings.alerts_retention.items.find((i) => i.id === 'reminder_email_enabled')!.current_value = true
    settings.alerts_retention.items.find((i) => i.id === 'notification_email')!.current_value = 'test@example.com'

    const isValid = validation.validateSettings(settings)

    expect(isValid).toBe(true)
    expect(validation.getError('notification_email')).toBeNull()
  })

  it('validates temperature range', () => {
    const validation = useSettingsValidation()
    const settings = createMockSettings()

    settings.temperature.items.find((i) => i.id === 'default_temp_min_c')!.current_value = '10'
    settings.temperature.items.find((i) => i.id === 'default_temp_max_c')!.current_value = '5'

    const isValid = validation.validateSettings(settings)

    expect(isValid).toBe(false)
    expect(validation.getError('default_temp_min_c')).toBe('Min temperatur må være mindre enn eller lik maks temperatur.')
    expect(validation.getError('default_temp_max_c')).toBe('Maks temperatur må være større enn eller lik min temperatur.')
  })

  it('accepts valid temperature range', () => {
    const validation = useSettingsValidation()
    const settings = createMockSettings()

    settings.temperature.items.find((i) => i.id === 'default_temp_min_c')!.current_value = '2'
    settings.temperature.items.find((i) => i.id === 'default_temp_max_c')!.current_value = '8'

    const isValid = validation.validateSettings(settings)

    expect(isValid).toBe(true)
    expect(validation.getError('default_temp_min_c')).toBeNull()
    expect(validation.getError('default_temp_max_c')).toBeNull()
  })

  it('validates at least one module is enabled', () => {
    const validation = useSettingsValidation()
    const settings = createMockSettings()

    settings.modules.items.find((i) => i.id === 'enable_food_module')!.current_value = false
    settings.modules.items.find((i) => i.id === 'enable_alcohol_module')!.current_value = false

    const isValid = validation.validateSettings(settings)

    expect(isValid).toBe(false)
    expect(validation.getError('enable_alcohol_module')).toBe('Minst én modul må være aktivert.')
  })

  it('clears specific error', () => {
    const validation = useSettingsValidation()
    const settings = createMockSettings()

    settings.modules.items.find((i) => i.id === 'enable_food_module')!.current_value = false
    settings.modules.items.find((i) => i.id === 'enable_alcohol_module')!.current_value = false

    validation.validateSettings(settings)
    expect(validation.getError('enable_alcohol_module')).not.toBeNull()

    validation.clearError('enable_alcohol_module')
    expect(validation.getError('enable_alcohol_module')).toBeNull()
  })

  it('clears all errors', () => {
    const validation = useSettingsValidation()
    const settings = createMockSettings()

    settings.modules.items.find((i) => i.id === 'enable_food_module')!.current_value = false
    settings.modules.items.find((i) => i.id === 'enable_alcohol_module')!.current_value = false
    settings.temperature.items.find((i) => i.id === 'default_temp_min_c')!.current_value = '10'
    settings.temperature.items.find((i) => i.id === 'default_temp_max_c')!.current_value = '5'

    validation.validateSettings(settings)
    expect(Object.keys(validation.validationErrors.value).length).toBeGreaterThan(0)

    validation.clearAllErrors()
    expect(Object.keys(validation.validationErrors.value)).toHaveLength(0)
  })
})
