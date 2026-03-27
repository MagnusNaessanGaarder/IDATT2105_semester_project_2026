<script setup lang="ts">
import { computed } from 'vue'
import alkoholData from '@/data/ik-alkohol.json'

type CertificateStatus = 'Gyldig' | 'Utløper snart' | 'Utgått'

interface EmployeeCertificate {
  name: string
  expire_date: string
}

interface EmployeeCertification {
  name: string
  certifications: EmployeeCertificate[]
}

const certificationTypes = alkoholData.certifications.types
const employees = alkoholData.certifications.employees as EmployeeCertification[]

const SOON_DAYS = 30

const getCertificate = (employee: EmployeeCertification, certificationName: string) => {
  return employee.certifications.find((certificate) => certificate.name === certificationName)
}

const getCertificateStatus = (expireDate: string): CertificateStatus => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  const expiry = new Date(expireDate)
  expiry.setHours(0, 0, 0, 0)

  const diffMs = expiry.getTime() - today.getTime()
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))

  if (diffDays < 0) {
    return 'Utgått'
  }

  if (diffDays <= SOON_DAYS) {
    return 'Utløper snart'
  }

  return 'Gyldig'
}

const statusCount = computed(() => {
  const counts: Record<CertificateStatus, number> = {
    Gyldig: 0,
    'Utløper snart': 0,
    Utgått: 0,
  }

  employees.forEach((employee) => {
    employee.certifications.forEach((certificate) => {
      counts[getCertificateStatus(certificate.expire_date)] += 1
    })
  })

  return counts
})

const formatDate = (value: string) => {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleDateString('nb-NO')
}
</script>

<template>
  <div class="view-page">
    <header class="page-header">
      <h1>Sertifiseringer</h1>
      <p class="subtitle">Oversikt over kunnskapsprøver og bevillinger</p>
    </header>

    <section class="status-summary" aria-label="Sertifikatstatus oversikt">
      <article class="status-box status-box--valid">
        <p class="status-box__label">Gyldig</p>
        <p class="status-box__value">{{ statusCount.Gyldig }}</p>
      </article>
      <article class="status-box status-box--soon">
        <p class="status-box__label">Utløper snart</p>
        <p class="status-box__value">{{ statusCount['Utløper snart'] }}</p>
      </article>
      <article class="status-box status-box--expired">
        <p class="status-box__label">Utgått</p>
        <p class="status-box__value">{{ statusCount.Utgått }}</p>
      </article>
    </section>

    <section class="matrix-section">
      <h2 class="matrix-title">Sertifiseringsmatrise</h2>
      <div class="matrix-table" role="table" aria-label="Ansatte og sertifiseringer">
        <div class="matrix-row matrix-row--head" role="row">
          <div class="matrix-cell matrix-cell--name" role="columnheader">Ansatt</div>
          <div v-for="type in certificationTypes" :key="type" class="matrix-cell" role="columnheader">{{ type }}</div>
        </div>
        <div v-for="employee in employees" :key="employee.name" class="matrix-row" role="row">
          <div class="matrix-cell matrix-cell--name" role="cell">{{ employee.name }}</div>
          <div v-for="type in certificationTypes" :key="`${employee.name}-${type}`" class="matrix-cell" role="cell">
            <template v-if="getCertificate(employee, type)">
              <span
                class="status-pill"
                :class="{
                  'status-pill--soon': getCertificateStatus(getCertificate(employee, type)!.expire_date) === 'Utløper snart',
                  'status-pill--expired': getCertificateStatus(getCertificate(employee, type)!.expire_date) === 'Utgått',
                }"
              >
                {{ getCertificateStatus(getCertificate(employee, type)!.expire_date) }}
              </span>
              <p class="expiry-date">Utløper: {{ formatDate(getCertificate(employee, type)!.expire_date) }}</p>
            </template>
            <template v-else>
              <span class="status-pill status-pill--missing">Mangler</span>
            </template>
          </div>
        </div>
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

.status-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr));
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.status-box {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.85rem;
  background: var(--color-card);
}

.status-box__label {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--color-gray-600);
}

.status-box__value {
  margin: 0.35rem 0 0;
  font-size: 1.625rem;
  font-weight: 700;
}

.status-box--valid {
  border-left: 0.25rem solid #15803d;
}

.status-box--soon {
  border-left: 0.25rem solid #b45309;
}

.status-box--expired {
  border-left: 0.25rem solid #b91c1c;
}

.matrix-section {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1.25rem;
}

.matrix-title {
  margin: 0 0 1rem;
  font-size: var(--text-lg);
}

.matrix-table {
  display: grid;
  gap: 0.5rem;
}

.matrix-row {
  display: grid;
  grid-template-columns: minmax(10rem, 1.5fr) repeat(2, minmax(10rem, 1fr));
  gap: 0.5rem;
}

.matrix-row--head .matrix-cell {
  font-weight: 700;
  color: var(--color-foreground);
}

.matrix-cell {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0.65rem;
  font-size: var(--text-sm);
  background: var(--color-accent);
}

.matrix-cell--name {
  font-weight: 600;
  background: var(--color-card);
}

.status-pill {
  display: inline-block;
  padding: 0.2rem 0.45rem;
  border-radius: var(--radius-sm);
  color: var(--color-background);
  background: #15803d;
  font-size: var(--text-xs);
  font-weight: 600;
}

.status-pill--soon {
  background: #b45309;
}

.status-pill--expired {
  background: #b91c1c;
}

.status-pill--missing {
  background: #6b7280;
}

.expiry-date {
  margin: 0.35rem 0 0;
  font-size: var(--text-xs);
  color: var(--color-gray-600);
}

@media (max-width: 48rem) {
  .matrix-row {
    grid-template-columns: 1fr;
  }
}
</style>