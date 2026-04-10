<!--
  BaseBadge - Status-merke komponent

  Brukes for å vise status (f.eks. "Fullført", "Venter", "Avvik")
  Støtter farger: success, warning, danger, info, neutral

  Eksempel:
  <BaseBadge variant="success">Fullført</BaseBadge>
  <BaseBadge variant="danger">Avvik</BaseBadge>
-->
<script setup lang="ts">
interface Props {
  variant?: 'success' | 'warning' | 'danger' | 'info' | 'neutral'
}

withDefaults(defineProps<Props>(), {
  variant: 'neutral',
})
</script>

<template>
  <span class="base-badge" :class="`base-badge--${variant}`">
    <slot />
  </span>
</template>

<style scoped>
.base-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.5rem;
  font-family: var(--font-family-ui);
  font-size: 0.6875rem;
  font-weight: var(--font-weight-bold);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  border-radius: 999px;
  border: 1px solid transparent;
}

.base-badge--success {
  background: var(--color-success-bg);
  color: var(--color-success);
  border-color: color-mix(in srgb, var(--color-success) 24%, var(--color-border));
}

.base-badge--warning {
  background: var(--color-warning-bg);
  color: var(--color-warning);
  border-color: color-mix(in srgb, var(--color-warning) 24%, var(--color-border));
}

.base-badge--danger {
  background: var(--color-danger-bg);
  color: var(--color-danger-fg);
  border-color: color-mix(in srgb, var(--color-danger) 24%, var(--color-border));
}

.base-badge--info {
  background: var(--color-info-bg);
  color: var(--color-info);
  border-color: color-mix(in srgb, var(--color-info) 24%, var(--color-border));
}

.base-badge--neutral {
  background: color-mix(in srgb, var(--color-card) 70%, var(--color-brand-ice-teal));
  color: var(--color-gray-700);
  border-color: var(--color-border);
}
</style>