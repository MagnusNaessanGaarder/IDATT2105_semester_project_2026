<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const user = computed(() => authStore.user)

const getWelcomeMessage = () => {
  const hour = new Date().getHours()
  if (hour < 12) return 'God morgen'
  if (hour < 18) return 'God ettermiddag'
  return 'God kveld'
}
</script>

<template>
  <div class="dashboard">
    <header class="dashboard-header">
      <h1>{{ getWelcomeMessage() }}, {{ user?.name?.split(' ')[0] || 'Bruker' }}</h1>
    </header>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 1200px;
  margin: 0 auto;
}

.dashboard-header {
  margin-bottom: 32px;
}

.dashboard-header h1 {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-foreground);
  margin-bottom: 8px;
}

.dashboard-subtitle {
  font-size: var(--font-size-base);
  color: var(--color-gray-500);
}
</style>
