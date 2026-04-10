<script setup lang="ts">
import { useAlkoholData } from '@/features/ik-alkohol/composables/useAlkoholData'

const { laws, demands, sectionsForLaw } = useAlkoholData()
</script>

<template>
  <div class="regulations-page">
    <header class="page-header">
      <h1>Regelverk</h1>
      <p class="subtitle">Lover, forskrifter og praktiske krav for ansvarlig alkoholservering</p>
    </header>

    <section class="laws-grid" aria-label="Lover og forskrifter">
      <article v-for="law in laws" :key="law.name" class="law-card">
        <header class="law-card__header">
          <div>
            <p class="law-card__type">{{ law.type }}</p>
            <h2 class="law-card__name">{{ law.name }}</h2>
            <p class="law-card__short">{{ law.short }}</p>
          </div>
          <a class="law-card__link" :href="law.link" target="_blank" rel="noopener noreferrer">Les på Lovdata</a>
        </header>

        <p class="law-card__description">{{ law.description }}</p>
        <p class="law-card__meta">Referanse: {{ law.last_updated_code }}</p>

        <ul class="law-card__sections">
          <li v-for="section in sectionsForLaw(law)" :key="`${law.name}-${section.section}`">
            <strong>{{ section.section }}</strong>: {{ section.description }}
          </li>
        </ul>
      </article>
    </section>

    <section class="demands-section" aria-label="Krav i praksis">
      <h2>Viktige krav for IK-Alkohol</h2>
      <div class="demands-grid">
        <article v-for="demand in demands" :key="demand.title" class="demand-card">
          <h3>{{ demand.title }}</h3>
          <ul>
            <li v-for="point in demand.bullet_points" :key="point">{{ point }}</li>
          </ul>
        </article>
      </div>
    </section>

    <section class="sanctions-card" aria-label="Konsekvenser ved brudd">
      <h2>Sanksjoner ved overtredelser</h2>
      <ul>
        <li>Advarsel fra kommunen</li>
        <li>Midlertidig stenging av virksomheten</li>
        <li>Inndragning av skjenkebevilling</li>
        <li>Boter til virksomheten eller ansvarlige personer</li>
        <li>Straff ved grove eller gjentatte overtredelser</li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.regulations-page {
  width: min(100%, 1200px);
  margin: 0 auto;
  display: grid;
  gap: var(--spacing-lg);
}

.page-header {
  display: grid;
  gap: var(--spacing-xs);
}

.page-header h1 {
  font-size: clamp(2rem, 2.5vw, var(--font-size-3xl));
  font-weight: var(--font-weight-bold);
  color: var(--color-brand-medium-violet);
  margin: 0;
}

.subtitle {
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
}

.laws-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(20rem, 1fr));
  gap: var(--spacing-md);
}

.law-card {
  background: var(--color-card);
  border: 1px solid color-mix(in srgb, var(--color-brand-medium-violet) 16%, var(--color-border));
  border-left: 4px solid var(--color-brand-medium-violet);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-sm);
}

.law-card__header {
  display: flex;
  justify-content: space-between;
  gap: var(--spacing-md);
  align-items: flex-start;
}

.law-card__type {
  margin: 0;
  font-size: 0.6875rem;
  text-transform: uppercase;
  color: var(--color-brand-medium-violet);
  letter-spacing: 0.08em;
  font-weight: var(--font-weight-bold);
}

.law-card__name {
  margin: 0.25rem 0;
  font-size: var(--font-size-lg);
}

.law-card__short,
.law-card__description,
.law-card__meta {
  margin: 0.4rem 0;
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
}

.law-card__link {
  color: var(--color-primary-foreground);
  background: var(--color-brand-medium-violet);
  border-radius: var(--radius-md);
  padding: 0.5rem 0.75rem;
  font-size: 0.6875rem;
  font-weight: var(--font-weight-bold);
  white-space: nowrap;
}

.law-card__sections {
  margin: 0;
  padding-left: 1.25rem;
  display: grid;
  gap: 0.45rem;
  font-size: var(--font-size-sm);
}

.demands-section {
  background: color-mix(in srgb, var(--color-brand-pale-lavender) 34%, var(--color-card));
  border: 1px solid color-mix(in srgb, var(--color-brand-medium-violet) 20%, var(--color-border));
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-sm);
}

.demands-section h2 {
  margin: 0 0 var(--spacing-md);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-brand-deep-violet);
}

.demands-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(14rem, 1fr));
  gap: var(--spacing-md);
}

.demand-card {
  border: 1px solid color-mix(in srgb, var(--color-brand-medium-violet) 28%, var(--color-border));
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--color-brand-soft-violet) 22%, var(--color-surface-raised));
  padding: var(--spacing-md);
}

.demand-card h3 {
  margin: 0 0 0.5rem;
  font-size: var(--font-size-base);
  color: var(--color-brand-deep-violet);
}

.demand-card ul {
  margin: 0;
  padding-left: 1.15rem;
  font-size: var(--font-size-sm);
  display: grid;
  gap: 0.35rem;
  color: var(--color-gray-800);
}

.sanctions-card {
  margin-top: var(--spacing-sm);
  border: 1px solid color-mix(in srgb, var(--color-brand-medium-violet) 26%, var(--color-border));
  background: color-mix(in srgb, var(--color-brand-pale-lavender) 30%, var(--color-surface-raised));
  border-radius: var(--radius-lg);
  padding: var(--spacing-md);
}

.sanctions-card h2 {
  margin: 0 0 0.5rem;
  font-size: var(--font-size-lg);
  color: var(--color-brand-deep-violet);
  font-weight: var(--font-weight-bold);
}

.sanctions-card ul {
  list-style: disc;
  padding-left: 1.25rem;
  display: grid;
  gap: 0.35rem;
}

.sanctions-card li {
  color: var(--color-gray-800);
  font-size: var(--font-size-sm);
}

@media (max-width: 48rem) {
  .law-card__header {
    flex-direction: column;
  }
}
</style>