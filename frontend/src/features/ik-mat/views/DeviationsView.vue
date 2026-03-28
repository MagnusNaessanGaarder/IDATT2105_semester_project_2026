<script setup lang="ts">
import { computed, ref } from 'vue'
import { useIkMatData } from '../composables/useIkMatData'

const { deviations, formatDate } = useIkMatData()

const selectedStatus = ref<'all' | 'open' | 'in-progress' | 'resolved'>('all')
const selectedId = ref<number | null>(deviations[0]?.id ?? null)

const filtered = computed(() => {
  if (selectedStatus.value === 'all') {
    return deviations
  }

  return deviations.filter((item) => item.status === selectedStatus.value)
})

const selectedDeviation = computed(() => {
  return filtered.value.find((item) => item.id === selectedId.value) ?? filtered.value[0] ?? null
})

const statusLabel = (status: 'open' | 'in-progress' | 'resolved') => {
  if (status === 'open') {
    return 'Åpen'
  }

  if (status === 'in-progress') {
    return 'Pågår'
  }

  return 'Løst'
}

const severityLabel = (severity: 'low' | 'medium' | 'high') => {
  if (severity === 'high') {
    return 'Hoy'
  }

  if (severity === 'medium') {
    return 'Medium'
  }

  return 'Lav'
}
</script>

<template>
  <div class="deviations-page">
    <header class="page-header">
      <h1>Avvik</h1>
      <p class="subtitle">Registrer, prioriter og lukk avvik i en samlet arbeidsflate</p>
    </header>

    <div class="filter-row" role="tablist" aria-label="Filtrer avvik">
      <button class="filter-chip" :class="{ 'filter-chip--active': selectedStatus === 'all' }" @click="selectedStatus = 'all'">Alle</button>
      <button class="filter-chip" :class="{ 'filter-chip--active': selectedStatus === 'open' }" @click="selectedStatus = 'open'">Ãpne</button>
      <button class="filter-chip" :class="{ 'filter-chip--active': selectedStatus === 'in-progress' }" @click="selectedStatus = 'in-progress'">Pågår</button>
      <button class="filter-chip" :class="{ 'filter-chip--active': selectedStatus === 'resolved' }" @click="selectedStatus = 'resolved'">Løste</button>
    </div>

    <section class="deviations-layout">
      <aside class="deviation-list" aria-label="Avviksliste">
        <button
          v-for="item in filtered"
          :key="item.id"
          class="deviation-list__item"
          :class="{ 'deviation-list__item--active': selectedDeviation?.id === item.id }"
          @click="selectedId = item.id"
        >
          <p class="deviation-list__title">{{ item.title }}</p>
          <p class="deviation-list__meta">{{ item.location }} · {{ formatDate(item.reported_date) }}</p>
          <div class="deviation-list__chips">
            <span class="status-chip" :class="item.status === 'resolved' ? 'status-chip--good' : item.status === 'in-progress' ? 'status-chip--warn' : 'status-chip--danger'">
              {{ statusLabel(item.status) }}
            </span>
            <span class="status-chip" :class="item.severity === 'high' ? 'status-chip--danger' : item.severity === 'medium' ? 'status-chip--warn' : 'status-chip--info'">
              {{ severityLabel(item.severity) }}
            </span>
          </div>
        </button>
      </aside>

      <article class="deviation-detail" v-if="selectedDeviation">
        <header class="deviation-detail__header">
          <h2>{{ selectedDeviation.title }}</h2>
          <span class="status-chip" :class="selectedDeviation.status === 'resolved' ? 'status-chip--good' : selectedDeviation.status === 'in-progress' ? 'status-chip--warn' : 'status-chip--danger'">
            {{ statusLabel(selectedDeviation.status) }}
          </span>
        </header>

        <p class="deviation-detail__description">{{ selectedDeviation.description }}</p>

        <div class="detail-grid">
          <div>
            <p class="detail-label">Meldt av</p>
            <p>{{ selectedDeviation.reported_by }}</p>
          </div>
          <div>
            <p class="detail-label">Tidspunkt</p>
            <p>{{ formatDate(selectedDeviation.reported_date) }} kl. {{ selectedDeviation.reported_time }}</p>
          </div>
          <div>
            <p class="detail-label">Umiddelbar handling</p>
            <p>{{ selectedDeviation.immediate_action }}</p>
          </div>
          <div>
            <p class="detail-label">Korrigerende tiltak</p>
            <p>{{ selectedDeviation.corrective_action }}</p>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.deviations-page {
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

.deviations-layout {
  display: grid;
  grid-template-columns: minmax(16rem, 22rem) 1fr;
  gap: 0.85rem;
}

.deviation-list {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  padding: 0.5rem;
  display: grid;
  gap: 0.45rem;
  align-content: start;
}

.deviation-list__item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-card);
  text-align: left;
  padding: 0.65rem;
}

.deviation-list__item--active {
  border-color: color-mix(in srgb, var(--ik-mat-primary) 45%, var(--color-border));
  background: color-mix(in srgb, var(--ik-mat-bg) 40%, var(--color-card));
}

.deviation-list__title {
  margin: 0;
  color: var(--color-foreground);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
}

.deviation-list__meta {
  margin: 0.2rem 0 0;
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
}

.deviation-list__chips {
  margin-top: 0.45rem;
  display: flex;
  gap: 0.4rem;
}

.deviation-detail {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  padding: 0.9rem;
}

.deviation-detail__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
  margin-bottom: 0.65rem;
}

.deviation-detail__header h2 {
  margin: 0;
  font-size: var(--font-size-lg);
  color: var(--color-foreground);
}

.deviation-detail__description {
  margin: 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
}

.detail-grid {
  margin-top: 0.8rem;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.7rem;
}

.detail-label {
  margin: 0;
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.detail-grid p {
  margin: 0.2rem 0 0;
  color: var(--color-foreground);
  font-size: var(--font-size-sm);
}

.status-chip {
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  padding: 0.2rem 0.45rem;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
}

.status-chip--good {
  color: var(--color-success);
  background: var(--color-success-bg);
  border-color: color-mix(in srgb, var(--color-success) 35%, var(--color-border));
}

.status-chip--warn {
  color: var(--color-warning);
  background: var(--color-warning-bg);
  border-color: color-mix(in srgb, var(--color-warning) 35%, var(--color-border));
}

.status-chip--danger {
  color: var(--color-danger);
  background: var(--color-danger-bg);
  border-color: color-mix(in srgb, var(--color-danger) 35%, var(--color-border));
}

.status-chip--info {
  color: var(--color-info);
  background: var(--color-info-bg);
  border-color: color-mix(in srgb, var(--color-info) 35%, var(--color-border));
}

@media (max-width: 62rem) {
  .deviations-layout {
    grid-template-columns: 1fr;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
