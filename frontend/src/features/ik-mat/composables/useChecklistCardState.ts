import { computed, type Ref } from 'vue'
import type { Checklist } from '../types/index'

export const useChecklistCardState = (checklist: Ref<Checklist>) => {
  const completedCount = computed(() => {
    return checklist.value.items.filter((item) => item.completed).length
  })

  const totalCount = computed(() => {
    return checklist.value.items.length
  })

  const completionPercentage = computed(() => {
    if (totalCount.value === 0) return 0
    return Math.round((completedCount.value / totalCount.value) * 100)
  })

  const statusColor = computed(() => {
    if (checklist.value.status === 'completed') return 'success'
    if (checklist.value.status === 'pending') return 'warning'
    return 'danger'
  })

  return {
    completedCount,
    totalCount,
    completionPercentage,
    statusColor,
  }
}
