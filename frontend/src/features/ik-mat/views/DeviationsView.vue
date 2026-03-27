<script setup lang="ts">
import { ref, computed } from 'vue'
import ikMatData from '@/data/ik-mat.json'
import DeviationCard from '../components/DeviationCard.vue'

interface Deviation {
  id: number
  title: string
  description: string
  severity: 'low' | 'medium' | 'high'
  reported_by: string
  reported_date: string
  reported_time: string
  location: string
  immediate_action: string
  corrective_action: string
  status: 'open' | 'in-progress' | 'resolved'
}

const deviations = ref<Deviation[]>(ikMatData.deviations as Deviation[])
const selectedStatus = ref<string>('all')

const statuses = ['open', 'in-progress', 'resolved']

const filteredDeviations = computed(() => {
  if (selectedStatus.value === 'all') {
    return deviations.value
  }
  return deviations.value.filter(d => d.status === selectedStatus.value)
})

const handleViewDeviation = (deviation: Deviation) => {
  void deviation
}
</script>

<template>
  <div class="view-page">
    <header class="page-header">
      <h1>Avvik</h1>
      <p class="subtitle">Registrere og følge opp avvik fra rutiner</p>
    </header>

    <div class="deviations-filters">
      <button 
        v-for="status in ['all', ...statuses]"
        :key="status"
        class="filter-btn"
        :class="{ 'filter-btn--active': selectedStatus === status }"
        @click="selectedStatus = status"
      >
        {{ status === 'all' ? 'Alle' : status === 'open' ? 'Åpne' : status === 'in-progress' ? 'Under behandling' : 'Løst' }}
      </button>
    </div>

    <div class="deviations-list">
      <DeviationCard 
        v-for="deviation in filteredDeviations"
        :key="deviation.id"
        :deviation="deviation"
        @view="handleViewDeviation(deviation)"
      />
    </div>

    <div v-if="filteredDeviations.length === 0" class="empty-state">
      <p>Ingen avvik funnet</p>
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

.deviations-filters {
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

.deviations-list {
  display: flex;
  flex-direction: column;
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
</style>
