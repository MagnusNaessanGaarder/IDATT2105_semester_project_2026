<script setup lang="ts">
import { computed, ref } from 'vue'
import fellesData from '@/data/felles.json'
import DocumentCard from '../components/DocumentCard.vue'

interface DocumentItem {
  id: number
  name: string
  category: string
  file_type: string
  uploaded_by: string
  uploaded_date: string
  size: string
  version: string
  status: 'active' | 'archived'
  description: string
}

const documents = ref<DocumentItem[]>(fellesData.documents as DocumentItem[])
const selectedCategory = ref<string>('all')

const categories = computed(() => {
  const unique = new Set(documents.value.map((doc) => doc.category))
  return Array.from(unique)
})

const filteredDocuments = computed(() => {
  if (selectedCategory.value === 'all') {
    return documents.value
  }

  return documents.value.filter((doc) => doc.category === selectedCategory.value)
})

const handleOpenDocument = (document: DocumentItem) => {
  void document
}

const handleDownloadDocument = (document: DocumentItem) => {
  void document
}
</script>

<template>
  <div class="view-page">
    <header class="page-header">
      <h1>Dokumenter</h1>
      <p class="subtitle">Sentralisert lagring av policyer og opplæringsmateriale</p>
    </header>

    <div class="documents-filters">
      <button
        class="filter-btn"
        :class="{ 'filter-btn--active': selectedCategory === 'all' }"
        @click="selectedCategory = 'all'"
      >
        Alle
      </button>
      <button
        v-for="category in categories"
        :key="category"
        class="filter-btn"
        :class="{ 'filter-btn--active': selectedCategory === category }"
        @click="selectedCategory = category"
      >
        {{ category }}
      </button>
    </div>

    <div class="documents-grid">
      <DocumentCard
        v-for="document in filteredDocuments"
        :key="document.id"
        :document="document"
        @view="handleOpenDocument(document)"
        @download="handleDownloadDocument(document)"
      />
    </div>

    <div v-if="filteredDocuments.length === 0" class="empty-state">
      <p>Ingen dokumenter matcher valgt filter</p>
    </div>
  </div>
</template>

<style scoped>
.view-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}

.page-header {
  margin-bottom: 2rem;
}

.page-header h1 {
  margin: 0;
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--color-foreground);
  margin-bottom: 0.5rem;
}

.subtitle {
  margin: 0;
  font-size: var(--text-base);
  color: var(--color-gray-600);
}

.documents-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-bottom: 2rem;
}

.filter-btn {
  padding: 0.5rem 1rem;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--color-gray-600);
  cursor: pointer;
  transition: all var(--transition-base);
}

.filter-btn:hover {
  background: var(--color-accent);
}

.filter-btn--active {
  background: var(--color-foreground);
  color: var(--color-background);
  border-color: var(--color-foreground);
}

.documents-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.empty-state {
  text-align: center;
  padding: 3rem 1.5rem;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-gray-600);
}

@media (max-width: 48rem) {
  .documents-grid {
    grid-template-columns: 1fr;
  }
}
</style>