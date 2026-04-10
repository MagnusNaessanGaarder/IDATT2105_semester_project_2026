<script setup lang="ts">
import { computed } from 'vue'
import { useAlkoholData } from '@/features/ik-alkohol/composables/useAlkoholData'

const { certificationTypes, employees, certificateCounts, certificateStatus, formattedDate, totalCertificates } = useAlkoholData()

const statusCount = computed(() => {
  return {
    Gyldig: certificateCounts.Gyldig,
    UtløperSnart: certificateCounts['Utløper snart'],
    Utgått: certificateCounts.Utgått,
  }
})

const rows = computed(() => {
  return employees.flatMap((employee) => {
    if (employee.certifications.length === 0) {
      return [
        {
          employee: employee.name,
          type: 'Ingen registrert',
          status: 'Mangler',
          expires: '-',
        },
      ]
    }

    return employee.certifications.map((certificate) => {
      const status = certificateStatus(certificate.expire_date)

      return {
        employee: employee.name,
        type: certificate.name,
        status,
        expires: formattedDate(certificate.expire_date),
      }
    })
  })
})
</script>

<template>
  <div class="certifications-page">
    <header class="page-header">
      <h1>Sertifiseringer</h1>
      <p class="subtitle">Oversikt over kunnskapsprover og sertifiseringer for alkoholservering</p>
    </header>

    <section class="status-summary" aria-label="Sertifikatstatus oversikt">
      <article class="status-box status-box--valid">
        <p class="status-box__label">Gyldig</p>
        <p class="status-box__value">{{ statusCount.Gyldig }}</p>
        <p class="status-box__meta">Av totalt {{ totalCertificates }} sertifikater</p>
      </article>
      <article class="status-box status-box--soon">
        <p class="status-box__label">Utløper snart</p>
        <p class="status-box__value">{{ statusCount.UtløperSnart }}</p>
        <p class="status-box__meta">Bor planlegges fornyet</p>
      </article>
      <article class="status-box status-box--expired">
        <p class="status-box__label">Utgått</p>
        <p class="status-box__value">{{ statusCount.Utgått }}</p>
        <p class="status-box__meta">Krever oppfølging umiddelbart</p>
      </article>
    </section>

    <section class="type-strip" aria-label="Sertifiseringstyper">
      <p class="type-strip__label">Typer i systemet:</p>
      <ul>
        <li v-for="type in certificationTypes" :key="type">{{ type }}</li>
      </ul>
    </section>

    <section class="matrix-section" aria-label="Personellsertifiseringer">
      <h2 class="matrix-title">Personell sertifiseringer</h2>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Ansatt</th>
              <th>Sertifikat</th>
              <th>Gyldig til</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="`${row.employee}-${row.type}`">
              <td>{{ row.employee }}</td>
              <td>{{ row.type }}</td>
              <td>{{ row.expires }}</td>
              <td>
                <span
                  class="status-pill"
                  :class="{
                    'status-pill--soon': row.status === 'Utløper snart',
                    'status-pill--expired': row.status === 'Utgått',
                    'status-pill--missing': row.status === 'Mangler',
                  }"
                >
                  {{ row.status }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="info-box">
        <h3>Krav til kunnskapsprove</h3>
        <p>
          Alle som selger, skjenker eller utleverer alkohol skal ha bestatt kunnskapsprove.
          Oversikten er laget fra dummy-data i ik-alkohol.json for utvikling og demo.
        </p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.certifications-page {
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

.status-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr));
  gap: var(--spacing-md);
}

.status-box {
  border: 1px solid color-mix(in srgb, var(--color-brand-soft-violet) 20%, var(--color-border));
  border-radius: var(--radius-lg);
  padding: var(--spacing-md);
  background: var(--color-card);
  box-shadow: var(--shadow-sm);
}

.status-box__label {
  margin: 0;
  font-size: 0.6875rem;
  color: var(--color-brand-medium-violet);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.status-box__value {
  margin: 0.35rem 0 0;
  font-size: 1.625rem;
  font-weight: 700;
}

.status-box__meta {
  margin: 0.35rem 0 0;
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
}

.status-box--valid {
  border-left: 4px solid var(--color-cta);
}

.status-box--soon {
  border-left: 4px solid var(--color-brand-soft-violet);
}

.status-box--expired {
  border-left: 4px solid var(--color-danger);
}

.type-strip {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.type-strip__label {
  margin: 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
}

.type-strip ul {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.type-strip li {
  padding: 4px 8px;
  border: 1px solid color-mix(in srgb, var(--color-brand-soft-violet) 18%, var(--color-border));
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-brand-pale-lavender) 40%, var(--color-card));
  font-size: var(--font-size-xs);
  color: var(--color-brand-deep-violet);
}

.matrix-section {
  background: var(--color-card);
  border: 1px solid color-mix(in srgb, var(--color-brand-soft-violet) 20%, var(--color-border));
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-sm);
}

.matrix-title {
  margin: 0 0 1rem;
  font-size: var(--font-size-lg);
}

.table-wrap {
  overflow-x: auto;
  margin-bottom: 14px;
}

.table-wrap table {
  width: 100%;
  border-collapse: collapse;
}

.table-wrap th {
  text-align: left;
  padding: 12px 8px;
  border-bottom: 1px solid var(--color-border);
  color: var(--color-brand-deep-violet);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  font-size: var(--font-size-xs);
}

.table-wrap td {
  padding: 12px 8px;
  border-bottom: 1px solid var(--color-border);
  color: var(--color-gray-700);
  font-size: var(--font-size-sm);
}

.status-pill {
  display: inline-block;
  padding: 0.2rem 0.45rem;
  border-radius: var(--radius-sm);
  color: var(--color-success);
  background: var(--color-success-bg);
  border: 1px solid color-mix(in srgb, var(--color-success) 30%, var(--color-border));
  font-size: var(--font-size-xs);
  font-weight: 600;
}

.status-pill--soon {
  color: var(--color-brand-deep-violet);
  background: var(--color-brand-soft-violet);
  border-color: color-mix(in srgb, var(--color-brand-soft-violet) 40%, var(--color-border));
}

.status-pill--expired {
  color: var(--color-danger);
  background: var(--color-danger-bg);
  border-color: color-mix(in srgb, var(--color-danger) 35%, var(--color-border));
}

.status-pill--missing {
  color: var(--color-gray-600);
  background: var(--color-gray-100);
  border-color: var(--color-border);
}

.info-box {
  padding: 14px;
  border-radius: var(--radius-lg);
  border: 1px solid color-mix(in srgb, var(--color-brand-soft-violet) 18%, var(--color-border));
  background: color-mix(in srgb, var(--color-brand-pale-lavender) 46%, var(--color-card));
}

.info-box h3 {
  margin: 0 0 4px;
  font-size: var(--font-size-base);
  color: var(--color-brand-deep-violet);
}

.info-box p {
  margin: 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
}

@media (max-width: 48rem) {
  .status-summary {
    grid-template-columns: 1fr;
  }
}
</style>