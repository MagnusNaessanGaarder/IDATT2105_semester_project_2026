<script setup lang="ts">
import { onMounted, onBeforeUnmount, computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import {
  useExport,
  exportTypeLabels,
  exportFormatLabels,
  exportStatusLabels,
  exportStatusTone,
} from '@/features/export/composables/useExport.ts'
import { exportApi } from '@/features/export/api.ts'
import { useAuthStore } from '@/stores/auth.ts'
import type { ExportResponse } from '@/features/export/api.ts'

const router = useRouter()
const authStore = useAuthStore()
const orgNumber = computed(() => authStore.currentOrg?.orgNumber ?? null)

const {
  exports,
  isLoadingList,
  error,
  downloadExport,
  loadExports,
  resetActiveJob,
} = useExport()

const previewJob        = ref<ExportResponse | null>(null)
const previewUrl        = ref<string | null>(null)
const previewJsonText   = ref<string | null>(null)
const isLoadingPreview  = ref(false)
const previewError      = ref<string | null>(null)

async function openPreview(job: ExportResponse) {
  if (!orgNumber.value) return
  previewJob.value       = job
  previewUrl.value       = null
  previewJsonText.value  = null
  previewError.value     = null
  isLoadingPreview.value = true

  try {
    const path = await exportApi.getDownloadUrl(orgNumber.value, job.exportJobId)
    const pathWithOrg = `${path}?orgNumber=${orgNumber.value}`
    const baseUrl = (import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1').replace('/api/v1', '')

    const mimeType = job.format === 'JSON' ? 'application/json' : 'application/pdf'
    const response = await axios.get(baseUrl + pathWithOrg, {
      responseType: 'blob',
      headers: { Authorization: `Bearer ${sessionStorage.getItem('accessToken')}` },
    })

    const blob = new Blob([response.data], { type: mimeType })

    if (job.format === 'JSON') {
      previewJsonText.value = await blob.text()
    } else {
      previewUrl.value = URL.createObjectURL(blob)
    }
  } catch {
    previewError.value = 'Forhåndsvisning er ikke tilgjengelig. Prøv å laste ned filen i stedet.'
  } finally {
    isLoadingPreview.value = false
  }
}

function closePreview() {
  if (previewUrl.value) { URL.revokeObjectURL(previewUrl.value); previewUrl.value = null }
  previewJob.value       = null
  previewJsonText.value  = null
  previewError.value     = null
  isLoadingPreview.value = false
}

function onOverlayKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') closePreview()
}


const query = ref('')
const activeType = ref<string>('all')

const allTypes = computed(() => {
  const seen = new Set<string>()
  exports.value.forEach((e) => seen.add(e.exportType))
  return Array.from(seen)
})

const filteredExports = computed(() => {
  const search = query.value.trim().toLowerCase()
  return exports.value.filter((e) => {
    const matchesType = activeType.value === 'all' || e.exportType === activeType.value
    const matchesSearch =
        !search ||
        (exportTypeLabels[e.exportType] ?? e.exportType).toLowerCase().includes(search)
    return matchesType && matchesSearch
  })
})

const completedCount = computed(() => exports.value.filter((e) => e.status === 'COMPLETED').length)
const pendingCount   = computed(() => exports.value.filter((e) => e.status === 'PENDING' || e.status === 'RUNNING').length)
const failedCount    = computed(() => exports.value.filter((e) => e.status === 'FAILED').length)


function formatDate(iso: string | null | undefined) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('nb-NO', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

/** Parse parametersJson into human-readable label→value pairs, skipping empty. */
function parseParams(json: string | null | undefined): { label: string; value: string }[] {
  if (!json) return []
  try {
    const obj = JSON.parse(json) as Record<string, unknown>
    const labelMap: Record<string, string> = {
      dateFrom:      'Fra',
      dateTo:        'Til',
      locationId:    'Lokasjon-ID',
      checklistType: 'Sjekklistetype',
    }
    return Object.entries(obj)
        .filter(([, v]) => v !== null && v !== undefined && v !== '')
        .map(([k, v]) => ({ label: labelMap[k] ?? k, value: String(v) }))
  } catch {
    return []
  }
}

function typeLabel(type: string): string {
  return exportTypeLabels[type] ?? type
}

function tabLabel(type: string): string {
  if (type === 'all') return 'Alle'
  return typeLabel(type)
}

onMounted(() => loadExports())
onBeforeUnmount(() => resetActiveJob())
</script>

<template>
  <div class="view-page reports-view">

    <header class="page-header">
      <div>
        <h1>Rapporter</h1>
        <p class="subtitle">Genererte samsvarsrapporter og eksporter</p>
      </div>
      <button
          type="button"
          class="generate-btn"
          @click="router.push({ name: 'Export' })"
      >
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="12" y1="18" x2="12" y2="12"/><line x1="9" y1="15" x2="15" y2="15"/></svg>
        Generer rapport
      </button>
    </header>

    <div v-if="error" class="error-banner" role="alert">
      <span>{{ error }}</span>
    </div>

    <section class="reports-stats" aria-label="Rapportstatistikk">
      <article class="reports-stat">
        <strong>{{ isLoadingList ? '…' : exports.length }}</strong>
        <span>Totalt</span>
      </article>
      <article class="reports-stat">
        <strong>{{ isLoadingList ? '…' : completedCount }}</strong>
        <span>Ferdige</span>
      </article>
      <article class="reports-stat">
        <strong>{{ isLoadingList ? '…' : pendingCount }}</strong>
        <span>Pågår</span>
      </article>
      <article class="reports-stat">
        <strong>{{ isLoadingList ? '…' : failedCount }}</strong>
        <span>Feilet</span>
      </article>
    </section>

    <section class="reports-toolbar" aria-label="Filtrering">
      <div class="tab-list" role="tablist" aria-label="Rapporttyper">
        <button
            type="button"
            class="tab"
            :class="{ 'tab--active': activeType === 'all' }"
            @click="activeType = 'all'"
        >Alle</button>
        <button
            v-for="type in allTypes"
            :key="type"
            type="button"
            class="tab"
            :class="{ 'tab--active': activeType === type }"
            @click="activeType = type"
        >{{ tabLabel(type) }}</button>
      </div>
      <input
          v-model="query"
          class="search"
          type="search"
          placeholder="Søk etter rapporttype…"
      />
    </section>

    <div v-if="isLoadingList" class="skeleton-list" aria-label="Laster rapporter">
      <div v-for="i in 4" :key="i" class="skeleton-row" />
    </div>

    <section v-else-if="filteredExports.length" class="reports-list" aria-label="Rapportliste">
      <article
          v-for="job in filteredExports"
          :key="job.exportJobId"
          class="report-item"
      >
        <div class="report-item__main">
          <p class="report-item__title">{{ typeLabel(job.exportType) }}</p>
          <div class="report-item__meta">
            <span class="pill">{{ exportFormatLabels[job.format] ?? job.format }}</span>
            <span
                class="pill"
                :class="`pill--${exportStatusTone[job.status] ?? 'gray'}`"
            >{{ exportStatusLabels[job.status] ?? job.status }}</span>
            <span v-if="job.recordCount != null" class="report-item__period">
              {{ job.recordCount }} poster
            </span>
          </div>
          <p class="report-item__details">
            <template v-if="job.requestedByDisplayName">
              {{ job.requestedByDisplayName }} ·
            </template>
            Opprettet {{ formatDate(job.requestedAt) }}
            <template v-if="job.completedAt">
              · Ferdig {{ formatDate(job.completedAt) }}
            </template>
            <template v-if="job.failureReason">
              · <span class="failure-reason">{{ job.failureReason }}</span>
            </template>
          </p>
          <div v-if="parseParams(job.parametersJson).length" class="report-item__params">
            <span
                v-for="p in parseParams(job.parametersJson)"
                :key="p.label"
                class="param-chip"
            >{{ p.label }}: {{ p.value }}</span>
          </div>
        </div>
        <div class="report-item__actions">
          <template v-if="job.status === 'COMPLETED'">
            <button
                type="button"
                class="action-btn action-btn--ghost"
                @click="openPreview(job)"
            >
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
              Vis
            </button>
            <button
                type="button"
                class="action-btn"
                @click="downloadExport(job.exportJobId)"
            >
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
              Last ned
            </button>
          </template>
          <span v-else-if="job.status === 'PENDING' || job.status === 'RUNNING'" class="in-progress">
            <span class="spinner" />
            {{ exportStatusLabels[job.status] }}
          </span>
        </div>
      </article>
    </section>

    <section v-else-if="!isLoadingList" class="empty-state">
      <svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
      <p>{{ query || activeType !== 'all' ? 'Ingen rapporter matcher filtreringen.' : 'Ingen rapporter generert enda.' }}</p>
      <button
          v-if="query || activeType !== 'all'"
          type="button"
          class="reset-btn"
          @click="query = ''; activeType = 'all'"
      >Nullstill filter</button>
      <button
          v-else
          type="button"
          class="generate-btn"
          @click="router.push({ name: 'Export' })"
      >Generer din første rapport</button>
    </section>

    <Teleport to="body">
      <div
          v-if="previewJob"
          class="preview-overlay"
          role="dialog"
          :aria-label="`Forhåndsvisning: ${typeLabel(previewJob.exportType)}`"
          aria-modal="true"
          @keydown="onOverlayKeydown"
          @click.self="closePreview"
      >
        <div class="preview-modal">

          <!-- Header -->
          <div class="preview-header">
            <div class="preview-header__info">
              <h2 class="preview-title">{{ typeLabel(previewJob.exportType) }}</h2>
              <div class="preview-meta">
                <span class="pill">{{ exportFormatLabels[previewJob.format] }}</span>
                <span class="preview-meta__text">
                  <template v-if="previewJob.requestedByDisplayName">
                    {{ previewJob.requestedByDisplayName }} ·
                  </template>
                  Generert {{ formatDate(previewJob.requestedAt) }}
                </span>
              </div>
              <div v-if="parseParams(previewJob.parametersJson).length" class="preview-params">
                <span
                    v-for="p in parseParams(previewJob.parametersJson)"
                    :key="p.label"
                    class="param-chip"
                >{{ p.label }}: {{ p.value }}</span>
              </div>
            </div>
            <div class="preview-header__actions">
              <button
                  type="button"
                  class="action-btn"
                  @click="downloadExport(previewJob.exportJobId)"
              >
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                Last ned
              </button>
              <button
                  type="button"
                  class="close-btn"
                  aria-label="Lukk forhåndsvisning"
                  @click="closePreview"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
          </div>

          <!-- Body -->
          <div class="preview-body">
            <!-- Loading -->
            <div v-if="isLoadingPreview" class="preview-center">
              <span class="spinner-lg" />
              <p>Laster rapport…</p>
            </div>

            <!-- Error -->
            <div v-else-if="previewError" class="preview-center preview-center--error">
              <p>{{ previewError }}</p>
              <button type="button" class="action-btn" @click="downloadExport(previewJob.exportJobId)">
                Last ned i stedet
              </button>
            </div>

            <!-- PDF iframe -->
            <iframe
                v-else-if="previewUrl"
                :src="previewUrl"
                class="preview-iframe"
                :title="`Forhåndsvisning: ${typeLabel(previewJob.exportType)}`"
            />

            <!-- JSON viewer -->
            <div v-else-if="previewJsonText" class="preview-json">
              <pre class="preview-json__pre">{{ previewJsonText }}</pre>
            </div>
          </div>

        </div>
      </div>
    </Teleport>

  </div>
</template>

<style scoped>
.reports-view { display: grid; gap: 1rem; }

/* Header */
.page-header { display: flex; justify-content: space-between; align-items: flex-end; gap: 1rem; }
.page-header h1 { margin: 0; font-size: var(--font-size-3xl); font-weight: 700; letter-spacing: -0.015em; }
.subtitle { margin-top: 0.4rem; color: var(--color-gray-500); }

.generate-btn {
  display: inline-flex; align-items: center; gap: 0.45rem;
  padding: 0.6rem 1.1rem; border-radius: var(--radius-md);
  background: var(--color-foreground); color: var(--color-card);
  font-size: var(--font-size-sm); font-weight: 600; font-family: inherit;
  cursor: pointer; border: none; white-space: nowrap;
  transition: opacity var(--transition-fast); flex-shrink: 0;
}
.generate-btn:hover { opacity: 0.85; }

/* Error */
.error-banner {
  padding: 0.75rem 1rem; border-radius: var(--radius-md);
  background: var(--color-danger-bg); color: var(--color-danger);
  border: 1px solid var(--color-danger-bg); font-size: var(--font-size-sm);
}

/* Stats */
.reports-stats { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 0.75rem; }
.reports-stat { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-md); text-align: center; padding: 0.8rem; }
.reports-stat strong { display: block; color: var(--color-gray-900); font-size: var(--font-size-xl); }
.reports-stat span { display: block; color: var(--color-gray-500); font-size: var(--font-size-xs); margin-top: 0.2rem; }

/* Toolbar */
.reports-toolbar { border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-card); padding: 0.75rem; display: grid; gap: 0.75rem; }
.tab-list { display: flex; flex-wrap: wrap; gap: 0.5rem; }
.tab { min-height: 2.15rem; padding: 0.35rem 0.75rem; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-card); font-size: var(--font-size-sm); font-weight: 500; color: var(--color-gray-600); cursor: pointer; transition: all var(--transition-fast); }
.tab:hover { border-color: var(--color-gray-400); }
.tab--active { background: var(--color-foreground); color: var(--color-card); border-color: var(--color-foreground); }
.search { width: 100%; min-height: 2.6rem; border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: 0 0.85rem; background: var(--color-gray-50); font-size: var(--font-size-sm); }

/* Skeleton */
.skeleton-list { display: grid; gap: 0.55rem; }
.skeleton-row { height: 4.5rem; border-radius: var(--radius-md); background: linear-gradient(90deg, var(--color-gray-100) 25%, var(--color-gray-50) 50%, var(--color-gray-100) 75%); background-size: 200% 100%; animation: shimmer 1.4s infinite; }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

/* Report list */
.reports-list { display: grid; gap: 0.55rem; }
.report-item {
  border: 1px solid var(--color-border); border-radius: var(--radius-md);
  background: var(--color-card); padding: 0.85rem;
  display: flex; justify-content: space-between; align-items: center; gap: 1rem;
}
.report-item__title { margin: 0; color: var(--color-gray-900); font-size: var(--font-size-base); font-weight: 600; }
.report-item__meta { display: flex; align-items: center; flex-wrap: wrap; gap: 0.35rem; margin-top: 0.35rem; }
.report-item__period { font-size: var(--font-size-xs); color: var(--color-gray-500); }
.report-item__details { margin-top: 0.35rem; font-size: var(--font-size-sm); color: var(--color-gray-500); }
.report-item__actions { flex-shrink: 0; display: flex; align-items: center; gap: 0.5rem; }
.failure-reason { color: var(--color-danger); }

/* Parameter chips */
.report-item__params { display: flex; flex-wrap: wrap; gap: 0.3rem; margin-top: 0.4rem; }
.preview-params { display: flex; flex-wrap: wrap; gap: 0.3rem; margin-top: 0.5rem; }
.param-chip {
  display: inline-flex; align-items: center;
  padding: 0.15rem 0.5rem; border-radius: var(--radius-sm);
  background: var(--color-gray-100); color: var(--color-gray-600);
  font-size: var(--font-size-xs); font-family: ui-monospace, monospace;
}

/* Pills */
.pill { font-size: var(--font-size-xs); border-radius: 999px; padding: 0.2rem 0.55rem; background: var(--color-gray-100); color: var(--color-gray-700); font-weight: 600; }
.pill--green { background: var(--color-success-bg); color: var(--color-success); }
.pill--blue  { background: var(--color-info-bg); color: var(--color-info); }
.pill--red   { background: var(--color-danger-bg); color: var(--color-danger); }
.pill--gray  { background: var(--color-gray-100); color: var(--color-gray-600); }

/* Action buttons */
.action-btn {
  display: inline-flex; align-items: center; gap: 0.4rem;
  min-height: 2.2rem; padding: 0.35rem 0.75rem;
  border-radius: var(--radius-md);
  background: var(--color-foreground); color: var(--color-card);
  border: none; font-size: var(--font-size-sm); font-weight: 600;
  font-family: inherit; cursor: pointer;
  transition: opacity var(--transition-fast); white-space: nowrap;
}
.action-btn:hover { opacity: 0.85; }
.action-btn--ghost {
  background: transparent; color: var(--color-gray-700);
  border: 1px solid var(--color-border);
}
.action-btn--ghost:hover { background: var(--color-gray-50); }

/* In-progress indicator */
.in-progress {
  display: inline-flex; align-items: center; gap: 0.4rem;
  font-size: var(--font-size-xs); color: var(--color-gray-500);
}

/* Spinner */
.spinner {
  display: inline-block; width: 0.7rem; height: 0.7rem;
  border: 2px solid var(--color-gray-300); border-top-color: var(--color-gray-600);
  border-radius: 50%; animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Reset btn */
.reset-btn {
  padding: 0.4rem 0.9rem; border-radius: var(--radius-sm);
  background: transparent; border: 1px solid var(--color-border);
  font-size: var(--font-size-sm); font-weight: 600;
  color: var(--color-gray-700); cursor: pointer;
}

/* Empty */
.empty-state {
  display: flex; flex-direction: column; align-items: center; gap: 0.75rem;
  padding: 3rem 2rem; background: var(--color-card);
  border: 1px solid var(--color-border); border-radius: var(--radius-lg);
  color: var(--color-gray-500); text-align: center;
}
.empty-state svg { opacity: 0.4; }

@media (max-width: 48rem) {
  .page-header { flex-direction: column; align-items: flex-start; }
  .generate-btn { width: 100%; justify-content: center; }
  .reports-stats { grid-template-columns: repeat(2, 1fr); }
  .report-item { flex-direction: column; align-items: flex-start; }
  .report-item__actions { width: 100%; }
  .action-btn { flex: 1; justify-content: center; }
}

/* Preview modal */
.preview-overlay {
  position: fixed; inset: 0; z-index: 200;
  background: rgba(15, 23, 42, 0.55);
  display: flex; align-items: center; justify-content: center;
  padding: 1.5rem; backdrop-filter: blur(2px);
}
.preview-modal {
  background: var(--color-card); border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  width: min(900px, 100%); height: min(88vh, 860px);
  display: flex; flex-direction: column; overflow: hidden;
}
.preview-header {
  display: flex; align-items: flex-start; justify-content: space-between;
  gap: 1rem; padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--color-border); flex-shrink: 0;
}
.preview-title {
  margin: 0; font-size: var(--font-size-base); font-weight: 700;
  color: var(--color-gray-900);
}
.preview-meta {
  display: flex; align-items: center; gap: 0.5rem; margin-top: 0.4rem;
}
.preview-meta__text { font-size: var(--font-size-xs); color: var(--color-gray-500); }
.preview-header__actions { display: flex; align-items: center; gap: 0.5rem; flex-shrink: 0; }
.close-btn {
  display: inline-flex; align-items: center; justify-content: center;
  width: 2rem; height: 2rem; border-radius: var(--radius-sm);
  background: transparent; border: 1px solid var(--color-border);
  color: var(--color-gray-600); cursor: pointer; transition: background var(--transition-fast);
}
.close-btn:hover { background: var(--color-gray-100); }
.preview-body { flex: 1; overflow: hidden; display: flex; flex-direction: column; }
.preview-iframe { width: 100%; height: 100%; border: none; flex: 1; }
.preview-json {
  flex: 1; overflow: auto;
  background: var(--color-gray-50);
  padding: 1rem 1.25rem;
}
.preview-json__pre {
  margin: 0; font-size: var(--font-size-xs);
  font-family: ui-monospace, 'Cascadia Code', 'Source Code Pro', monospace;
  color: var(--color-gray-800); white-space: pre-wrap; word-break: break-all;
}
.preview-center {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  gap: 1rem; padding: 2rem; text-align: center;
  color: var(--color-gray-600); font-size: var(--font-size-sm);
}
.preview-center--error { color: var(--color-danger); }
.spinner-lg {
  display: inline-block; width: 36px; height: 36px;
  border: 3px solid var(--color-gray-200); border-top-color: var(--color-foreground);
  border-radius: 50%; animation: spin 0.7s linear infinite;
}

@media (max-width: 48rem) {
  .preview-overlay { padding: 0; align-items: flex-end; }
  .preview-modal {
    width: 100%; height: 92vh;
    border-bottom-left-radius: 0; border-bottom-right-radius: 0;
  }
}
</style>