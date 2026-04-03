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
  gap: 12px;
  padding: 16px;
  background: var(--color-danger-bg);
  border-left: 3px solid var(--color-danger);
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
  color: var(--color-danger);
  font-size: 14px;
}

.error-message__retry {
  margin-top: 8px;
  padding: 6px 12px;
  font-size: 14px;
  background: var(--color-danger);
  color: white;
  border: none;
  cursor: pointer;
}

.error-message__retry:hover {
  background: #b30000;
}
</style>