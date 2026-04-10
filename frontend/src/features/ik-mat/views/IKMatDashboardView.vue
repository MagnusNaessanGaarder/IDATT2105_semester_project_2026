<script setup lang="ts">
import { computed } from 'vue'
import { useIkMatData } from '../composables/useIkMatData'

const {
  dashboardStats,
  temperatureRecords,
  deviations,
  isTemperatureInRange,
  formatDate,
  isLoading,
  error,
  reload,
} = useIkMatData()

const openDeviations = computed(() => deviations.filter((item) => item.status !== 'resolved'))

const temperatureAlerts = computed(() => {
  return temperatureRecords.filter((record) => !isTemperatureInRange(record))
})

const cardTone = (color: 'success' | 'warning' | 'info') => {
  if (color === 'success') {
    return 'stat-card--success'
  }

  if (color === 'warning') {
    return 'stat-card--warning'
  }

  return 'stat-card--info'
}
</script>

<template>
  <div class="view-page ik-mat-dashboard">
    <header class="page-header">
      <h1>Ik-mat</h1>
      <p class="subtitle">Everest Sushi &amp; Fusion - internkontroll for matsikkerhet og hygiene</p>
    </header>

    <section class="stats-grid" aria-label="Nøkkeltall for IK-MAT">
      <article v-for="stat in dashboardStats" :key="stat.label" class="stat-card" :class="cardTone(stat.color)">
        <p class="stat-card__label">{{ stat.label }}</p>
        <p class="stat-card__value">
          {{ stat.value }}<span v-if="stat.unit" class="stat-card__unit">{{ stat.unit }}</span>
        </p>
      </article>
    </section>

    <section v-if="error" class="app-surface ik-mat-dashboard__panel" aria-label="API-feil">
      <header class="ik-mat-dashboard__panel-header">
        <h2>Kunne ikke hente IK-MAT data</h2>
        <button type="button" class="status-chip status-chip--warn" @click="reload">Prøv igjen</button>
      </header>
      <p class="item-row__meta">{{ error }}</p>
    </section>

    <section v-else-if="isLoading" class="app-surface ik-mat-dashboard__panel" aria-label="Laster IK-MAT data">
      <p class="item-row__meta">Laster IK-MAT data...</p>
    </section>

    <section class="quick-actions" aria-label="Snarveier">
      <router-link :to="{ name: 'Checklists' }" class="action-card">
        <p class="action-card__eyebrow">Daglig drift</p>
        <h2>Sjekklister</h2>
        <p>Følg daglige, ukentlige og manedlige kontroller med tydelig progresjon.</p>
      </router-link>
      <router-link :to="{ name: 'Temperature' }" class="action-card">
        <p class="action-card__eyebrow">Overvåking</p>
        <h2>Temperatur</h2>
        <p>Hold oversikt over kjøle- og frysesoner med avvik i sanntid.</p>
      </router-link>
      <router-link :to="{ name: 'Deviations' }" class="action-card">
        <p class="action-card__eyebrow">Oppfølging</p>
        <h2>Avvik</h2>
        <p>Prioriter åpne hendelser og dokumenter korrigerende tiltak.</p>
      </router-link>
      <router-link :to="{ name: 'HACCP' }" class="action-card">
        <p class="action-card__eyebrow">Kritiske punkter</p>
        <h2>HACCP-plan</h2>
        <p>Se kritiske kontrollpunkter, grenser og ansvar fordelt i teamet.</p>
      </router-link>
    </section>

    <section class="details-grid" aria-label="Detaljoversikt">
      <article class="app-surface ik-mat-dashboard__panel">
        <header class="ik-mat-dashboard__panel-header">
          <h2>Operative varsler</h2>
          <span class="status-chip" :class="temperatureAlerts.length > 0 ? 'status-chip--danger' : 'status-chip--good'">
            {{ temperatureAlerts.length }} temperaturavvik
          </span>
        </header>

        <ul class="item-list">
          <li v-for="record in temperatureRecords.slice(0, 4)" :key="record.id" class="item-row">
            <div>
              <p class="item-row__title">{{ record.location }}</p>
              <p class="item-row__meta">{{ record.temperature_c }}°C · grense {{ record.min_temp }} til {{ record.max_temp }}°C</p>
            </div>
            <span class="status-chip" :class="isTemperatureInRange(record) ? 'status-chip--good' : 'status-chip--danger'">
              {{ isTemperatureInRange(record) ? 'OK' : 'Avvik' }}
            </span>
          </li>
        </ul>
      </article>

      <article class="app-surface ik-mat-dashboard__panel">
        <header class="ik-mat-dashboard__panel-header">
          <h2>Åpne avvik</h2>
          <span class="status-chip" :class="openDeviations.length > 0 ? 'status-chip--warn' : 'status-chip--good'">
            {{ openDeviations.length }} aktive
          </span>
        </header>

        <ul class="item-list">
          <li v-for="deviation in openDeviations" :key="deviation.id" class="item-row">
            <div>
              <p class="item-row__title">{{ deviation.title }}</p>
              <p class="item-row__meta">{{ deviation.location }} · meldt {{ formatDate(deviation.reported_date) }} kl. {{ deviation.reported_time }}</p>
            </div>
            <span class="status-chip" :class="deviation.severity === 'high' ? 'status-chip--danger' : 'status-chip--warn'">
              {{ deviation.severity === 'high' ? 'Høy' : 'Medium' }}
            </span>
          </li>
        </ul>
      </article>
    </section>
  </div>
</template>

<style scoped>
.ik-mat-dashboard {
  display: grid;
  gap: var(--spacing-lg);
}

.ik-mat-dashboard__panel {
  padding: 1rem;
}

.ik-mat-dashboard__panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.subtitle {
  margin: 0.4rem 0 0;
  color: var(--color-gray-500);
  font-size: var(--font-size-sm);
}

.ik-mat-dashboard__panel-header h2 {
  margin: 0;
  font-size: var(--font-size-base);
  color: var(--color-foreground);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr));
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.stat-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 7rem;
  border: none;
  border-radius: var(--radius-lg);
  padding: 1rem 1.1rem;
  box-shadow: var(--shadow-sm);
  color: var(--color-primary-foreground);
  background: var(--color-secondary);
}

.stat-card--success {
  background: var(--color-primary);
}

.stat-card--warning {
  background: var(--color-cta);
  color: var(--color-cta-foreground);
}

.stat-card--info {
  background: var(--color-secondary);
}

.stat-card__label {
  margin: 0;
  color: color-mix(in srgb, currentColor 82%, transparent);
  font-size: var(--font-size-xs);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-weight: var(--font-weight-semibold);
}

.stat-card__value {
  margin: 0.45rem 0 0;
  color: currentColor;
  font-size: 1.6rem;
  font-weight: var(--font-weight-bold);
}

.stat-card__unit {
  margin-left: 0.2rem;
  font-size: var(--font-size-sm);
  color: color-mix(in srgb, currentColor 82%, transparent);
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: var(--spacing-md);
  margin-bottom: 0;
}

.action-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 1rem;
  box-shadow: var(--shadow-sm);
  text-decoration: none;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast), border-color var(--transition-fast), background-color var(--transition-fast);
}

.action-card:hover {
  border-color: var(--ik-mat-primary);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.action-card__eyebrow {
  margin: 0;
  color: var(--ik-mat-primary);
  font-size: var(--font-size-xs);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-weight: var(--font-weight-semibold);
}

.action-card h2 {
  margin: 6px 0;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-bold);
  color: var(--color-foreground);
}

.action-card p {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
}

.details-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--spacing-md);
}

.item-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 0.55rem;
}

.item-row {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: center;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.8rem;
  background: color-mix(in srgb, var(--color-accent) 40%, var(--color-card));
}

.item-row__title {
  margin: 0;
  color: var(--color-foreground);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
}

.item-row__meta {
  margin: 0.2rem 0 0;
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
}

.status-chip {
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  padding: 0.25rem 0.5rem;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  white-space: nowrap;
}

.status-chip--good {
  color: var(--color-success);
  background: var(--color-success-bg);
  border-color: color-mix(in srgb, var(--color-success) 30%, var(--color-border));
}

.status-chip--warn {
  color: var(--color-warning);
  background: var(--color-warning-bg);
  border-color: color-mix(in srgb, var(--color-warning) 35%, var(--color-border));
}

.status-chip--danger {
  color: var(--color-danger);
  background: var(--color-danger-bg);
  border-color: color-mix(in srgb, var(--color-danger) 35%, var(--color-border));
}

@media (max-width: 56rem) {
  .details-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 47.99rem) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
