<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import BaseModal from '@/shared/components/BaseModal.vue'
import { useAuthStore } from '@/stores/auth'
import { useIkMatData } from '../composables/useIkMatData'

const authStore = useAuthStore()

const {
  haccpPlan,
  formatDate,
  createHaccpControlPoint,
  updateHaccpControlPoint,
  deleteHaccpControlPoint,
  uploadSupportingDocument,
  updateSupportingDocument,
  deleteSupportingDocument,
  downloadSupportingDocument,
} = useIkMatData()

const canAdmin = computed(() => authStore.hasRole('ADMIN'))

const pointModalOpen = ref(false)
const pointMode = ref<'create' | 'edit'>('create')
const pointError = ref<string | null>(null)
const pointInFlight = ref(false)
const editingPointId = ref<number | null>(null)

const pointForm = reactive({
  name: '',
  description: '',
  hazardsText: 'Biologisk fare, Temperaturavvik',
  critical_limits: '',
  monitoring: 'Daglig',
  corrective_actions: 'Registrer avvik og gjennomfor korrigerende tiltak',
  verification: 'Daglig gjennomgang av ansvarlig leder',
  responsible: 'Driftsansvarlig',
})

const docUploadModalOpen = ref(false)
const docEditModalOpen = ref(false)
const docOpenModalOpen = ref(false)
const docError = ref<string | null>(null)
const docInFlight = ref(false)
const selectedDocId = ref<number | null>(null)
const selectedDocName = ref('')
const openDocMenuId = ref<number | null>(null)
const openPointMenuId = ref<number | null>(null)

const docForm = reactive({
  title: '',
  description: '',
  file: null as File | null,
})

const ccpStatusSummary = computed(() => {
  const total = haccpPlan.critical_control_points.length
  const followUp = haccpPlan.critical_control_points.filter((point) => {
    return point.name.toLowerCase().includes('varmholding') || point.name.toLowerCase().includes('nedkjøling')
  }).length

  return {
    total,
    followUp,
    ok: total - followUp,
  }
})

const resetPointForm = () => {
  pointForm.name = ''
  pointForm.description = ''
  pointForm.hazardsText = 'Biologisk fare, Temperaturavvik'
  pointForm.critical_limits = ''
  pointForm.monitoring = 'Daglig'
  pointForm.corrective_actions = 'Registrer avvik og gjennomfor korrigerende tiltak'
  pointForm.verification = 'Daglig gjennomgang av ansvarlig leder'
  pointForm.responsible = 'Driftsansvarlig'
}

const openCreatePointModal = () => {
  pointMode.value = 'create'
  editingPointId.value = null
  resetPointForm()
  pointError.value = null
  pointModalOpen.value = true
}

const openEditPointModal = (point: (typeof haccpPlan.critical_control_points)[number]) => {
  openPointMenuId.value = null
  pointMode.value = 'edit'
  editingPointId.value = point.id
  pointForm.name = point.name
  pointForm.description = point.description
  pointForm.hazardsText = point.hazards.join(', ')
  pointForm.critical_limits = point.critical_limits
  pointForm.monitoring = point.monitoring
  pointForm.corrective_actions = point.corrective_actions
  pointForm.verification = point.verification
  pointForm.responsible = point.responsible
  pointError.value = null
  pointModalOpen.value = true
}

const submitPoint = async () => {
  if (!pointForm.name.trim() || !pointForm.critical_limits.trim()) {
    return
  }

  pointError.value = null
  pointInFlight.value = true

  try {
    const payload = {
      name: pointForm.name.trim(),
      description: pointForm.description.trim() || 'Ingen beskrivelse registrert',
      hazards: pointForm.hazardsText.split(',').map((s) => s.trim()).filter(Boolean),
      critical_limits: pointForm.critical_limits.trim(),
      monitoring: pointForm.monitoring.trim() || 'Daglig',
      corrective_actions: pointForm.corrective_actions.trim() || 'Registrer avvik og gjennomfor korrigerende tiltak',
      verification: pointForm.verification.trim() || 'Daglig gjennomgang av ansvarlig leder',
      responsible: pointForm.responsible.trim() || 'Driftsansvarlig',
    }

    if (pointMode.value === 'create') {
      await createHaccpControlPoint(payload)
    } else if (editingPointId.value != null) {
      await updateHaccpControlPoint(editingPointId.value, payload)
    }

    pointModalOpen.value = false
  } catch {
    pointError.value = 'Kunne ikke lagre kontrollpunkt. Prov igjen.'
  } finally {
    pointInFlight.value = false
  }
}

const removePoint = async (id: number) => {
  if (!canAdmin.value) return
  if (!window.confirm('Slette dette kontrollpunktet?')) return
  try {
    await deleteHaccpControlPoint(id)
    openPointMenuId.value = null
  } catch {
    pointError.value = 'Kunne ikke slette kontrollpunkt.'
  }
}

const togglePointMenu = (pointId: number) => {
  openPointMenuId.value = openPointMenuId.value === pointId ? null : pointId
}

const openDocUploadModal = () => {
  docForm.title = ''
  docForm.description = ''
  docForm.file = null
  docError.value = null
  docUploadModalOpen.value = true
}

const handleDocFile = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0] ?? null
  docForm.file = file
  if (file && file.type !== 'application/pdf') {
    docError.value = 'Kun PDF-filer er tillatt.'
    docForm.file = null
  }
}

const submitDocUpload = async () => {
  if (!docForm.file) return
  docError.value = null
  docInFlight.value = true
  try {
    await uploadSupportingDocument(docForm.file, docForm.title || docForm.file.name, docForm.description)
    docUploadModalOpen.value = false
  } catch {
    docError.value = 'Kunne ikke laste opp dokumentet.'
  } finally {
    docInFlight.value = false
  }
}

const openDocModal = (doc: (typeof haccpPlan.supporting_documents)[number]) => {
  openDocMenuId.value = null
  selectedDocId.value = doc.id
  selectedDocName.value = doc.name
  docOpenModalOpen.value = true
}

const toggleDocMenu = (docId: number) => {
  openDocMenuId.value = openDocMenuId.value === docId ? null : docId
}

const downloadCurrentDoc = async () => {
  if (selectedDocId.value == null) return
  const blob = await downloadSupportingDocument(selectedDocId.value)
  const url = URL.createObjectURL(blob)
  window.open(url, '_blank', 'noopener,noreferrer')
}

const openEditDocModal = (doc: (typeof haccpPlan.supporting_documents)[number]) => {
  openDocMenuId.value = null
  selectedDocId.value = doc.id
  docForm.title = doc.name
  docForm.description = doc.description
  docError.value = null
  docEditModalOpen.value = true
}

const submitDocEdit = async () => {
  if (selectedDocId.value == null || !docForm.title.trim()) return
  docError.value = null
  docInFlight.value = true
  try {
    await updateSupportingDocument(selectedDocId.value, {
      title: docForm.title.trim(),
      description: docForm.description.trim() || '',
    })
    docEditModalOpen.value = false
  } catch {
    docError.value = 'Kunne ikke oppdatere dokument.'
  } finally {
    docInFlight.value = false
  }
}

const removeDocument = async (docId: number) => {
  if (!canAdmin.value) return
  if (!window.confirm('Slette dette dokumentet?')) return
  try {
    await deleteSupportingDocument(docId)
    openDocMenuId.value = null
  } catch {
    docError.value = 'Kunne ikke slette dokument.'
  }
}
</script>

<template>
  <div class="haccp-page">
    <header class="page-header">
      <h1>HACCP-plan</h1>
      <p class="subtitle">{{ haccpPlan.plan_name }}</p>
      <p class="meta">Versjon {{ haccpPlan.version }} · oppdatert {{ formatDate(haccpPlan.last_updated) }}</p>
    </header>

    <section class="summary-grid" aria-label="HACCP sammendrag">
      <article class="summary-card summary-card--good">
        <p>CCP OK</p>
        <strong>{{ ccpStatusSummary.ok }}</strong>
      </article>
      <article class="summary-card summary-card--warn">
        <p>Krever oppfølging</p>
        <strong>{{ ccpStatusSummary.followUp }}</strong>
      </article>
      <article class="summary-card summary-card--info">
        <p>Totale CCP</p>
        <strong>{{ ccpStatusSummary.total }}</strong>
      </article>
    </section>

    <section class="table-card" aria-label="Kritiske kontrollpunkter">
      <div class="table-card__header">
        <h2>Kritiske kontrollpunkter</h2>
      </div>
      <table>
        <thead>
          <tr>
            <th>CCP</th>
            <th>Prosesstrinn</th>
            <th>Farer</th>
            <th>Kritisk grense</th>
            <th>Overvåking</th>
            <th>Korrigerende tiltak</th>
            <th>Ansvarlig</th>
            <th v-if="canAdmin">Handlinger</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="point in haccpPlan.critical_control_points" :key="point.id">
            <td>{{ point.number }}</td>
            <td>{{ point.name }}</td>
            <td>{{ point.hazards.join(', ') }}</td>
            <td>{{ point.critical_limits }}</td>
            <td>{{ point.monitoring }}</td>
            <td>{{ point.corrective_actions }}</td>
            <td>{{ point.responsible }}</td>
            <td v-if="canAdmin" class="admin-cell">
              <div class="options-menu">
                <button
                  class="options-menu__trigger"
                  type="button"
                  aria-label="Apne handlinger"
                  :aria-expanded="openPointMenuId === point.id"
                  @click="togglePointMenu(point.id)"
                >
                  <span class="dot" />
                  <span class="dot" />
                  <span class="dot" />
                </button>

                <div v-if="openPointMenuId === point.id" class="options-menu__list" role="menu">
                  <button type="button" role="menuitem" class="options-menu__item" @click="openEditPointModal(point)">Rediger</button>
                  <button type="button" role="menuitem" class="options-menu__item options-menu__item--danger" @click="removePoint(point.id)">Slett</button>
                </div>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <div v-if="canAdmin" class="table-card__actions">
      <button type="button" class="admin-btn" @click="openCreatePointModal">+ Nytt kontrollpunkt</button>
    </div>

    <section class="docs-card" aria-label="Støttedokumenter">
      <div class="docs-header">
        <h2>Støttedokumenter</h2>
        <button v-if="canAdmin" type="button" class="admin-btn" @click="openDocUploadModal">+ Last opp PDF</button>
      </div>
      <ul>
        <li v-for="doc in haccpPlan.supporting_documents" :key="doc.id" class="doc-item" @click="openDocModal(doc)">
          <span class="doc-icon" aria-hidden="true">PDF</span>
          <div class="doc-body">
            <p>{{ doc.name }}</p>
            <span>Oppdatert {{ formatDate(doc.date_updated) }} · {{ doc.description }}</span>
          </div>
          <div v-if="canAdmin" class="options-menu" @click.stop>
            <button
              class="options-menu__trigger"
              type="button"
              aria-label="Apne handlinger"
              :aria-expanded="openDocMenuId === doc.id"
              @click="toggleDocMenu(doc.id)"
            >
              <span class="dot" />
              <span class="dot" />
              <span class="dot" />
            </button>

            <div v-if="openDocMenuId === doc.id" class="options-menu__list" role="menu">
              <button type="button" role="menuitem" class="options-menu__item" @click="openEditDocModal(doc)">Rediger</button>
              <button type="button" role="menuitem" class="options-menu__item options-menu__item--danger" @click="removeDocument(doc.id)">Slett</button>
            </div>
          </div>
        </li>
      </ul>
    </section>

    <BaseModal :open="pointModalOpen" :title="pointMode === 'create' ? 'Nytt kontrollpunkt' : 'Rediger kontrollpunkt'" @close="pointModalOpen = false">
      <form class="modal-form" @submit.prevent="submitPoint">
        <label>Navn<input v-model="pointForm.name" type="text" required /></label>
        <label>Beskrivelse<textarea v-model="pointForm.description" rows="2" /></label>
        <label>Farer (kommaseparert)<input v-model="pointForm.hazardsText" type="text" /></label>
        <label>Kritisk grense<textarea v-model="pointForm.critical_limits" rows="2" required /></label>
        <label>Overvaking<input v-model="pointForm.monitoring" type="text" /></label>
        <label>Korrigerende tiltak<textarea v-model="pointForm.corrective_actions" rows="2" /></label>
        <label>Verifikasjon<textarea v-model="pointForm.verification" rows="2" /></label>
        <label>Ansvarlig<input v-model="pointForm.responsible" type="text" /></label>
        <p v-if="pointError" class="modal-form__error">{{ pointError }}</p>
      </form>
      <template #footer>
        <button type="button" class="modal-btn modal-btn--ghost" @click="pointModalOpen = false">Avbryt</button>
        <button type="button" class="modal-btn" :disabled="pointInFlight" @click="submitPoint">Lagre</button>
      </template>
    </BaseModal>

    <BaseModal :open="docUploadModalOpen" title="Last opp dokument (PDF)" @close="docUploadModalOpen = false">
      <form class="modal-form" @submit.prevent="submitDocUpload">
        <label>Tittel<input v-model="docForm.title" type="text" /></label>
        <label>Beskrivelse<textarea v-model="docForm.description" rows="2" /></label>
        <label>PDF-fil<input type="file" accept="application/pdf" @change="handleDocFile" required /></label>
        <p v-if="docError" class="modal-form__error">{{ docError }}</p>
      </form>
      <template #footer>
        <button type="button" class="modal-btn modal-btn--ghost" @click="docUploadModalOpen = false">Avbryt</button>
        <button type="button" class="modal-btn" :disabled="docInFlight || !docForm.file" @click="submitDocUpload">Last opp</button>
      </template>
    </BaseModal>

    <BaseModal :open="docEditModalOpen" title="Rediger dokument" @close="docEditModalOpen = false">
      <form class="modal-form" @submit.prevent="submitDocEdit">
        <label>Tittel<input v-model="docForm.title" type="text" required /></label>
        <label>Beskrivelse<textarea v-model="docForm.description" rows="2" /></label>
        <p v-if="docError" class="modal-form__error">{{ docError }}</p>
      </form>
      <template #footer>
        <button type="button" class="modal-btn modal-btn--ghost" @click="docEditModalOpen = false">Avbryt</button>
        <button type="button" class="modal-btn" :disabled="docInFlight" @click="submitDocEdit">Lagre</button>
      </template>
    </BaseModal>

    <BaseModal :open="docOpenModalOpen" :title="selectedDocName" @close="docOpenModalOpen = false">
      <p>Dokumentet kan apnes i ny fane eller lastes ned.</p>
      <template #footer>
        <button type="button" class="modal-btn modal-btn--ghost" @click="docOpenModalOpen = false">Lukk</button>
        <button type="button" class="modal-btn" @click="downloadCurrentDoc">Apne / Last ned</button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.haccp-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 1.25rem;
}

.page-header h1 {
  margin: 0;
  font-size: var(--font-size-2xl);
  color: var(--ik-mat-primary);
}

.subtitle {
  margin: 0.35rem 0 0;
  color: var(--color-foreground);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
}

.meta {
  margin: 0.2rem 0 0;
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr));
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.summary-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  padding: 0.8rem;
}

.summary-card p {
  margin: 0;
  color: var(--color-gray-600);
  font-size: var(--font-size-xs);
}

.summary-card strong {
  display: block;
  margin-top: 0.35rem;
  color: var(--color-foreground);
  font-size: 1.45rem;
}

.summary-card--good {
  border-left: 0.25rem solid var(--color-success);
}

.summary-card--warn {
  border-left: 0.25rem solid var(--color-warning);
}

.summary-card--info {
  border-left: 0.25rem solid var(--color-info);
}

.table-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  padding: 0.7rem;
  overflow-x: auto;
  margin-bottom: 0.95rem;
}

.table-card h2,
.docs-card h2 {
  margin: 0 0 0.65rem;
  font-size: var(--font-size-base);
  color: var(--color-foreground);
}

.table-card__header,
.docs-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.6rem;
}

.docs-header {
  flex-direction: column;
  margin-bottom: 0.75rem;
}

.table-card__actions {
  display: flex;
  justify-content: center;
  margin-top: 0.75rem;
  margin-bottom: 0.95rem;
}

.admin-btn {
  border: 1px solid var(--ik-mat-primary);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--ik-mat-primary) 10%, var(--color-card));
  color: var(--ik-mat-primary);
  padding: 0.4rem 0.7rem;
  font-size: var(--font-size-xs);
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  text-align: left;
  padding: 0.55rem;
  border-bottom: 1px solid var(--color-border);
  font-size: var(--font-size-sm);
  vertical-align: top;
}

th {
  font-size: var(--font-size-xs);
  color: var(--color-gray-600);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

td {
  color: var(--color-foreground);
}

.docs-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  padding: 0.8rem;
}

.docs-card ul {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 0.6rem;
}

.doc-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 0.5rem;
  align-items: center;
  cursor: pointer;
  position: relative;
}

.doc-icon {
  font-size: 1.1rem;
}

.doc-body {
  min-width: 0;
}

.admin-cell {
  text-align: right;
}

.options-menu {
  position: relative;
}

.options-menu__trigger {
  aspect-ratio: 1 / 1;
  width: var(--touch-target);
  height: var(--touch-target);
  min-width: var(--touch-target);
  min-height: var(--touch-target);
  border: 1px solid var(--color-border);
  background: var(--color-card);
  border-radius: var(--radius-sm);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.15rem;
  cursor: pointer;
  padding: 0;
}

.dot {
  width: 0.2rem;
  height: 0.2rem;
  background: var(--color-gray-600);
  border-radius: 100%;
}

.options-menu__list {
  position: absolute;
  top: calc(100% + 0.25rem);
  right: 0;
  z-index: 20;
  min-width: 9rem;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.options-menu__item {
  width: 100%;
  border: 0;
  background: transparent;
  padding: 0.6rem 0.8rem;
  text-align: left;
  color: var(--color-foreground);
  font-size: var(--font-size-sm);
}

.options-menu__item:hover {
  background: var(--color-accent);
}

.options-menu__item--danger {
  color: var(--color-danger);
}

.modal-form {
  display: grid;
  gap: 0.6rem;
}

.modal-form label {
  display: grid;
  gap: 0.25rem;
  font-size: var(--font-size-sm);
}

.modal-form input,
.modal-form textarea {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0.5rem 0.65rem;
  background: var(--color-card);
}

.modal-form__error {
  margin: 0;
  color: var(--color-danger);
  font-size: var(--font-size-xs);
}

.modal-btn {
  border: 1px solid var(--ik-mat-primary);
  background: var(--ik-mat-primary);
  color: var(--color-primary-foreground);
  border-radius: var(--radius-sm);
  padding: 0.45rem 0.8rem;
}

.modal-btn--ghost {
  border-color: var(--color-border);
  background: transparent;
  color: var(--color-foreground);
}

.docs-card li {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0.65rem;
  background: color-mix(in srgb, var(--color-accent) 45%, var(--color-card));
}

.docs-card p {
  margin: 0;
  color: var(--color-foreground);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
}

.docs-card span {
  display: block;
  margin-top: 0.2rem;
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
}
</style>
