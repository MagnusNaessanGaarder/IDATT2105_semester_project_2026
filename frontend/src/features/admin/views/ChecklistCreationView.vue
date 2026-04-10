<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { client } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import BulkUploadChecklist from '@/shared/components/BulkUploadChecklist.vue'

type Frequency = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'CUSTOM'
type ModuleType = 'FOOD' | 'ALCOHOL'
type ItemType = 'BOOLEAN' | 'TEXT' | 'NUMBER' | 'TEMPERATURE' | 'CHOICE'

interface TemplateForm {
  title: string
  description: string
  frequency: Frequency
  moduleType: ModuleType
  isActive: boolean
}

interface ChecklistItem {
  sortOrder: number
  label: string
  description?: string
  itemType: ItemType
  isRequired: boolean
  expectedText?: string
  expectedNumericMin?: number | null
  expectedNumericMax?: number | null
  choiceOptionsJson?: string
}

interface ChecklistTemplateResponse {
  templateId: number
  orgNumber: number
  title: string
  description?: string | null
  moduleType: ModuleType
  frequency: Frequency
  isActive: boolean
  createdAt?: string
  updatedAt?: string
  items?: ChecklistItem[]
}

const authStore = useAuthStore()
const orgNumber = computed(() => authStore.currentOrg?.orgNumber)

const templates = ref<ChecklistTemplateResponse[]>([])
const loading = ref(false)
const creating = ref(false)
const error = ref<string | null>(null)
const feedback = ref<{ type: 'success' | 'error'; msg: string } | null>(null)

const totalCount = computed(() => templates.value.length)
const activeCount = computed(() => templates.value.filter((t) => t.isActive).length)
const foodCount = computed(() => templates.value.filter((t) => t.moduleType === 'FOOD').length)
const alcoholCount = computed(() => templates.value.filter((t) => t.moduleType === 'ALCOHOL').length)

async function fetchTemplates() {
  if (!orgNumber.value) return

  loading.value = true
  error.value = null

  try {
    const { data } = await client.get<ChecklistTemplateResponse[]>('/api/v1/checklists/templates', {
      params: { orgNumber: orgNumber.value },
    })

    templates.value = [...data].sort((a, b) => {
      const da = a.createdAt ?? ''
      const db = b.createdAt ?? ''
      return db.localeCompare(da)
    })
  } catch (err: unknown) {
    const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
    error.value = msg ?? 'Kunne ikke hente sjekklister.'
  } finally {
    loading.value = false
  }
}

async function handleCreated(payload: { template: TemplateForm; items: ChecklistItem[] }) {
  if (!orgNumber.value) {
    flash('error', 'Fant ikke valgt organisasjon.')
    return
  }

  creating.value = true
  error.value = null

  try {
    const requestBody = {
      title: payload.template.title.trim(),
      description: payload.template.description.trim() || null,
      frequency: payload.template.frequency,
      moduleType: payload.template.moduleType,
      items: payload.items.map((item) => ({
        sortOrder: item.sortOrder,
        label: item.label,
        description: item.description || null,
        itemType: item.itemType,
        isRequired: item.isRequired,
        expectedText: item.expectedText || null,
        expectedNumericMin: item.expectedNumericMin ?? null,
        expectedNumericMax: item.expectedNumericMax ?? null,
        choiceOptionsJson: item.choiceOptionsJson || null,
      })),
    }

    const { data } = await client.post<ChecklistTemplateResponse>(
        '/api/v1/checklists/templates',
        requestBody,
        { params: { orgNumber: orgNumber.value } },
    )

    templates.value = [data, ...templates.value].sort((a, b) => {
      const da = a.createdAt ?? ''
      const db = b.createdAt ?? ''
      return db.localeCompare(da)
    })

    flash('success', `Sjekklisten «${data.title}» ble opprettet.`)
  } catch (err: unknown) {
    const status = (err as { response?: { status?: number } })?.response?.status
    const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message

    if (status === 403) {
      flash('error', 'Du har ikke tilgang til å opprette sjekklister.')
    } else {
      flash('error', msg ?? 'Kunne ikke opprette sjekklisten.')
    }
  } finally {
    creating.value = false
  }
}

function flash(type: 'success' | 'error', msg: string) {
  feedback.value = { type, msg }
  setTimeout(() => {
    feedback.value = null
  }, 4500)
}

function fmtDate(iso?: string | null) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('nb-NO', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

function moduleLabel(moduleType: ModuleType) {
  return moduleType === 'FOOD' ? 'IK-Mat' : 'IK-Alkohol'
}

function frequencyLabel(frequency: Frequency) {
  switch (frequency) {
    case 'DAILY': return 'Daglig'
    case 'WEEKLY': return 'Ukentlig'
    case 'MONTHLY': return 'Månedlig'
    case 'CUSTOM': return 'Egendefinert'
    default: return frequency
  }
}

onMounted(fetchTemplates)
</script>

<template>
  <div class="training-view">
    <header class="page-header">
      <div>
        <h1>Bulkopplasting av sjekklister</h1>
        <p class="subtitle">Opprett sjekklister fra JSON-, CSV- eller Excel-filer</p>
      </div>
    </header>

    <div v-if="feedback" class="feedback" :class="`feedback--${feedback.type}`" role="status">
      {{ feedback.msg }}
    </div>

    <div v-if="error" class="inline-error">{{ error }}</div>

    <div v-if="!orgNumber" class="inline-error">
      Ingen organisasjon er valgt.
    </div>

    <div v-else class="stats-row">
      <div class="stat">
        <strong>{{ totalCount }}</strong>
        <span>Totalt</span>
      </div>
      <div class="stat stat--ok">
        <strong>{{ activeCount }}</strong>
        <span>Aktive</span>
      </div>
      <div class="stat">
        <strong>{{ foodCount }}</strong>
        <span>IK-Mat</span>
      </div>
      <div class="stat">
        <strong>{{ alcoholCount }}</strong>
        <span>IK-Alkohol</span>
      </div>
    </div>

    <div class="info-box">
      <h3>Om opplastingen</h3>
      <p>
        Siden oppretter sjekklistemalen direkte mot API-et og legger ved alle punktene i samme kall.
        Etter opprettelse vises den nye malen i oversikten under.
      </p>
    </div>

    <BulkUploadChecklist v-if="orgNumber" @created="handleCreated" />

    <div v-if="creating" class="loading">Oppretter sjekkliste…</div>
    <div v-else-if="loading" class="loading">Laster sjekklister…</div>

    <div v-else-if="templates.length === 0" class="empty">
      <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <path d="M7 3h7l5 5v13a1 1 0 0 1-1 1H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z" />
        <path d="M14 3v6h6" />
        <path d="M9 13h6" />
        <path d="M9 17h4" />
      </svg>
      <p>Ingen sjekklister registrert ennå.</p>
    </div>

    <div v-else class="table-card">
      <table>
        <thead>
        <tr>
          <th>Tittel</th>
          <th>Modul</th>
          <th>Frekvens</th>
          <th>Punkter</th>
          <th>Opprettet</th>
          <th>Status</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="template in templates" :key="template.templateId">
          <td>
            <p class="cell-name">{{ template.title }}</p>
            <p class="cell-sub">{{ template.description || '—' }}</p>
          </td>
          <td>
            <span class="type-pill">{{ moduleLabel(template.moduleType) }}</span>
          </td>
          <td class="td-meta">{{ frequencyLabel(template.frequency) }}</td>
          <td class="td-meta">{{ template.items?.length ?? 0 }}</td>
          <td class="td-meta">{{ fmtDate(template.createdAt) }}</td>
          <td>
              <span class="status-pill" :class="template.isActive ? 'status-pill--ok' : 'status-pill--neutral'">
                {{ template.isActive ? 'Aktiv' : 'Inaktiv' }}
              </span>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.training-view { display: flex; flex-direction: column; gap: 1.1rem; }

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 1rem;
}

.page-header h1 {
  margin: 0;
  font-size: clamp(1.5rem, 2.4vw, var(--font-size-3xl));
  font-weight: 700;
  letter-spacing: -0.015em;
}

.subtitle {
  margin: 0.3rem 0 0;
  color: var(--color-gray-500);
  font-size: var(--font-size-sm);
}

.feedback {
  padding: 0.7rem 1rem;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.feedback--success {
  background: var(--color-success-bg);
  color: var(--color-success);
  border: 1px solid var(--color-success);
}

.feedback--error {
  background: var(--color-danger-bg);
  color: var(--color-danger);
  border: 1px solid var(--color-danger);
}

.inline-error {
  padding: 0.65rem 0.9rem;
  background: var(--color-danger-bg);
  color: var(--color-danger);
  border: 1px solid color-mix(in srgb, var(--color-danger) 30%, var(--color-border));
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
}

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

.stat strong {
  font-size: 1.6rem;
  font-weight: 700;
  color: var(--color-foreground);
}

.stat span {
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
}

.stat--ok { border-left: 3px solid var(--color-success); }

.info-box {
  padding: 0.95rem 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  background: var(--color-card);
}

.info-box h3 {
  margin: 0 0 0.35rem;
  font-size: var(--font-size-base);
  color: var(--color-foreground);
}

.info-box p {
  margin: 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
  line-height: 1.55;
}

.loading {
  padding: 3rem 1rem;
  text-align: center;
  color: var(--color-gray-400);
  font-size: var(--font-size-sm);
}

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

.empty p {
  margin: 0;
  font-size: var(--font-size-sm);
}

.table-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-card);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  text-align: left;
  padding: 0.8rem 1rem;
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

.cell-name {
  font-size: var(--font-size-sm);
  font-weight: 500;
  margin: 0;
}

.cell-sub {
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
  margin: 0.1rem 0 0;
}

.td-meta {
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
  white-space: nowrap;
}

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

.status-pill {
  display: inline-flex;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-size: var(--font-size-xs);
  font-weight: 600;
  white-space: nowrap;
}

.status-pill--ok {
  background: var(--color-success-bg);
  color: var(--color-success);
}

.status-pill--neutral {
  background: var(--color-gray-100);
  color: var(--color-gray-600);
}

@media (max-width: 48rem) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }

  th:nth-child(4),
  td:nth-child(4) {
    display: none;
  }
}
</style>