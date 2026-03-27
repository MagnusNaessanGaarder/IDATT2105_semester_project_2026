<script setup lang="ts">
import { ref, computed } from 'vue'
import ikMatData from '@/data/ik-mat.json'
import ChecklistCard from '../components/ChecklistCard.vue'

interface Checklist {
  id: number
  name: string
  category: string
  frequency: string
  description: string
  created_date: string
  law_unit: string
  items: Array<{
    id: number
    task: string
    required: boolean
    completed: boolean
    notes: string | null
  }>
  completed_by: string | null
  completion_date: string | null
  completion_time: string | null
  status: 'completed' | 'pending' | 'overdue'
}

const checklists = ref<Checklist[]>(ikMatData.checklists as Checklist[])
const selectedCategory = ref<string>('all')

const categories = computed(() => {
  const cats = new Set(checklists.value.map(c => c.category))
  return Array.from(cats).sort()
})

const filteredChecklists = computed(() => {
  if (selectedCategory.value === 'all') {
    return checklists.value
  }
  return checklists.value.filter(c => c.category === selectedCategory.value)
})

const handleViewChecklist = (checklist: Checklist) => {
  void checklist
}
</script>

<template>
  <div class="view-page">
    <header class="page-header">
      <h1>Sjekklister</h1>
      <p class="subtitle">Daglige, ukentlige og månedlige kontroller</p>
    </header>

    <div class="checklists-filters">
      <button 
        v-for="cat in ['all', ...categories]"
        :key="cat"
        class="filter-btn"
        :class="{ 'filter-btn--active': selectedCategory === cat }"
        @click="selectedCategory = cat"
      >
        {{ cat === 'all' ? 'Alle' : cat }}
      </button>
    </div>

    <div class="checklists-grid">
      <ChecklistCard 
        v-for="checklist in filteredChecklists"
        :key="checklist.id"
        :checklist="checklist"
        @view="handleViewChecklist(checklist)"
      />
    </div>

    <div v-if="filteredChecklists.length === 0" class="empty-state">
      <p>Ingen sjekklister funnet</p>
    </div>
  </div>
</template>

<style scoped>
.view-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}

.page-header {
  margin-bottom: 2rem;
}

.page-header h1 {
  margin: 0;
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--color-foreground);
  margin-bottom: 0.5rem;
}

.subtitle {
  margin: 0;
  font-size: var(--text-base);
  color: var(--color-gray-600);
}

.checklists-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-bottom: 2rem;
}

.filter-btn {
  padding: 0.5rem 1rem;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-base);
  color: var(--color-gray-600);
}

.filter-btn:hover {
  background: var(--color-accent);
  border-color: var(--color-border-focus);
}

.filter-btn--active {
  background: var(--color-foreground);
  color: var(--color-background);
  border-color: var(--color-foreground);
}

.checklists-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.empty-state {
  text-align: center;
  padding: 3rem 1.5rem;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-gray-600);
}

@media (max-width: 48rem) {
  .checklists-grid {
    grid-template-columns: 1fr;
  }
}
</style>
