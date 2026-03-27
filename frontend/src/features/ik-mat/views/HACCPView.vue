<script setup lang="ts">
import ikMatData from '@/data/ik-mat.json'

const haccpPlan = ikMatData.haccp
</script>

<template>
  <div class="view-page">
    <header class="page-header">
      <h1>HACCP-plan</h1>
      <p class="subtitle">Kritiske kontrollpunkter basert på Codex Alimentarius</p>
    </header>

    <section class="plan-summary">
      <h2>{{ haccpPlan.plan_name }}</h2>
      <p>Versjon {{ haccpPlan.version }} - Opprettet {{ haccpPlan.created_date }} - Sist oppdatert {{ haccpPlan.last_updated }}</p>
    </section>

    <section class="ccp-grid" aria-label="Kritiske kontrollpunkter">
      <article v-for="point in haccpPlan.critical_control_points" :key="point.id" class="ccp-card">
        <header class="ccp-card__header">
          <p class="ccp-card__number">{{ point.number }}</p>
          <h3 class="ccp-card__name">{{ point.name }}</h3>
        </header>
        <p class="ccp-card__desc">{{ point.description }}</p>
        <p><strong>Farer:</strong> {{ point.hazards.join(', ') }}</p>
        <p><strong>Kritiske grenser:</strong> {{ point.critical_limits }}</p>
        <p><strong>Overvaking:</strong> {{ point.monitoring }}</p>
        <p><strong>Korrigerende tiltak:</strong> {{ point.corrective_actions }}</p>
        <p><strong>Verifisering:</strong> {{ point.verification }}</p>
        <p><strong>Ansvarlig:</strong> {{ point.responsible }}</p>
      </article>
    </section>

    <section class="supporting-docs">
      <h2>Stottedokumenter</h2>
      <ul>
        <li v-for="doc in haccpPlan.supporting_documents" :key="doc.id">
          <strong>{{ doc.name }}</strong>
          <span> - Oppdatert {{ doc.date_updated }} - {{ doc.description }}</span>
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.view-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-foreground);
  margin-bottom: 8px;
}

.subtitle {
  font-size: var(--font-size-base);
  color: var(--color-gray-500);
}

.plan-summary,
.supporting-docs {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1.25rem;
  margin-bottom: 1.5rem;
}

.plan-summary h2,
.supporting-docs h2 {
  margin: 0 0 0.75rem;
}

.plan-summary p {
  margin: 0;
  color: var(--color-gray-600);
}

.ccp-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(20rem, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.ccp-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1rem;
}

.ccp-card p {
  margin: 0.4rem 0;
  font-size: var(--text-sm);
}

.ccp-card__header {
  margin-bottom: 0.75rem;
}

.ccp-card__number {
  margin: 0;
  font-size: var(--text-xs);
  text-transform: uppercase;
  color: var(--color-gray-600);
}

.ccp-card__name {
  margin: 0.25rem 0 0;
  font-size: var(--text-base);
}

.ccp-card__desc {
  color: var(--color-gray-600);
}

.supporting-docs ul {
  margin: 0;
  padding-left: 1.25rem;
  display: grid;
  gap: 0.5rem;
}
</style>
