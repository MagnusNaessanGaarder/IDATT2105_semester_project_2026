<script setup lang="ts">
import { computed } from 'vue'

interface ReportSection {
  name: string
  content: string
}

interface Report {
  id: number
  title: string
  type: string
  created_by: string
  created_date: string
  period: string
  status: 'draft' | 'finalized'
  sections: ReportSection[]
  file_url: string | null
  file_size: string | null
}

const props = defineProps<{
  report: Report
}>()

const emit = defineEmits<{
  view: []
  download: []
}>()

const statusLabel = computed(() => {
  return props.report.status === 'draft' ? 'Utkast' : 'Ferdig'
})

const statusColor = computed(() => {
  return props.report.status === 'draft' ? 'warning' : 'success'
})
</script>

<template>
  <div class="report-card" :class="`report-card--${statusColor}`">
    <div class="report-card__header">
      <div class="report-card__title-section">
        <h3 class="report-card__title">{{ report.title }}</h3>
        <div class="report-card__meta">
          <span class="report-card__type">{{ report.type }}</span>
          <span class="report-card__badge" :class="`report-card__badge--${statusColor}`">
            {{ statusLabel }}
          </span>
        </div>
      </div>
      <div class="report-card__date">
        <p class="report-card__created">Opprettet {{ report.created_date }}</p>
        <p class="report-card__creator">Av {{ report.created_by }}</p>
      </div>
    </div>

    <div class="report-card__body">
      <p class="report-card__period">Periode: {{ report.period }}</p>
      <div class="report-card__sections">
        <div v-for="(section, idx) in report.sections" :key="idx" class="report-card__section">
          <h4 class="report-card__section-title">{{ section.name }}</h4>
          <p class="report-card__section-text">{{ section.content }}</p>
        </div>
      </div>
    </div>

    <div class="report-card__footer">
      <div class="report-card__info">
        <span v-if="report.file_size" class="report-card__file-info">
          📄 {{ report.file_size }}
        </span>
      </div>
      <div class="report-card__actions">
        <button class="report-card__action-btn" @click="emit('view')">Se rapport</button>
        <button v-if="report.file_url" class="report-card__action-btn report-card__action-btn--secondary" @click="emit('download')">Last ned</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.report-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: box-shadow var(--transition-base);
  display: flex;
  flex-direction: column;
}

.report-card:hover {
  box-shadow: var(--shadow-md);
}

.report-card--success {
  border-left: 0.25rem solid var(--color-success);
}

.report-card--warning {
  border-left: 0.25rem solid var(--color-warning);
}

.report-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  padding: 1.5rem;
  border-bottom: 1px solid var(--color-border);
}

.report-card__title-section {
  flex: 1;
}

.report-card__title {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-foreground);
}

.report-card__meta {
  display: flex;
  gap: 0.75rem;
  margin-top: 0.5rem;
}

.report-card__type {
  display: inline-block;
  background: var(--color-accent);
  color: var(--color-accent-foreground);
  padding: 0.25rem 0.75rem;
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  font-weight: 600;
  text-transform: uppercase;
}

.report-card__badge {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  font-weight: 600;
  text-transform: uppercase;
}

.report-card__badge--success {
  background: var(--color-success-bg);
  color: var(--color-success);
}

.report-card__badge--warning {
  background: var(--color-warning-bg);
  color: var(--color-warning);
}

.report-card__date {
  text-align: right;
}

.report-card__created {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--color-gray-600);
}

.report-card__creator {
  margin: 0.25rem 0 0;
  font-size: var(--text-xs);
  color: var(--color-gray-600);
}

.report-card__body {
  padding: 1.5rem;
  flex: 1;
}

.report-card__period {
  margin: 0 0 1rem;
  font-size: var(--text-sm);
  color: var(--color-gray-600);
  font-weight: 500;
}

.report-card__sections {
  display: grid;
  gap: 1rem;
}

.report-card__section {
  padding: 0.75rem;
  background: var(--color-accent);
  border-radius: var(--radius-sm);
}

.report-card__section-title {
  margin: 0;
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-foreground);
}

.report-card__section-text {
  margin: 0.5rem 0 0;
  font-size: var(--text-sm);
  color: var(--color-gray-600);
  line-height: 1.5;
}

.report-card__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.5rem;
  background: var(--color-accent);
  border-top: 1px solid var(--color-border);
}

.report-card__file-info {
  font-size: var(--text-sm);
  color: var(--color-gray-600);
}

.report-card__actions {
  display: flex;
  gap: 0.5rem;
}

.report-card__action-btn {
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

.report-card__action-btn:hover {
  background: var(--color-gray-900);
}

.report-card__action-btn--secondary {
  background: var(--color-border);
  color: var(--color-foreground);
}

.report-card__action-btn--secondary:hover {
  background: var(--color-gray-200);
}

@media (max-width: 48rem) {
  .report-card__header {
    flex-direction: column;
  }

  .report-card__date {
    text-align: left;
  }

  .report-card__footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
