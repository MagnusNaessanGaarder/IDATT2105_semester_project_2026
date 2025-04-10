<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useAlkoholData } from '../composables/useAlkoholData'
import { useAuthStore } from '@/stores/auth'
import { createChecklistTemplate, createRun, uncompleteRun, updateChecklistTemplate, updateRunItem } from '../api/checklistsRun'
import ControllItem from '../components/ControllItem.vue'
import ControlProgressCard from '../components/ControlProgressCard.vue'
import BaseModal from '@/shared/components/BaseModal.vue'
import type { DailyControlItem } from '../types'
import { serializeLawReference } from '../utils/lawReference'

const { dailyControls, laws, reload } = useAlkoholData()
const authStore = useAuthStore()

const controls = ref<DailyControlItem[]>([])
const filter = ref<'all' | 'checked' | 'pending'>('all')
const isAdmin = computed(() => authStore.isAdmin)

const detailsItem = ref<DailyControlItem | null>(null)
const showFormModal = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const editingItemId = ref<number | null>(null)

const formState = ref({
  name: '',
  law_document_id: '',
  employee: '',
  comment: '',
  completion_date: {
    date: '',
    time: '',
  },
  attachment: '',
  is_checked: false,
})
const isSubmittingItemId = ref<number | null>(null)
const isSavingForm = ref(false)

const todayDateString = () => {
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

const toCompletionDateParts = (value?: string | null) => {
  if (!value) {
    return {
      date: '',
      time: '',
    }
  }

  if (value.includes('T')) {
    const [date = '', time = ''] = value.split('T')
    return {
      date,
      time: time.replace('Z', '').slice(0, 8),
    }
  }

  return {
    date: value.slice(0, 10),
    time: '',
  }
}

const completed = computed(() => controls.value.filter((item) => item.is_checked).length)
const total = computed(() => controls.value.length)

const toggleControl = async (id: number) => {
  const selected = controls.value.find((item) => item.id === id)
  const orgNumber = authStore.currentOrg?.orgNumber

  if (!selected || !orgNumber || isSubmittingItemId.value === selected.id) {
    return
  }

  isSubmittingItemId.value = selected.id

  try {
    if (selected.is_checked && selected.run_id && selected.run_status === 'COMPLETED') {
      await uncompleteRun(selected.run_id, orgNumber)
      controls.value = controls.value.map((item) =>
        item.id === selected.id
          ? {
              ...item,
              is_checked: false,
              run_status: 'DRAFT',
            }
          : item,
      )
      return
    }

    if (!selected.template_item_id) {
      return
    }

    let runId = selected.run_id
    let runItemId = selected.run_item_id

    if (!runId) {
      if (!selected.template_id) {
        return
      }

      const createdRun = await createRun(
        selected.template_id,
        todayDateString(),
        orgNumber,
      )

      runId = createdRun.runId ?? null

      if (!runId) {
        return
      }

      // Find the run item matching our template item in the newly created run
      const matchingItem = createdRun.items?.find(
        (item) => item.templateItemId === selected.template_item_id,
      )
      runItemId = matchingItem?.runItemId ?? null
    }

    if (!runItemId) {
      return
    }

    const updatedItem = await updateRunItem(
      runId,
      runItemId,
      orgNumber,
      !selected.is_checked,
    )
    controls.value = controls.value.map((item) =>
      item.id === selected.id
        ? {
            ...item,
            run_id: runId,
            is_checked: !selected.is_checked,
            run_status: !selected.is_checked ? 'DRAFT' : item.run_status,
            employee: !selected.is_checked ? authStore.userDisplayName : item.employee,
            completion_date: !selected.is_checked
              ? toCompletionDateParts(updatedItem.updatedAt ?? updatedItem.createdAt ?? null)
              : item.completion_date,
          }
        : item,
    )
  } catch (error) {
    console.error('Failed to complete checklist run', error)
  } finally {
    isSubmittingItemId.value = null
  }
}

const openDetails = (id: number) => {
  const selected = controls.value.find((item) => item.id === id)
  if (!selected) {
    return
  }

  detailsItem.value = selected
}

const closeDetails = () => {
  detailsItem.value = null
}

const resetForm = () => {
  formState.value = {
    name: '',
    law_document_id: '',
    employee: '',
    comment: '',
    completion_date: {
      date: '',
      time: '',
    },
    attachment: '',
    is_checked: false,
  }
}

const lawOptions = computed(() =>
  laws.value
    .map((law) => ({
      value: String(law.documentId),
      label: law.name,
    }))
    .sort((left, right) => left.label.localeCompare(right.label, 'nb')),
)

const selectedLaw = computed(() => {
  const documentId = Number(formState.value.law_document_id)
  if (!Number.isFinite(documentId)) {
    return null
  }

  return laws.value.find((law) => law.documentId === documentId) ?? null
})

const openAddModal = () => {
  formMode.value = 'add'
  editingItemId.value = null
  resetForm()
  showFormModal.value = true
}

const openEditModal = (id: number) => {
  const selected = controls.value.find((item) => item.id === id)
  if (!selected) {
    return
  }

  formMode.value = 'edit'
  editingItemId.value = id
  formState.value = {
    name: selected.name,
    law_document_id:
      selected.law_document_id != null
        ? String(selected.law_document_id)
        : String(laws.value.find((law) => law.name === selected.law_unit)?.documentId ?? ''),
    employee: selected.employee,
    comment: selected.comment,
    completion_date: {
      date: selected.completion_date.date,
      time: selected.completion_date.time,
    },
    attachment: selected.attachment ?? '',
    is_checked: selected.is_checked,
  }
  showFormModal.value = true
}

const closeFormModal = () => {
  showFormModal.value = false
}

const saveControlItem = async () => {
  if (!formState.value.name.trim() || !selectedLaw.value) {
    return
  }

  if (formMode.value === 'add') {
    const orgNumber = authStore.currentOrg?.orgNumber
    if (!orgNumber || isSavingForm.value) {
      return
    }

    isSavingForm.value = true

    try {
      const createdTemplate = await createChecklistTemplate(
        {
          title: formState.value.name.trim(),
          description: serializeLawReference(selectedLaw.value),
          moduleType: 'ALCOHOL',
          frequency: 'DAILY',
          items: [
            {
              sortOrder: 1,
              label: formState.value.name.trim(),
              description: formState.value.comment.trim() || selectedLaw.value.name,
              itemType: 'BOOLEAN',
              isRequired: true,
            },
          ],
        },
        orgNumber,
      )

      await createRun(
        createdTemplate.templateId,
        todayDateString(),
        orgNumber,
      )

      await reload()
    } catch (error) {
      console.error('Failed to create daily checklist template', error)
      return
    } finally {
      isSavingForm.value = false
    }
  }

  if (formMode.value === 'edit' && editingItemId.value !== null) {
    const orgNumber = authStore.currentOrg?.orgNumber
    const selected = controls.value.find((item) => item.id === editingItemId.value)

    if (!orgNumber || !selected?.template_id || isSavingForm.value) {
      return
    }

    isSavingForm.value = true

    try {
      await updateChecklistTemplate(
        selected.template_id,
        {
          title: formState.value.name.trim(),
          description: serializeLawReference(selectedLaw.value),
          moduleType: 'ALCOHOL',
          frequency: 'DAILY',
          items: [
            {
              sortOrder: 1,
              label: formState.value.name.trim(),
              description: formState.value.comment.trim() || selectedLaw.value.name,
              itemType: 'BOOLEAN',
              isRequired: true,
            },
          ],
        },
        orgNumber,
      )

      await reload()
    } catch (error) {
      console.error('Failed to update daily checklist template', error)
      return
    } finally {
      isSavingForm.value = false
    }
  }

  showFormModal.value = false
}

const deleteControl = (id: number) => {
  controls.value = controls.value.filter((item) => item.id !== id)
}

const attachmentLink = computed(() => {
  if (!detailsItem.value?.attachment) {
    return null
  }

  return detailsItem.value.attachment
})

const openAttachment = () => {
  if (!attachmentLink.value) {
    return
  }

  window.open(attachmentLink.value, '_blank', 'noopener,noreferrer')
}

const filteredControls = computed(() => {
  if (filter.value === 'checked') {
    return controls.value.filter((item) => item.is_checked)
  }

  if (filter.value === 'pending') {
    return controls.value.filter((item) => !item.is_checked)
  }

  return controls.value
})

watch(
  dailyControls,
  (items) => {
    controls.value = items.map((item) => ({ ...item }))
  },
  { immediate: true },
)
</script>

<template>
  <div class="daily-control-page">
    <header class="page-header">
      <h1>Daglig kontroll</h1>
      <p class="subtitle">Daglige kontrollpunkter for ansvarlig alkoholservering</p>
    </header>

    <ControlProgressCard :completed="completed" :total="total" />

    <div class="control-filters" role="tablist" aria-label="Filtrer kontrollpunkter">
      <button class="filter-btn" :class="{ 'filter-btn--active': filter === 'all' }" @click="filter = 'all'">Alle</button>
      <button class="filter-btn" :class="{ 'filter-btn--active': filter === 'checked' }" @click="filter = 'checked'">Fullført</button>
      <button class="filter-btn" :class="{ 'filter-btn--active': filter === 'pending' }" @click="filter = 'pending'">Mangler</button>
    </div>

    <div class="control-list">
      <article v-for="item in filteredControls" :key="item.id" class="control-card">
        <ControllItem
          :item="item"
          :can-manage="isAdmin"
          @toggle="toggleControl"
          @edit="openEditModal"
          @delete="deleteControl"
          @view-more="openDetails"
        />
      </article>
    </div>

    <button v-if="isAdmin" type="button" class="add-item-btn" @click="openAddModal">
      + Legg til kontrollpunkt
    </button>

    <div v-if="filteredControls.length === 0" class="empty-state">
      Ingen kontroller matcher valgt filter.
    </div>

    <BaseModal :open="showFormModal" :title="formMode === 'add' ? 'Legg til kontrollpunkt' : 'Rediger kontrollpunkt'" @close="closeFormModal">
      <form class="control-form" @submit.prevent="saveControlItem">
        <label>
          Navn
          <input v-model="formState.name" type="text" required />
        </label>
        <label>
          Lovgrunnlag
          <select v-model="formState.law_document_id" required>
            <option disabled value="">Velg lovverk</option>
            <option
              v-for="law in lawOptions"
              :key="law.value"
              :value="law.value"
            >
              {{ law.label }}
            </option>
          </select>
        </label>
        <p class="control-form__hint">
          Bruker og tidspunkt settes automatisk når kontrollpunktet blir utført.
        </p>
        <label>
          Kommentar
          <textarea v-model="formState.comment" rows="3" />
        </label>
        <label>
          Vedlegg (url eller filnavn)
          <input v-model="formState.attachment" type="text" />
        </label>
        <label class="control-form__checkbox">
          <input v-model="formState.is_checked" type="checkbox" />
          Fullført
        </label>
      </form>

      <template #footer>
        <button type="button" class="modal-btn modal-btn--ghost" @click="closeFormModal">Avbryt</button>
        <button type="button" class="modal-btn" @click="saveControlItem">Lagre</button>
      </template>
    </BaseModal>

    <BaseModal :open="Boolean(detailsItem)" title="Kontrollrapport" @close="closeDetails">
      <div v-if="detailsItem" class="details-report">
        <p><strong>ID:</strong> {{ detailsItem.id }}</p>
        <p><strong>Navn:</strong> {{ detailsItem.name }}</p>
        <p><strong>Lovgrunnlag:</strong> {{ detailsItem.law_unit }}</p>
        <p><strong>Ansatt:</strong> {{ detailsItem.employee }}</p>
        <p><strong>Dato:</strong> {{ detailsItem.completion_date.date }}</p>
        <p><strong>Tid:</strong> {{ detailsItem.completion_date.time }}</p>
        <p><strong>Status:</strong> {{ detailsItem.is_checked ? 'Fullført' : 'Mangler' }}</p>
        <p><strong>Kommentar:</strong> {{ detailsItem.comment || 'Ingen kommentar' }}</p>

        <button
          v-if="attachmentLink"
          type="button"
          class="attachment-btn"
          @click="openAttachment"
        >
          Åpne vedlegg
        </button>
      </div>
    </BaseModal>
  </div>
</template>

<style scoped>
.daily-control-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 32px;
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

.control-filters {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-bottom: 1.5rem;
}

.filter-btn {
  padding: 0.5rem 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-card);
  color: var(--color-gray-600);
  font-size: var(--font-size-sm);
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--transition-fast), border-color var(--transition-fast), transform var(--transition-fast);
}

.filter-btn:hover {
  border-color: var(--color-border-strong);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.filter-btn--active {
  background: var(--ik-alkohol-primary);
  color: var(--color-primary-foreground);
  border-color: var(--ik-alkohol-primary);
}

.control-list {
  display: grid;
  gap: 1rem;
}

.control-card {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1.4rem;
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--transition-fast), border-color var(--transition-fast), transform var(--transition-fast);
}

.control-card:hover {
  border-color: var(--color-border-strong);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.empty-state {
  margin-top: 1rem;
  text-align: center;
  color: var(--color-gray-600);
}

.add-item-btn {
  margin-top: 1rem;
  padding: 0.65rem 1rem;
  border: 1px solid var(--ik-alkohol-primary);
  background: color-mix(in srgb, var(--ik-alkohol-primary) 7%, var(--color-card));
  color: var(--ik-alkohol-primary);
  font-weight: var(--font-weight-semibold);
  border-radius: var(--radius-md);
}

.control-form__hint {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
}

.add-item-btn:hover {
  background: color-mix(in srgb, var(--ik-alkohol-primary) 12%, var(--color-card));
}

.control-form {
  display: grid;
  gap: 0.8rem;
}

.control-form label {
  display: grid;
  gap: 0.3rem;
  font-size: var(--font-size-sm);
  color: var(--color-gray-700);
}

.control-form input,
.control-form textarea {
  border: 1px solid var(--color-border);
  padding: 0.55rem 0.7rem;
  border-radius: var(--radius-sm);
  background: var(--color-card);
}

.control-form__checkbox {
  grid-auto-flow: column;
  justify-content: start;
  align-items: center;
  gap: 0.45rem;
  color: var(--color-gray-800);
}

.control-form__checkbox input[type='checkbox'] {
  width: 1.1rem;
  height: 1.1rem;
  min-height: 0;
  appearance: none;
  -webkit-appearance: none;
  display: inline-grid;
  place-content: center;
  background: var(--color-card);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
}

.control-form__checkbox input[type='checkbox']::after {
  content: '';
  width: 0.6rem;
  height: 0.34rem;
  border-left: 2px solid var(--color-cta-foreground);
  border-bottom: 2px solid var(--color-cta-foreground);
  transform: rotate(-45deg) translateY(-0.04rem);
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.control-form__checkbox input[type='checkbox']:checked {
  background: var(--color-cta);
  border-color: color-mix(in srgb, var(--color-cta) 65%, var(--color-border-strong));
}

.control-form__checkbox input[type='checkbox']:checked::after {
  opacity: 1;
}

.control-form__checkbox input[type='checkbox']:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.modal-btn {
  border: 1px solid var(--ik-alkohol-primary);
  background: var(--ik-alkohol-primary);
  color: var(--color-secondary-foreground);
  border-radius: var(--radius-sm);
  padding: 0.45rem 0.8rem;
}

.modal-btn--ghost {
  background: transparent;
  color: var(--color-brand-medium-violet);
  border-color: var(--color-border);
}

.details-report {
  display: grid;
  gap: 0.45rem;
}

.details-report p {
  margin: 0;
  color: var(--color-gray-700);
  font-size: var(--font-size-sm);
}

.attachment-btn {
  margin-top: 0.6rem;
  border: 1px solid var(--color-brand-medium-violet);
  color: var(--color-brand-medium-violet);
  background: transparent;
  padding: 0.5rem 0.7rem;
  border-radius: var(--radius-sm);
  width: fit-content;
}

</style>
