import type { Checklist, TemperatureRecord } from '../types'

export const formatDate = (value: string | null): string => {
  if (!value) {
    return '-'
  }

  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return value
  }

  return parsed.toLocaleDateString('nb-NO')
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
