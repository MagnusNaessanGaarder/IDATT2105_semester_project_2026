<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import fellesData from '@/data/felles.json'

const authStore = useAuthStore()

const user = computed(() => authStore.user)
const stats = fellesData.dashboard.stats
const quickActions = fellesData.dashboard.quick_actions

const getWelcomeMessage = () => {
  const hour = new Date().getHours()
  if (hour < 12) return 'God morgen'
  if (hour < 18) return 'God ettermiddag'
  return 'God kveld'
}
</script>

<template>
  <div class="dashboard">
    <header class="dashboard__header">
      <h1 class="dashboard__title">{{ getWelcomeMessage() }}, {{ user?.name?.split(' ')[0] || 'Bruker' }}</h1>
      <p class="dashboard__subtitle">Her er en oversikt over ditt system</p>
    </header>

    <div class="dashboard__stats">
      <div v-for="stat in stats" :key="stat.label" class="stat-card">
        <div class="stat-card__content">
          <p class="stat-card__label">{{ stat.label }}</p>
          <div class="stat-card__value-group">
            <span class="stat-card__value">{{ stat.value }}</span>
            <span v-if="stat.unit" class="stat-card__unit">{{ stat.unit }}</span>
          </div>
          <span class="stat-card__trend" :class="`stat-card__trend--${stat.trend}`">
            {{ stat.trend === 'up' ? '↑' : stat.trend === 'down' ? '↓' : '→' }}
          </span>
        </div>
      </div>
    </div>

    <section class="quick-actions" aria-label="Hurtighandlinger">
      <h2 class="quick-actions__title">Hurtighandlinger</h2>
      <div class="quick-actions__grid">
        <router-link v-for="action in quickActions" :key="action.title" :to="{ name: action.route }" class="quick-action-card">
          <p class="quick-action-card__icon">{{ action.icon }}</p>
          <p class="quick-action-card__title">{{ action.title }}</p>
        </router-link>
      </div>
    </section>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}

.dashboard__header {
  margin-bottom: 2.5rem;
}

.dashboard__title {
  margin: 0;
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--color-foreground);
  margin-bottom: 0.5rem;
}

.dashboard__subtitle {
  margin: 0;
  font-size: var(--text-base);
  color: var(--color-gray-600);
}

.dashboard__stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.stat-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1.5rem;
  transition: all var(--transition-base);
}

.stat-card:hover {
  box-shadow: var(--shadow-md);
}

.stat-card__content {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.stat-card__label {
  margin: 0;
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-gray-600);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.stat-card__value-group {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
}

.stat-card__value {
  font-size: 2rem;
  font-weight: 700;
  color: var(--color-foreground);
}

.stat-card__unit {
  font-size: var(--text-sm);
  color: var(--color-gray-600);
}

.stat-card__trend {
  font-size: 1.25rem;
  color: var(--color-gray-600);
}

.stat-card__trend--up {
  color: #10b981;
}

.stat-card__trend--down {
  color: #ef4444;
}

.stat-card__trend--neutral {
  color: var(--color-gray-600);
}

.quick-actions {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1.25rem;
}

.quick-actions__title {
  margin: 0 0 1rem;
  font-size: var(--text-lg);
}

.quick-actions__grid {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: repeat(auto-fit, minmax(12rem, 1fr));
}

.quick-action-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-accent);
  padding: 0.85rem;
  text-decoration: none;
  color: inherit;
  transition: box-shadow var(--transition-fast);
}

.quick-action-card:hover {
  box-shadow: var(--shadow-sm);
}

.quick-action-card__icon {
  margin: 0;
  font-size: var(--text-xs);
  color: var(--color-gray-600);
}

.quick-action-card__title {
  margin: 0.35rem 0 0;
  font-weight: 600;
}

@media (max-width: 48rem) {
  .dashboard__stats {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
