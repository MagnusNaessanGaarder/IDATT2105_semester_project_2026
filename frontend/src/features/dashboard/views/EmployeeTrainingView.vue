<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useCertifications } from '@/features/ik-alkohol/composables/useCertifications'
import { getCertificationCatalog, type CertificationCatalogItem } from '@/features/ik-alkohol/api/certificationCatalog'
import { useAuthStore } from '@/stores/auth'
import { certificateStatusForDate } from '@/features/ik-alkohol/composables/useAlkoholData'

const {
  items: records,
  isLoading,
  loadItems,
} = useCertifications()

const authStore = useAuthStore()
const orgNumber = computed(() => authStore.currentOrg?.orgNumber)
const myEmail   = computed(() => authStore.email?.toLowerCase())

const catalog = ref<CertificationCatalogItem[]>([])

//  filters 
const typeFilter   = ref('ALL')
const statusFilter = ref('ALL')
const showMineOnly = ref(false)

//  derived 
const typeLabels = computed<Record<string, string>>(() => {
  if (catalog.value.length > 0)
    return Object.fromEntries(catalog.value.map((c) => [c.trainingType, c.displayName]))
  return {
    FOOD_HYGIENE:                'Mathygiene',
    ALLERGEN_HANDLING:           'Allergenhåndtering',
    TEMPERATURE_CONTROL:        'Temperaturkontroll',
    CLEANING_ROUTINES:           'Rengjøringsrutiner',
    RESPONSIBLE_ALCOHOL_SERVICE: 'Ansvarlig alkoholservering',
    AGE_VERIFICATION:            'Alderskontroll',
    OTHER:                       'Annet',
  }
})

const filtered = computed(() => {
  let result = [...records.value]

  if (showMineOnly.value)
    result = result.filter((r) => r.user?.email?.toLowerCase() === myEmail.value)

  if (typeFilter.value !== 'ALL')
    result = result.filter((r) => r.trainingType === typeFilter.value)

  if (statusFilter.value !== 'ALL')
    result = result.filter((r) => derivedStatus(r) === statusFilter.value)

  return result.sort((a, b) => {
    // Sort: expired first, then by expiry date ascending, then no-expiry last
    const ta = a.expiresAt ?? '9999'
    const tb = b.expiresAt ?? '9999'
    return ta.localeCompare(tb)
  })
})

// Compute display status from expiresAt (same logic as CertificationsView)
function derivedStatus(r: { expiresAt: string | null; status: string }): string {
  if (r.expiresAt) return certificateStatusForDate(r.expiresAt.slice(0, 10))
  return r.status === 'COMPLETED' ? 'Gyldig' : 'Tildelt'
}

const statusCounts = computed(() => ({
  gyldig:  records.value.filter((r) => derivedStatus(r) === 'Gyldig').length,
  snart:   records.value.filter((r) => derivedStatus(r) === 'Utløper snart').length,
  utgatt:  records.value.filter((r) => derivedStatus(r) === 'Utgått').length,
  tildelt: records.value.filter((r) => derivedStatus(r) === 'Tildelt').length,
}))

//  helpers 
function fmtDate(iso: string | null) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('nb-NO', { day: '2-digit', month: 'short', year: 'numeric' })
}

function statusTone(r: typeof records.value[number]) {
  const s = derivedStatus(r)
  if (s === 'Gyldig')        return 'ok'
  if (s === 'Utløper snart') return 'warn'
  if (s === 'Utgått')        return 'danger'
  return 'neutral'
}

function isMe(r: typeof records.value[number]) {
  return r.user?.email?.toLowerCase() === myEmail.value
}

//  load 
onMounted(async () => {
  if (!orgNumber.value) return
  const [, catalogResult] = await Promise.all([
    loadItems(orgNumber.value),
    getCertificationCatalog(orgNumber.value),
  ])
  if (catalogResult.ok) catalog.value = catalogResult.data
})
</script>

<template>
  <div class="training-overview">

    <header class="page-header">
      <div>
        <h1>Opplæringsregister</h1>
        <p class="subtitle">Oversikt over sertifikater og opplæring i organisasjonen</p>
      </div>
    </header>

    <!-- Stats -->
    <div class="stats-row">
      <div class="stat stat--ok">
        <strong>{{ statusCounts.gyldig }}</strong>
        <span>Gyldige</span>
      </div>
      <div class="stat stat--warn">
        <strong>{{ statusCounts.snart }}</strong>
        <span>Utløper snart</span>
      </div>
      <div class="stat stat--danger">
        <strong>{{ statusCounts.utgatt }}</strong>
        <span>Utgåtte</span>
      </div>
      <div class="stat stat--neutral">
        <strong>{{ statusCounts.tildelt }}</strong>
        <span>Ikke fullført</span>
      </div>
    </div>

    <!-- Filters -->
    <div class="filter-panel">

      <!-- Mine only toggle -->
      <div class="filter-row filter-row--mine">
        <button
            class="mine-toggle"
            :class="{ 'mine-toggle--active': showMineOnly }"
            type="button"
            @click="showMineOnly = !showMineOnly"
        >
          <span class="mine-toggle__dot" />
          Vis kun mine sertifikater
        </button>
      </div>

      <!-- Type chips -->
      <div class="filter-row">
        <span class="filter-label">Type</span>
        <div class="chip-row">
          <button class="chip" :class="{ 'chip--active': typeFilter === 'ALL' }" type="button" @click="typeFilter = 'ALL'">Alle</button>
          <button
              v-for="(label, value) in typeLabels"
              :key="value"
              class="chip"
              :class="{ 'chip--active': typeFilter === value }"
              type="button"
              @click="typeFilter = value"
          >
            {{ label }}
          </button>
        </div>
      </div>

      <!-- Status chips -->
      <div class="filter-row">
        <span class="filter-label">Status</span>
        <div class="chip-row">
          <button class="chip" :class="{ 'chip--active': statusFilter === 'ALL' }"          type="button" @click="statusFilter = 'ALL'">Alle</button>
          <button class="chip chip--ok"      :class="{ 'chip--active': statusFilter === 'Gyldig' }"        type="button" @click="statusFilter = 'Gyldig'">Gyldig</button>
          <button class="chip chip--warn"    :class="{ 'chip--active': statusFilter === 'Utløper snart' }" type="button" @click="statusFilter = 'Utløper snart'">Utløper snart</button>
          <button class="chip chip--danger"  :class="{ 'chip--active': statusFilter === 'Utgått' }"        type="button" @click="statusFilter = 'Utgått'">Utgått</button>
          <button class="chip chip--neutral" :class="{ 'chip--active': statusFilter === 'Tildelt' }"       type="button" @click="statusFilter = 'Tildelt'">Ikke fullført</button>
        </div>
      </div>

    </div>

    <!-- Loading -->
    <div v-if="isLoading" class="loading">Laster opplæringsregister…</div>

    <!-- Empty -->
    <div v-else-if="filtered.length === 0" class="empty">
      <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/>
      </svg>
      <p>Ingen sertifikater matcher gjeldende filter.</p>
    </div>

    <!-- Table -->
    <div v-else class="table-card">
      <div class="result-count">{{ filtered.length }} registrering{{ filtered.length === 1 ? '' : 'er' }}</div>
      <table>
        <thead>
        <tr>
          <th>Ansatt</th>
          <th>Type</th>
          <th>Tittel</th>
          <th>Fullført</th>
          <th>Utløper</th>
          <th>Status</th>
        </tr>
        </thead>
        <tbody>
        <tr
            v-for="r in filtered"
            :key="r.trainingRecordId"
            :class="{
              'tr--mine':    isMe(r),
              'tr--expired': statusTone(r) === 'danger',
              'tr--warn':    statusTone(r) === 'warn',
            }"
        >
          <td>
            <span class="cell-name">{{ r.user?.displayName ?? '—' }}</span>
            <span v-if="isMe(r)" class="you-badge">deg</span>
          </td>
          <td>
            <span class="type-pill">{{ typeLabels[r.trainingType] ?? r.trainingType }}</span>
          </td>
          <td class="td-title">{{ r.title }}</td>
          <td class="td-meta">{{ fmtDate(r.completedAt) }}</td>
          <td class="td-meta">
              <span
                  :class="{
                  'expiry--warn':    statusTone(r) === 'warn',
                  'expiry--danger':  statusTone(r) === 'danger',
                }"
              >{{ fmtDate(r.expiresAt) }}</span>
          </td>
          <td>
              <span class="status-pill" :class="`status-pill--${statusTone(r)}`">
                {{ derivedStatus(r) }}
              </span>
          </td>
        </tr>
        </tbody>
      </table>
    </div>

  </div>
</template>

<style scoped>
.training-overview {
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}

/*  Header  */
.page-header h1 {
  margin: 0;
  font-size: clamp(1.5rem, 2.4vw, var(--font-size-3xl));
  font-weight: 700;
  letter-spacing: -0.015em;
}
.subtitle { margin: 0.3rem 0 0; color: var(--color-gray-500); font-size: var(--font-size-sm); }

/*  Stats  */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.75rem;
}
.stat {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  padding: 0.75rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}
.stat strong { font-size: 1.6rem; font-weight: 700; color: var(--color-foreground); }
.stat span   { font-size: var(--font-size-xs); color: var(--color-gray-500); }
.stat--ok      { border-left: 3px solid var(--color-success); }
.stat--warn    { border-left: 3px solid var(--color-warning); }
.stat--danger  { border-left: 3px solid var(--color-danger); }
.stat--neutral { border-left: 3px solid var(--color-gray-300); }

/*  Filter panel  */
.filter-panel {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  padding: 0.9rem 1rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-card);
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.filter-row--mine { border-bottom: 1px solid var(--color-border); padding-bottom: 0.65rem; }

.filter-label {
  font-size: var(--font-size-xs);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-gray-500);
  white-space: nowrap;
  min-width: 3.5rem;
}

/*  Mine toggle  */
.mine-toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.4rem 0.85rem 0.4rem 0.6rem;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: var(--color-background);
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}
.mine-toggle:hover { border-color: var(--color-gray-400); color: var(--color-foreground); }
.mine-toggle--active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-primary-foreground);
  font-weight: 600;
}
.mine-toggle__dot {
  width: 0.55rem;
  height: 0.55rem;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.7;
  flex-shrink: 0;
}

/*  Chips  */
.chip-row { display: flex; gap: 0.35rem; flex-wrap: wrap; }
.chip {
  padding: 0.2rem 0.7rem;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: var(--color-background);
  color: var(--color-gray-600);
  font-size: var(--font-size-xs);
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: all var(--transition-fast);
}
.chip:hover:not(.chip--active) { border-color: var(--color-gray-400); color: var(--color-foreground); }
.chip--active { background: var(--color-foreground); border-color: var(--color-foreground); color: var(--color-primary-foreground); font-weight: 600; }

/* Coloured active states for status chips */
.chip--ok.chip--active     { background: var(--color-success); border-color: var(--color-success); }
.chip--warn.chip--active   { background: var(--color-warning); border-color: var(--color-warning); color: #fff; }
.chip--danger.chip--active { background: var(--color-danger);  border-color: var(--color-danger); }
.chip--neutral.chip--active{ background: var(--color-gray-500); border-color: var(--color-gray-500); }

/*  Loading / empty  */
.loading { padding: 3rem 1rem; text-align: center; color: var(--color-gray-400); font-size: var(--font-size-sm); }
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 4rem 1rem;
  color: var(--color-gray-400);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-lg);
}
.empty p { margin: 0; font-size: var(--font-size-sm); }

/*  Table  */
.table-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-card);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}
.result-count {
  padding: 0.6rem 1rem;
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-gray-50);
}
table { width: 100%; border-collapse: collapse; }
th, td {
  text-align: left;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--color-gray-100);
  vertical-align: middle;
}
th {
  font-size: var(--font-size-xs);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-gray-500);
  background: var(--color-gray-50);
}
tr:last-child td { border-bottom: none; }

.tr--mine    { background: color-mix(in srgb, var(--color-primary) 4%, var(--color-card)); }
.tr--expired { background: color-mix(in srgb, var(--color-danger-bg) 60%, var(--color-card)); }
.tr--warn    { background: color-mix(in srgb, var(--color-warning-bg, #fef3c7) 50%, var(--color-card)); }

.cell-name { font-size: var(--font-size-sm); font-weight: 500; }
.you-badge {
  display: inline-flex;
  margin-left: 0.4rem;
  padding: 0.05rem 0.45rem;
  border-radius: 999px;
  background: var(--color-primary);
  color: var(--color-primary-foreground);
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  vertical-align: middle;
}
.td-title { font-size: var(--font-size-sm); color: var(--color-foreground); }
.td-meta  { font-size: var(--font-size-sm); color: var(--color-gray-600); white-space: nowrap; }

.type-pill {
  display: inline-flex;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-size: var(--font-size-xs);
  font-weight: 500;
  background: var(--color-gray-100);
  color: var(--color-gray-700);
  white-space: nowrap;
}

.expiry--warn   { color: var(--color-warning); font-weight: 600; }
.expiry--danger { color: var(--color-danger);  font-weight: 600; }

.status-pill {
  display: inline-flex;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-size: var(--font-size-xs);
  font-weight: 600;
  white-space: nowrap;
}
.status-pill--ok      { background: var(--color-success-bg); color: var(--color-success); }
.status-pill--warn    { background: var(--color-warning-bg, #fef3c7); color: var(--color-warning); }
.status-pill--danger  { background: var(--color-danger-bg);  color: var(--color-danger); }
.status-pill--neutral { background: var(--color-gray-100);   color: var(--color-gray-600); }

/*  Responsive  */
@media (max-width: 48rem) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  th:nth-child(4), td:nth-child(4) { display: none; }
}
</style>