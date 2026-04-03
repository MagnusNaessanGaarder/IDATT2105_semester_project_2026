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
  padding: 48px 24px;
  text-align: center;
}

.empty-state__icon {
  color: var(--color-gray-400);
  margin-bottom: 16px;
}

.empty-state__title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-foreground);
  margin: 0 0 8px 0;
}

.empty-state__description {
  font-size: 14px;
  color: var(--color-gray-500);
  margin: 0;
}
</style>