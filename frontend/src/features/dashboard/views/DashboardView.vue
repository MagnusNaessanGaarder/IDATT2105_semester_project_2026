<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useFellesData } from '../composables/useFellesData'
import { getOrgNumber } from '@/shared/utils/orgContext'
import { formatDateForOrganization } from '@/shared/utils/orgSettings'

const router = useRouter()
const authStore = useAuthStore()
const {
  dashboardStats,
  quickActions,
  reports,
  documents,
  notifications,
  sortedNotifications,
  sortedDocuments,
  formatDate,
  notificationTone,
  isLoading,
  error,
  reload,
} = useFellesData()

const viewMode = ref<'oversikt' | 'analyse'>('oversikt')

const user = computed(() => authStore.user)
const stats = dashboardStats

const unreadNotifications = computed(() => notifications.filter((item) => !item.read))
const highPriorityNotifications = computed(() => unreadNotifications.value.filter((item) => item.priority === 'high'))
const latestNotifications = computed(() => sortedNotifications.slice(0, 3))
const latestDocuments = computed(() => sortedDocuments.slice(0, 3))

const reportCompletion = computed(() => {
  if (reports.length === 0) {
    return 0
  }

  const finalized = reports.filter((report) => report.status === 'finalized').length
  return Math.round((finalized / reports.length) * 100)
})

const currentDate = computed(() => {
  return formatDateForOrganization(new Date(), getOrgNumber(), {
    weekday: 'long',
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  })
})

const getWelcomeMessage = () => {
  const hour = new Date().getHours()
  if (hour < 12) return 'God morgen'
  if (hour < 18) return 'God ettermiddag'
  return 'God kveld'
}

const trendLabel = (trend: 'up' | 'down' | 'neutral'): string => {
  if (trend === 'up') return 'Opp'
  if (trend === 'down') return 'Ned'
  return 'Stabil'
}

const trendSymbol = (trend: 'up' | 'down' | 'neutral'): string => {
  if (trend === 'up') return '↑'
  if (trend === 'down') return '↓'
  return '→'
}

const notificationTypeLabel = (type: 'warning' | 'info' | 'success' | 'error'): string => {
  if (type === 'warning') return 'Varsel'
  if (type === 'error') return 'Kritisk'
  if (type === 'success') return 'OK'
  return 'Info'
}

const goToNotificationAction = (actionUrl: string | null) => {
  if (!actionUrl) {
    router.push({ name: 'Notifications' })
    return
  }

  if (actionUrl.includes('ik-mat/deviations')) {
    router.push({ name: 'Deviations' })
    return
  }

  if (actionUrl.includes('ik-mat/temperature')) {
    router.push({ name: 'Temperature' })
    return
  }

  if (actionUrl.includes('admin/certifications')) {
    router.push({ name: 'Certifications' })
    return
  }

  if (actionUrl.includes('felles/reports')) {
    router.push({ name: 'Reports' })
    return
  }

  router.push({ name: 'Notifications' })
}
</script>

<template>
  <div class="view-page dashboard">
    <header class="dashboard__header">
      <div>
        <h1 class="dashboard__title">{{ getWelcomeMessage() }}, {{ user?.name?.split(' ')[0] || 'Bruker' }}</h1>
        <p class="dashboard__subtitle">{{ currentDate }} · Felles oversikt for IK-kontroll</p>
      </div>

      <div class="dashboard__tabs" role="tablist" aria-label="Dashboard-visning">
        <button class="dashboard__tab" :class="{ 'dashboard__tab--active': viewMode === 'oversikt' }" @click="viewMode = 'oversikt'">
          Oversikt
        </button>
        <button class="dashboard__tab" :class="{ 'dashboard__tab--active': viewMode === 'analyse' }" @click="viewMode = 'analyse'">
          Analyse
        </button>
      </div>
    </header>

    <section
      v-if="highPriorityNotifications.length > 0"
      class="alert-banner"
      aria-label="Kritisk oversikt"
    >
      <div>
        <p class="alert-banner__title">{{ highPriorityNotifications.length }} varsel krever oppfølging</p>
        <p class="alert-banner__text">{{ highPriorityNotifications[0]?.title }}: {{ highPriorityNotifications[0]?.message }}</p>
      </div>
      <router-link class="alert-banner__link" :to="{ name: 'Notifications' }">Se varsler</router-link>
    </section>

    <section v-if="error" class="alert-banner" aria-label="API-feil">
      <div>
        <p class="alert-banner__title">Kunne ikke hente dashboard-data</p>
        <p class="alert-banner__text">{{ error }}</p>
      </div>
      <button class="alert-banner__link" type="button" @click="reload">Prøv igjen</button>
    </section>

    <section v-else-if="isLoading" class="alert-banner" aria-label="Laster dashboard-data">
      <div>
        <p class="alert-banner__title">Laster dashboard-data...</p>
      </div>
    </section>

    <template v-if="viewMode === 'oversikt'">
      <section class="dashboard__stats" aria-label="Nøkkeltall">
        <article v-for="stat in stats" :key="stat.label" class="stat-card" :class="`stat-card--${stat.color}`">
          <p class="stat-card__label">{{ stat.label }}</p>
          <div class="stat-card__value-row">
            <p class="stat-card__value">{{ stat.value }}<span v-if="stat.unit">{{ stat.unit }}</span></p>
            <p class="stat-card__trend" :class="`stat-card__trend--${stat.trend}`">{{ trendSymbol(stat.trend) }} {{ trendLabel(stat.trend) }}</p>
          </div>
        </article>
      </section>

      <section class="dashboard__content-grid" aria-label="Siste aktivitet">
        <article class="panel">
          <header class="panel__header">
            <h2>Siste varsler</h2>
            <router-link :to="{ name: 'Notifications' }">Alle varsler</router-link>
          </header>
          <ul class="panel__list">
            <li v-for="notification in latestNotifications" :key="notification.id" class="panel__item">
              <button class="panel__item-button" @click="goToNotificationAction(notification.action_url)">
                <span class="panel__badge" :class="`panel__badge--${notificationTone(notification.type)}`">
                  {{ notificationTypeLabel(notification.type) }}
                </span>
                <span class="panel__item-title">{{ notification.title }}</span>
                <span class="panel__item-meta">{{ formatDate(notification.created_date) }}</span>
              </button>
            </li>
          </ul>
        </article>

        <article class="panel">
          <header class="panel__header">
            <h2>Nye dokumenter</h2>
            <router-link :to="{ name: 'Documents' }">Alle dokumenter</router-link>
          </header>
          <ul class="panel__list">
            <li v-for="document in latestDocuments" :key="document.id" class="panel__item panel__item--stacked">
              <p class="panel__item-title">{{ document.name }}</p>
              <p class="panel__item-description">{{ document.category }} · v{{ document.version }} · {{ document.size }}</p>
              <p class="panel__item-meta">Lastet opp av {{ document.uploaded_by }} · {{ formatDate(document.uploaded_date) }}</p>
            </li>
          </ul>
        </article>
      </section>

      <section class="quick-actions" aria-label="Hurtighandlinger">
        <h2 class="quick-actions__title">Hurtighandlinger</h2>
        <div class="quick-actions__grid">
          <router-link
            v-for="action in quickActions"
            :key="action.title"
            :to="{ name: action.route }"
            class="quick-action-card"
          >
            <p class="quick-action-card__title">{{ action.title }}</p>
            <p class="quick-action-card__meta">Åpne {{ action.route.toLowerCase() }}</p>
          </router-link>
        </div>
      </section>
    </template>

    <template v-else>
      <section class="analysis-grid" aria-label="Analysekort">
        <article class="analysis-card">
          <p>Uleste varsler</p>
          <h2>{{ unreadNotifications.length }}</h2>
          <span>{{ highPriorityNotifications.length }} med høy prioritet</span>
        </article>

        <article class="analysis-card">
          <p>Rapporter ferdigstilt</p>
          <h2>{{ reportCompletion }}%</h2>
          <span>{{ reports.filter((report) => report.status === 'finalized').length }} av {{ reports.length }}</span>
        </article>

        <article class="analysis-card">
          <p>Dokumentkategorier</p>
          <h2>{{ new Set(documents.map((doc) => doc.category)).size }}</h2>
          <span>{{ documents.length }} dokumenter totalt</span>
        </article>
      </section>

      <section class="analysis-panel">
        <h2>Rapportstatus</h2>
        <div class="progress-row">
          <span>Ferdige rapporter</span>
          <div class="progress-track">
            <div class="progress-value" :style="{ width: `${reportCompletion}%` }" />
          </div>
          <strong>{{ reportCompletion }}%</strong>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.dashboard {
  display: grid;
  gap: var(--spacing-lg);
}

.dashboard__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: var(--spacing-md);
}

.dashboard__title {
  margin: 0;
  font-size: clamp(1.8rem, 2.4vw, var(--font-size-3xl));
  font-weight: 700;
  letter-spacing: -0.015em;
}

.dashboard__subtitle {
  margin-top: 0.5rem;
  color: var(--color-gray-500);
  text-transform: capitalize;
}

.dashboard__tabs {
  display: inline-flex;
  gap: 0.5rem;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  padding: 0.25rem;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.dashboard__tab {
  min-height: 2.5rem;
  padding: 0.5rem 1rem;
  border-radius: var(--radius-md);
  border: none;
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
  transition: background-color var(--transition-fast), color var(--transition-fast), transform var(--transition-fast);
}

.dashboard__tab--active {
  background: var(--color-foreground);
  color: var(--color-background);
}

.alert-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-md);
  border: 1px solid color-mix(in srgb, var(--color-danger) 70%, black);
  background: var(--color-danger);
  border-radius: var(--radius-lg);
  padding: 1rem 1.25rem;
  box-shadow: var(--shadow-sm);
}

.alert-banner__title {
  margin: 0;
  color: var(--color-primary-foreground);
  font-weight: 600;
}

.alert-banner__text {
  margin-top: 0.25rem;
  color: color-mix(in srgb, var(--color-primary-foreground) 92%, transparent);
  font-size: var(--font-size-sm);
}

.alert-banner__link {
  background: color-mix(in srgb, var(--color-primary-foreground) 92%, transparent);
  color: var(--color-danger);
  border-radius: var(--radius-md);
  padding: 0.5rem 0.75rem;
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.dashboard__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--spacing-md);
}

.stat-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-card);
  padding: 1rem;
  box-shadow: var(--shadow-sm);
}

.stat-card--warning {
  border-color: var(--color-warning);
  background: var(--color-warning-bg);
}

.stat-card--success {
  border-color: var(--color-success);
  background: var(--color-success-bg);
}

.stat-card__label {
  margin: 0;
  font-size: var(--font-size-xs);
  color: var(--color-gray-600);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.stat-card__value-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 0.75rem;
  margin-top: 0.45rem;
}

.stat-card__value {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
}

.stat-card__trend {
  margin: 0;
  font-size: var(--font-size-xs);
  font-weight: 600;
}

.stat-card__trend--up {
  color: var(--color-success);
}

.stat-card__trend--down {
  color: var(--color-danger);
}

.stat-card__trend--neutral {
  color: var(--color-gray-500);
}

.dashboard__content-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--spacing-md);
}

.panel {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 1rem;
  box-shadow: var(--shadow-sm);
}

.panel__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.panel__header h2 {
  margin: 0;
  font-size: var(--font-size-lg);
}

.panel__header a {
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
  text-decoration: underline;
}

.panel__list {
  display: grid;
  gap: 0.5rem;
}

.panel__item {
  background: var(--color-gray-50);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 0.8rem;
}

.panel__item-button {
  width: 100%;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 0.5rem;
  text-align: left;
}

.panel__item--stacked {
  display: grid;
  gap: 0.2rem;
}

.panel__badge {
  font-size: var(--font-size-xs);
  border-radius: var(--radius-sm);
  padding: 0.2rem 0.4rem;
  font-weight: 600;
}

.panel__badge--red {
  background: var(--color-danger-bg);
  color: var(--color-danger-fg);
}

.panel__badge--amber {
  background: var(--color-warning-bg);
  color: var(--color-warning);
}

.panel__badge--blue {
  background: var(--color-info-bg);
  color: var(--color-info);
}

.panel__badge--green {
  background: var(--color-success-bg);
  color: var(--color-success);
}

.panel__item-title {
  color: var(--color-gray-800);
  font-size: var(--font-size-sm);
}

.panel__item-description {
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
}

.panel__item-meta {
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
}

.quick-actions {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 1rem;
  box-shadow: var(--shadow-sm);
}

.quick-actions__title {
  margin: 0 0 0.75rem;
  font-size: var(--font-size-lg);
}

.quick-actions__grid {
  display: grid;
  gap: var(--spacing-md);
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.quick-action-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-gray-50);
  padding: 0.95rem 1rem;
  text-decoration: none;
  transition: all var(--transition-fast);
}

.quick-action-card:hover {
  border-color: var(--color-gray-400);
  background: var(--color-card);
}

.action-card:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
  border-color: var(--color-focus);
}

.quick-action-card__title {
  margin: 0;
  color: var(--color-gray-900);
  font-size: var(--font-size-base);
  font-weight: 600;
}

.quick-action-card__meta {
  margin-top: 0.25rem;
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
}

.analysis-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--spacing-md);
}

.analysis-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 1rem;
  box-shadow: var(--shadow-sm);
}

.analysis-card p {
  margin: 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-xs);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.analysis-card h2 {
  margin: 0.35rem 0;
  font-size: 1.8rem;
}

.analysis-card span {
  color: var(--color-gray-500);
  font-size: var(--font-size-sm);
}

.analysis-panel {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 1rem;
  box-shadow: var(--shadow-sm);
}

.analysis-panel h2 {
  margin: 0;
  font-size: var(--font-size-lg);
}

.progress-row {
  margin-top: 1rem;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 0.75rem;
}

.progress-row span,
.progress-row strong {
  font-size: var(--font-size-sm);
}

.progress-track {
  height: 0.55rem;
  background: var(--color-gray-200);
  border-radius: 999px;
  overflow: hidden;
}

.progress-value {
  height: 100%;
  background: var(--color-success);
  border-radius: inherit;
}

@media (max-width: 48rem) {
  .dashboard__header {
    flex-direction: column;
    align-items: flex-start;
  }

  .dashboard__tabs {
    width: 100%;
  }

  .dashboard__tab {
    flex: 1;
  }

  .alert-banner {
    flex-direction: column;
    align-items: flex-start;
  }

  .dashboard__stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .dashboard__content-grid,
  .quick-actions__grid,
  .analysis-grid {
    grid-template-columns: 1fr;
  }

  .panel__item-button {
    grid-template-columns: 1fr;
  }
}
</style>
