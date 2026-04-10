<script setup lang="ts">
import { computed, ref } from 'vue'
import { useDocuments } from '../composables/useDocuments'
import UploadDocumentModal from '../components/UploadDocumentModal.vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const {
  documents, isLoading, error,
  showUploadModal, versionTargetDoc, isUploading, uploadError,
  previewDoc, previewUrl, isLoadingPreview, previewError,
  downloadingId,
  openUploadNew, openUploadVersion, closeUploadModal,
  fetchDocuments, uploadDocument, uploadVersion, downloadDocument,
  openPreview, closePreview,
  formatDate,
} = useDocuments()

const selectedCategory = ref<string>('all')
const query = ref('')
const openMenuId = ref<number | null>(null)
const canManageDocuments = computed(() => authStore.hasRole('ADMIN', 'MANAGER'))

const categories = computed(() => {
  const unique = new Set(documents.value.map((d) => d.documentType))
  return Array.from(unique).sort()
})

const filteredDocuments = computed(() =>
  documents.value.filter((doc) => {
    const matchesCategory = selectedCategory.value === 'all' || doc.documentType === selectedCategory.value
    const search = query.value.trim().toLowerCase()
    return matchesCategory && (
      !search ||
      doc.title.toLowerCase().includes(search) ||
      (doc.description ?? '').toLowerCase().includes(search)
    )
  })
)

const latestUpload = computed(() => {
  if (!documents.value.length) return '–'
  const sorted = [...documents.value].sort(
    (a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime(),
  )
  const latest = sorted[0]
  return latest ? formatDate(latest.updatedAt) : '–'
})

const canEmbed = computed(() => {
  if (!previewUrl.value || !previewDoc.value) return false
  const t = previewDoc.value.title.toLowerCase()
  return ['.pdf','.png','.jpg','.jpeg','.gif','.webp'].some((ext) => t.endsWith(ext))
})

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') closePreview()
}

let closeMenuTimeout: ReturnType<typeof setTimeout> | null = null

function scheduleCloseMenu() {
  clearCloseMenuTimeout()
  closeMenuTimeout = setTimeout(() => {
    openMenuId.value = null
  }, 90)
}

function clearCloseMenuTimeout() {
  if (closeMenuTimeout) {
    clearTimeout(closeMenuTimeout)
    closeMenuTimeout = null
  }
}

</script>

<template>
  <div class="view-page documents-view">

    <header class="page-header">
      <div>
        <h1>Dokumenter</h1>
        <p class="subtitle">Sentralisert lagring av retningslinjer, prosedyrer og opplæringsfiler</p>
      </div>
      <button v-if="canManageDocuments" class="upload-btn" type="button" @click="openUploadNew">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
        Last opp dokument
      </button>
    </header>

    <UploadDocumentModal
      :open="showUploadModal"
      :succeeds-document="versionTargetDoc ?? undefined"
      @close="closeUploadModal"
      @upload-document="uploadDocument"
      @upload-version="uploadVersion"
    />

    <!-- Upload error -->
    <div v-if="uploadError" class="error-banner" role="alert">
      <span>{{ uploadError }}</span>
    </div>

    <!-- Fetch error -->
    <div v-if="error" class="error-banner" role="alert">
      <span>{{ error }}</span>
      <button class="retry-btn" type="button" @click="fetchDocuments">Prøv igjen</button>
    </div>

    <section class="documents-stats" aria-label="Dokumentstatistikk">
      <article class="documents-stat">
        <strong>{{ isLoading ? '…' : categories.length }}</strong>
        <span>Kategorier</span>
      </article>
      <article class="documents-stat">
        <strong>{{ isLoading ? '…' : documents.length }}</strong>
        <span>Dokumenter totalt</span>
      </article>
      <article class="documents-stat">
        <strong>{{ isLoading ? '…' : latestUpload }}</strong>
        <span>Sist oppdatert</span>
      </article>
      <article class="documents-stat">
        <strong>{{ isLoading ? '…' : filteredDocuments.length }}</strong>
        <span>Viser nå</span>
      </article>
    </section>

    <section class="documents-toolbar" aria-label="Dokumentfiltre">
      <input v-model="query" type="search" placeholder="Søk i dokumenter…" class="search-input" />
      <div class="category-tabs">
        <button
          class="category-tab"
          :class="{ 'category-tab--active': selectedCategory === 'all' }"
          type="button"
          @click="selectedCategory = 'all'"
        >Alle</button>
        <button
          v-for="cat in categories"
          :key="cat"
          class="category-tab"
          :class="{ 'category-tab--active': selectedCategory === cat }"
          type="button"
          @click="selectedCategory = cat"
        >{{ cat }}</button>
      </div>
    </section>

    <div v-if="isLoading" class="skeleton-list" aria-label="Laster dokumenter">
      <div v-for="i in 5" :key="i" class="skeleton-row" />
    </div>

    <section v-else-if="filteredDocuments.length" class="table-wrapper" aria-label="Dokumentliste">
      <table>
        <thead>
          <tr>
            <th>Tittel</th>
            <th>Type</th>
            <th>Versjon</th>
            <th>Sist endret</th>
            <th>Opprettet</th>
            <th>Status</th>
            <th class="actions-col">Handlinger</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="doc in filteredDocuments" :key="doc.documentId">
            <td>
              <p class="table-title">{{ doc.title }}</p>
              <p v-if="doc.description" class="table-subtitle">{{ doc.description }}</p>
            </td>
            <td><span class="type-badge">{{ doc.documentType }}</span></td>
            <td class="mono">v{{ doc.currentVersion }}</td>
            <td class="date-cell">{{ formatDate(doc.updatedAt) }}</td>
            <td class="date-cell">{{ formatDate(doc.createdAt) }}</td>
            <td>
              <span class="status-pill" :class="{ 'status-pill--active': doc.active }">
                {{ doc.active ? 'Aktiv' : 'Arkivert' }}
              </span>
            </td>
            <td class="actions-cell">
              <!-- Preview -->
              <button
                  class="action-btn action-btn--ghost"
                  type="button"
                  title="Forhåndsvis"
                  @click="openPreview(doc)"
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                Vis
              </button>

              <div class="row-menu" @mouseenter="clearCloseMenuTimeout" @mouseleave="scheduleCloseMenu">
                <button
                    class="action-btn action-btn--ghost row-menu__trigger"
                    type="button"
                    :aria-label="`Flere handlinger for ${doc.title}`"
                    :aria-expanded="openMenuId === doc.documentId"
                    @click="openMenuId = openMenuId === doc.documentId ? null : doc.documentId"
                >
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="5" cy="12" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/></svg>
                </button>

                <div v-if="openMenuId === doc.documentId" class="row-menu__dropdown" role="menu" @mouseenter="clearCloseMenuTimeout" @mouseleave="scheduleCloseMenu">
                  <button
                      v-if="canManageDocuments"
                      class="row-menu__item"
                      type="button"
                      role="menuitem"
                      :disabled="downloadingId === doc.documentId"
                      @click="openMenuId = null; downloadDocument(doc)"
                  >
                    <span v-if="downloadingId === doc.documentId" class="spinner-sm" />
                    <svg
                        v-else
                        width="13"
                        height="13"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2.2"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        aria-hidden="true"
                    >
                      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                      <polyline points="7 10 12 15 17 10"/>
                      <line x1="12" y1="15" x2="12" y2="3"/>
                    </svg>
                    {{ downloadingId === doc.documentId ? 'Laster…' : 'Last ned' }}
                  </button>

                  <button
                      class="row-menu__item"
                      type="button"
                      role="menuitem"
                      @click="openMenuId = null; openUploadVersion(doc)"
                  >
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="17 1 21 5 17 9"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 23 3 19 7 15"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/></svg>
                    Last opp ny versjon
                    <span class="row-menu__hint">v{{ doc.currentVersion + 1 }}</span>
                  </button>
                </div>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <section v-else-if="!isLoading" class="empty-state">
      <svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
      <p>Ingen dokumenter matcher filtreringen.</p>
      <button
        v-if="query || selectedCategory !== 'all'"
        class="retry-btn"
        type="button"
        @click="query = ''; selectedCategory = 'all'"
      >Nullstill filter</button>
    </section>

    <Teleport to="body">
      <div
        v-if="previewDoc"
        class="preview-overlay"
        role="dialog"
        :aria-label="`Forhåndsvisning: ${previewDoc.title}`"
        aria-modal="true"
        @keydown="onKeydown"
        @click.self="closePreview"
      >
        <div class="preview-modal">
          <div class="preview-header">
            <div class="preview-header__info">
              <h2 class="preview-title">{{ previewDoc.title }}</h2>
              <p v-if="previewDoc.description" class="preview-subtitle">{{ previewDoc.description }}</p>
              <div class="preview-meta">
                <span class="type-badge">{{ previewDoc.documentType }}</span>
                <span class="meta-item">v{{ previewDoc.currentVersion }}</span>
                <span class="meta-item">Oppdatert {{ formatDate(previewDoc.updatedAt) }}</span>
              </div>
            </div>
            <div class="preview-header__actions">
              <button class="action-btn action-btn--ghost" type="button" @click="downloadDocument(previewDoc)">
                Last ned
              </button>
              <button
                v-if="canManageDocuments"
                class="action-btn action-btn--ghost"
                type="button"
                @click="() => { closePreview(); openUploadVersion(previewDoc!) }"
              >
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="17 1 21 5 17 9"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 23 3 19 7 15"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/></svg>
                Ny versjon
              </button>
              <button class="action-btn action-btn--icon" type="button" aria-label="Lukk forhåndsvisning" @click="closePreview">
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
          </div>

          <div class="preview-body">
            <div v-if="isLoadingPreview" class="preview-center">
              <span class="spinner-lg" />
              <p>Laster dokument…</p>
            </div>
            <div v-else-if="previewError" class="preview-center preview-center--error">
              <p>{{ previewError }}</p>
              <button class="action-btn action-btn--ghost" type="button" @click="downloadDocument(previewDoc)">Last ned i stedet</button>
            </div>
            <iframe
              v-else-if="previewUrl && canEmbed"
              :src="previewUrl"
              class="preview-iframe"
              :title="`Forhåndsvisning: ${previewDoc.title}`"
            />
            <div v-else-if="previewUrl" class="preview-center">
              <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
              <p>Forhåndsvisning ikke tilgjengelig for denne filtypen.</p>
              <button class="action-btn action-btn--ghost" type="button" @click="downloadDocument(previewDoc)">Last ned for å åpne</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.documents-view { display: grid; gap: 1rem; }

/* Header */
.page-header { display: flex; justify-content: space-between; align-items: flex-end; gap: 1rem; }
.page-header h1 { margin: 0; font-size: var(--font-size-3xl); font-weight: 700; letter-spacing: -0.015em; }
.subtitle { margin-top: 0.4rem; color: var(--color-gray-500); }

.upload-btn {
  display: inline-flex; align-items: center; gap: 0.45rem;
  padding: 0.6rem 1.1rem; border-radius: var(--radius-md);
  background: var(--color-foreground); color: var(--color-card);
  font-size: var(--font-size-sm); font-weight: 600; font-family: inherit;
  cursor: pointer; border: none; white-space: nowrap;
  transition: opacity var(--transition-fast); flex-shrink: 0;
}
.upload-btn:hover { opacity: 0.85; }

/* Banners */
.error-banner {
  display: flex; align-items: center; justify-content: space-between; gap: 1rem;
  padding: 0.75rem 1rem; border-radius: var(--radius-md);
  background: var(--color-danger-bg); color: var(--color-danger);
  border: 1px solid var(--color-danger-bg); font-size: var(--font-size-sm);
}

/* Stats */
.documents-stats { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 0.75rem; }
.documents-stat { border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-card); text-align: center; padding: 0.85rem; }
.documents-stat strong { display: block; font-size: var(--font-size-xl); color: var(--color-gray-900); }
.documents-stat span { display: block; margin-top: 0.2rem; font-size: var(--font-size-xs); color: var(--color-gray-500); }

/* Toolbar */
.documents-toolbar { border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-card); padding: 0.75rem; display: grid; gap: 0.75rem; }
.search-input { width: 100%; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-gray-50); min-height: 2.6rem; padding: 0 0.85rem; color: var(--color-gray-700); font-size: var(--font-size-sm); }
.category-tabs { display: flex; flex-wrap: wrap; gap: 0.5rem; }
.category-tab { min-height: 2.15rem; padding: 0.35rem 0.75rem; border-radius: var(--radius-md); background: var(--color-card); border: 1px solid var(--color-border); font-size: var(--font-size-sm); font-weight: 500; color: var(--color-gray-600); cursor: pointer; transition: all 0.15s; }
.category-tab:hover { border-color: var(--color-gray-400); }
.category-tab--active { background: var(--color-foreground); color: var(--color-background); border-color: var(--color-foreground); }

/* Skeleton */
.skeleton-list { display: grid; gap: 0.5rem; }
.skeleton-row { height: 3.25rem; border-radius: var(--radius-md); background: linear-gradient(90deg, var(--color-gray-100) 25%, var(--color-gray-50) 50%, var(--color-gray-100) 75%); background-size: 200% 100%; animation: shimmer 1.4s infinite; }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

/* Table */
.table-wrapper { border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-card); overflow-x: auto; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 0.7rem 0.85rem; text-align: left; border-bottom: 1px solid var(--color-gray-100); vertical-align: middle; }
tbody tr:last-child td { border-bottom: none; }
tbody tr:hover { background: var(--color-gray-50); }
th { font-size: var(--font-size-xs); text-transform: uppercase; letter-spacing: 0.06em; color: var(--color-gray-500); background: var(--color-gray-50); white-space: nowrap; }
.table-title { font-size: var(--font-size-sm); font-weight: 500; color: var(--color-gray-900); margin: 0; }
.table-subtitle { font-size: var(--font-size-xs); color: var(--color-gray-500); margin: 0.2rem 0 0; max-width: 28ch; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.mono { font-family: monospace; font-size: var(--font-size-xs); color: var(--color-gray-600); }
.date-cell { white-space: nowrap; font-size: var(--font-size-xs); color: var(--color-gray-600); }
.actions-col { width: 1%; white-space: nowrap; }
.actions-cell { display: flex; gap: 0.4rem; align-items: center; flex-wrap: nowrap; }

/* Badges */
.type-badge { display: inline-flex; padding: 0.2rem 0.55rem; border-radius: 999px; background: var(--color-gray-100); color: var(--color-gray-700); font-size: var(--font-size-xs); font-weight: 600; white-space: nowrap; }
.status-pill { display: inline-flex; padding: 0.2rem 0.55rem; border-radius: 999px; background: var(--color-gray-100); color: var(--color-gray-600); font-size: var(--font-size-xs); font-weight: 600; white-space: nowrap; }
.status-pill--active { background: var(--color-success-bg); color: var(--color-success); }

/* Action buttons */
.action-btn {
  display: inline-flex; align-items: center; gap: 0.35rem;
  padding: 0.35rem 0.7rem; border-radius: var(--radius-sm);
  font-size: var(--font-size-xs); font-weight: 600; font-family: inherit;
  cursor: pointer; transition: all 0.15s; white-space: nowrap;
}
.action-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.action-btn--ghost { background: transparent; color: var(--color-gray-600); border: 1px solid var(--color-border); }
.action-btn--ghost:hover:not(:disabled) { background: var(--color-gray-50); border-color: var(--color-gray-400); }

/* ⋯ row menu */
.row-menu { position: relative; }
.row-menu__trigger { padding: 0.35rem 0.5rem; }
.row-menu__dropdown {
  position: absolute; right: 0; top: calc(100% + 4px); z-index: 50;
  background: var(--color-card); border: 1px solid var(--color-border);
  border-radius: var(--radius-md); box-shadow: var(--shadow-md);
  min-width: 13rem; padding: 0.25rem;
}
.row-menu__item {
  display: flex; align-items: center; gap: 0.5rem; width: 100%;
  padding: 0.5rem 0.65rem; border-radius: var(--radius-sm);
  background: transparent; border: none; font-family: inherit;
  font-size: var(--font-size-xs); font-weight: 500; color: var(--color-gray-700);
  cursor: pointer; text-align: left; transition: background var(--transition-fast);
}
.row-menu__item:hover { background: var(--color-gray-100); }
.row-menu__hint {
  margin-left: auto; font-size: var(--font-size-xs);
  color: var(--color-gray-400); font-weight: 400;
}

.retry-btn { padding: 0.3rem 0.75rem; border-radius: var(--radius-sm); background: transparent; border: 1px solid currentColor; font-size: var(--font-size-xs); font-weight: 600; cursor: pointer; }

/* Empty state */
.empty-state { display: flex; flex-direction: column; align-items: center; gap: 0.75rem; padding: 3rem 2rem; background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); color: var(--color-gray-500); text-align: center; }
.empty-state svg { opacity: 0.4; }

/* Spinners */
.spinner-sm { display: inline-block; width: 12px; height: 12px; border: 2px solid color-mix(in srgb, var(--color-primary-foreground) 40%, transparent); border-top-color: var(--color-primary-foreground); border-radius: 50%; animation: spin 0.7s linear infinite; }
.spinner-lg { display: inline-block; width: 36px; height: 36px; border: 3px solid var(--color-gray-200); border-top-color: var(--color-foreground); border-radius: 50%; animation: spin 0.7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* Preview modal */
.preview-overlay { position: fixed; inset: 0; z-index: 1000; background: rgba(0, 39, 43, 0.58); display: flex; align-items: center; justify-content: center; padding: var(--spacing-lg); }
.preview-modal { background: var(--color-surface-raised); border: none; border-radius: var(--radius-sm); box-shadow: var(--shadow-md); display: flex; flex-direction: column; width: min(900px, 100%); height: min(85vh, 800px); overflow: hidden; }
.preview-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; padding: var(--spacing-lg); border-bottom: 1px solid var(--color-border); flex-shrink: 0; }
.preview-header__info { min-width: 0; }
.preview-title { margin: 0; font-size: var(--font-size-base); font-weight: 700; color: var(--color-foreground); }
.preview-subtitle { margin: 0.2rem 0 0; font-size: var(--font-size-xs); color: var(--color-gray-500); }
.preview-meta { display: flex; flex-wrap: wrap; align-items: center; gap: 0.5rem; margin-top: 0.5rem; }
.meta-item { font-size: var(--font-size-xs); color: var(--color-gray-500); }
.preview-header__actions { display: flex; align-items: center; gap: 0.5rem; flex-shrink: 0; }
.close-btn { display: inline-flex; align-items: center; justify-content: center; width: 2rem; height: 2rem; border-radius: var(--radius-sm); background: transparent; border: none; color: var(--color-foreground); cursor: pointer; transition: all var(--transition-fast); }
.close-btn:hover { background: var(--color-info-bg); }
.preview-body { flex: 1; overflow: hidden; display: flex; flex-direction: column; }
.preview-iframe { width: 100%; height: 100%; border: none; flex: 1; }
.preview-center { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 1rem; padding: 2rem; text-align: center; color: var(--color-gray-600); font-size: var(--font-size-sm); }
.preview-center--error { color: var(--color-danger); }
.preview-center svg { opacity: 0.35; }

/* Responsive */
@media (max-width: 48rem) {
  .page-header { flex-direction: column; align-items: flex-start; }
  .documents-stats { grid-template-columns: repeat(2, 1fr); }
  th:nth-child(n + 4), td:nth-child(n + 4) { display: none; }
  .actions-cell { flex-wrap: wrap; }
  .preview-overlay { padding: 0; align-items: flex-end; }
  .preview-modal { width: 100%; height: 92vh; border-bottom-left-radius: 0; border-bottom-right-radius: 0; }
}
</style>
