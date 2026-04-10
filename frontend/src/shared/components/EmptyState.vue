<!--
  EmptyState - Tom-tilstand komponent
  
  Brukes når lister er tomme eller ingen data finnes.
  Viser ikon, tittel og valgfri beskrivelse
  
  Eksempel:
  <EmptyState 
    v-if="items.length === 0" 
    title="Ingen sjekklister funnet"
    description="Opprett din første sjekkliste for å komme i gang"
  />
-->
<script setup lang="ts">
  interface Props {
    title: string
    description?: string
  }

  withDefaults(defineProps<Props>(), {
    description: '',
  })
</script>

<template>
  <div class="empty-state">
    <div class="empty-state__icon">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
        <line x1="9" y1="9" x2="15" y2="9"></line>
        <line x1="9" y1="12" x2="15" y2="12"></line>
        <line x1="9" y1="15" x2="12" y2="15"></line>
      </svg>
    </div>
    <h3 class="empty-state__title">{{ title }}</h3>
    <p v-if="description" class="empty-state__description">{{ description }}</p>
    <slot />
  </div>
</template>

<style scoped>
  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: var(--spacing-3xl) var(--spacing-lg);
    text-align: center;
  }

  .empty-state__icon {
    color: var(--color-brand-light-teal);
    margin-bottom: var(--spacing-md);
  }

  .empty-state__title {
    font-family: var(--font-family-display);
    font-size: var(--font-size-lg);
    font-weight: var(--font-weight-semibold);
    color: var(--color-foreground);
    margin: 0 0 var(--spacing-xs) 0;
  }

  .empty-state__description {
    font-size: var(--font-size-sm);
    color: var(--color-gray-500);
    margin: 0;
  }
</style>