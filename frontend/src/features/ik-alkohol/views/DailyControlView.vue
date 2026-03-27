<script setup lang="ts">
import { computed, ref } from 'vue'
import alkoholData from '@/data/ik-alkohol.json'

interface DailyControlItem {
  id: number
  name: string
  law_unit: string
  employee: string
  comment: string
  completion_date: {
    date: string
    time: string
  }
  attachment: string | null
  is_checked: boolean
}

const controls = ref<DailyControlItem[]>(alkoholData['daily-control'] as DailyControlItem[])
const filter = ref<'all' | 'checked' | 'pending'>('all')

const filteredControls = computed(() => {
  if (filter.value === 'checked') {
    return controls.value.filter((item) => item.is_checked)
  }

  if (filter.value === 'pending') {
    return controls.value.filter((item) => !item.is_checked)
  }

  return controls.value
})
</script>

<template>
  <div class="view-page">
    <header class="page-header">
      <h1>Daglig kontroll</h1>
      <p class="subtitle">Daglige kontroller for alkoholservering</p>
    </header>

    <div class="control-filters">
      <button class="filter-btn" :class="{ 'filter-btn--active': filter === 'all' }" @click="filter = 'all'">Alle</button>
      <button class="filter-btn" :class="{ 'filter-btn--active': filter === 'checked' }" @click="filter = 'checked'">Fullfort</button>
      <button class="filter-btn" :class="{ 'filter-btn--active': filter === 'pending' }" @click="filter = 'pending'">Mangler</button>
    </div>

    <div class="control-list">
      <article v-for="item in filteredControls" :key="item.id" class="control-card">
        <header class="control-card__header">
          <h2 class="control-card__title">{{ item.name }}</h2>
          <span class="control-card__status" :class="{ 'control-card__status--pending': !item.is_checked }">
            {{ item.is_checked ? 'Fullfort' : 'Mangler' }}
          </span>
        </header>
        <p class="control-card__law">{{ item.law_unit }}</p>
        <p class="control-card__comment">{{ item.comment }}</p>
        <p class="control-card__meta">Ansatt: {{ item.employee }} - {{ item.completion_date.date }} kl. {{ item.completion_date.time }}</p>
        <p v-if="item.attachment" class="control-card__attachment">Vedlegg: {{ item.attachment }}</p>
      </article>
    </div>

    <div v-if="filteredControls.length === 0" class="empty-state">
      Ingen kontroller matcher valgt filter.
    </div>
  </div>
</template>

<style scoped>
.view-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-foreground);
  margin-bottom: 8px;
}

.subtitle {
  font-size: var(--font-size-base);
  color: var(--color-gray-500);
}

.control-filters {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-bottom: 1.5rem;
}

.filter-btn {
  padding: 0.5rem 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-card);
  color: var(--color-gray-600);
  font-size: var(--text-sm);
  cursor: pointer;
}

.filter-btn--active {
  background: var(--color-foreground);
  color: var(--color-background);
  border-color: var(--color-foreground);
}

.control-list {
  display: grid;
  gap: 1rem;
}

.control-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1rem;
}

.control-card__header {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.5rem;
}

.control-card__title {
  margin: 0;
  font-size: var(--text-base);
}

.control-card__status {
  font-size: var(--text-xs);
  background: #15803d;
  color: var(--color-background);
  padding: 0.25rem 0.5rem;
  border-radius: var(--radius-sm);
}

.control-card__status--pending {
  background: #b45309;
}

.control-card__law,
.control-card__comment,
.control-card__meta,
.control-card__attachment {
  margin: 0.35rem 0;
  font-size: var(--text-sm);
  color: var(--color-gray-600);
}

.empty-state {
  margin-top: 1rem;
  text-align: center;
  color: var(--color-gray-600);
}
</style>