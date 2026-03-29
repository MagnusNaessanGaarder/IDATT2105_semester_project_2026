<script setup lang="ts">
import { computed } from 'vue'

interface ChecklistItem {
  id: number
  task: string
  required: boolean
  completed: boolean
  notes: string | null
}

interface Checklist {
  id: number
  name: string
  category: string
  frequency: string
  description: string
  created_date: string
  law_unit: string
  items: ChecklistItem[]
  completed_by: string | null
  completion_date: string | null
  completion_time: string | null
  status: 'completed' | 'pending' | 'overdue'
}

const props = defineProps<{
  checklist: Checklist
}>()

const emit = defineEmits<{
  view: []
  edit: []
}>()

const completedCount = computed(() => {
  return props.checklist.items.filter(item => item.completed).length
})

const totalCount = computed(() => {
  return props.checklist.items.length
})

const completionPercentage = computed(() => {
  if (totalCount.value === 0) return 0
  return Math.round((completedCount.value / totalCount.value) * 100)
})

const statusColor = computed(() => {
  if (props.checklist.status === 'completed') return 'success'
  if (props.checklist.status === 'pending') return 'warning'
  return 'danger'
})
</script>

<template>
  <div class="checklist-card" :class="`checklist-card--${statusColor}`">
    <div class="checklist-card__header">
      <div class="checklist-card__title-section">
        <h3 class="checklist-card__title">{{ checklist.name }}</h3>
        <div class="checklist-card__meta">
          <span class="checklist-card__badge">{{ checklist.category }}</span>
          <span class="checklist-card__frequency">{{ checklist.frequency }}</span>
        </div>
      </div>
      <div class="checklist-card__progress-container">
        <div class="checklist-card__progress-bar">
          <div 
            class="checklist-card__progress-fill" 
            :style="{ width: `${completionPercentage}%` }"
          />
        </div>
        <span class="checklist-card__progress-text">{{ completedCount }}/{{ totalCount }}</span>
      </div>
    </div>

    <div class="checklist-card__body">
      <p class="checklist-card__description">{{ checklist.description }}</p>
      
      <div class="checklist-card__items">
        <div 
          v-for="item in checklist.items" 
          :key="item.id"
          class="checklist-card__item"
          :class="{ 'checklist-card__item--completed': item.completed }"
        >
          <input 
            :id="`item-${checklist.id}-${item.id}`"
            type="checkbox" 
            :checked="item.completed"
            disabled
            class="checklist-card__checkbox"
          >
          <label :for="`item-${checklist.id}-${item.id}`" class="checklist-card__item-label">
            {{ item.task }}
          </label>
          <span v-if="item.notes" class="checklist-card__item-note">
            {{ item.notes }}
          </span>
        </div>
      </div>
    </div>

    <div class="checklist-card__footer">
      <div class="checklist-card__info">
        <span class="checklist-card__law-unit">{{ checklist.law_unit }}</span>
        <span v-if="checklist.completed_by" class="checklist-card__completed-by">
          Gjennomført av {{ checklist.completed_by }}
        </span>
      </div>
      <div class="checklist-card__actions">
        <button class="checklist-card__action-btn" @click="emit('view')">Se detaljer</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.checklist-card {
  background: var(--color-card);
  border: 0.0625rem solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: box-shadow var(--transition-base), border-color var(--transition-base);
}

.checklist-card:hover {
  box-shadow: var(--shadow-md);
  border-color: var(--color-border-focus);
}

.checklist-card--success {
  border-left: 0.25rem solid #10b981;
}

.checklist-card--warning {
  border-left: 0.25rem solid #f59e0b;
}

.checklist-card--danger {
  border-left: 0.25rem solid #ef4444;
}

.checklist-card__header {
  padding: 1.5rem;
  border-bottom: 0.0625rem solid var(--color-border);
}

.checklist-card__title-section {
  margin-bottom: 1rem;
}

.checklist-card__title {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-foreground);
}

.checklist-card__meta {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.checklist-card__badge {
  display: inline-block;
  background: var(--color-accent);
  color: var(--color-accent-foreground);
  padding: 0.25rem 0.75rem;
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.checklist-card__frequency {
  display: inline-block;
  color: var(--color-gray-600);
  font-size: var(--text-sm);
}

.checklist-card__progress-container {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.checklist-card__progress-bar {
  flex: 1;
  height: 0.5rem;
  background: var(--color-gray-200);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.checklist-card__progress-fill {
  height: 100%;
  background: #10b981;
  transition: width var(--transition-base);
}

.checklist-card__progress-text {
  min-width: 3rem;
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-gray-600);
  text-align: right;
}

.checklist-card__body {
  padding: 1.5rem;
}

.checklist-card__description {
  margin: 0 0 1rem;
  font-size: var(--text-sm);
  color: var(--color-gray-600);
}

.checklist-card__items {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.checklist-card__item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.5rem;
  border-radius: var(--radius-sm);
  transition: background-color var(--transition-fast);
}

.checklist-card__item:hover {
  background: var(--color-accent);
}

.checklist-card__item--completed {
  opacity: 0.6;
}

.checklist-card__checkbox {
  margin-top: 0.25rem;
  cursor: pointer;
}

.checklist-card__item-label {
  flex: 1;
  font-size: var(--text-sm);
  color: var(--color-foreground);
  cursor: pointer;
}

.checklist-card__item--completed .checklist-card__item-label {
  text-decoration: line-through;
  color: var(--color-gray-500);
}

.checklist-card__item-note {
  font-size: var(--text-xs);
  color: var(--color-gray-500);
  font-style: italic;
}

.checklist-card__footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.5rem;
  background: var(--color-accent);
  border-top: 0.0625rem solid var(--color-border);
}

.checklist-card__info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.checklist-card__law-unit {
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--color-gray-600);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.checklist-card__completed-by {
  font-size: var(--text-xs);
  color: var(--color-gray-600);
}

.checklist-card__actions {
  display: flex;
  gap: 0.5rem;
}

.checklist-card__action-btn {
  padding: 0.5rem 1rem;
  background: var(--color-foreground);
  color: var(--color-background);
  border: none;
  border-radius: var(--radius-sm);
  font-size: var(--text-sm);
  font-weight: 600;
  cursor: pointer;
  transition: background-color var(--transition-fast);
}

.checklist-card__action-btn:hover {
  background: var(--color-gray-900);
}

@media (max-width: 48rem) {
  .checklist-card__header {
    padding: 1rem;
  }

  .checklist-card__progress-container {
    flex-direction: column;
    align-items: flex-start;
  }

  .checklist-card__progress-text {
    min-width: auto;
    text-align: left;
  }

  .checklist-card__footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
