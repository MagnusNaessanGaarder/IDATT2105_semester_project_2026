<!--
  BaseButton - Gjenbrukbar knapp-komponent
  
  Brukes for alle knapper i applikasjonen.
  Støtter varianter: primary, secondary, danger, ghost
  Støtter størrelser: sm, md, lg
  Har innebygd loading-tilstand og disabled-tilstand
  
  Eksempel:
  <BaseButton variant="primary" @click="handleSave">Lagre</BaseButton>
  <BaseButton variant="danger" :loading="isDeleting">Slett</BaseButton>
-->
<script setup lang="ts">
interface Props {
  type?: 'button' | 'submit' | 'reset'
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost'
  size?: 'sm' | 'md' | 'lg'
  disabled?: boolean
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  type: 'button',
  variant: 'primary',
  size: 'md',
  disabled: false,
  loading: false,
})

const emit = defineEmits<{
  click: []
}>()

const handleClick = () => {
  if (!props.disabled && !props.loading) {
    emit('click')
  }
}
</script>

<template>
  <button
    :type="type"
    class="base-button"
    :class="[
      `base-button--${variant}`,
      `base-button--${size}`,
      { 'base-button--disabled': disabled || loading },
    ]"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <span v-if="loading" class="base-button__spinner"></span>
    <slot />
  </button>
</template>

<style scoped>
.base-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  font-family: var(--font-family-ui);
  font-weight: var(--font-weight-semibold);
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  min-height: var(--touch-target);
  cursor: pointer;
  transform: translateY(0);
  transition: background-color var(--transition-fast), color var(--transition-fast), transform var(--transition-fast), box-shadow var(--transition-fast), border-color var(--transition-fast);
}

.base-button--primary {
  background: var(--color-primary);
  color: var(--color-primary-foreground);
  box-shadow: var(--shadow-sm);
  border-color: color-mix(in srgb, var(--color-primary) 40%, black);
}

.base-button--primary:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.base-button--primary:active:not(:disabled) {
  background: var(--color-primary-active);
}

.base-button--secondary {
  background: var(--color-accent);
  border-color: var(--color-border);
  color: var(--color-foreground);
}

.base-button--secondary:hover:not(:disabled) {
  background: var(--color-accent-hover);
}

.base-button--danger {
  background: var(--color-danger);
  color: var(--color-primary-foreground);
}

.base-button--danger:hover:not(:disabled) {
  background: var(--color-danger-hover);
}

.base-button--ghost {
  background: transparent;
  color: var(--color-gray-700);
}

.base-button--ghost:hover:not(:disabled) {
  color: var(--color-foreground);
  background: var(--color-accent);
}

.base-button--sm {
  padding: var(--button-padding-sm);
  font-size: var(--font-size-sm);
}

.base-button--md {
  padding: var(--button-padding-md);
  font-size: var(--font-size-base);
}

.base-button--lg {
  padding: var(--button-padding-lg);
  font-size: var(--font-size-lg);
}

.base-button--disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

.base-button:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
  box-shadow: var(--shadow-focus);
}

.base-button:active:not(:disabled) {
  transform: scale(0.98);
}

.base-button__spinner {
  width: 16px;
  height: 16px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>