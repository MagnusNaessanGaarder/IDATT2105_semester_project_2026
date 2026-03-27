<script setup lang="ts">
import ikMatData from '@/data/ik-mat.json'

const stats = ikMatData.dashboard.stats
const recentChecks = ikMatData.dashboard.recent_checks
</script>

<template>
  <div class="ikmat-dashboard">
    <header class="page-header">
      <h1>IK-Mat Dashboard</h1>
      <p class="subtitle">Matsikkerhet og hygienekontroll</p>
    </header>

    <section class="stats-grid" aria-label="Nøkkeltall for IK-Mat">
      <article v-for="stat in stats" :key="stat.label" class="stat-card">
        <p class="stat-card__label">{{ stat.label }}</p>
        <p class="stat-card__value">
          {{ stat.value }}<span v-if="stat.unit" class="stat-card__unit">{{ stat.unit }}</span>
        </p>
      </article>
    </section>
    
    <div class="dashboard-grid">
      <div class="card">
        <h2>Sjekklister</h2>
        <p>Oversikt over daglige, ukentlige og månedlige sjekklister</p>
        <router-link :to="{ name: 'Checklists' }" class="btn">
          Se sjekklister
        </router-link>
      </div>
      
      <div class="card">
        <h2>Temperatur</h2>
        <p>Registrering og overvåking av matlagringstemperaturer</p>
        <router-link :to="{ name: 'Temperature' }" class="btn">
          Registrer temperatur
        </router-link>
      </div>
      
      <div class="card">
        <h2>Avvik</h2>
        <p>Rapportering og oppfølging av avvik fra standarder</p>
        <router-link :to="{ name: 'Deviations' }" class="btn">
          Se avvik
        </router-link>
      </div>
      
      <div class="card">
        <h2>HACCP-plan</h2>
        <p>Oversikt over kritiske kontrollpunkter</p>
        <router-link :to="{ name: 'HACCP' }" class="btn">
          Se HACCP-plan
        </router-link>
      </div>
    </div>

    <section class="recent-section" aria-label="Siste gjennomførte kontroller">
      <h2 class="recent-title">Siste kontroller</h2>
      <ul class="recent-list">
        <li v-for="check in recentChecks" :key="check.id" class="recent-item">
          <div>
            <p class="recent-item__name">{{ check.name }}</p>
            <p class="recent-item__meta">{{ check.completed_by }} - {{ check.completed_date }} kl. {{ check.completed_time }}</p>
          </div>
          <span class="recent-item__status">{{ check.status === 'completed' ? 'Fullfort' : check.status }}</span>
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.ikmat-dashboard {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--ikmat-primary);
  margin-bottom: 8px;
}

.subtitle {
  font-size: var(--font-size-base);
  color: var(--color-gray-500);
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
  margin-bottom: 2rem;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(12rem, 1fr));
  gap: 1rem;
  margin-bottom: 2rem;
}

.stat-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1rem;
}

.stat-card__label {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--color-gray-600);
}

.stat-card__value {
  margin: 0.5rem 0 0;
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--color-foreground);
}

.stat-card__unit {
  margin-left: 0.25rem;
  font-size: var(--text-sm);
  color: var(--color-gray-600);
}

.recent-section {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1.25rem;
}

.recent-title {
  margin: 0 0 1rem;
  font-size: var(--text-lg);
}

.recent-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.75rem;
}

.recent-item {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: center;
  padding: 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-accent);
}

.recent-item__name {
  margin: 0;
  font-weight: 600;
}

.recent-item__meta {
  margin: 0.25rem 0 0;
  font-size: var(--text-sm);
  color: var(--color-gray-600);
}

.recent-item__status {
  font-size: var(--text-xs);
  font-weight: 700;
  padding: 0.25rem 0.5rem;
  border-radius: var(--radius-sm);
  color: var(--color-background);
  background: #15803d;
}

.card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 24px;
  box-shadow: var(--shadow-sm);
}

.card h2 {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-foreground);
  margin-bottom: 8px;
}

.card p {
  font-size: var(--font-size-base);
  color: var(--color-gray-500);
  margin-bottom: 16px;
}

.btn {
  display: inline-flex;
  align-items: center;
  padding: 10px 16px;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--ikmat-primary);
  background: var(--ikmat-bg);
  border: 1px solid var(--ikmat-primary);
  border-radius: var(--radius-md);
  text-decoration: none;
  transition: all var(--transition-fast);
}

.btn:hover {
  background: var(--ikmat-primary);
  color: white;
  text-decoration: none;
}
</style>