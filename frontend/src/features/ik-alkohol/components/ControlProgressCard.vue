<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  completed: number
  total: number
}>()

const progress = computed(() => {
  if (props.total === 0) {
    return 0
  }

  return Math.round((props.completed / props.total) * 100)
})

const progressColor = computed(() => {
  const percentage = progress.value

  if (percentage <= 50) {
    const warningMix = Math.round((percentage / 50) * 100)
    return `color-mix(in srgb, var(--color-warning) ${warningMix}%, var(--color-danger))`
  }

  const successMix = Math.round(((percentage - 50) / 50) * 100)
  return `color-mix(in srgb, var(--color-success) ${successMix}%, var(--color-warning))`
})

const statusTagStyle = computed(() => {
  const tone = progressColor.value

  return {
    color: `color-mix(in srgb, ${tone} 78%, var(--color-foreground))`,
    background: `color-mix(in srgb, ${tone} 16%, var(--color-card))`,
    borderColor: `color-mix(in srgb, ${tone} 35%, var(--color-border))`,
  }
})

const fillStyle = computed(() => {
  return {
    width: `${progress.value}%`,
    background: progressColor.value,
  }
})
</script>

<template>
  <section class="progress-card" aria-label="Fremdrift for daglig kontroll">
    <div class="progress-card__head">
      <p class="progress-card__label">Status i dag</p>
      <span class="status-pill" :style="statusTagStyle">{{ completed }} / {{ total }} fullført</span>
    </div>
    <div class="progress-track">
      <div class="progress-track__fill" :style="fillStyle" />
    </div>
    <p class="progress-card__meta">{{ progress }}% gjennomført av dagens kontrollpunkter.</p>
  </section>
</template>

<style scoped>
.progress-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  padding: 14px;
  margin-bottom: 12px;
}

.progress-card__head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
  margin-bottom: 8px;
}

.progress-card__label {
  margin: 0;
  color: var(--color-foreground);
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
}

.status-pill {
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  border: 1px solid transparent;
}

.progress-track {
  height: 8px;
  background: var(--color-gray-200);
  border-radius: 999px;
  overflow: hidden;
}

.progress-track__fill {
  height: 100%;
  background: var(--color-danger);
  transition: width var(--transition-base);
}

.progress-card__meta {
  margin: 8px 0 0;
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
}

@media (max-width: 48rem) {
  .progress-card__head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
