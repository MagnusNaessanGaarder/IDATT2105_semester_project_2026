<script setup lang="ts">
import alkoholData from '@/data/ik-alkohol.json'

interface LawSection {
  section: string
  description: string
}

interface LawItem {
  name: string
  type: string
  short: string
  description: string
  link: string
  last_updated_code: string
  sub_sections?: LawSection[]
  'sub-sections'?: LawSection[]
}

const laws = alkoholData.law_framework.laws as LawItem[]
const demands = alkoholData.law_framework.demands

const getSections = (law: LawItem) => law.sub_sections ?? law['sub-sections'] ?? []
</script>

<template>
  <div class="view-page">
    <header class="page-header">
      <h1>Regelverk</h1>
      <p class="subtitle">Alkoholloven og skjenkeforskriften</p>
    </header>

    <section class="laws-grid" aria-label="Lover og forskrifter">
      <article v-for="law in laws" :key="law.name" class="law-card">
        <header class="law-card__header">
          <p class="law-card__type">{{ law.type }}</p>
          <h2 class="law-card__name">{{ law.name }}</h2>
          <p class="law-card__short">{{ law.short }}</p>
        </header>
        <p class="law-card__description">{{ law.description }}</p>
        <p class="law-card__meta">Kode: {{ law.last_updated_code }}</p>
        <a class="law-card__link" :href="law.link" target="_blank" rel="noopener noreferrer">Apen lovtekst</a>
        <ul class="law-card__sections">
          <li v-for="section in getSections(law)" :key="`${law.name}-${section.section}`">
            <strong>{{ section.section }}</strong>: {{ section.description }}
          </li>
        </ul>
      </article>
    </section>

    <section class="demands-section" aria-label="Krav i praksis">
      <h2>Krav i praksis</h2>
      <div class="demands-grid">
        <article v-for="demand in demands" :key="demand.title" class="demand-card">
          <h3>{{ demand.title }}</h3>
          <ul>
            <li v-for="point in demand.bullet_points" :key="point">{{ point }}</li>
          </ul>
        </article>
      </div>
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

.laws-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(20rem, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.law-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1.25rem;
}

.law-card__type {
  margin: 0;
  font-size: var(--text-xs);
  text-transform: uppercase;
  color: var(--color-gray-600);
}

.law-card__name {
  margin: 0.25rem 0;
  font-size: var(--text-lg);
}

.law-card__short,
.law-card__description,
.law-card__meta {
  margin: 0.4rem 0;
  font-size: var(--text-sm);
  color: var(--color-gray-600);
}

.law-card__link {
  display: inline-block;
  margin: 0.4rem 0 0.6rem;
  color: var(--color-foreground);
  font-weight: 600;
}

.law-card__sections {
  margin: 0;
  padding-left: 1.25rem;
  display: grid;
  gap: 0.45rem;
  font-size: var(--text-sm);
}

.demands-section {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1.25rem;
}

.demands-section h2 {
  margin: 0 0 1rem;
}

.demands-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(14rem, 1fr));
  gap: 1rem;
}

.demand-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-accent);
  padding: 0.85rem;
}

.demand-card h3 {
  margin: 0 0 0.5rem;
  font-size: var(--text-base);
}

.demand-card ul {
  margin: 0;
  padding-left: 1.15rem;
  font-size: var(--text-sm);
  display: grid;
  gap: 0.35rem;
}
</style>