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
  padding: 4px 8px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border-radius: 999px;
  border: 1px solid transparent;
}

.base-badge--success {
  background: var(--color-success-bg);
  color: var(--color-success);
  border-color: rgba(5, 150, 105, 0.25);
}

.base-badge--warning {
  background: var(--color-warning-bg);
  color: var(--color-warning);
  border-color: rgba(217, 119, 6, 0.25);
}

.base-badge--danger {
  background: var(--color-danger-bg);
  color: var(--color-danger-fg);
  border-color: rgba(220, 38, 38, 0.25);
}

.base-badge--info {
  background: var(--color-info-bg);
  color: var(--color-info);
  border-color: rgba(2, 132, 199, 0.25);
}

.base-badge--neutral {
  background: var(--color-accent);
  color: var(--color-gray-700);
  border-color: var(--color-border);
}
</style>