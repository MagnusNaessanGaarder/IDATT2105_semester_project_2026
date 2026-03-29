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
}

.base-badge--success {
  background: var(--color-success-bg);
  color: var(--color-success);
}

.base-badge--warning {
  background: var(--color-warning-bg);
  color: var(--color-warning);
}

.base-badge--danger {
  background: var(--color-danger-bg);
  color: var(--color-danger);
}

.base-badge--info {
  background: var(--color-info-bg);
  color: var(--color-info);
}

.base-badge--neutral {
  background: var(--color-gray-200);
  color: var(--color-gray-700);
}
</style>