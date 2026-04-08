<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import {
  DOCUMENT_TYPES,
  type DocumentTypeValue,
  type OrganizationDocument,
  type UploadNewDocumentPayload,
  type UploadNewVersionPayload,
} from '../api/documents'

const props = defineProps<{
  open: boolean
  /**
   * When provided the modal operates in "new version" mode:
   * the document type selector is hidden, title/description are
   * pre-filled from the parent, and the submit action calls `upload-version`.
   */
  succeedsDocument?: OrganizationDocument
}>()

const emit = defineEmits<{
  close: []
  /** Emitted in "new document" mode */
  'upload-document': [payload: UploadNewDocumentPayload]
  /** Emitted in "new version" mode */
  'upload-version':  [payload: UploadNewVersionPayload]
}>()

const isVersionMode = computed(() => props.succeedsDocument != null)

const modalTitle = computed(() =>
  isVersionMode.value
    ? `Ny versjon — ${props.succeedsDocument!.title}`
    : 'Last opp dokument'
)

const modalSubtitle = computed(() =>
  isVersionMode.value
    ? `Nåværende versjon: v${props.succeedsDocument!.currentVersion}. Den nye filen blir v${props.succeedsDocument!.currentVersion + 1}.`
    : 'Fyll ut feltene nedenfor og velg filen du vil laste opp.'
)

const MAX_FILE_BYTES = 10 * 1024 * 1024
const MAX_TITLE_LEN  = 255
const MAX_DESC_LEN   = 1000
const SAFE_TEXT_RE   = /^[^<>`\x00-\x1F\x7F-\x9F]*$/
const ALLOWED_EXTENSIONS = ['.pdf','.doc','.docx','.xls','.xlsx','.ppt','.pptx','.txt','.png','.jpg','.jpeg','.gif','.webp']

const file        = ref<File | null>(null)
const title       = ref('')
const description = ref('')
const docType     = ref<DocumentTypeValue>('OTHER')
const isDragging  = ref(false)
const errors      = ref<Record<string, string>>({})

const fileLabel = computed(() => {
  if (!file.value) return null
  const mb = (file.value.size / (1024 * 1024)).toFixed(2)
  return `${file.value.name} (${mb} MB)`
})

const titlePlaceholder = computed(() => {
  if (isVersionMode.value && props.succeedsDocument) return props.succeedsDocument.title
  return file.value ? file.value.name : 'Tittel på dokumentet (valgfritt)'
})

const descriptionPlaceholder = computed(() =>
  isVersionMode.value && props.succeedsDocument?.description
    ? props.succeedsDocument.description
    : 'Kort beskrivelse av dokumentet…'
)

const submitLabel = computed(() =>
  isVersionMode.value ? `Last opp v${(props.succeedsDocument?.currentVersion ?? 0) + 1}` : 'Last opp'
)

const canSubmit = computed(() =>
  file.value !== null && Object.keys(errors.value).length === 0
)

function validateFile(f: File): string | null {
  const ext = '.' + (f.name.split('.').pop() ?? '').toLowerCase()
  if (!ALLOWED_EXTENSIONS.includes(ext))
    return `Filtype ikke tillatt. Tillatte: ${ALLOWED_EXTENSIONS.join(' ')}`
  if (f.size === 0)            return 'Filen er tom.'
  if (f.size > MAX_FILE_BYTES) return 'Filen er større enn 10 MB.'
  return null
}

function validateTitle(v: string): string | null {
  if (!v) return null
  if (v.length > MAX_TITLE_LEN) return `Maks ${MAX_TITLE_LEN} tegn.`
  if (!SAFE_TEXT_RE.test(v))    return 'Tittelen inneholder ugyldige tegn.'
  return null
}

function validateDescription(v: string): string | null {
  if (!v) return null
  if (v.length > MAX_DESC_LEN) return `Maks ${MAX_DESC_LEN} tegn.`
  if (!SAFE_TEXT_RE.test(v))   return 'Beskrivelsen inneholder ugyldige tegn.'
  return null
}

function runValidation() {
  const errs: Record<string, string> = {}
  if (!file.value)                     errs.file        = 'Du må velge en fil.'
  else { const e = validateFile(file.value); if (e) errs.file = e }
  const te = validateTitle(title.value);       if (te) errs.title       = te
  const de = validateDescription(description.value); if (de) errs.description = de
  errors.value = errs
}

watch(title, () => {
  if (errors.value.title) {
    const e = validateTitle(title.value)
    const updated = { ...errors.value }
    if (e) updated.title = e; else delete updated.title
    errors.value = updated
  }
})
watch(description, () => {
  if (errors.value.description) {
    const e = validateDescription(description.value)
    const updated = { ...errors.value }
    if (e) updated.description = e; else delete updated.description
    errors.value = updated
  }
})

function applyFile(f: File) {
  const err = validateFile(f)
  if (err) { errors.value = { ...errors.value, file: err }; return }
  file.value = f
  if (!title.value.trim() && !isVersionMode.value) {
    title.value = f.name.replace(/\.[^.]+$/, '').replace(/[_-]/g, ' ')
  }
  const updated = { ...errors.value }
  delete updated.file
  errors.value = updated
}

function onFileInput(event: Event) {
  const input = event.target as HTMLInputElement
  if (input.files?.[0]) applyFile(input.files[0])
  // Reset the input so the same file can be re-selected after clearing
  input.value = ''
}

function onDrop(event: DragEvent) {
  isDragging.value = false
  const dropped = event.dataTransfer?.files?.[0]
  if (dropped) applyFile(dropped)
}

function clearFile() {
  file.value = null
  if (!isVersionMode.value) title.value = ''
}

function triggerFilePicker() {
  document.getElementById('doc-file-input')?.click()
}

function handleSubmit() {
  runValidation()
  if (!canSubmit.value || !file.value) return

  if (isVersionMode.value) {
    emit('upload-version', {
      file: file.value,
      title:       title.value.trim()       || undefined,
      description: description.value.trim() || undefined,
    })
  } else {
    emit('upload-document', {
      file: file.value,
      documentType: docType.value,
      title:       title.value.trim()       || undefined,
      description: description.value.trim() || undefined,
    })
  }
}

watch(() => props.open, (isOpen) => {
  if (!isOpen) {
    setTimeout(() => {
      file.value        = null
      title.value       = ''
      description.value = ''
      docType.value     = 'OTHER'
      errors.value      = {}
      isDragging.value  = false
    }, 200)
  } else if (isOpen && isVersionMode.value && props.succeedsDocument) {
    // Pre-fill metadata so the user only has to change what's actually new
    title.value       = props.succeedsDocument.title
    description.value = props.succeedsDocument.description ?? ''
  }
})

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') emit('close')
}
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="open"
        class="overlay"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="isVersionMode ? 'upload-modal-title-version' : 'upload-modal-title-new'"
        @keydown="onKeydown"
        @click.self="emit('close')"
      >
        <div class="modal">

          <div class="modal__header">
            <div>
              <h2
                :id="isVersionMode ? 'upload-modal-title-version' : 'upload-modal-title-new'"
                class="modal__title"
              >
                {{ modalTitle }}
              </h2>
              <p class="modal__subtitle">{{ modalSubtitle }}</p>
            </div>
            <button class="modal__close" type="button" aria-label="Lukk" @click="emit('close')">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
          <div class="modal__body">

            <div v-if="isVersionMode && succeedsDocument" class="version-banner">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="17 1 21 5 17 9"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 23 3 19 7 15"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/></svg>
              <span>
                Erstatter <strong>v{{ succeedsDocument.currentVersion }}</strong> av
                <strong>{{ succeedsDocument.title }}</strong>.
                Den gamle versjonen forblir tilgjengelig i databasen.
              </span>
            </div>

            <div
              class="dropzone"
              :class="{
                'dropzone--dragging': isDragging,
                'dropzone--filled':   file !== null,
                'dropzone--error':    !!errors.file,
              }"
              role="button"
              tabindex="0"
              :aria-label="file
                ? `Valgt fil: ${file.name}. Trykk for å bytte.`
                : 'Dra og slipp fil hit, eller trykk for å velge'"
              @click="triggerFilePicker"
              @keydown.enter.space.prevent="triggerFilePicker"
              @dragover.prevent="isDragging = true"
              @dragleave.prevent="isDragging = false"
              @drop.prevent="onDrop"
            >
              <input
                id="doc-file-input"
                type="file"
                class="dropzone__input"
                :accept="ALLOWED_EXTENSIONS.join(',')"
                aria-hidden="true"
                tabindex="-1"
                @change="onFileInput"
              />

              <template v-if="!file">
                <div class="dropzone__icon" aria-hidden="true">
                  <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                </div>
                <p class="dropzone__label">
                  <span class="dropzone__cta">Dra og slipp</span> eller
                  <span class="dropzone__cta">klikk for å velge</span>
                </p>
                <p class="dropzone__hint">{{ ALLOWED_EXTENSIONS.join(' · ') }} — maks 10 MB</p>
              </template>

              <template v-else>
                <div class="dropzone__file">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
                  <span class="dropzone__filename">{{ fileLabel }}</span>
                  <button
                    class="dropzone__remove"
                    type="button"
                    aria-label="Fjern valgt fil"
                    @click.stop="clearFile"
                  >
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                  </button>
                </div>
                <p class="dropzone__change-hint">Klikk for å velge en annen fil</p>
              </template>
            </div>

            <p v-if="errors.file" class="field-error field-error--standalone" role="alert">{{ errors.file }}</p>

            <div v-if="!isVersionMode" class="field">
              <label for="doc-type" class="field__label">
                Dokumenttype <span class="field__required" aria-hidden="true">*</span>
              </label>
              <select id="doc-type" v-model="docType" class="field__select">
                <option v-for="dt in DOCUMENT_TYPES" :key="dt.value" :value="dt.value">
                  {{ dt.label }}
                </option>
              </select>
            </div>

            <!-- Title -->
            <div class="field">
              <label for="doc-title" class="field__label">
                Tittel
                <span class="field__optional">
                  {{ isVersionMode ? '(valgfritt — oppdaterer dokumenttittelen)' : '(valgfritt — standard er filnavnet)' }}
                </span>
              </label>
              <input
                id="doc-title"
                v-model="title"
                type="text"
                class="field__input"
                :class="{ 'field__input--error': errors.title }"
                :placeholder="titlePlaceholder"
                :maxlength="MAX_TITLE_LEN"
                autocomplete="off"
                spellcheck="false"
                :aria-invalid="!!errors.title"
                aria-describedby="doc-title-error doc-title-count"
              />
              <div class="field__footer">
                <span v-if="errors.title" id="doc-title-error" class="field-error" role="alert">{{ errors.title }}</span>
                <span v-else id="doc-title-error" />
                <span
                  id="doc-title-count"
                  class="field__count"
                  :class="{ 'field__count--warn': title.length > MAX_TITLE_LEN - 20 }"
                >{{ title.length }}/{{ MAX_TITLE_LEN }}</span>
              </div>
            </div>

            <!-- Description -->
            <div class="field">
              <label for="doc-description" class="field__label">
                Beskrivelse
                <span class="field__optional">
                  {{ isVersionMode ? '(valgfritt — oppdaterer dokumentbeskrivelsen)' : '(valgfritt)' }}
                </span>
              </label>
              <textarea
                id="doc-description"
                v-model="description"
                class="field__textarea"
                :class="{ 'field__input--error': errors.description }"
                :placeholder="descriptionPlaceholder"
                :maxlength="MAX_DESC_LEN"
                rows="3"
                :aria-invalid="!!errors.description"
                aria-describedby="doc-desc-error doc-desc-count"
              />
              <div class="field__footer">
                <span v-if="errors.description" id="doc-desc-error" class="field-error" role="alert">{{ errors.description }}</span>
                <span v-else id="doc-desc-error" />
                <span
                  id="doc-desc-count"
                  class="field__count"
                  :class="{ 'field__count--warn': description.length > MAX_DESC_LEN - 50 }"
                >{{ description.length }}/{{ MAX_DESC_LEN }}</span>
              </div>
            </div>

          </div>

          <div class="modal__footer">
            <button class="btn btn--ghost" type="button" @click="emit('close')">Avbryt</button>
            <button
              class="btn btn--primary"
              :class="{ 'btn--version': isVersionMode }"
              type="button"
              :disabled="!canSubmit"
              :title="!file ? 'Velg en fil først' : undefined"
              @click="handleSubmit"
            >
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
              {{ submitLabel }}
            </button>
          </div>

        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.overlay {
  position: fixed; inset: 0; z-index: 200;
  background: rgba(15, 23, 42, 0.5);
  display: flex; align-items: center; justify-content: center;
  padding: 1.5rem;
  backdrop-filter: blur(3px);
}

.modal {
  background: var(--color-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  width: min(560px, 100%);
  max-height: 90vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

/* Header */
.modal__header {
  display: flex; align-items: flex-start; justify-content: space-between;
  gap: 1rem; padding: 1.25rem 1.5rem 1rem;
  border-bottom: 1px solid var(--color-border); flex-shrink: 0;
}
.modal__title {
  margin: 0; font-size: var(--font-size-lg); font-weight: 700;
  color: var(--color-gray-900);
  /* long titles from succeedsDocument should wrap not overflow */
  overflow-wrap: break-word; max-width: 36ch;
}
.modal__subtitle {
  margin: 0.2rem 0 0; font-size: var(--font-size-sm); color: var(--color-gray-500);
}
.modal__close {
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0; width: 2rem; height: 2rem;
  border-radius: var(--radius-sm); background: transparent;
  border: 1px solid var(--color-border); color: var(--color-gray-500);
  cursor: pointer; transition: all var(--transition-fast);
}
.modal__close:hover { background: var(--color-gray-100); color: var(--color-gray-900); }

/* Body */
.modal__body {
  padding: 1.25rem 1.5rem;
  display: flex; flex-direction: column; gap: 1.1rem; flex: 1;
}

/* Version banner */
.version-banner {
  display: flex; align-items: flex-start; gap: 0.6rem;
  padding: 0.65rem 0.9rem;
  border-radius: var(--radius-md);
  background: var(--color-info-bg);
  color: var(--color-info);
  font-size: var(--font-size-sm);
  border: 1px solid rgba(2, 132, 199, 0.2);
}
.version-banner svg { flex-shrink: 0; margin-top: 1px; }

/* Drop zone */
.dropzone {
  position: relative;
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-gray-50);
  padding: 1.75rem 1.25rem;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 0.5rem; cursor: pointer;
  transition: border-color var(--transition-fast), background var(--transition-fast);
  text-align: center; min-height: 9rem;
}
.dropzone:hover, .dropzone:focus-visible {
  border-color: var(--color-gray-400); background: var(--color-gray-100); outline: none;
}
.dropzone--dragging { border-color: var(--color-focus); background: var(--color-info-bg); }
.dropzone--filled   { border-style: solid; border-color: var(--color-success); background: var(--color-success-bg); }
.dropzone--error    { border-color: var(--color-danger); background: var(--color-danger-bg); }
.dropzone__input    { position: absolute; width: 1px; height: 1px; opacity: 0; pointer-events: none; }
.dropzone__icon     { color: var(--color-gray-400); }
.dropzone__label    { font-size: var(--font-size-sm); color: var(--color-gray-600); margin: 0; }
.dropzone__cta      { font-weight: 600; color: var(--color-gray-800); text-decoration: underline; text-underline-offset: 2px; }
.dropzone__hint     { font-size: var(--font-size-xs); color: var(--color-gray-400); margin: 0; }
.dropzone__file     { display: flex; align-items: center; gap: 0.6rem; color: var(--color-success); }
.dropzone__filename { font-size: var(--font-size-sm); font-weight: 600; color: var(--color-gray-800); word-break: break-all; }
.dropzone__remove   {
  display: inline-flex; align-items: center; justify-content: center;
  width: 1.4rem; height: 1.4rem; border-radius: 999px;
  background: var(--color-gray-200); border: none;
  color: var(--color-gray-600); cursor: pointer; flex-shrink: 0;
  transition: background var(--transition-fast);
}
.dropzone__remove:hover { background: var(--color-danger-bg); color: var(--color-danger); }
.dropzone__change-hint  { font-size: var(--font-size-xs); color: var(--color-gray-500); margin: 0; }

/* Fields */
.field { display: flex; flex-direction: column; gap: 0.35rem; }
.field__label { font-size: var(--font-size-sm); font-weight: 600; color: var(--color-gray-700); }
.field__required { color: var(--color-danger); margin-left: 0.1rem; }
.field__optional { font-weight: 400; color: var(--color-gray-400); font-size: var(--font-size-xs); margin-left: 0.3rem; }

.field__select,
.field__input,
.field__textarea {
  width: 100%; padding: 0.55rem 0.75rem;
  border: 1px solid var(--color-border); border-radius: var(--radius-md);
  background: var(--color-card); font-size: var(--font-size-sm);
  font-family: inherit; color: var(--color-gray-800);
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
  box-sizing: border-box;
}
.field__select:focus,
.field__input:focus,
.field__textarea:focus {
  outline: none; border-color: var(--color-focus);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}
.field__input--error { border-color: var(--color-danger); }
.field__input--error:focus { box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.15); }
.field__textarea { resize: vertical; min-height: 5rem; }

.field__footer {
  display: flex; align-items: baseline; justify-content: space-between;
  gap: 0.5rem; min-height: 1.2rem;
}
.field__count      { font-size: var(--font-size-xs); color: var(--color-gray-400); white-space: nowrap; flex-shrink: 0; }
.field__count--warn { color: var(--color-warning); font-weight: 600; }

.field-error               { font-size: var(--font-size-xs); color: var(--color-danger); font-weight: 500; }
.field-error--standalone   { margin-top: -0.5rem; }

/* Footer */
.modal__footer {
  display: flex; justify-content: flex-end; gap: 0.6rem;
  padding: 1rem 1.5rem; border-top: 1px solid var(--color-border); flex-shrink: 0;
}

.btn {
  display: inline-flex; align-items: center; gap: 0.4rem;
  padding: 0.55rem 1.1rem; border-radius: var(--radius-md);
  font-size: var(--font-size-sm); font-weight: 600;
  font-family: inherit; cursor: pointer;
  transition: all var(--transition-fast); white-space: nowrap;
}
.btn--ghost   { background: transparent; color: var(--color-gray-600); border: 1px solid var(--color-border); }
.btn--ghost:hover { background: var(--color-gray-100); }
.btn--primary { background: var(--color-foreground); color: var(--color-card); border: 1px solid transparent; }
.btn--primary:hover:not(:disabled) { opacity: 0.85; }
.btn--primary:disabled { opacity: 0.45; cursor: not-allowed; }
/* Version mode — teal accent to visually distinguish from a plain upload */
.btn--version { background: var(--ik-mat-primary, #0d7377); }

/* Transitions */
.modal-enter-active, .modal-leave-active { transition: opacity var(--transition-fast); }
.modal-enter-active .modal, .modal-leave-active .modal { transition: transform var(--transition-fast), opacity var(--transition-fast); }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-from .modal, .modal-leave-to .modal { transform: translateY(12px); opacity: 0; }

/* Responsive */
@media (max-width: 48rem) {
  .overlay { padding: 0; align-items: flex-end; }
  .modal { width: 100%; max-height: 95vh; border-bottom-left-radius: 0; border-bottom-right-radius: 0; }
  .modal__title { max-width: 100%; }
}
</style>
