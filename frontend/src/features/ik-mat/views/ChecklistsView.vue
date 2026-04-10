<script setup lang="ts">
import { computed, ref } from 'vue'
import { useIkMatData, type Checklist } from '../composables/useIkMatData'
import { useAuthStore } from '@/stores/auth'
import BaseModal from '@/shared/components/BaseModal.vue'

const authStore = useAuthStore()
const { checklists, completionForChecklist } = useIkMatData()

const isAdmin = computed(() => authStore.isAdmin)

const selectedFrequency = ref<'Alle' | 'Daglig' | 'Ukentlig' | 'Månedlig'>('Alle')
const expandedId = ref<number | null>(null)
const optionsOpenId = ref<number | null>(null)
const showEditModal = ref(false)
const selectedChecklist = ref<Checklist | null>(null)

const checklistState = ref<Checklist[]>(checklists.map((item) => ({ ...item, items: item.items.map((task) => ({ ...task })) })))

const formData = ref({
  name: '',
  description: '',
  frequency: 'Daglig' as 'Daglig' | 'Ukentlig' | 'Månedlig',
  law_unit: '',
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

const completionTone = (percentage: number): 'level-1' | 'level-2' | 'level-3' | 'level-4' | 'level-5' => {
  if (percentage >= 100) return 'level-5'
  if (percentage >= 75) return 'level-4'
  if (percentage >= 50) return 'level-3'
  if (percentage >= 25) return 'level-2'
  return 'level-1'
}

const toggleExpanded = (id: number) => {
  expandedId.value = expandedId.value === id ? null : id
  optionsOpenId.value = null
}

const toggleOptions = (id: number, event: Event) => {
  event.stopPropagation()
  optionsOpenId.value = optionsOpenId.value === id ? null : id
}

const openEditModal = (checklist: Checklist, event: Event) => {
  event.stopPropagation()
  optionsOpenId.value = null
  selectedChecklist.value = checklist
  formData.value = {
    name: checklist.name,
    description: checklist.description,
    frequency: checklist.frequency as 'Daglig' | 'Ukentlig' | 'Månedlig',
    law_unit: checklist.law_unit,
  }
  showEditModal.value = true
}

const closeEditModal = () => {
  showEditModal.value = false
  selectedChecklist.value = null
}

const handleUpdateChecklist = async () => {
  if (!selectedChecklist.value) return

  checklistState.value = checklistState.value.map((checklist) => {
    if (checklist.id !== selectedChecklist.value?.id) {
      return checklist
    }
    return {
      ...checklist,
      name: formData.value.name,
      description: formData.value.description,
      frequency: formData.value.frequency,
      law_unit: formData.value.law_unit,
    }
  })

  closeEditModal()
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
      <p class="subtitle">Operative kontrollpunkter sortert etter lavest progresjon</p>
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
        <!-- Header: Split into expandable area and actions to avoid nested buttons -->
        <div class="checklist-header">
          <div
            class="checklist-header__content"
            role="button"
            :aria-expanded="expandedId === checklist.id"
            tabindex="0"
            @click="toggleExpanded(checklist.id)"
            @keydown.enter="toggleExpanded(checklist.id)"
          >
            <div>
              <p class="checklist-header__title">{{ checklist.name }}</p>
              <p class="checklist-header__meta">{{ checklist.frequency }} · {{ checklist.law_unit }}</p>
            </div>

            <div class="checklist-header__progress" :class="`checklist-header__progress--${completionTone(completionForChecklist(checklist))}`">
              <div class="progress-track" role="progressbar" :aria-valuenow="completionForChecklist(checklist)" aria-valuemin="0" aria-valuemax="100">
                <div class="progress-track__fill" :style="{ width: `${completionForChecklist(checklist)}%` }" />
              </div>
              <span class="checklist-header__status-tag">{{ completionForChecklist(checklist) }}%</span>
            </div>
          </div>

          <!-- Admin options menu - separate from expandable header -->
          <div v-if="isAdmin" class="options-menu">
            <button
              class="options-menu__trigger"
              type="button"
              aria-label="Åpne handlinger"
              :aria-expanded="optionsOpenId === checklist.id"
              @click="toggleOptions(checklist.id, $event)"
            >
              <span class="dot" />
              <span class="dot" />
              <span class="dot" />
            </button>

            <div v-if="optionsOpenId === checklist.id" class="options-menu__list" role="menu">
              <button class="options-menu__item" type="button" role="menuitem" @click="openEditModal(checklist, $event)">Rediger</button>
            </div>
          </div>
        </div>

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

    <!-- Edit Checklist Modal -->
    <BaseModal :open="showEditModal" title="Rediger sjekkliste" @close="closeEditModal">
      <form id="editChecklistForm" class="checklist-form" @submit.prevent="handleUpdateChecklist">
        <div class="form-group">
          <label for="checklistName">Navn</label>
          <input id="checklistName" v-model="formData.name" type="text" required />
        </div>
        <div class="form-group">
          <label for="checklistDescription">Beskrivelse</label>
          <textarea id="checklistDescription" v-model="formData.description" rows="3" />
        </div>
        <div class="form-group">
          <label for="checklistFrequency">Frekvens</label>
          <select id="checklistFrequency" v-model="formData.frequency" required>
            <option value="Daglig">Daglig</option>
            <option value="Ukentlig">Ukentlig</option>
            <option value="Månedlig">Månedlig</option>
          </select>
        </div>
        <div class="form-group">
          <label for="checklistLawUnit">Lovhenvisning</label>
          <input id="checklistLawUnit" v-model="formData.law_unit" type="text" required />
        </div>
      </form>
      <template #footer>
        <button type="button" class="action-btn action-btn--ghost" @click="closeEditModal">Avbryt</button>
        <button type="submit" form="editChecklistForm" class="action-btn">Lagre endringer</button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.checklists-page {
  max-width: 1200px;
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
  border: 1px solid var(--color-border);
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
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  overflow: hidden;
}

/* Split header into content (expandable) and actions (buttons) */
.checklist-header {
  display: flex;
  align-items: center;
  padding: 0.9rem;
  gap: 0.75rem;
}

.checklist-header__content {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  cursor: pointer;
  user-select: none;
}

.checklist-header__content:focus {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.checklist-header__title {
  margin: 0;
  color: var(--color-foreground);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
}

.checklist-header__meta {
  margin: 0.25rem 0 0;
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
}

.checklist-header__progress {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 8rem;
}

.checklist-header__progress span {
  font-size: var(--font-size-xs);
  color: var(--color-gray-600);
  font-weight: var(--font-weight-semibold);
  min-width: 2.2rem;
  text-align: right;
}

.checklist-header__status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 3rem;
  min-height: 1.8rem;
  padding: 0 0.55rem;
  border-radius: var(--radius-sm);
  border: 1px solid transparent;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  line-height: 1;
}

.progress-track {
  height: 0.4rem;
  flex: 1;
  background: var(--color-gray-200);
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--color-border-strong) 60%, var(--color-border));
  box-shadow: inset 0 1px 1px rgba(0, 39, 43, 0.12), 0 1px 1px rgba(255, 255, 255, 0.8);
  overflow: hidden;
}

.progress-track__fill {
  height: 100%;
  background: var(--color-success-scale-2);
  transition: width var(--transition-base);
}

.checklist-header__progress--level-1 .progress-track__fill {
  background: var(--color-success-scale-1);
}

.checklist-header__progress--level-2 .progress-track__fill {
  background: var(--color-success-scale-2);
}

.checklist-header__progress--level-3 .progress-track__fill {
  background: var(--color-success-scale-3);
}

.checklist-header__progress--level-4 .progress-track__fill {
  background: var(--color-success-scale-4);
}

.checklist-header__progress--level-5 .progress-track__fill {
  background: var(--color-success-scale-5);
}

.checklist-header__progress--level-1 .checklist-header__status-tag {
  background: color-mix(in srgb, var(--color-success-scale-1) 35%, var(--color-card));
  color: var(--color-foreground);
  border-color: color-mix(in srgb, var(--color-success-scale-2) 45%, var(--color-border));
}

.checklist-header__progress--level-2 .checklist-header__status-tag {
  background: color-mix(in srgb, var(--color-success-scale-2) 45%, var(--color-card));
  color: var(--color-foreground);
  border-color: color-mix(in srgb, var(--color-success-scale-3) 50%, var(--color-border));
}

.checklist-header__progress--level-3 .checklist-header__status-tag {
  background: color-mix(in srgb, var(--color-success-scale-3) 55%, var(--color-card));
  color: var(--color-foreground);
  border-color: color-mix(in srgb, var(--color-success-scale-4) 55%, var(--color-border));
}

.checklist-header__progress--level-4 .checklist-header__status-tag {
  background: var(--color-success-scale-4);
  color: var(--color-primary-foreground);
  border-color: color-mix(in srgb, var(--color-success-scale-5) 55%, black);
}

.checklist-header__progress--level-5 .checklist-header__status-tag {
  background: var(--color-success-scale-5);
  color: var(--color-primary-foreground);
  border-color: color-mix(in srgb, var(--color-success-scale-5) 70%, black);
}

/* Options menu - now outside the button */
.options-menu {
  position: relative;
  flex-shrink: 0;
}

.options-menu__trigger {
  aspect-ratio: 1 / 1;
  width: var(--touch-target);
  height: var(--touch-target);
  min-width: var(--touch-target);
  min-height: var(--touch-target);
  border: 1px solid var(--color-border);
  background: var(--color-card);
  border-radius: var(--radius-sm);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.15rem;
  cursor: pointer;
  padding: 0;
}

.options-menu__trigger:hover {
  background: var(--color-accent);
}

.dot {
  width: 0.2rem;
  height: 0.2rem;
  background: var(--color-gray-600);
  border-radius: 100%;
}

.options-menu__list {
  position: absolute;
  top: calc(100% + 0.25rem);
  right: 0;
  z-index: 20;
  min-width: 9rem;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.options-menu__item {
  width: 100%;
  border: 0;
  background: transparent;
  padding: 0.6rem 0.8rem;
  text-align: left;
  color: var(--color-foreground);
  font-size: var(--font-size-sm);
  cursor: pointer;
}

.options-menu__item:hover {
  background: var(--color-accent);
}

.checklist-body {
  border-top: 1px solid var(--color-border);
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
  border: 1px solid var(--color-border);
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
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  color: var(--color-gray-600);
  text-align: center;
  padding: 1.2rem;
}

/* Form styles */
.checklist-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.form-group label {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--color-gray-700);
}

.form-group input,
.form-group textarea,
.form-group select {
  min-height: 2.5rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--color-border);
  background: var(--color-background-soft);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-family: inherit;
}

.form-group textarea {
  min-height: 5rem;
  resize: vertical;
}

.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
  outline: none;
  border-color: var(--ik-mat-primary);
}

.action-btn {
  min-height: 2.5rem;
  padding: 0 1rem;
  border: none;
  border-radius: var(--radius-md);
  background: var(--ik-mat-primary);
  color: var(--color-primary-foreground);
  font-size: var(--font-size-sm);
  font-weight: 600;
  cursor: pointer;
}

.action-btn:hover {
  opacity: 0.9;
}

.action-btn--ghost {
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-gray-700);
}

.action-btn--ghost:hover {
  background: var(--color-accent);
}

@media (max-width: 48rem) {
  .checklist-header {
    flex-wrap: wrap;
  }

  .checklist-header__content {
    width: 100%;
  }

  .checklist-header__progress {
    min-width: 5rem;
  }

  .options-menu {
    margin-left: auto;
  }

  .options-menu__list {
    right: 0;
    left: auto;
  }
}
</style>
