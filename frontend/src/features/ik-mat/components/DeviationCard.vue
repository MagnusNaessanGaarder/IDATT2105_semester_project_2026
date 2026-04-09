<script setup lang="ts">
import { toRef } from 'vue'
import type { Deviation } from '../types'
import { useDeviationCardState } from '../composables/useDeviationCardState'

const props = defineProps<{
  deviation: Deviation
}>()

const emit = defineEmits<{
  view: []
}>()

const { severityLabel, statusLabel, statusIcon } = useDeviationCardState(toRef(props, 'deviation'))
</script>

<template>
  <div class="deviation-card" :class="`deviation-card--${deviation.severity}`">
    <div class="deviation-card__header">
      <div class="deviation-card__title-section">
        <h3 class="deviation-card__title">{{ deviation.title }}</h3>
        <p class="deviation-card__description">{{ deviation.description }}</p>
      </div>
      <div class="deviation-card__badges">
        <span class="deviation-card__badge deviation-card__badge--severity" :class="`deviation-card__badge--${deviation.severity}`">
          {{ severityLabel }}
        </span>
        <span class="deviation-card__badge deviation-card__badge--status" :class="`deviation-card__badge--${deviation.status}`">
          <span class="deviation-card__badge-icon">{{ statusIcon }}</span>
          {{ statusLabel }}
        </span>
      </div>
    </div>

    <div class="deviation-card__body">
      <div class="deviation-card__section">
        <h4 class="deviation-card__section-title">Sted</h4>
        <p class="deviation-card__section-content">{{ deviation.location }}</p>
      </div>

      <div class="deviation-card__section">
        <h4 class="deviation-card__section-title">Umiddelbar handling</h4>
        <p class="deviation-card__section-content">{{ deviation.immediate_action }}</p>
      </div>

      <div class="deviation-card__section">
        <h4 class="deviation-card__section-title">Korrigerende handling</h4>
        <p class="deviation-card__section-content">{{ deviation.corrective_action }}</p>
      </div>
    </div>

    <div class="deviation-card__footer">
      <div class="deviation-card__info">
        <span class="deviation-card__reported">Rapportert av {{ deviation.reported_by }}</span>
        <time class="deviation-card__datetime">{{ deviation.reported_date }} kl. {{ deviation.reported_time }}</time>
      </div>
      <button class="deviation-card__action-btn" @click="emit('view')">Se detaljer</button>
    </div>
  </div>
</template>

<style scoped>
.deviation-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: box-shadow var(--transition-base), border-color var(--transition-base);
}

.deviation-card:hover {
  box-shadow: var(--shadow-md);
}

.deviation-card--low {
  border-left: 0.25rem solid var(--color-info);
}

.deviation-card--medium {
  border-left: 0.25rem solid var(--color-warning);
}

.deviation-card--high {
  border-left: 0.25rem solid var(--color-danger);
}

.deviation-card__header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 1rem;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid var(--color-border);
}

.deviation-card__title-section {
  flex: 1;
  min-width: 0;
}

.deviation-card__title {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-foreground);
}

.deviation-card__description {
  margin: 0.5rem 0 0;
  font-size: var(--text-sm);
  color: var(--color-gray-600);
}

.deviation-card__badges {
  display: flex;
  gap: 0.5rem;
}

.deviation-card__badge {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.375rem 0.75rem;
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.deviation-card__badge--severity {
  background: var(--color-gray-100);
  color: var(--color-gray-900);
}

.deviation-card__badge--low {
  background: var(--color-info-bg);
  color: var(--color-info);
}

.deviation-card__badge--medium {
  background: var(--color-warning-bg);
  color: var(--color-warning);
}

.deviation-card__badge--high {
  background: var(--color-danger-bg);
  color: var(--color-danger-fg);
}

.deviation-card__badge--status {
  color: inherit;
}

.deviation-card__badge--open {
  background: var(--color-danger-bg);
  color: var(--color-danger-fg);
}

.deviation-card__badge--in-progress {
  background: var(--color-info-bg);
  color: var(--color-info);
}

.deviation-card__badge--resolved {
  background: var(--color-success-bg);
  color: var(--color-success);
}

.deviation-card__badge-icon {
  font-size: 1rem;
}

.deviation-card__body {
  padding: 1.5rem;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.deviation-card__section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.deviation-card__section:nth-child(3) {
  grid-column: 1 / -1;
}

.deviation-card__section-title {
  margin: 0;
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--color-gray-600);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.deviation-card__section-content {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--color-foreground);
  line-height: 1.5;
}

.deviation-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.5rem;
  background: var(--color-accent);
  border-top: 1px solid var(--color-border);
}

.deviation-card__info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.deviation-card__reported {
  font-size: var(--text-sm);
  color: var(--color-gray-600);
}

.deviation-card__datetime {
  font-size: var(--text-xs);
  color: var(--color-gray-600);
}

.deviation-card__action-btn {
  padding: 0.5rem 1rem;
  background: var(--color-foreground);
  color: var(--color-background);
  border: none;
  border-radius: var(--radius-sm);
  font-size: var(--text-sm);
  font-weight: 600;
  cursor: pointer;
  transition: background-color var(--transition-fast);
  white-space: nowrap;
}

.deviation-card__action-btn:hover {
  background: var(--color-gray-900);
}

@media (max-width: 48rem) {
  .deviation-card__header {
    flex-direction: column;
  }

  .deviation-card__body {
    grid-template-columns: 1fr;
  }

  .deviation-card__section:nth-child(3) {
    grid-column: 1;
  }

  .deviation-card__footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
