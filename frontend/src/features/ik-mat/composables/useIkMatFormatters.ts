import type { Checklist, TemperatureRecord } from '../types'
import { getOrgNumber } from '@/shared/utils/orgContext'
import { formatDateForOrganization } from '@/shared/utils/orgSettings'

export const formatDate = (value: string | null): string => {
  if (!value) {
    return '-'
  }

  return formatDateForOrganization(value, getOrgNumber(), {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

export const completionForChecklist = (checklist: Checklist): number => {
  if (checklist.items.length === 0) {
    return 0
  }

  const completed = checklist.items.filter((item) => item.completed).length
  return Math.round((completed / checklist.items.length) * 100)
}

export const isTemperatureInRange = (record: TemperatureRecord): boolean => {
  return record.temperature_c >= record.min_temp && record.temperature_c <= record.max_temp
}
