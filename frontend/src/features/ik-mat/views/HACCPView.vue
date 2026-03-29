<script setup lang="ts">
import { computed } from 'vue'
import { useIkMatData } from '../composables/useIkMatData'

const { haccpPlan, formatDate } = useIkMatData()

const ccpStatusSummary = computed(() => {
  const total = haccpPlan.critical_control_points.length
  const followUp = haccpPlan.critical_control_points.filter((point) => {
    return point.name.toLowerCase().includes('varmholding') || point.name.toLowerCase().includes('nedkjøling')
  }).length

  return {
    total,
    followUp,
    ok: total - followUp,
  }
})
</script>

<template>
  <div class="haccp-page">
    <header class="page-header">
      <h1>HACCP-plan</h1>
      <p class="subtitle">{{ haccpPlan.plan_name }}</p>
      <p class="meta">Versjon {{ haccpPlan.version }} · oppdatert {{ formatDate(haccpPlan.last_updated) }}</p>
    </header>

    <section class="summary-grid" aria-label="HACCP sammendrag">
      <article class="summary-card summary-card--good">
        <p>CCP OK</p>
        <strong>{{ ccpStatusSummary.ok }}</strong>
      </article>
      <article class="summary-card summary-card--warn">
        <p>Krever oppfølging</p>
        <strong>{{ ccpStatusSummary.followUp }}</strong>
      </article>
      <article class="summary-card summary-card--info">
        <p>Totale CCP</p>
        <strong>{{ ccpStatusSummary.total }}</strong>
      </article>
    </section>

    <section class="table-card" aria-label="Kritiske kontrollpunkter">
      <h2>Kritiske kontrollpunkter</h2>
      <table>
        <thead>
          <tr>
            <th>CCP</th>
            <th>Prosesstrinn</th>
            <th>Farer</th>
            <th>Kritisk grense</th>
            <th>Overvåking</th>
            <th>Korrigerende tiltak</th>
            <th>Ansvarlig</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="point in haccpPlan.critical_control_points" :key="point.id">
            <td>{{ point.number }}</td>
            <td>{{ point.name }}</td>
            <td>{{ point.hazards.join(', ') }}</td>
            <td>{{ point.critical_limits }}</td>
            <td>{{ point.monitoring }}</td>
            <td>{{ point.corrective_actions }}</td>
            <td>{{ point.responsible }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="docs-card" aria-label="Støttedokumenter">
      <h2>Støttedokumenter</h2>
      <ul>
        <li v-for="doc in haccpPlan.supporting_documents" :key="doc.id">
          <p>{{ doc.name }}</p>
          <span>Oppdatert {{ formatDate(doc.date_updated) }} · {{ doc.description }}</span>
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.haccp-page {
  max-width: 75rem;
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
  color: var(--color-foreground);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
}

.meta {
  margin: 0.2rem 0 0;
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr));
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.summary-card {
  border: 0.0625rem solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
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

.summary-card--info {
  border-left: 0.25rem solid var(--color-info);
}

.table-card {
  border: 0.0625rem solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  padding: 0.7rem;
  overflow-x: auto;
  margin-bottom: 0.95rem;
}

.table-card h2,
.docs-card h2 {
  margin: 0 0 0.65rem;
  font-size: var(--font-size-base);
  color: var(--color-foreground);
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  text-align: left;
  padding: 0.55rem;
  border-bottom: 0.0625rem solid var(--color-border);
  font-size: var(--font-size-sm);
  vertical-align: top;
}

th {
  font-size: var(--font-size-xs);
  color: var(--color-gray-600);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

td {
  color: var(--color-foreground);
}

.docs-card {
  border: 0.0625rem solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  padding: 0.8rem;
}

.docs-card ul {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 0.6rem;
}

.docs-card li {
  border: 0.0625rem solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0.65rem;
  background: color-mix(in srgb, var(--color-accent) 45%, var(--color-card));
}

.docs-card p {
  margin: 0;
  color: var(--color-foreground);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
}

.docs-card span {
  display: block;
  margin-top: 0.2rem;
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
}
</style>
