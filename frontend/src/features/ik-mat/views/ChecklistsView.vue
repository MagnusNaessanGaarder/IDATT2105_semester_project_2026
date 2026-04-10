<script setup lang="ts">
import { computed, ref, watch, watchEffect } from 'vue'
import { useIkMatData, type Checklist } from '../composables/useIkMatData'
import { useAuthStore } from '@/stores/auth'
import { getOrgNumber } from '@/shared/utils/orgContext'
import { ikMatApi } from '../api/ikMatApi'
import { updateRunItem, completeRun } from '../api/checklists'
import BaseModal from '@/shared/components/BaseModal.vue'

const { checklists, completionForChecklist, reload } = useIkMatData()
const isAdmin = computed(() => useAuthStore().isAdmin)

// ── Local snapshot of server data ─────────────────────────────────────────────
// We keep a local copy so optimistic checkbox updates don't cause cards to
// re-sort. displayOrder is only updated when the server data reloads.
const local = ref<Checklist[]>([])
const displayOrder = ref<number[]>([])

// Overlay of checkbox overrides: { [checklistId]: { [taskId]: boolean } }
// Survives reloads so state is not lost when checklists is refreshed.
const checkOverrides = ref<Record<number, Record<number, boolean>>>({})

// Client-side completed tracking — persisted to localStorage so it survives page refreshes.
const LS_KEY = 'ikmat_completed_ids'
const LS_META_KEY = 'ikmat_completion_meta'

interface CompletionMeta { date: string; time: string; by: string }

const readLS = (): number[] => { try { return JSON.parse(localStorage.getItem(LS_KEY) ?? '[]') } catch { return [] } }
const writeLS = (ids: Set<number>) => { try { localStorage.setItem(LS_KEY, JSON.stringify([...ids])) } catch { /* noop */ } }
const readMetaLS = (): Record<number, CompletionMeta> => { try { return JSON.parse(localStorage.getItem(LS_META_KEY) ?? '{}') } catch { return {} } }
const writeMetaLS = (meta: Record<number, CompletionMeta>) => { try { localStorage.setItem(LS_META_KEY, JSON.stringify(meta)) } catch { /* noop */ } }

const clientCompletedIds = ref<Set<number>>(new Set(readLS()))
const clientCompletionMeta = ref<Record<number, CompletionMeta>>(readMetaLS())

watchEffect(() => writeLS(clientCompletedIds.value))
watchEffect(() => writeMetaLS(clientCompletionMeta.value))

const applyOverrides = (list: Checklist[]): Checklist[] =>
  list.map(c => {
    const done = clientCompletedIds.value.has(c.id)
    if (done) {
      const meta = clientCompletionMeta.value[c.id]
      return {
        ...c,
        status: 'completed',
        items: c.items.map(t => ({ ...t, completed: true })),
        completion_date: c.completion_date || meta?.date || null,
        completion_time: c.completion_time || meta?.time || null,
        completed_by: c.completed_by || meta?.by || null,
      }
    }
    const overrides = checkOverrides.value[c.id]
    if (!overrides) return c
    return {
      ...c,
      items: c.items.map(t => {
        const override = overrides[t.id]
        return override === undefined ? t : { ...t, completed: override }
      }),
    }
  })

watch(checklists, (fresh) => {
  if (!Array.isArray(fresh)) return
  // Update display order only on fresh server data
  displayOrder.value = [...fresh]
    .sort((a, b) => completionForChecklist(a) - completionForChecklist(b))
    .map(c => c.id)
  // Merge server data with any pending local overrides
  local.value = applyOverrides(fresh.map(c => ({ ...c, items: c.items.map(t => ({ ...t })) })))
}, { immediate: true, deep: true })

const syncLocal = () => {
  local.value = applyOverrides(
    (checklists as Checklist[]).map(c => ({ ...c, items: c.items.map(t => ({ ...t })) }))
  )
}

// ── Filter + stable sort ──────────────────────────────────────────────────────
const FREQS = ['Alle', 'Daglig', 'Ukentlig', 'Månedlig'] as const
type Freq = typeof FREQS[number]
const selectedFreq = ref<Freq>('Alle')

const freqLabel = (f: string) => ({ DAILY: 'Daglig', WEEKLY: 'Ukentlig', MONTHLY: 'Månedlig' }[f] ?? f)
const statusLabel = (s: string) => s === 'completed' ? 'Fullført' : s === 'overdue' ? 'Forfalt' : 'Pågående'
const tone = (pct: number) => pct >= 100 ? 'l5' : pct >= 75 ? 'l4' : pct >= 50 ? 'l3' : pct >= 25 ? 'l2' : 'l1'
const allRequiredDone = (c: Checklist) => c.items.filter(t => t.required).every(t => t.completed)

const filtered = computed(() =>
  selectedFreq.value === 'Alle'
    ? local.value
    : local.value.filter(c => freqLabel(c.frequency) === selectedFreq.value || c.frequency === selectedFreq.value)
)

const sorted = computed(() => {
  const order = new Map(displayOrder.value.map((id, i) => [id, i]))
  return [...filtered.value].sort((a, b) => (order.get(a.id) ?? 999) - (order.get(b.id) ?? 999))
})

const completionTone = (percentage: number): 'level-1' | 'level-2' | 'level-3' | 'level-4' | 'level-5' => {
  if (percentage >= 100) return 'level-5'
  if (percentage >= 75) return 'level-4'
  if (percentage >= 50) return 'level-3'
  if (percentage >= 25) return 'level-2'
  return 'level-1'
}

const isCompleted = (checklist: Checklist) => {
  return checklist.status === 'completed' || clientCompletedIds.value.has(checklist.id)
}

const activeChecklists = computed(() => sorted.value.filter(c => !isCompleted(c)))
const completedChecklists = computed(() => sorted.value.filter(c => isCompleted(c)))
const showCompleted = ref(false)

// ── Expand / auto-start run ───────────────────────────────────────────────────
const expandedId = ref<number | null>(null)
const autoStartingId = ref<number | null>(null)
const expandError = ref<string | null>(null)
// Tracks checklist IDs where a run was successfully created/confirmed this session.
// Used so Fullfør stays visible even when GET /runs is still recovering (e.g. needs backend restart).
const runReadyIds = ref(new Set<number>())

const toggleExpanded = async (checklist: Checklist) => {
  const opening = expandedId.value !== checklist.id
  expandedId.value = opening ? checklist.id : null
  if (!opening) return

  // If runId is already known, nothing to do
  if (checklist.runId) {
    runReadyIds.value.add(checklist.id)
    return
  }

  autoStartingId.value = checklist.id
  expandError.value = null
  try {
    try {
      await ikMatApi.createChecklistRun({
        templateId: checklist.id,
        runDate: new Date().toISOString().slice(0, 10),
      })
    } catch (err: unknown) {
      // 400 = run already exists (e.g. from scheduler) — still valid, reload
      if ((err as { response?: { status?: number } })?.response?.status !== 400) throw err
    }
    // Run exists (created or confirmed) — show Fullfør even if GET /runs still fails
    runReadyIds.value.add(checklist.id)
    await reload()
    syncLocal()
  } catch {
    expandError.value = 'Kunne ikke starte runde. Prøv igjen.'
  } finally {
    autoStartingId.value = null
  }
}

// ── Toggle checkbox ───────────────────────────────────────────────────────────
const togglingItemId = ref<number | null>(null)

const toggleTask = async (checklist: Checklist, taskId: number) => {
  const task = checklist.items.find(t => t.id === taskId)
  if (!task) return
  const newVal = !task.completed

  // Save override so it survives future reloads
  checkOverrides.value = {
    ...checkOverrides.value,
    [checklist.id]: { ...(checkOverrides.value[checklist.id] ?? {}), [taskId]: newVal },
  }
  // Apply immediately to local state
  local.value = local.value.map(c =>
    c.id !== checklist.id ? c
      : { ...c, items: c.items.map(t => t.id === taskId ? { ...t, completed: newVal } : t) }
  )

  if (!checklist.runId || task.runItemId == null) return
  togglingItemId.value = task.runItemId
  try {
    await updateRunItem(checklist.runId, task.runItemId, getOrgNumber(), { booleanValue: newVal })
    // Saved — can clear the override now that server reflects truth
    const overrides = { ...(checkOverrides.value[checklist.id] ?? {}) }
    delete overrides[taskId]
    checkOverrides.value = { ...checkOverrides.value, [checklist.id]: overrides }
  } catch {
    // Revert override and local state
    const overrides = { ...(checkOverrides.value[checklist.id] ?? {}) }
    delete overrides[taskId]
    checkOverrides.value = { ...checkOverrides.value, [checklist.id]: overrides }
    local.value = local.value.map(c =>
      c.id !== checklist.id ? c
        : { ...c, items: c.items.map(t => t.id === taskId ? { ...t, completed: task.completed } : t) }
    )
  } finally {
    togglingItemId.value = null
  }
}

// ── Complete run ──────────────────────────────────────────────────────────────
const completingId = ref<number | null>(null)
const completeError = ref<string | null>(null)

const markComplete = async (checklist: Checklist) => {
  completingId.value = checklist.id
  completeError.value = null
  try {
    if (checklist.runId) {
      await completeRun(checklist.runId, getOrgNumber())
    }
    // Record completion metadata (date, time, user) client-side
    const now = new Date()
    const meta: CompletionMeta = {
      date: now.toISOString().slice(0, 10),
      time: now.toTimeString().slice(0, 5),
      by: useAuthStore().user?.name ?? useAuthStore().user?.email ?? 'Ukjent',
    }
    clientCompletedIds.value = new Set([...clientCompletedIds.value, checklist.id])
    clientCompletionMeta.value = { ...clientCompletionMeta.value, [checklist.id]: meta }
    // Update local state so status badge + progress reflect completion right away
    local.value = local.value.map(c =>
      c.id !== checklist.id ? c
        : { ...c, status: 'completed', completion_date: meta.date, completion_time: meta.time, completed_by: meta.by, items: c.items.map(t => ({ ...t, completed: true })) }
    )
    // Clear overrides — checklist is done
    const overrides = { ...checkOverrides.value }
    delete overrides[checklist.id]
    checkOverrides.value = overrides
    expandedId.value = null
    showCompleted.value = true
    await reload()
    syncLocal()
  } catch {
    completeError.value = 'Kunne ikke fullføre. Prøv igjen.'
  } finally {
    completingId.value = null
  }
}

// ── CRUD (admin) ──────────────────────────────────────────────────────────────
const showCreate = ref(false)
const showEdit = ref(false)
const showDelete = ref(false)
const editTarget = ref<Checklist | null>(null)
const saving = ref(false)
const formError = ref<string | null>(null)

interface TaskDraft { label: string; required: boolean }
const emptyForm = () => ({ name: '', description: '', frequency: 'DAILY' as 'DAILY' | 'WEEKLY' | 'MONTHLY', tasks: [] as TaskDraft[] })
const form = ref(emptyForm())
const addTask = () => form.value.tasks.push({ label: '', required: true })
const removeTask = (i: number) => form.value.tasks.splice(i, 1)

const validate = () => {
  if (!form.value.name.trim()) { formError.value = 'Navn er påkrevd'; return false }
  if (!form.value.tasks.length) { formError.value = 'Minst én oppgave er påkrevd'; return false }
  if (form.value.tasks.some(t => !t.label.trim())) { formError.value = 'Alle oppgaver må ha navn'; return false }
  formError.value = null; return true
}

const buildPayload = () => ({
  title: form.value.name.trim(),
  description: form.value.description.trim(),
  moduleType: 'FOOD' as const,
  frequency: form.value.frequency,
  items: form.value.tasks.map((t, i) => ({ label: t.label.trim(), itemType: 'BOOLEAN' as const, isRequired: t.required, sortOrder: i + 1 })),
})

const openCreate = () => { form.value = emptyForm(); formError.value = null; showCreate.value = true }
const openEdit = (c: Checklist, e: Event) => {
  e.stopPropagation()
  editTarget.value = c
  const m: Record<string, 'DAILY' | 'WEEKLY' | 'MONTHLY'> = { Daglig: 'DAILY', Ukentlig: 'WEEKLY', Månedlig: 'MONTHLY' }
  form.value = { name: c.name, description: c.description, frequency: m[c.frequency] ?? 'DAILY', tasks: c.items.map(t => ({ label: t.task, required: t.required })) }
  formError.value = null; showEdit.value = true
}
const openDelete = (c: Checklist, e: Event) => { e.stopPropagation(); editTarget.value = c; showDelete.value = true }

const handleCreate = async () => {
  if (!validate()) return
  saving.value = true
  try { await ikMatApi.createChecklistTemplate(buildPayload()); await reload(); syncLocal(); showCreate.value = false }
  catch { formError.value = 'Kunne ikke opprette. Prøv igjen.' }
  finally { saving.value = false }
}

const handleEdit = async () => {
  if (!editTarget.value || !validate()) return
  saving.value = true
  try { await ikMatApi.updateChecklistTemplate(editTarget.value.id, buildPayload()); await reload(); syncLocal(); showEdit.value = false; editTarget.value = null }
  catch { formError.value = 'Kunne ikke lagre. Prøv igjen.' }
  finally { saving.value = false }
}

const handleDelete = async () => {
  if (!editTarget.value) return
  saving.value = true
  try {
    await ikMatApi.deleteChecklistTemplate(editTarget.value.id)
    if (expandedId.value === editTarget.value.id) expandedId.value = null
    await reload(); syncLocal(); showDelete.value = false; editTarget.value = null
  } catch { formError.value = 'Kunne ikke slette. Prøv igjen.' }
  finally { saving.value = false }
}
</script>

<template>
  <div class="checklists-page">

    <!-- Page header -->
    <header class="page-header">
      <div class="page-header__row">
        <div>
          <h1>Sjekklister</h1>
          <p class="subtitle">Operative kontrollpunkter sortert etter lavest progresjon</p>
        </div>
        <button v-if="isAdmin" class="btn btn--primary" type="button" @click="openCreate">
          + Ny sjekkliste
        </button>
      </div>
    </header>

    <!-- Frequency filter -->
    <div class="filter-row" role="group" aria-label="Filtrer etter frekvens">
      <button
        v-for="f in FREQS" :key="f"
        class="filter-chip"
        :class="{ 'filter-chip--active': selectedFreq === f }"
        :aria-pressed="selectedFreq === f"
        @click="selectedFreq = f"
      >{{ f }}</button>
    </div>

    <!-- Active checklist cards -->
    <div class="checklist-list">
      <template v-for="checklist in activeChecklists" :key="checklist.id">
        <article class="checklist-card">

          <!-- Card header -->
          <div class="checklist-header">
            <div
              class="checklist-header__content"
              role="button"
              tabindex="0"
              :aria-expanded="expandedId === checklist.id"
              :aria-label="`${checklist.name} – ${expandedId === checklist.id ? 'Skjul' : 'Vis'} oppgaver`"
              @click="toggleExpanded(checklist)"
              @keydown.enter.prevent="toggleExpanded(checklist)"
              @keydown.space.prevent="toggleExpanded(checklist)"
            >
              <div class="checklist-header__info">
                <div class="checklist-header__title-row">
                  <p class="checklist-header__title">{{ checklist.name }}</p>
                  <span class="status-badge" :class="`status-badge--${checklist.status}`">
                    {{ statusLabel(checklist.status) }}
                  </span>
                </div>
                <p class="checklist-header__meta">
                  {{ freqLabel(checklist.frequency) }}
                  <template v-if="checklist.location"> · {{ checklist.location }}</template>
                  <template v-if="checklist.due_date"> · Frist: {{ checklist.due_date }}</template>
                </p>
              </div>

            <div class="checklist-header__progress" :class="`checklist-header__progress--${completionTone(completionForChecklist(checklist))}`">
              <div class="progress-track" role="progressbar" :aria-valuenow="completionForChecklist(checklist)" aria-valuemin="0" aria-valuemax="100">
                <div class="progress-track__fill" :style="{ width: `${completionForChecklist(checklist)}%` }" />
              </div>
              <span class="checklist-header__status-tag">{{ completionForChecklist(checklist) }}%</span>
            </div>

            <!-- Admin actions -->
            <div v-if="isAdmin" class="card-actions" @click.stop>
              <button class="card-action-btn" :aria-label="`Rediger ${checklist.name}`" @click="openEdit(checklist, $event)">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                  <path d="M12 20h9" />
                  <path d="M16.5 3.5a2.121 2.121 0 1 1 3 3L7 19l-4 1 1-4 12.5-12.5z" />
                </svg>
              </button>
              <button class="card-action-btn card-action-btn--danger" :aria-label="`Slett ${checklist.name}`" @click="openDelete(checklist, $event)">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                  <polyline points="3 6 5 6 21 6" />
                  <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" />
                  <path d="M10 11v6" />
                  <path d="M14 11v6" />
                  <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2" />
                </svg>
              </button>
            </div>
          </div>
          </div>

          <!-- Expanded body -->
          <div v-if="expandedId === checklist.id" class="checklist-body">
            <p v-if="checklist.description" class="checklist-body__desc">{{ checklist.description }}</p>

            <!-- Task list -->
            <ul v-if="checklist.items.length" class="task-list">
              <li
                v-for="task in checklist.items"
                :key="task.id"
                class="task-row"
                :class="{ 'task-row--deviation': task.isDeviation }"
              >
                <!-- Label fills entire row — click anywhere toggles checkbox -->
                <label class="task-label">
                  <input
                    type="checkbox"
                    :checked="task.completed"
                    :disabled="togglingItemId !== null && togglingItemId === task.runItemId"
                    :aria-label="task.task + (task.required ? ' (obligatorisk)' : '')"
                    @change="toggleTask(checklist, task.id)"
                  />
                  <span class="task-label__body">
                    <span :class="{ 'task-done': task.completed }">
                      {{ task.task }}<span v-if="task.required" class="req-mark" aria-hidden="true">*</span>
                    </span>
                    <span v-if="task.isDeviation || task.notes" class="task-meta">
                      <span v-if="task.isDeviation" class="deviation-badge">Avvik registrert</span>
                      <small v-if="task.notes" class="task-notes">{{ task.notes }}</small>
                    </span>
                  </span>
                </label>
              </li>
            </ul>
            <p v-else class="no-tasks">Ingen oppgaver i denne sjekklisten.</p>

            <!-- Bottom action row -->
            <div class="action-row">
              <p v-if="expandError || completeError" class="action-error" role="alert">
                {{ expandError || completeError }}
              </p>
              <span v-if="autoStartingId === checklist.id" class="action-hint">Forbereder runde…</span>
              <button
                v-else-if="(checklist.runId || runReadyIds.has(checklist.id)) && !isCompleted(checklist)"
                class="btn btn--primary btn--sm"
                :disabled="completingId === checklist.id || !allRequiredDone(checklist)"
                :title="!allRequiredDone(checklist) ? 'Fullfør alle obligatoriske oppgaver (*) først' : undefined"
                @click="markComplete(checklist)"
              >
                {{ completingId === checklist.id ? 'Lagrer…' : 'Fullfør' }}
              </button>
            </div>
          </div>

        </article>
      </template>
    </div>

    <p v-if="activeChecklists.length === 0 && completedChecklists.length === 0" class="empty-state">Ingen sjekklister matcher valgt filter.</p>

    <!-- Completed section -->
    <section v-if="completedChecklists.length > 0" class="completed-section">
      <button
        class="completed-section__toggle"
        :aria-expanded="showCompleted"
        @click="showCompleted = !showCompleted"
      >
        <span class="completed-section__title">
          Fullførte sjekklister
          <span class="completed-section__count">{{ completedChecklists.length }}</span>
        </span>
        <span class="completed-section__chevron" :class="{ 'completed-section__chevron--open': showCompleted }">▾</span>
      </button>

      <div v-if="showCompleted" class="checklist-list checklist-list--completed">
        <template v-for="checklist in completedChecklists" :key="checklist.id">
          <article class="checklist-card checklist-card--done">

            <!-- Card header -->
            <div class="checklist-header">
              <div
                class="checklist-header__content"
                role="button"
                tabindex="0"
                :aria-expanded="expandedId === checklist.id"
                :aria-label="`${checklist.name} – ${expandedId === checklist.id ? 'Skjul' : 'Vis'} oppgaver`"
                @click="toggleExpanded(checklist)"
                @keydown.enter.prevent="toggleExpanded(checklist)"
                @keydown.space.prevent="toggleExpanded(checklist)"
              >
                <div class="checklist-header__info">
                  <div class="checklist-header__title-row">
                    <p class="checklist-header__title">{{ checklist.name }}</p>
                    <span class="status-badge" :class="`status-badge--${checklist.status}`">
                      {{ statusLabel(checklist.status) }}
                    </span>
                  </div>
                  <p class="checklist-header__meta">
                    {{ freqLabel(checklist.frequency) }}
                    <template v-if="checklist.location"> · {{ checklist.location }}</template>
                  </p>
                  <div class="checklist-done-meta">
                    <span v-if="checklist.completion_date" class="done-meta-chip done-meta-chip--date">
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
                      {{ checklist.completion_date }}<template v-if="checklist.completion_time"> kl. {{ checklist.completion_time }}</template>
                    </span>
                    <span v-if="checklist.completed_by" class="done-meta-chip">
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                      {{ checklist.completed_by }}
                    </span>
                    <span v-if="checklist.assignedTo" class="done-meta-chip">
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                      Tildelt: {{ checklist.assignedTo }}
                    </span>
                    <span class="done-meta-chip">
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><polyline points="9 11 12 14 22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
                      {{ checklist.items.filter(t => t.completed).length }}/{{ checklist.items.length }} oppgaver
                    </span>
                    <span v-if="checklist.items.some(t => t.isDeviation)" class="done-meta-chip done-meta-chip--deviation">
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
                      {{ checklist.items.filter(t => t.isDeviation).length }} avvik
                    </span>
                    <span v-if="checklist.due_date" class="done-meta-chip done-meta-chip--due">
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                      Frist: {{ checklist.due_date }}
                    </span>
                  </div>
                </div>

                <div class="checklist-header__progress checklist-header__progress--good">
                  <div
                    class="progress-track"
                    role="progressbar"
                    aria-valuemin="0"
                    aria-valuemax="100"
                    :aria-valuenow="completionForChecklist(checklist)"
                    :aria-valuetext="`${completionForChecklist(checklist)} prosent fullført`"
                  >
                    <div class="progress-track__fill" :style="{ width: `${completionForChecklist(checklist)}%` }" />
                  </div>
                  <span class="progress-pct">{{ completionForChecklist(checklist) }}%</span>
                </div>
              </div>

              <!-- Admin actions -->
              <div v-if="isAdmin" class="card-actions" @click.stop>
                <button class="card-action-btn" :aria-label="`Rediger ${checklist.name}`" @click="openEdit(checklist, $event)">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <path d="M12 20h9" />
                    <path d="M16.5 3.5a2.121 2.121 0 1 1 3 3L7 19l-4 1 1-4 12.5-12.5z" />
                  </svg>
                </button>
                <button class="card-action-btn card-action-btn--danger" :aria-label="`Slett ${checklist.name}`" @click="openDelete(checklist, $event)">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <polyline points="3 6 5 6 21 6" />
                    <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" />
                    <path d="M10 11v6" />
                    <path d="M14 11v6" />
                    <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2" />
                  </svg>
                </button>
              </div>
            </div>

            <!-- Expanded body -->
            <div v-if="expandedId === checklist.id" class="checklist-body">
              <p v-if="checklist.description" class="checklist-body__desc">{{ checklist.description }}</p>
              <ul v-if="checklist.items.length" class="task-list">
                <li
                  v-for="task in checklist.items"
                  :key="task.id"
                  class="task-row task-row--done"
                >
                  <label class="task-label">
                    <input type="checkbox" :checked="task.completed" disabled class="task-checkbox" />
                    <span class="task-label__body">
                      <span class="task-done">{{ task.task }}{{ task.required ? ' *' : '' }}</span>
                      <span v-if="task.notes" class="task-notes">{{ task.notes }}</span>
                    </span>
                  </label>
                </li>
              </ul>
            </div>

          </article>
        </template>
      </div>
    </section>

    <!-- ── Create modal ────────────────────────────────────────────────── -->
    <BaseModal :open="showCreate" title="Ny sjekkliste" @close="showCreate = false; formError = null">
      <form id="createForm" class="modal-form" @submit.prevent="handleCreate">
        <div class="form-group">
          <label for="c-name">Navn</label>
          <input id="c-name" v-model="form.name" type="text" required />
        </div>
        <div class="form-group">
          <label for="c-desc">Beskrivelse</label>
          <textarea id="c-desc" v-model="form.description" rows="2" />
        </div>
        <div class="form-group">
          <label for="c-freq">Frekvens</label>
          <select id="c-freq" v-model="form.frequency">
            <option value="DAILY">Daglig</option>
            <option value="WEEKLY">Ukentlig</option>
            <option value="MONTHLY">Månedlig</option>
          </select>
        </div>
        <div class="form-group">
          <div class="tasks-header">
            <label>Oppgaver</label>
            <button type="button" class="add-task-btn" @click="addTask">+ Legg til</button>
          </div>
          <p v-if="!form.tasks.length" class="tasks-empty">Ingen oppgaver ennå.</p>
          <ul v-else class="tasks-edit-list">
            <li v-for="(t, i) in form.tasks" :key="i" class="task-edit-row">
              <input v-model="t.label" type="text" placeholder="Oppgave…" class="task-edit-input" />
              <label class="task-req-label"><input v-model="t.required" type="checkbox" /> Obligatorisk</label>
              <button type="button" class="remove-task-btn" @click="removeTask(i)">×</button>
            </li>
          </ul>
        </div>
      </form>
      <template #footer>
        <p v-if="formError" class="modal-error" role="alert">{{ formError }}</p>
        <button type="button" class="btn btn--ghost" @click="showCreate = false; formError = null">Avbryt</button>
        <button type="submit" form="createForm" class="btn btn--primary" :disabled="saving">
          {{ saving ? 'Lagrer…' : 'Opprett' }}
        </button>
      </template>
    </BaseModal>

    <!-- ── Edit modal ──────────────────────────────────────────────────── -->
    <BaseModal :open="showEdit" title="Rediger sjekkliste" @close="showEdit = false; editTarget = null; formError = null">
      <form id="editForm" class="modal-form" @submit.prevent="handleEdit">
        <div class="form-group">
          <label for="e-name">Navn</label>
          <input id="e-name" v-model="form.name" type="text" required />
        </div>
        <div class="form-group">
          <label for="e-desc">Beskrivelse</label>
          <textarea id="e-desc" v-model="form.description" rows="2" />
        </div>
        <div class="form-group">
          <label for="e-freq">Frekvens</label>
          <select id="e-freq" v-model="form.frequency">
            <option value="DAILY">Daglig</option>
            <option value="WEEKLY">Ukentlig</option>
            <option value="MONTHLY">Månedlig</option>
          </select>
        </div>
        <div class="form-group">
          <div class="tasks-header">
            <label>Oppgaver</label>
            <button type="button" class="add-task-btn" @click="addTask">+ Legg til</button>
          </div>
          <p v-if="!form.tasks.length" class="tasks-empty">Ingen oppgaver ennå.</p>
          <ul v-else class="tasks-edit-list">
            <li v-for="(t, i) in form.tasks" :key="i" class="task-edit-row">
              <input v-model="t.label" type="text" placeholder="Oppgave…" class="task-edit-input" />
              <label class="task-req-label"><input v-model="t.required" type="checkbox" /> Obligatorisk</label>
              <button type="button" class="remove-task-btn" @click="removeTask(i)">×</button>
            </li>
          </ul>
        </div>
      </form>
      <template #footer>
        <p v-if="formError" class="modal-error" role="alert">{{ formError }}</p>
        <button type="button" class="btn btn--ghost" @click="showEdit = false; editTarget = null; formError = null">Avbryt</button>
        <button type="submit" form="editForm" class="btn btn--primary" :disabled="saving">
          {{ saving ? 'Lagrer…' : 'Lagre' }}
        </button>
      </template>
    </BaseModal>

    <!-- ── Delete modal ────────────────────────────────────────────────── -->
    <BaseModal :open="showDelete" title="Slett sjekkliste" @close="showDelete = false; editTarget = null">
      <p class="delete-text">
        Er du sikker på at du vil slette <strong>{{ editTarget?.name }}</strong>? Dette kan ikke angres.
      </p>
      <template #footer>
        <button type="button" class="btn btn--ghost" @click="showDelete = false; editTarget = null">Avbryt</button>
        <button type="button" class="btn btn--danger" :disabled="saving" @click="handleDelete">
          {{ saving ? 'Sletter…' : 'Slett' }}
        </button>
      </template>
    </BaseModal>

  </div>
</template>

<style scoped>
.checklists-page { max-width: 1200px; margin: 0 auto; }

/* Header */
.page-header { margin-bottom: 1.25rem; }
.page-header__row { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
.page-header h1 { margin: 0; font-size: var(--font-size-2xl); color: var(--ik-mat-primary); }
.subtitle { margin: 0.35rem 0 0; color: var(--color-gray-500); font-size: var(--font-size-sm); }

/* Filters */
.filter-row { display: flex; flex-wrap: wrap; gap: 0.5rem; margin-bottom: 1rem; }
.filter-chip {
  min-height: 2.75rem; padding: 0.4rem 1rem;
  border: 1px solid var(--color-border); border-radius: var(--radius-md);
  background: var(--color-card); color: var(--color-gray-600);
  font-size: var(--font-size-sm); cursor: pointer; transition: background 0.15s, border-color 0.15s;
}
.filter-chip:hover { border-color: var(--ik-mat-primary); color: var(--ik-mat-primary); }
.filter-chip:focus-visible { outline: 2px solid var(--ik-mat-primary); outline-offset: 2px; }
.filter-chip--active { border-color: var(--ik-mat-primary); background: var(--ik-mat-primary); color: #fff; }

/* Card list */
.checklist-list { display: grid; gap: 0.75rem; }

/* Completed section */
.completed-section { margin-top: 2rem; }
.completed-section__toggle {
  display: flex; align-items: center; justify-content: space-between;
  width: 100%; background: none; border: none; cursor: pointer;
  padding: 0.6rem 0; gap: 0.75rem;
  color: var(--color-text-secondary); font-size: var(--font-size-sm);
  font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em;
  border-top: 1px solid var(--color-border);
}
.completed-section__toggle:hover { color: var(--color-text); }
.completed-section__title { display: flex; align-items: center; gap: 0.5rem; }
.completed-section__count {
  background: var(--color-gray-200); color: var(--color-gray-600);
  border-radius: 999px; padding: 0 0.45rem; font-size: var(--font-size-xs);
  font-weight: 700; line-height: 1.5;
}
.completed-section__chevron { font-size: 1rem; transition: transform 0.2s; }
.completed-section__chevron--open { transform: rotate(180deg); }
.checklist-list--completed { margin-top: 0.75rem; }

/* Card */
.checklist-card {
  border: 1px solid var(--color-border); border-radius: var(--radius-md);
  background: var(--color-card); transition: border-color 0.15s, box-shadow 0.15s;
}
.checklist-card:hover {
  border-color: var(--ik-mat-primary);
  box-shadow: 0 2px 8px color-mix(in srgb, var(--ik-mat-primary) 12%, transparent);
}
.checklist-card--done { opacity: 0.6; }
.checklist-card--done:hover { opacity: 1; }

/* Card header */
.checklist-header { display: flex; align-items: center; padding: 0.9rem; gap: 0.75rem; }
.checklist-header__content {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  cursor: pointer;
  user-select: none;
}

.checklist-header__content:focus {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.checklist-header__title {
  margin: 0;
  color: var(--color-foreground);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
}

.checklist-header__meta {
  margin: 0.25rem 0 0;
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
}

.checklist-header__progress {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 8rem;
}

.checklist-header__progress span {
  font-size: var(--font-size-xs);
  color: var(--color-gray-600);
  font-weight: var(--font-weight-semibold);
  min-width: 2.2rem;
  text-align: right;
}

.checklist-header__status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 3rem;
  min-height: 1.8rem;
  padding: 0 0.55rem;
  border-radius: var(--radius-sm);
  border: 1px solid transparent;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  line-height: 1;
}

.progress-track {
  height: 0.4rem;
  flex: 1;
  background: var(--color-gray-200);
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--color-border-strong) 60%, var(--color-border));
  box-shadow: inset 0 1px 1px rgba(0, 39, 43, 0.12), 0 1px 1px rgba(255, 255, 255, 0.8);
  overflow: hidden;
}

.progress-track__fill {
  height: 100%;
  background: var(--color-success-scale-2);
  transition: width var(--transition-base);
}

.checklist-header__progress--level-1 .progress-track__fill {
  background: var(--color-success-scale-1);
}

.checklist-header__progress--level-2 .progress-track__fill {
  background: var(--color-success-scale-2);
}

.checklist-header__progress--level-3 .progress-track__fill {
  background: var(--color-success-scale-3);
}

.checklist-header__progress--level-4 .progress-track__fill {
  background: var(--color-success-scale-4);
}

.checklist-header__progress--level-5 .progress-track__fill {
  background: var(--color-success-scale-5);
}

.checklist-header__progress--level-1 .checklist-header__status-tag {
  background: color-mix(in srgb, var(--color-success-scale-1) 35%, var(--color-card));
  color: var(--color-foreground);
  border-color: color-mix(in srgb, var(--color-success-scale-2) 45%, var(--color-border));
}

.checklist-header__progress--level-2 .checklist-header__status-tag {
  background: color-mix(in srgb, var(--color-success-scale-2) 45%, var(--color-card));
  color: var(--color-foreground);
  border-color: color-mix(in srgb, var(--color-success-scale-3) 50%, var(--color-border));
}

.checklist-header__progress--level-3 .checklist-header__status-tag {
  background: color-mix(in srgb, var(--color-success-scale-3) 55%, var(--color-card));
  color: var(--color-foreground);
  border-color: color-mix(in srgb, var(--color-success-scale-4) 55%, var(--color-border));
}

.checklist-header__progress--level-4 .checklist-header__status-tag {
  background: var(--color-success-scale-4);
  color: var(--color-primary-foreground);
  border-color: color-mix(in srgb, var(--color-success-scale-5) 55%, black);
}

.checklist-header__progress--level-5 .checklist-header__status-tag {
  background: var(--color-success-scale-5);
  color: var(--color-primary-foreground);
  border-color: color-mix(in srgb, var(--color-success-scale-5) 70%, black);
}

/* Options menu - now outside the button */
.options-menu {
  position: relative;
  flex-shrink: 0;
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

.options-menu__trigger:hover {
  background: var(--color-accent);
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
  cursor: pointer;
}

.options-menu__item:hover {
  background: var(--color-accent);
}
.card-action-btn:hover { background: var(--color-accent); color: var(--color-foreground); border-color: var(--ik-mat-primary); }
.card-action-btn:focus-visible { outline: 2px solid var(--ik-mat-primary); outline-offset: 2px; }
.card-action-btn--danger { color: var(--color-danger); border-color: color-mix(in srgb, var(--color-danger) 30%, var(--color-border)); background: color-mix(in srgb, var(--color-danger) 6%, var(--color-card)); }
.card-action-btn--danger:hover { background: color-mix(in srgb, var(--color-danger) 15%, transparent); color: var(--color-danger); border-color: var(--color-danger); }

/* Expanded body */
.checklist-body {
  border-top: 1px solid var(--color-border);
  padding: 0.85rem 0.9rem;
  background: color-mix(in srgb, var(--ik-mat-bg, #f8fafc) 50%, var(--color-card));
  border-radius: 0 0 var(--radius-md) var(--radius-md);
  display: flex; flex-direction: column; gap: 0.7rem;
}
.checklist-body__desc { margin: 0; color: var(--color-gray-600); font-size: var(--font-size-sm); }

/* Task list */
.task-list { list-style: none; margin: 0; padding: 0; display: grid; gap: 0.4rem; }
.task-row {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-card);
  padding: 0.55rem;
}

.task-row label {
  display: flex;
  gap: 0.55rem;
  align-items: center;
  cursor: pointer;
}

.task-row input {
  width: 1rem;
  height: 1rem;
}

.task-row span {
  font-size: var(--font-size-sm);
  color: var(--color-foreground);
}

.task-row small {
  display: block;
  margin-top: 0.35rem;
  color: var(--color-gray-500);
  font-size: var(--font-size-xs);
}

.task-done {
  color: var(--color-gray-500);
  text-decoration: line-through;
}

.empty-state {
  margin-top: 0.9rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  color: var(--color-gray-600);
  text-align: center;
  padding: 1.2rem;
}

/* Form styles */
.checklist-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.form-group label {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--color-gray-700);
}

.form-group input,
.form-group textarea,
.form-group select {
  min-height: 2.5rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--color-border);
  background: var(--color-background-soft);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-family: inherit;
}

.form-group textarea {
  min-height: 5rem;
  resize: vertical;
}

.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
  outline: none;
  border-color: var(--ik-mat-primary);
}

.action-btn {
  min-height: 2.5rem;
  padding: 0 1rem;
  border: none;
  border-radius: var(--radius-md);
  background: var(--ik-mat-primary);
  color: var(--color-primary-foreground);
  font-size: var(--font-size-sm);
  font-weight: 600;
  cursor: pointer;
}

.action-btn:hover {
  opacity: 0.9;
}

.action-btn--ghost {
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-gray-700);
}

.action-btn--ghost:hover {
  background: var(--color-accent);
}

@media (max-width: 48rem) {
  .page-header__row { flex-direction: column; }
  .checklist-header { flex-wrap: wrap; }
  .checklist-header__content { width: 100%; }
  .checklist-header__progress { min-width: 5rem; }
  .card-actions { margin-left: auto; }
}
</style>
