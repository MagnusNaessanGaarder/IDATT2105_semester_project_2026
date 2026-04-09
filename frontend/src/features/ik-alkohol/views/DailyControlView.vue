<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useAlkoholData } from '../composables/useAlkoholData'
import { useAuthStore } from '@/stores/auth'
import { uncompleteRun, updateRunItem } from '../api/checklistsRun'
import ControllItem from '../components/ControllItem.vue'
import ControlProgressCard from '../components/ControlProgressCard.vue'
import BaseModal from '@/shared/components/BaseModal.vue'
import type { DailyControlItem } from '../types'

const { dailyControls } = useAlkoholData()
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
  law_unit: '',
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

    if (!selected.run_id || !selected.template_item_id) {
      return
    }

    await updateRunItem(
      selected.run_id,
      selected.template_item_id,
      orgNumber,
      !selected.is_checked,
    )
    controls.value = controls.value.map((item) =>
      item.id === selected.id
        ? {
            ...item,
            is_checked: !selected.is_checked,
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
    law_unit: '',
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
    law_unit: selected.law_unit,
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

const saveControlItem = () => {
  if (!formState.value.name.trim() || !formState.value.law_unit.trim()) {
    return
  }

  if (!formState.value.completion_date.date || !formState.value.completion_date.time) {
    return
  }

  if (formMode.value === 'add') {
    const nextId = controls.value.length > 0 ? Math.max(...controls.value.map((item) => item.id)) + 1 : 1
    controls.value.push({
      id: nextId,
      run_id: null,
      template_item_id: null,
      run_status: null,
      name: formState.value.name.trim(),
      law_unit: formState.value.law_unit.trim(),
      employee: formState.value.employee.trim() || 'Ukjent',
      comment: formState.value.comment.trim(),
      completion_date: {
        date: formState.value.completion_date.date,
        time: formState.value.completion_date.time,
      },
      attachment: formState.value.attachment.trim() || null,
      is_checked: formState.value.is_checked,
    })
  }

  if (formMode.value === 'edit' && editingItemId.value !== null) {
    controls.value = controls.value.map((item) => {
      if (item.id !== editingItemId.value) {
        return item
      }

      return {
        ...item,
        name: formState.value.name.trim(),
        law_unit: formState.value.law_unit.trim(),
        employee: formState.value.employee.trim() || item.employee,
        comment: formState.value.comment.trim(),
        completion_date: {
          date: formState.value.completion_date.date,
          time: formState.value.completion_date.time,
        },
        attachment: formState.value.attachment.trim() || null,
        is_checked: formState.value.is_checked,
      }
    })
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
          <input v-model="formState.law_unit" type="text" required />
        </label>
        <label>
          Ansatt
          <input v-model="formState.employee" type="text" />
        </label>
        <label>
          Dato
          <input v-model="formState.completion_date.date" type="date" required />
        </label>
        <label>
          Tid
          <input v-model="formState.completion_date.time" type="time" required />
        </label>
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
}

.filter-btn--active {
  background: var(--ik-alkohol-primary);
  color: #ffffff;
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
}

.modal-btn {
  border: 1px solid var(--ik-alkohol-primary);
  background: var(--ik-alkohol-primary);
  color: #fff;
  border-radius: var(--radius-sm);
  padding: 0.45rem 0.8rem;
}

.modal-btn--ghost {
  background: var(--color-card);
  color: var(--color-gray-700);
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
  border: 1px solid var(--ik-alkohol-primary);
  color: var(--ik-alkohol-primary);
  background: transparent;
  padding: 0.5rem 0.7rem;
  border-radius: var(--radius-sm);
  width: fit-content;
}

</style>
