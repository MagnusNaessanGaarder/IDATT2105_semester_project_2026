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
  gap: 8px;
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
  padding: 6px 12px;
  font-size: 14px;
}

.base-button--md {
  padding: 10px 16px;
  font-size: 16px;
}

.base-button--lg {
  padding: 14px 24px;
  font-size: 18px;
}

.base-button--disabled {
  opacity: 0.5;
  cursor: not-allowed;
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