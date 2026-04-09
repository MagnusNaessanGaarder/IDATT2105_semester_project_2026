import { computed, type Ref } from 'vue'
import type { Deviation } from '../types/index'

export const useDeviationCardState = (deviation: Ref<Deviation>) => {
  const severityLabel = computed(() => {
    const { severity } = deviation.value
    if (severity === 'high') return 'Høy'
    if (severity === 'medium') return 'Medium'
    return 'Lav'
  })

  const statusLabel = computed(() => {
    const { status } = deviation.value
    if (status === 'resolved') return 'Løst'
    if (status === 'in-progress') return 'Under behandling'
    return 'Åpent'
  })

  const statusIcon = computed(() => {
    const { status } = deviation.value
    if (status === 'resolved') return '✓'
    if (status === 'in-progress') return '⟳'
    return '!'
  })

  return {
    severityLabel,
    statusLabel,
    statusIcon,
  }
}
