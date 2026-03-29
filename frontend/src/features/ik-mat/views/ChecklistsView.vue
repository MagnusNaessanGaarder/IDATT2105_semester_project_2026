<script setup lang="ts">
import { computed, ref } from 'vue'
import { useIkMatData, type Checklist } from '@/features/ik-mat/composables/useIkMatData'

const { checklists, completionForChecklist } = useIkMatData()

const selectedFrequency = ref('Alle')
const expandedId = ref<number | null>(null)

const checklistState = ref<Checklist[]>(checklists.map((item) => ({ ...item, items: item.items.map((task) => ({ ...task })) })))

const frequencies = computed(() => {
  const fromData = Array.from(new Set(checklistState.value.map((item) => item.frequency)))
  return ['Alle', ...fromData]
})

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
</script>

<template>
  <div class="checklists-page">
    <header class="page-header">
      <h1>Sjekklister</h1>
      <p class="subtitle">Operative kontrollpunkter for daglig og periodisk oppfølging</p>
    </header>

    <div class="filter-row" role="tablist" aria-label="Filtrer etter frekvens">
      <button
        v-for="frequency in frequencies"
        :key="frequency"
        class="filter-chip"
        :class="{ 'filter-chip--active': selectedFrequency === frequency }"
        @click="selectedFrequency = frequency"
      >
        {{ frequency }}
      </button>
    </div>

    <div class="checklist-list">
      <article v-for="checklist in sorted" :key="checklist.id" class="checklist-card">
        <button class="checklist-head" :aria-expanded="expandedId === checklist.id" @click="toggleExpanded(checklist.id)">
          <div>
            <p class="checklist-head__title">{{ checklist.name }}</p>
            <p class="checklist-head__meta">Sjekkliste {{ checklist.id }} · {{ checklist.frequency }} · {{ checklist.law_unit }}</p>
          </div>

          <div class="checklist-head__progress">
            <div class="progress-track" role="progressbar" :aria-valuenow="completionForChecklist(checklist)" aria-valuemin="0" aria-valuemax="100">
              <div class="progress-track__fill" :style="{ width: `${completionForChecklist(checklist)}%` }" />
            </div>
            <span>{{ completionForChecklist(checklist) }}%</span>
          </div>
        </button>

        <div v-if="expandedId === checklist.id" class="checklist-body">
          <p class="checklist-body__description">{{ checklist.description }}</p>

          <ul class="task-list">
            <li v-for="task in checklist.items" :key="task.id" class="task-row">
              <label>
                <input type="checkbox" :checked="task.completed" @change="toggleTask(checklist.id, task.id)" />
                <span :class="{ 'task-done': task.completed }">{{ task.task }}</span>
              </label>
              <small v-if="task.notes">{{ task.notes }}</small>
            </li>
          </ul>
        </div>
      </article>
    </div>

    <div v-if="sorted.length === 0" class="empty-state">Ingen sjekklister matcher valgt filter.</div>
  </div>
</template>

<style scoped>
.checklists-page {
  max-width: 75rem;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 1.25rem;
}

.page-header h1 {
  margin: 0;
  font-size: var(--font-size-2xl);
  color: var(--ik-mat-primary);
}

.subtitle {
  margin: 0.35rem 0 0;
  color: var(--color-gray-500);
  font-size: var(--font-size-sm);
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.filter-chip {
  border: 0.0625rem solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.4rem 0.8rem;
  background: var(--color-card);
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
}

.filter-chip--active {
  border-color: var(--ik-mat-primary);
  background: var(--ik-mat-primary);
  color: var(--color-primary-foreground);
}

.checklist-list {
  display: grid;
  gap: 0.75rem;
}

.checklist-card {
  border: 0.0625rem solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  overflow: hidden;
}

.checklist-head {
  border: 0;
  background: transparent;
  width: 100%;
  padding: 0.9rem;
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: center;
  text-align: left;
}

.checklist-head__title {
  margin: 0;
  color: var(--color-foreground);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
}

.checklist-head__meta {
  margin: 0.25rem 0 0;
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
}

.checklist-head__progress {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 8rem;
}

.checklist-head__progress span {
  font-size: var(--font-size-xs);
  color: var(--color-gray-600);
  font-weight: var(--font-weight-semibold);
  min-width: 2.2rem;
  text-align: right;
}

.progress-track {
  height: 0.4rem;
  flex: 1;
  background: var(--color-gray-200);
  border-radius: 62.4375rem;
  overflow: hidden;
}

.progress-track__fill {
  height: 100%;
  background: var(--ik-mat-primary);
  transition: width var(--transition-base);
}

.checklist-body {
  border-top: 0.0625rem solid var(--color-border);
  padding: 0.85rem 0.9rem;
  background: color-mix(in srgb, var(--ik-mat-bg) 40%, var(--color-card));
}

.checklist-body__description {
  margin: 0 0 0.7rem;
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
}

.task-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.45rem;
}

.task-row {
  border: 0.0625rem solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-card);
  padding: 0.55rem;
}

.task-row label {
  display: flex;
  gap: 0.55rem;
  align-items: center;
  cursor: pointer;
}

.task-row input {
  width: 1rem;
  height: 1rem;
}

.task-row span {
  font-size: var(--font-size-sm);
  color: var(--color-foreground);
}

.task-row small {
  display: block;
  margin-top: 0.35rem;
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
}

.task-done {
  color: var(--color-gray-500);
  text-decoration: line-through;
}

.empty-state {
  margin-top: 0.9rem;
  border: 0.0625rem solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  color: var(--color-gray-600);
  text-align: center;
  padding: 1.2rem;
}
</style>
