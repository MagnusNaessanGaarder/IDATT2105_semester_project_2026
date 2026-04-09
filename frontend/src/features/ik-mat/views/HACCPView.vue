<script setup lang="ts">
import BaseModal from '@/shared/components/BaseModal.vue'
import { useIkMatData } from '../composables/useIkMatData'
import { useHaccpViewState } from '../composables/useHaccpViewState'

const { formatDate } = useIkMatData()

const {
  haccpPlan,
  canAdmin,
  pointModalOpen,
  pointMode,
  pointError,
  pointInFlight,
  pointForm,
  docUploadModalOpen,
  docEditModalOpen,
  docOpenModalOpen,
  docError,
  docInFlight,
  selectedDocName,
  openDocMenuId,
  openPointMenuId,
  docForm,
  ccpStatusSummary,
  openCreatePointModal,
  openEditPointModal,
  submitPoint,
  removePoint,
  togglePointMenu,
  openDocUploadModal,
  handleDocFile,
  submitDocUpload,
  openDocModal,
  toggleDocMenu,
  downloadCurrentDoc,
  openEditDocModal,
  submitDocEdit,
  removeDocument,
} = useHaccpViewState()
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
        <button v-if="canAdmin" type="button" class="admin-btn" @click="openCreatePointModal">+ Nytt kontrollpunkt</button>
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
  align-items: center;
  gap: 0.6rem;
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
  border: 1px solid var(--color-border);
  background: var(--color-card);
  border-radius: var(--radius-sm);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.15rem;
  cursor: pointer;
  width: 2rem;
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
  color: #fff;
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
