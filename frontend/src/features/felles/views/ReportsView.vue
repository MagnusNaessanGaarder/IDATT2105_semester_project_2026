<script setup lang="ts">
import { ref } from 'vue'
import fellesData from '@/data/felles.json'
import ReportCard from '../components/ReportCard.vue'

interface Report {
  id: number
  title: string
  type: string
  created_by: string
  created_date: string
  period: string
  status: 'draft' | 'finalized'
  sections: Array<{ name: string; content: string }>
  file_url: string | null
  file_size: string | null
}

const reports = ref<Report[]>(fellesData.reports as Report[])

const handleViewReport = (report: Report) => {
  void report
}

const handleDownloadReport = (report: Report) => {
  void report
}
</script>

<template>
  <div class="view-page">
    <header class="page-header">
      <h1>Rapporter</h1>
      <p class="subtitle">Månedlige og spesielle rapporter</p>
    </header>

    <div class="reports-grid">
      <ReportCard 
        v-for="report in reports"
        :key="report.id"
        :report="report"
        @view="handleViewReport(report)"
        @download="handleDownloadReport(report)"
      />
    </div>

    <div v-if="reports.length === 0" class="empty-state">
      <p>Ingen rapporter funnet</p>
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

.reports-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
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
  .reports-grid {
    grid-template-columns: 1fr;
  }
}
</style>