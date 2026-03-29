<script setup lang="ts">
import { computed, ref } from 'vue'
import { useFellesData } from '../composables/useFellesData'

const data = useFellesData()

const tabs = ['all', 'monthly', 'deviation', 'haccp'] as const
const activeTab = ref<(typeof tabs)[number]>('all')
const query = ref('')

const filteredReports = computed(() => {
  return data.reports.filter((report) => {
    const matchesTab = activeTab.value === 'all' || report.type === activeTab.value
    const search = query.value.trim().toLowerCase()
    const matchesSearch =
      search.length === 0 ||
      report.title.toLowerCase().includes(search) ||
      report.created_by.toLowerCase().includes(search)

    return matchesTab && matchesSearch
  })
})

const monthlyCount = computed(() => data.reports.filter((report) => report.type === 'monthly').length)
const finalizedCount = computed(() => data.reports.filter((report) => report.status === 'finalized').length)
const draftCount = computed(() => data.reports.filter((report) => report.status === 'draft').length)

const tabLabel = (tab: (typeof tabs)[number]): string => {
  const labels: Record<(typeof tabs)[number], string> = {
    all: 'Alle',
    monthly: 'Månedlig',
    deviation: 'Avvik',
    haccp: 'HACCP',
  }

  return labels[tab]
}
</script>

<template>
  <div class="view-page reports-view">
    <header class="page-header">
      <div>
        <h1>Rapporter</h1>
        <p class="subtitle">Månedlige kontroller og avviksrapporter samlet på ett sted</p>
      </div>
      <div class="header-actions">
        <button type="button" class="header-btn header-btn--ghost">Eksporter</button>
        <button type="button" class="header-btn">Generer rapport</button>
      </div>
    </header>

    <section class="reports-stats" aria-label="Rapportstatistikk">
      <article class="reports-stat">
        <strong>{{ data.reports.length }}</strong>
        <span>Totalt</span>
      </article>
      <article class="reports-stat">
        <strong>{{ finalizedCount }}</strong>
        <span>Ferdige</span>
      </article>
      <article class="reports-stat">
        <strong>{{ draftCount }}</strong>
        <span>Pågår</span>
      </article>
      <article class="reports-stat">
        <strong>{{ monthlyCount }}</strong>
        <span>Månedlige</span>
      </article>
    </section>

    <section class="reports-toolbar" aria-label="Filtrering">
      <div class="tab-list" role="tablist" aria-label="Rapporttyper">
        <button
          v-for="tab in tabs"
          :key="tab"
          type="button"
          class="tab"
          :class="{ 'tab--active': activeTab === tab }"
          @click="activeTab = tab"
        >
          {{ tabLabel(tab) }}
        </button>
      </div>
      <input v-model="query" class="search" type="search" placeholder="Søk etter rapport eller ansvarlig" />
    </section>

    <section class="reports-list" aria-label="Rapportliste">
      <article v-for="report in filteredReports" :key="report.id" class="report-item">
        <div class="report-item__main">
          <p class="report-item__title">{{ report.title }}</p>
          <div class="report-item__meta">
            <span class="pill">{{ data.reportTypeLabel(report.type) }}</span>
            <span class="pill" :class="`pill--${data.reportStatusTone(report.status)}`">{{ data.reportStatusLabel(report.status) }}</span>
            <span class="report-item__period">{{ report.period }}</span>
          </div>
          <p class="report-item__details">
            Opprettet av {{ report.created_by }} · {{ data.formatDate(report.created_date) }}
          </p>
        </div>
        <div class="report-item__actions">
          <button type="button" class="action-btn action-btn--light">Vis</button>
          <button type="button" class="action-btn" :disabled="!report.file_url">Last ned</button>
        </div>
      </article>
    </section>

    <section v-if="filteredReports.length === 0" class="empty-state">
      <p>Ingen rapporter matcher filtreringen.</p>
    </section>
  </div>
</template>

<style scoped>
.reports-view {
  display: grid;
  gap: 1rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 1rem;
}

.page-header h1 {
  margin: 0;
  font-size: var(--font-size-3xl);
  font-weight: 700;
  letter-spacing: -0.015em;
}

.subtitle {
  margin-top: 0.4rem;
  color: var(--color-gray-500);
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.header-btn {
  min-height: 2.65rem;
  padding: 0.45rem 0.85rem;
  border-radius: var(--radius-md);
  background: var(--color-foreground);
  color: var(--color-background);
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.header-btn--ghost {
  background: #fff;
  border: 1px solid var(--color-border);
  color: var(--color-gray-700);
}

.reports-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.75rem;
}

.reports-stat {
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  text-align: center;
  padding: 0.8rem;
}

.reports-stat strong {
  color: var(--color-gray-900);
  font-size: var(--font-size-xl);
}

.reports-stat span {
  display: block;
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
  margin-top: 0.2rem;
}

.reports-toolbar {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #fff;
  padding: 0.75rem;
  display: grid;
  gap: 0.75rem;
}

.tab-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.tab {
  min-height: 2.15rem;
  padding: 0.35rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #fff;
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
}

.tab--active {
  background: var(--color-foreground);
  color: #fff;
  border-color: var(--color-foreground);
}

.search {
  width: 100%;
  min-height: 2.6rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0 0.85rem;
  background: var(--color-gray-50);
}

.reports-list {
  display: grid;
  gap: 0.55rem;
}

.report-item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #fff;
  padding: 0.85rem;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.report-item__title {
  margin: 0;
  color: var(--color-gray-900);
  font-size: var(--font-size-base);
  font-weight: 600;
}

.report-item__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.35rem;
  margin-top: 0.35rem;
}

.pill {
  font-size: var(--font-size-xs);
  border-radius: 999px;
  padding: 0.2rem 0.55rem;
  background: var(--color-gray-100);
  color: var(--color-gray-700);
  font-weight: 600;
}

.pill--green {
  background: var(--color-success-bg);
  color: var(--color-success);
}

.pill--amber {
  background: var(--color-warning-bg);
  color: var(--color-warning);
}

.report-item__period {
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
}

.report-item__details {
  margin-top: 0.35rem;
  font-size: var(--font-size-sm);
  color: var(--color-gray-500);
}

.report-item__actions {
  display: flex;
  gap: 0.5rem;
}

.action-btn {
  min-height: 2.2rem;
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-md);
  background: var(--color-foreground);
  color: var(--color-background);
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.action-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.action-btn--light {
  background: #fff;
  border: 1px solid var(--color-border);
  color: var(--color-gray-700);
}

.empty-state {
  text-align: center;
  padding: 2rem;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  color: var(--color-gray-600);
}

@media (max-width: 48rem) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .reports-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .report-item {
    flex-direction: column;
  }

  .report-item__actions {
    width: 100%;
  }

  .action-btn {
    flex: 1;
  }

  .header-actions {
    width: 100%;
  }

  .header-btn {
    flex: 1;
  }

  .reports-list {
    grid-template-columns: 1fr;
  }
}
</style>