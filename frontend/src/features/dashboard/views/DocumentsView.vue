<script setup lang="ts">
import { computed, ref } from 'vue'
import { useFellesData } from '../composables/useFellesData'

const data = useFellesData()
const selectedCategory = ref<string>('all')
const query = ref('')

const categories = computed(() => {
  const unique = new Set(data.documents.map((doc) => doc.category))
  return Array.from(unique)
})

const filteredDocuments = computed(() => {
  return data.documents.filter((doc) => {
    const matchesCategory = selectedCategory.value === 'all' || doc.category === selectedCategory.value
    const search = query.value.trim().toLowerCase()
    const matchesQuery =
      search.length === 0 ||
      doc.name.toLowerCase().includes(search) ||
      doc.description.toLowerCase().includes(search) ||
      doc.uploaded_by.toLowerCase().includes(search)

    return matchesCategory && matchesQuery
  })
})

const storageUsage = computed(() => {
  const totalMb = data.documents.reduce((sum, doc) => {
    const numeric = Number.parseFloat(doc.size)
    return Number.isFinite(numeric) ? sum + numeric : sum
  }, 0)

  return `${totalMb.toFixed(1)} MB`
})

const latestUpload = computed(() => {
  return data.sortedDocuments[0] ? data.formatDate(data.sortedDocuments[0].uploaded_date) : '-'
})
</script>

<template>
  <div class="view-page documents-view">
    <header class="page-header">
      <div>
        <h1>Dokumenter</h1>
        <p class="subtitle">Sentralisert lagring av retningslinjer, prosedyrer og opplæringsfiler</p>
      </div>
      <button class="upload-btn" type="button">Last opp dokument</button>
    </header>

    <section class="documents-stats" aria-label="Dokumentstatistikk">
      <article class="documents-stat">
        <strong>{{ categories.length }}</strong>
        <span>Kategorier</span>
      </article>
      <article class="documents-stat">
        <strong>{{ data.documents.length }}</strong>
        <span>Dokumenter totalt</span>
      </article>
      <article class="documents-stat">
        <strong>{{ latestUpload }}</strong>
        <span>Sist oppdatert</span>
      </article>
      <article class="documents-stat">
        <strong>{{ storageUsage }}</strong>
        <span>Lagring brukt</span>
      </article>
    </section>

    <section class="documents-toolbar" aria-label="Dokumentfiltre">
      <input v-model="query" type="search" placeholder="Søk i dokumenter" class="search-input" />
      <div class="category-tabs">
        <button class="category-tab" :class="{ 'category-tab--active': selectedCategory === 'all' }" @click="selectedCategory = 'all'">
          Alle
        </button>
        <button
          v-for="category in categories"
          :key="category"
          class="category-tab"
          :class="{ 'category-tab--active': selectedCategory === category }"
          @click="selectedCategory = category"
        >
          {{ category }}
        </button>
      </div>
    </section>

    <section aria-label="Kategorier" class="category-grid">
      <article v-for="category in categories" :key="category" class="category-card">
        <h2>{{ category }}</h2>
        <p>{{ data.documents.filter((doc) => doc.category === category).length }} filer</p>
      </article>
    </section>

    <section class="table-wrapper" aria-label="Dokumentliste">
      <table>
        <thead>
          <tr>
            <th>Filnavn</th>
            <th>Kategori</th>
            <th>Størrelse</th>
            <th>Opplastet av</th>
            <th>Dato</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="document in filteredDocuments" :key="document.id">
            <td>
              <p class="table-title">{{ document.name }}</p>
              <p class="table-subtitle">v{{ document.version }} · {{ document.file_type }}</p>
            </td>
            <td>{{ document.category }}</td>
            <td>{{ document.size }}</td>
            <td>{{ document.uploaded_by }}</td>
            <td>{{ data.formatDate(document.uploaded_date) }}</td>
            <td>
              <span class="status-pill" :class="{ 'status-pill--active': document.status === 'active' }">
                {{ document.status === 'active' ? 'Aktiv' : 'Arkivert' }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <section v-if="filteredDocuments.length === 0" class="empty-state">
      <p>Ingen dokumenter matcher filtreringen.</p>
    </section>
  </div>
</template>

<style scoped>
.documents-view {
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

.upload-btn {
  min-height: 2.75rem;
  padding: 0.5rem 1rem;
  border-radius: var(--radius-md);
  background: var(--color-foreground);
  color: var(--color-background);
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.documents-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.75rem;
}

.documents-stat {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  text-align: center;
  padding: 0.85rem;
}

.documents-stat strong {
  font-size: var(--font-size-xl);
  color: var(--color-gray-900);
}

.documents-stat span {
  display: block;
  margin-top: 0.2rem;
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
}

.documents-toolbar {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  padding: 0.75rem;
  display: grid;
  gap: 0.75rem;
}

.search-input {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-gray-50);
  min-height: 2.6rem;
  padding: 0 0.85rem;
  color: var(--color-gray-700);
}

.category-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.category-tab {
  min-height: 2.15rem;
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-md);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--color-gray-600);
}

.category-tab--active {
  background: var(--color-foreground);
  color: var(--color-background);
  border-color: var(--color-foreground);
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0.6rem;
}

.category-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  padding: 0.75rem;
}

.category-card h2 {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-gray-800);
}

.category-card p {
  margin-top: 0.2rem;
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
}

.table-wrapper {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: 0.75rem;
  text-align: left;
  border-bottom: 1px solid var(--color-gray-100);
  vertical-align: middle;
}

th {
  font-size: var(--font-size-xs);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-gray-500);
  background: var(--color-gray-50);
}

.table-title {
  color: var(--color-gray-900);
  font-size: var(--font-size-sm);
}

.table-subtitle {
  margin-top: 0.15rem;
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
}

.status-pill {
  display: inline-flex;
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  background: var(--color-gray-100);
  color: var(--color-gray-600);
  font-size: var(--font-size-xs);
  font-weight: 600;
}

.status-pill--active {
  background: var(--color-success-bg);
  color: var(--color-success);
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

  .documents-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .category-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  th:nth-child(n + 3),
  td:nth-child(n + 3) {
    display: none;
  }

}
</style>