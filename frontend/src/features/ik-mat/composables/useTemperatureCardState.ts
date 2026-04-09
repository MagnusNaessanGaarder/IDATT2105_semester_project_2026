import { computed, type Ref } from 'vue'
import type { TemperatureRecord } from '../types/index'

export const useTemperatureCardState = (record: Ref<TemperatureRecord>) => {
  const statusLabel = computed(() => {
    const { status } = record.value
    if (status === 'critical') return 'Kritisk'
    if (status === 'warning') return 'Advarsel'
    return 'OK'
  })

  const statusIcon = computed(() => {
    const { status } = record.value
    if (status === 'critical') return '⚠️'
    if (status === 'warning') return '⚡'
    return '✓'
  })

  const isWithinRange = computed(() => {
    const { temperature_c, min_temp, max_temp } = record.value
    return temperature_c >= min_temp && temperature_c <= max_temp
  })

  return {
    statusLabel,
    statusIcon,
    isWithinRange,
  }
}
