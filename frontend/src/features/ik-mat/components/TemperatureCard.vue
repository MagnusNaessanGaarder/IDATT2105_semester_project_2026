<script setup lang="ts">
import { toRef } from 'vue'
import type { TemperatureRecord } from '../types'
import { useTemperatureCardState } from '../composables/useTemperatureCardState'

const props = defineProps<{
  record: TemperatureRecord
}>()

const { statusLabel, statusIcon, isWithinRange } = useTemperatureCardState(toRef(props, 'record'))
</script>

<template>
  <div class="temperature-card" :class="`temperature-card--${record.status}`">
    <div class="temperature-card__header">
      <div class="temperature-card__location">
        <h3 class="temperature-card__location-name">{{ record.location }}</h3>
        <p class="temperature-card__recorded-by">Av {{ record.recorded_by }}</p>
      </div>
      <div class="temperature-card__status-badge" :class="`temperature-card__status--${record.status}`">
        <span class="temperature-card__status-icon">{{ statusIcon }}</span>
        <span class="temperature-card__status-label">{{ statusLabel }}</span>
      </div>
    </div>

    <div class="temperature-card__body">
      <div class="temperature-card__temperature-display">
        <div class="temperature-card__current">
          <span class="temperature-card__temp-value">{{ record.temperature_c }}</span>
          <span class="temperature-card__temp-unit">°C</span>
        </div>
        <div class="temperature-card__range-info">
          <span class="temperature-card__range-label">Tillatt område:</span>
          <span class="temperature-card__range-values">
            {{ record.min_temp }}°C til {{ record.max_temp }}°C
          </span>
          <div class="temperature-card__range-indicator">
            <div class="temperature-card__range-bar">
              <div 
                class="temperature-card__range-marker"
                :style="{ 
                  left: `${((record.temperature_c - record.min_temp) / (record.max_temp - record.min_temp)) * 100}%` 
                }"
                :class="{ 'temperature-card__range-marker--outside': !isWithinRange }"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="temperature-card__footer">
      <time class="temperature-card__datetime">
        {{ record.recorded_date }} kl. {{ record.recorded_time }}
      </time>
    </div>
  </div>
</template>

<style scoped>
.temperature-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: box-shadow var(--transition-base), border-color var(--transition-base);
}

.temperature-card:hover {
  box-shadow: var(--shadow-md);
}

.temperature-card--ok {
  border-left: 0.25rem solid #10b981;
}

.temperature-card--warning {
  border-left: 0.25rem solid #f59e0b;
  background: rgba(245, 158, 11, 0.02);
}

.temperature-card--critical {
  border-left: 0.25rem solid #ef4444;
  background: rgba(239, 68, 68, 0.02);
}

.temperature-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid var(--color-border);
}

.temperature-card__location {
  flex: 1;
}

.temperature-card__location-name {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-foreground);
}

.temperature-card__recorded-by {
  margin: 0.25rem 0 0;
  font-size: var(--text-sm);
  color: var(--color-gray-600);
}

.temperature-card__status-badge {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: var(--color-accent);
  border-radius: var(--radius-full);
  font-size: var(--text-sm);
  font-weight: 600;
}

.temperature-card__status--ok {
  background: rgba(16, 185, 129, 0.1);
  color: #047857;
}

.temperature-card__status--warning {
  background: rgba(245, 158, 11, 0.1);
  color: #b45309;
}

.temperature-card__status--critical {
  background: rgba(239, 68, 68, 0.1);
  color: #991b1b;
}

.temperature-card__status-icon {
  font-size: 1rem;
}

.temperature-card__status-label {
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.temperature-card__body {
  padding: 1.5rem;
}

.temperature-card__temperature-display {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
}

.temperature-card__current {
  display: flex;
  align-items: baseline;
  gap: 0.25rem;
}

.temperature-card__temp-value {
  font-size: 2.5rem;
  font-weight: 700;
  color: var(--color-foreground);
}

.temperature-card__temp-unit {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-gray-600);
}

.temperature-card__range-info {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.temperature-card__range-label {
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--color-gray-600);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.temperature-card__range-values {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--color-foreground);
}

.temperature-card__range-indicator {
  margin-top: 0.5rem;
}

.temperature-card__range-bar {
  position: relative;
  height: 0.5rem;
  background: var(--color-gray-200);
  border-radius: var(--radius-full);
  overflow: visible;
}

.temperature-card__range-marker {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 1rem;
  height: 1rem;
  background: #10b981;
  border: 0.125rem solid white;
  border-radius: var(--radius-full);
  box-shadow: var(--shadow-sm);
  transition: background-color var(--transition-fast);
}

.temperature-card__range-marker--outside {
  background: #ef4444;
}

.temperature-card__footer {
  padding: 1rem 1.5rem;
  background: var(--color-accent);
  border-top: 1px solid var(--color-border);
}

.temperature-card__datetime {
  font-size: var(--text-xs);
  color: var(--color-gray-600);
}

@media (max-width: 48rem) {
  .temperature-card__header {
    flex-direction: column;
    align-items: stretch;
  }

  .temperature-card__temperature-display {
    grid-template-columns: 1fr;
  }

  .temperature-card__status-badge {
    align-self: flex-start;
  }

  .temperature-card__temp-value {
    font-size: 2rem;
  }
}
</style>
