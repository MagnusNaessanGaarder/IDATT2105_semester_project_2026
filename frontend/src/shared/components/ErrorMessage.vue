<!--
  ErrorMessage - Feilmelding komponent
  
  Brukes for å vise feilmeldinger fra API eller validering.
  Støtter retry-knapp for å prøve igjen
  Har innebygd WCAG-tilgjengelighet (role="alert")
  
  Eksempel:
  <ErrorMessage v-if="error" :message="error" />
  <ErrorMessage 
    v-if="error" 
    :message="error" 
    showRetry 
    @retry="fetchData" 
  />
-->
<script setup lang="ts">
  interface Props {
    message: string
    showRetry?: boolean
  }

  withDefaults(defineProps<Props>(), {
    showRetry: false,
  })

  const emit = defineEmits<{
    retry: []
  }>()
</script>

<template>
  <div class="error-message" role="alert">
    <div class="error-message__icon">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"></circle>
        <line x1="12" y1="8" x2="12" y2="12"></line>
        <line x1="12" y1="16" x2="12.01" y2="16"></line>
      </svg>
    </div>
    <div class="error-message__content">
      <p class="error-message__text">{{ message }}</p>
      <button
        v-if="showRetry"
        class="error-message__retry"
        @click="emit('retry')"
      >
        Prøv igjen
      </button>
    </div>
  </div>
</template>

<style scoped>
  .error-message {
    display: flex;
    align-items: flex-start;
    gap: var(--spacing-sm);
    padding: var(--spacing-md);
    background: var(--color-danger-bg);
    border-left: 3px solid var(--color-danger);
    border-radius: var(--radius-md);
  }

  .error-message__icon {
    flex-shrink: 0;
    color: var(--color-danger);
  }

  .error-message__content {
    flex: 1;
  }

  .error-message__text {
    margin: 0;
    color: var(--color-danger-fg);
    font-size: var(--font-size-sm);
  }

  .error-message__retry {
    margin-top: var(--spacing-sm);
    padding: var(--button-padding-sm);
    font-size: var(--font-size-sm);
    background: var(--color-danger);
    color: var(--color-primary-foreground);
    border: none;
    border-radius: var(--radius-sm);
    cursor: pointer;
    transition: background-color var(--transition-fast), box-shadow var(--transition-fast);
  }

  .error-message__retry:hover {
    background: var(--color-danger-hover);
  }

  .error-message__retry:focus-visible {
    outline: 2px solid var(--color-focus);
    outline-offset: 2px;
  }
</style>