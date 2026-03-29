<!--
  BaseSpinner - Laste-indikator komponent
  
  Brukes for å vise at data lastes.
  Støtter størrelser: sm, md, lg
  Har innebygd skjermleser-støtte (sr-only)
  
  Eksempel:
  <BaseSpinner v-if="isLoading" />
  <BaseSpinner size="lg" label="Larger sjekklister..." />
-->
<script setup lang="ts">
interface Props {
  size?: 'sm' | 'md' | 'lg'
  label?: string
}

withDefaults(defineProps<Props>(), {
  size: 'md',
  label: 'Laster...',
})
</script>

<template>
  <div class="base-spinner" :class="`base-spinner--${size}`">
    <div class="base-spinner__circle" role="status">
      <span class="sr-only">{{ label }}</span>
    </div>
  </div>
</template>

<style scoped>
.base-spinner {
  display: flex;
  align-items: center;
  justify-content: center;
}

.base-spinner__circle {
  border: 0.125rem solid var(--color-gray-300);
  border-top-color: var(--color-accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.base-spinner--sm .base-spinner__circle {
  width: 1rem;
  height: 1rem;
}

.base-spinner--md .base-spinner__circle {
  width: 1.5rem;
  height: 1.5rem;
}

.base-spinner--lg .base-spinner__circle {
  width: 2.5rem;
  height: 2.5rem;
  border-width: 0.1875rem;
}

.sr-only {
  position: absolute;
  width: 0.0625rem;
  height: 0.0625rem;
  padding: 0;
  margin: -0.0625rem;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>