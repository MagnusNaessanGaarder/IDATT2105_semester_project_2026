<script setup lang="ts">
import { computed } from 'vue'
import { useAlkoholData } from '@/features/ik-alkohol/composables/useAlkoholData'

const {
  dailyControls,
  laws,
  employees,
  certificateCounts,
  completedControls,
  pendingControls,
  completionRate,
  staffWithExpired,
  certificateStatus,
  formattedDate,
} = useAlkoholData()

const latestControls = computed(() => dailyControls.value.slice(0, 5))

const staffRows = computed(() => {
  return employees.value.map((employee) => {
    const latestCertificate = employee.certifications
      .slice()
      .sort((a, b) => b.expire_date.localeCompare(a.expire_date))[0]

    if (!latestCertificate) {
      return {
        name: employee.name,
        status: 'Mangler',
        detail: 'Ingen sertifisering registrert',
        tone: 'muted',
      }
    }

    const status = certificateStatus(latestCertificate.expire_date)

    return {
      name: employee.name,
      status,
      detail: `${latestCertificate.name} - utlop ${formattedDate(latestCertificate.expire_date)}`,
      tone: status === 'Gyldig' ? 'good' : status === 'Utløper snart' ? 'warn' : 'danger',
    }
  })
})
</script>

<template>
  <div class="alkohol-dashboard">
    <header class="page-header">
      <h1>IK-Alkohol</h1>
      <p class="subtitle">Internkontroll etter alkoholloven og skjenkeforskriften</p>
    </header>

    <section v-if="staffWithExpired.length > 0" class="alert-strip" aria-live="polite">
      <p>
        <strong>{{ staffWithExpired.length }} ansatte</strong> har utgåtte sertifiseringer.
        Sjekk sertifiseringsoversikten for oppfølging.
      </p>
    </section>

    <section class="stats-grid" aria-label="Nøkkelstatus for IK-Alkohol">
      <article class="stat-card">
        <p class="stat-card__label">Daglig kontroll</p>
        <p class="stat-card__value">{{ completedControls }} / {{ dailyControls.length }}</p>
        <p class="stat-card__meta">{{ completionRate }}% fullført i dag</p>
      </article>
      <article class="stat-card">
        <p class="stat-card__label">Sertifiseringer</p>
        <p class="stat-card__value">{{ certificateCounts.Gyldig }}</p>
        <p class="stat-card__meta">Gyldige av totalt {{ certificateCounts.Gyldig + certificateCounts['Utløper snart'] + certificateCounts.Utgått }}</p>
      </article>
      <article class="stat-card">
        <p class="stat-card__label">Kritiske oppfølginger</p>
        <p class="stat-card__value">{{ pendingControls + certificateCounts.Utgått }}</p>
        <p class="stat-card__meta">Mangler i kontroll og utgåtte kurs</p>
      </article>
      <article class="stat-card">
        <p class="stat-card__label">Aktive regelsett</p>
        <p class="stat-card__value">{{ laws.length }}</p>
        <p class="stat-card__meta">Lover og forskrifter i oversikten</p>
      </article>
    </section>

    <section class="dashboard-grid" aria-label="Hovedseksjoner for IK-Alkohol">
      <router-link :to="{ name: 'DailyControl' }" class="action-card">
        <p class="action-card__eyebrow">Operativ drift</p>
        <h2>Daglig kontroll</h2>
        <p>Følg opp alle daglige kontrollpunkter med lovhenvisning og ansvarlig ansatt.</p>
      </router-link>

      <router-link :to="{ name: 'Certifications' }" class="action-card">
        <p class="action-card__eyebrow">Kompetanse</p>
        <h2>Sertifiseringer</h2>
        <p>Se hvem som er gyldig, hvem som utløper snart, og hva som allerede er utgått.</p>
      </router-link>

      <router-link :to="{ name: 'Regulations' }" class="action-card">
        <p class="action-card__eyebrow">Etterlevelse</p>
        <h2>Regelverk</h2>
        <p>Hold oversikt over alkoholloven, forskrifter og praktiske krav i virksomheten.</p>
      </router-link>
    </section>

    <section class="details-grid">
      <article class="detail-card">
        <h2>Daglig kontroll - status</h2>
        <ul>
          <li v-for="item in latestControls" :key="item.id" :class="{ 'detail-row--ok': item.is_checked }" class="detail-row">
            <div>
              <p class="detail-row__title">{{ item.name }}</p>
              <p class="detail-row__meta">{{ item.law_unit }} - {{ item.employee }}</p>
            </div>
            <span :class="{ 'tag--pending': !item.is_checked }" class="tag">{{ item.is_checked ? 'Fullført' : 'Mangler' }}</span>
          </li>
        </ul>
      </article>

      <article class="detail-card">
        <h2>Ansvarlige - sertifiseringer</h2>
        <ul>
          <li v-for="row in staffRows" :key="row.name" class="detail-row" :class="{ 'detail-row--danger': row.tone === 'danger' }">
            <div>
              <p class="detail-row__title">{{ row.name }}</p>
              <p class="detail-row__meta">{{ row.detail }}</p>
            </div>
            <span class="tag" :class="`tag--${row.tone}`">{{ row.status }}</span>
          </li>
        </ul>
      </article>
    </section>
  </div>
</template>

<style scoped>
.alkohol-dashboard {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--ik-alkohol-primary);
  margin-bottom: 8px;
}

.subtitle {
  font-size: var(--font-size-sm);
  color: var(--color-gray-500);
}

.alert-strip {
  border: 1px solid #fecaca;
  background: #fee2e2;
  color: #7f1d1d;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  margin-bottom: 16px;
}

.alert-strip p {
  color: inherit;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.stat-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 14px;
}

.stat-card__label {
  margin: 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-xs);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.stat-card__value {
  margin: 8px 0 4px;
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-foreground);
}

.stat-card__meta {
  margin: 0;
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.action-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 16px;
  box-shadow: var(--shadow-sm);
  text-decoration: none;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast), border-color var(--transition-fast);
}

.action-card:hover {
  border-color: var(--ik-alkohol-primary);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.action-card__eyebrow {
  margin: 0;
  color: var(--ik-alkohol-primary);
  font-size: var(--font-size-xs);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-weight: var(--font-weight-semibold);
}

.action-card h2 {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-bold);
  color: var(--color-foreground);
  margin: 6px 0;
}

.action-card p {
  margin: 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
}

.details-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 12px;
}

.detail-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 14px;
}

.detail-card h2 {
  margin: 0 0 10px;
  font-size: var(--font-size-base);
}

.detail-card ul {
  display: grid;
  gap: 8px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #f8fafc;
}

.detail-row--ok {
  background: #f0fdf4;
}

.detail-row--danger {
  background: #fef2f2;
}

.detail-row__title {
  margin: 0;
  color: var(--color-foreground);
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
}

.detail-row__meta {
  margin: 2px 0 0;
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
}

.tag {
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  background: var(--color-success-bg);
  color: var(--color-success);
  border: 1px solid color-mix(in srgb, var(--color-success) 30%, var(--color-border));
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  white-space: nowrap;
}

.tag--pending,
.tag--warn {
  background: var(--color-warning-bg);
  color: var(--color-warning);
  border-color: color-mix(in srgb, var(--color-warning) 35%, var(--color-border));
}

.tag--danger {
  background: var(--color-danger-bg);
  color: var(--color-danger);
  border-color: color-mix(in srgb, var(--color-danger) 35%, var(--color-border));
}

.tag--muted {
  background: var(--color-gray-100);
  color: var(--color-gray-600);
  border-color: var(--color-border);
}

@media (max-width: 48rem) {
  .detail-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
