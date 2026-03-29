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
  gap: 0.5rem;
  font-family: inherit;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: background-color 0.15s;
}

.base-button--primary {
  background: var(--color-primary);
  color: var(--color-primary-foreground);
}

.base-button--primary:hover:not(:disabled) {
  background: #333;
}

.base-button--secondary {
  background: var(--color-gray-200);
  color: var(--color-foreground);
}

.base-button--secondary:hover:not(:disabled) {
  background: var(--color-gray-300);
}

.base-button--danger {
  background: var(--color-danger);
  color: white;
}

.base-button--danger:hover:not(:disabled) {
  background: #b30000;
}

.base-button--ghost {
  background: transparent;
  color: var(--color-foreground);
}

.base-button--ghost:hover:not(:disabled) {
  background: var(--color-gray-100);
}

.base-button--sm {
  padding: 0.375rem 0.75rem;
  font-size: 0.875rem;
}

.base-button--md {
  padding: 0.625rem 1rem;
  font-size: 1rem;
}

.base-button--lg {
  padding: 0.875rem 1.5rem;
  font-size: 1.125rem;
}

.base-button--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.base-button__spinner {
  width: 1rem;
  height: 1rem;
  border: 0.125rem solid currentColor;
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