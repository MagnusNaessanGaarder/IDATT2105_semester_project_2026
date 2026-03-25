/**
 * Constants - Applikasjonskonstanter
 * 
 * Sentrale konstanter som brukes på tvers av applikasjonen.
 * Bruk disse istedenfor hardkodede strenger for bedre type-sikkerhet.
 * 
 * Inneholder:
 * - ROLES: Brukerroller (ADMIN, MANAGER, EMPLOYEE)
 * - CHECKLIST_STATUS: Sjekklistestatuser
 * - DEVIATION_STATUS: Avviksstatuser
 * - TEMPERATURE_LIMITS: Godkjente temperaturområder
 */
export const ROLES = {
  ADMIN: 'ADMIN',
  MANAGER: 'MANAGER',
  EMPLOYEE: 'EMPLOYEE',
} as const

export const CHECKLIST_STATUS = {
  PENDING: 'PENDING',
  COMPLETED: 'COMPLETED',
  OVERDUE: 'OVERDUE',
} as const

export const DEVIATION_STATUS = {
  OPEN: 'OPEN',
  RESOLVED: 'RESOLVED',
  IGNORED: 'IGNORED',
} as const

export const TEMPERATURE_LIMITS = {
  FISH_COOLER: { min: -2, max: 2, label: 'Fiskekjøl' },
  MEAT_COOLER: { min: 0, max: 4, label: 'Kjøttkjøl' },
  DAIRY_COOLER: { min: 0, max: 4, label: 'Meieriprodukter' },
  VEGETABLE_COOLER: { min: 2, max: 8, label: 'Grønnsaker' },
  FREEZER: { min: -25, max: -18, label: 'Fryser' },
} as const