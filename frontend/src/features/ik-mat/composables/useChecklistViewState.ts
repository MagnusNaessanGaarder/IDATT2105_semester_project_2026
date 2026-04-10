import { computed, ref, watchEffect } from 'vue'
import type { Checklist } from '../types'

export const useChecklistViewState = (
  checklistsSource: Checklist[],
  completionForChecklist: (checklist: Checklist) => number,
) => {
  const selectedFrequency = ref<'Alle' | 'Daglig' | 'Ukentlig' | 'Månedlig'>('Alle')
  const expandedId = ref<number | null>(null)
  const checklistState = ref<Checklist[]>([])

  const cloneChecklist = (item: Checklist): Checklist => ({
    ...item,
    items: item.items.map((task) => ({ ...task })),
  })

  watchEffect(() => {
    const currentById = new Map(checklistsSource.map((item) => [item.id, item]))

    checklistState.value = checklistsSource.map((item) => {
      const existing = currentById.get(item.id)
      return existing ? cloneChecklist(existing) : cloneChecklist(item)
    })

    if (!checklistState.value.some((item) => item.id === expandedId.value)) {
      expandedId.value = null
    }
  })

  const frequencies = computed(() => ['Alle', 'Daglig', 'Ukentlig', 'Månedlig'] as const)

  const filtered = computed(() => {
    if (selectedFrequency.value === 'Alle') {
      return checklistState.value
    }

    return checklistState.value.filter((item) => item.frequency === selectedFrequency.value)
  })

  const sorted = computed(() => {
    return [...filtered.value].sort((a, b) => completionForChecklist(a) - completionForChecklist(b))
  })

  const toggleExpanded = (id: number) => {
    expandedId.value = expandedId.value === id ? null : id
  }

  const toggleTask = (checklistId: number, itemId: number) => {
    checklistState.value = checklistState.value.map((checklist) => {
      if (checklist.id !== checklistId) {
        return checklist
      }

      const updatedItems = checklist.items.map((task) => {
        if (task.id !== itemId) {
          return task
        }

        return {
          ...task,
          completed: !task.completed,
        }
      })

      return {
        ...checklist,
        items: updatedItems,
        status: updatedItems.every((task) => task.completed) ? 'completed' : 'pending',
      }
    })
  }

  return {
    selectedFrequency,
    expandedId,
    frequencies,
    sorted,
    toggleExpanded,
    toggleTask,
  }
}
