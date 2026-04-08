<script setup lang="ts">
import { computed } from 'vue'
import { useIkMatData } from '../composables/useIkMatData'
import TemperatureCard from '../components/TemperatureCard.vue'

const { temperatureRecords, isTemperatureInRange } = useIkMatData()

const alerts = computed(() => temperatureRecords.filter((record) => !isTemperatureInRange(record)))
const okCount = computed(() => temperatureRecords.filter((record) => isTemperatureInRange(record)).length)
</script>

<template>
  <div class="temperature-page">
    <header class="page-header">
      <h1>Temperaturkontroll</h1>
      <p class="subtitle">Kontinuerlig overvåking av kjøl, frys og varmholding</p>
    </header>

    <section v-if="alerts.length > 0" class="alert-banner" role="alert">
      <p><strong>{{ alerts.length }}</strong> Målinger er utenfor grenseverdi og krever oppfølging.</p>
      <router-link :to="{ name: 'Deviations' }">Se avvik</router-link>
    </section>

    <section class="summary-grid" aria-label="Temperaturstatus">
      <article class="summary-card">
        <p>Registrerte målinger</p>
        <strong>{{ temperatureRecords.length }}</strong>
      </article>
      <article class="summary-card summary-card--good">
        <p>Innenfor grense</p>
        <strong>{{ okCount }}</strong>
      </article>
      <article class="summary-card summary-card--warn">
        <p>Avvik</p>
        <strong>{{ alerts.length }}</strong>
      </article>
    </section>

    <section class="card-grid" aria-label="Temperaturkort">
      <TemperatureCard v-for="record in temperatureRecords" :key="record.id" :record="record" />
    </section>
  </div>
</template>

<style scoped>
.temperature-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 1.25rem;
}

.page-header h1 {
  margin: 0;
  font-size: var(--font-size-2xl);
  color: var(--ik-mat-primary);
}

.subtitle {
  margin: 0.35rem 0 0;
  color: var(--color-gray-500);
  font-size: var(--font-size-sm);
}

.alert-banner {
  border: 1px solid color-mix(in srgb, var(--color-danger) 35%, var(--color-border));
  background: var(--color-danger-bg);
  color: color-mix(in srgb, var(--color-danger) 70%, var(--color-foreground));
  border-radius: var(--radius-md);
  padding: 0.75rem 0.9rem;
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.95rem;
}

.alert-banner p {
  margin: 0;
  font-size: var(--font-size-sm);
}

.alert-banner a {
  color: inherit;
  text-decoration: underline;
  font-size: var(--font-size-sm);
  white-space: nowrap;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr));
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.summary-card {
  border: 1px solid var(--color-border);
  background: var(--color-card);
  border-radius: var(--radius-md);
  padding: 0.8rem;
}

.summary-card p {
  margin: 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-xs);
}

.summary-card strong {
  display: block;
  margin-top: 0.35rem;
  color: var(--color-foreground);
  font-size: 1.45rem;
}

.summary-card--good {
  border-left: 0.25rem solid var(--color-success);
}

.summary-card--warn {
  border-left: 0.25rem solid var(--color-warning);
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(18rem, 1fr));
  gap: 0.9rem;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  padding: 0.2rem 0.45rem;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
}

.status-pill--good {
  color: var(--color-success);
  background: var(--color-success-bg);
  border-color: color-mix(in srgb, var(--color-success) 35%, var(--color-border));
}

.status-pill--danger {
  color: var(--color-danger);
  background: var(--color-danger-bg);
  border-color: color-mix(in srgb, var(--color-danger) 35%, var(--color-border));
}
</style>
