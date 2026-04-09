<!--
  BulkUploadChecklist - reusable component for bulk uploading checklists

  Lets users with checklist:write-permissions upload a checklist
  via JSON-, CSV- or XLSX-file.

  Usage:
    <BulkUploadChecklist @created="handleCreated" />

  Emits:
    created(payload) – Throws { template, items } when «Opprett sjekkliste»-button is pressed
                        and validation passes
-->
<script setup lang="ts">
import { ref, computed } from 'vue'
import * as XLSX from 'xlsx'

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

const emit = defineEmits<{
  created: [payload: { template: TemplateForm; items: ChecklistItem[] }]
}>()

// ─── State ──────────────────────────────────────────────────────────────────

const template = ref<TemplateForm>({
  title: '',
  description: '',
  frequency: 'DAILY',
  moduleType: 'FOOD',
  isActive: true,
})

const items = ref<ChecklistItem[]>([])
const fileError = ref<string | null>(null)
const fileName = ref<string | null>(null)
const isDragging = ref(false)
const showFormatGuide = ref(false)
const submitAttempted = ref(false)

const templateErrors = computed(() => {
  const errors: Partial<Record<keyof TemplateForm, string>> = {}
  if (!template.value.title.trim()) errors.title = 'Tittel er påkrevd'
  if (!template.value.frequency) errors.frequency = 'Frekvens er påkrevd'
  if (!template.value.moduleType) errors.moduleType = 'Modul er påkrevd'
  return errors
})

const hasTemplateErrors = computed(() => Object.keys(templateErrors.value).length > 0)
const hasItems = computed(() => items.value.length > 0)
const canSubmit = computed(() => !hasTemplateErrors.value && hasItems.value)

const REQUIRED_ITEM_KEYS: (keyof ChecklistItem)[] = ['sortOrder', 'label', 'itemType']
const VALID_ITEM_TYPES: ItemType[] = ['BOOLEAN', 'TEXT', 'NUMBER', 'TEMPERATURE', 'CHOICE']

function normalizeItem(raw: Record<string, unknown>, index: number): ChecklistItem {
  const itemType = String(raw.itemType ?? raw.item_type ?? '').toUpperCase() as ItemType
  return {
    sortOrder: Number(raw.sortOrder ?? raw.sort_order ?? index + 1),
    label: String(raw.label ?? '').trim(),
    description: raw.description ? String(raw.description).trim() : undefined,
    itemType,
    isRequired: raw.isRequired !== undefined
        ? raw.isRequired === true || raw.isRequired === 'true' || raw.isRequired === 1
        : raw.is_required !== undefined
            ? raw.is_required === true || raw.is_required === 'true' || raw.is_required === 1
            : true,
    expectedText: raw.expectedText
        ? String(raw.expectedText)
        : raw.expected_text
            ? String(raw.expected_text)
            : undefined,
    expectedNumericMin: raw.expectedNumericMin != null
        ? Number(raw.expectedNumericMin)
        : raw.expected_numeric_min != null
            ? Number(raw.expected_numeric_min)
            : null,
    expectedNumericMax: raw.expectedNumericMax != null
        ? Number(raw.expectedNumericMax)
        : raw.expected_numeric_max != null
            ? Number(raw.expected_numeric_max)
            : null,
    choiceOptionsJson: raw.choiceOptionsJson
        ? String(raw.choiceOptionsJson)
        : raw.choice_options_json
            ? String(raw.choice_options_json)
            : undefined,
  }
}

function validateItems(parsed: ChecklistItem[]): string | null {
  if (parsed.length === 0) return 'Filen inneholder ingen punkter'

  for (let i = 0; i < parsed.length; i++) {
    const item = parsed[i]
    if (!item) {
      return `Rad ${i + 1}: mangler data`
    }
    if (!item.label) return `Rad ${i + 1}: «label» mangler`
    if (!VALID_ITEM_TYPES.includes(item.itemType)) {
      return `Rad ${i + 1}: ugyldig itemType «${item.itemType}». Gyldige: ${VALID_ITEM_TYPES.join(', ')}`
    }
    if (item.itemType === 'CHOICE' && !item.choiceOptionsJson) {
      return `Rad ${i + 1}: CHOICE-type krever «choiceOptionsJson»`
    }
  }
  return null
}

function parseJSON(text: string): ChecklistItem[] {
  const data = JSON.parse(text)
  const rows: Record<string, unknown>[] = Array.isArray(data) ? data : data.items ?? []
  return rows.map((r, i) => normalizeItem(r, i))
}

function parseCSV(text: string): ChecklistItem[] {
  const lines = text.trim().split('\n')
  if (lines.length < 2) throw new Error('CSV mangler innhold')
  const firstLine = lines[0]
  if (!firstLine) throw new Error('CSV mangler header')

  const headers = firstLine.split(',').map((h) => h.trim().replace(/^"|"$/g, ''))
  return lines.slice(1).map((line, i) => {
    const values = line.split(',').map((v) => v.trim().replace(/^"|"$/g, ''))
    const row: Record<string, unknown> = {}
    headers.forEach((h, j) => { row[h] = values[j] ?? '' })
    return normalizeItem(row, i)
  })
}

function parseXLSX(buffer: ArrayBuffer): ChecklistItem[] {
  const wb = XLSX.read(buffer, { type: 'array' })
  const firstSheetName = wb.SheetNames[0]
  if (!firstSheetName) {
    return []
  }
  const ws = wb.Sheets[firstSheetName]
  if (!ws) {
    return []
  }
  const rows: Record<string, unknown>[] = XLSX.utils.sheet_to_json(ws, { defval: '' })
  return rows.map((r, i) => normalizeItem(r, i))
}

async function processFile(file: File) {
  fileError.value = null
  fileName.value = file.name
  items.value = []

  try {
    const ext = file.name.split('.').pop()?.toLowerCase()
    let parsed: ChecklistItem[] = []

    if (ext === 'json') {
      const text = await file.text()
      parsed = parseJSON(text)
    } else if (ext === 'csv') {
      const text = await file.text()
      parsed = parseCSV(text)
    } else if (ext === 'xlsx' || ext === 'xls') {
      const buffer = await file.arrayBuffer()
      parsed = parseXLSX(buffer)
    } else {
      fileError.value = 'Ugyldig filtype. Bruk .json, .csv eller .xlsx'
      return
    }

    const validationError = validateItems(parsed)
    if (validationError) {
      fileError.value = validationError
      return
    }

    items.value = parsed
  } catch (err: unknown) {
    fileError.value = err instanceof Error ? `Feil ved lesing: ${err.message}` : 'Klarte ikke lese filen'
  }
}

function onFileInput(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (file) processFile(file)
}

function onDrop(event: DragEvent) {
  isDragging.value = false
  const file = event.dataTransfer?.files[0]
  if (file) processFile(file)
}

function clearFile() {
  items.value = []
  fileName.value = null
  fileError.value = null
}

function handleSubmit() {
  submitAttempted.value = true
  if (!canSubmit.value) return
  emit('created', { template: { ...template.value }, items: [...items.value] })
}
</script>

<template>
  <section class="bulk-upload">
    <div class="bulk-upload__inner">
      <div class="bulk-upload__section">
        <h3 class="bulk-upload__section-title">Sjekklistemal</h3>
        <p class="bulk-upload__section-desc">
          Fyll ut informasjon om sjekklisten. Dette kan gjøres i hvilken som helst rekkefølge.
        </p>

        <div class="form-grid">
          <div class="form-group form-group--wide">
            <label class="form-label" for="bulk-title">
              Tittel <span class="form-label__required" aria-hidden="true">*</span>
            </label>
            <input
                id="bulk-title"
                v-model="template.title"
                class="form-input"
                :class="{ 'form-input--error': submitAttempted && templateErrors.title }"
                type="text"
                placeholder="F.eks. Daglig temperaturkontroll"
            />
            <p v-if="submitAttempted && templateErrors.title" class="form-error">
              {{ templateErrors.title }}
            </p>
          </div>

          <div class="form-group form-group--wide">
            <label class="form-label" for="bulk-description">Beskrivelse</label>
            <textarea
                id="bulk-description"
                v-model="template.description"
                class="form-input form-input--textarea"
                rows="2"
                placeholder="Valgfri beskrivelse av hva sjekklisten dekker"
            />
          </div>

          <div class="form-group">
            <label class="form-label" for="bulk-frequency">
              Frekvens <span class="form-label__required" aria-hidden="true">*</span>
            </label>
            <select
                id="bulk-frequency"
                v-model="template.frequency"
                class="form-input"
                :class="{ 'form-input--error': submitAttempted && templateErrors.frequency }"
            >
              <option value="DAILY">Daglig</option>
              <option value="WEEKLY">Ukentlig</option>
              <option value="MONTHLY">Månedlig</option>
              <option value="CUSTOM">Egendefinert</option>
            </select>
            <p v-if="submitAttempted && templateErrors.frequency" class="form-error">
              {{ templateErrors.frequency }}
            </p>
          </div>

          <div class="form-group">
            <label class="form-label" for="bulk-module">
              Modul <span class="form-label__required" aria-hidden="true">*</span>
            </label>
            <select
                id="bulk-module"
                v-model="template.moduleType"
                class="form-input"
                :class="{ 'form-input--error': submitAttempted && templateErrors.moduleType }"
            >
              <option value="FOOD">IK-Mat</option>
              <option value="ALCOHOL">IK-Alkohol</option>
            </select>
            <p v-if="submitAttempted && templateErrors.moduleType" class="form-error">
              {{ templateErrors.moduleType }}
            </p>
          </div>

          <div class="form-group form-group--checkbox">
            <label class="form-checkbox-label">
              <input v-model="template.isActive" type="checkbox" class="form-checkbox" />
              <span>Aktiv ved opprettelse</span>
            </label>
          </div>
        </div>
      </div>

      <div class="bulk-upload__section">
        <h3 class="bulk-upload__section-title">Last opp punkter</h3>
        <p class="bulk-upload__section-desc">
          Last opp en fil med sjekklistepunkter. Støttede formater: JSON, CSV, XLSX.
          Minst ett punkt er påkrevd.
        </p>

        <!-- Drop zone -->
        <label
            class="drop-zone"
            :class="{
            'drop-zone--dragging': isDragging,
            'drop-zone--loaded': hasItems && !fileError,
            'drop-zone--error': !!fileError || (submitAttempted && !hasItems),
          }"
            @dragover.prevent="isDragging = true"
            @dragleave.prevent="isDragging = false"
            @drop.prevent="onDrop"
        >
          <input
              type="file"
              accept=".json,.csv,.xlsx,.xls"
              class="drop-zone__input"
              @change="onFileInput"
          />

          <template v-if="!hasItems && !fileError">
            <span class="drop-zone__icon" aria-hidden="true">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="17 8 12 3 7 8" />
                <line x1="12" y1="3" x2="12" y2="15" />
              </svg>
            </span>
            <p class="drop-zone__primary">Dra og slipp fil her, eller <span class="drop-zone__link">velg fil</span></p>
            <p class="drop-zone__secondary">JSON · CSV · XLSX — maks 5 MB</p>
          </template>

          <template v-else-if="hasItems">
            <span class="drop-zone__icon drop-zone__icon--success" aria-hidden="true">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="20 6 9 17 4 12" />
              </svg>
            </span>
            <p class="drop-zone__primary">{{ fileName }}</p>
            <p class="drop-zone__secondary">{{ items.length }} punkt{{ items.length !== 1 ? 'er' : '' }} lastet inn</p>
          </template>

          <template v-else-if="fileError">
            <span class="drop-zone__icon drop-zone__icon--error" aria-hidden="true">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
            </span>
            <p class="drop-zone__primary drop-zone__primary--error">{{ fileError }}</p>
            <p class="drop-zone__secondary">Klikk for å prøve igjen</p>
          </template>
        </label>

        <p
            v-if="submitAttempted && !hasItems && !fileError"
            class="form-error form-error--standalone"
        >
          Du må laste opp en fil med minst ett punkt
        </p>

        <!-- Loaded items preview -->
        <div v-if="hasItems" class="items-preview">
          <div class="items-preview__header">
            <p class="items-preview__count">{{ items.length }} punkt{{ items.length !== 1 ? 'er' : '' }}</p>
            <button type="button" class="items-preview__clear" @click.stop="clearFile">
              Fjern fil
            </button>
          </div>

          <ul class="items-list">
            <li v-for="(item, idx) in items" :key="idx" class="items-list__row">
              <span class="items-list__order">{{ item.sortOrder }}</span>
              <span class="items-list__label">{{ item.label }}</span>
              <span class="items-list__badge" :class="`items-list__badge--${item.itemType.toLowerCase()}`">
                {{ item.itemType }}
              </span>
              <span v-if="!item.isRequired" class="items-list__optional">valgfritt</span>
            </li>
          </ul>
        </div>
      </div>

      <div class="bulk-upload__section bulk-upload__section--guide">
        <button
            type="button"
            class="guide-toggle"
            :aria-expanded="showFormatGuide"
            @click="showFormatGuide = !showFormatGuide"
        >
          <svg
              class="guide-toggle__icon"
              :class="{ 'guide-toggle__icon--open': showFormatGuide }"
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
          >
            <polyline points="6 9 12 15 18 9" />
          </svg>
          <span>Vis forventet filformat</span>
        </button>

        <div v-if="showFormatGuide" class="guide-content">
          <p class="guide-content__intro">
            Filen skal inneholde sjekklistepunkter. Hvert punkt trenger minst
            <code>label</code>, <code>itemType</code> og <code>sortOrder</code>.
          </p>

          <div class="guide-grid">
            <!-- JSON eksempel -->
            <div class="guide-block">
              <div class="guide-block__header">
                <span class="guide-block__badge">JSON</span>
                <span class="guide-block__filename">items.json</span>
              </div>
              <pre class="guide-code"><code>[
  {
    "sortOrder": 1,
    "label": "Er kjøleskaptemperaturen under 4°C?",
    "description": "Mål med kalibrert termometer",
    "itemType": "TEMPERATURE",
    "isRequired": true,
    "expectedNumericMin": -2,
    "expectedNumericMax": 4
  },
  {
    "sortOrder": 2,
    "label": "Er rengjøringslogg signert?",
    "itemType": "BOOLEAN",
    "isRequired": true
  },
  {
    "sortOrder": 3,
    "label": "Velg rengjøringsmiddel",
    "itemType": "CHOICE",
    "isRequired": false,
    "choiceOptionsJson": "[\"Produkt A\",\"Produkt B\"]"
  }
]</code></pre>
            </div>

            <!-- CSV eksempel -->
            <div class="guide-block">
              <div class="guide-block__header">
                <span class="guide-block__badge guide-block__badge--csv">CSV</span>
                <span class="guide-block__filename">items.csv</span>
              </div>
              <pre class="guide-code"><code>sortOrder,label,description,itemType,isRequired,expectedNumericMin,expectedNumericMax,choiceOptionsJson
1,Er kjøleskaptemperaturen under 4°C?,Mål med termometer,TEMPERATURE,true,-2,4,
2,Er rengjøringslogg signert?,,BOOLEAN,true,,,
3,Kommentar fra ansvarlig,,TEXT,false,,,
4,Velg rengjøringsmiddel,,CHOICE,false,,,["Produkt A","Produkt B"]</code></pre>
            </div>
          </div>

          <div class="guide-table-wrap">
            <p class="guide-table-title">Feltoversikt</p>
            <table class="guide-table">
              <thead>
              <tr>
                <th>Felt</th>
                <th>Type</th>
                <th>Påkrevd</th>
                <th>Beskrivelse</th>
              </tr>
              </thead>
              <tbody>
              <tr>
                <td><code>sortOrder</code></td>
                <td>heltall</td>
                <td class="guide-table__req">Ja</td>
                <td>Rekkefølge i listen</td>
              </tr>
              <tr>
                <td><code>label</code></td>
                <td>tekst</td>
                <td class="guide-table__req">Ja</td>
                <td>Spørsmålet / oppgaveteksten</td>
              </tr>
              <tr>
                <td><code>itemType</code></td>
                <td>enum</td>
                <td class="guide-table__req">Ja</td>
                <td>BOOLEAN · TEXT · NUMBER · TEMPERATURE · CHOICE</td>
              </tr>
              <tr>
                <td><code>description</code></td>
                <td>tekst</td>
                <td>Nei</td>
                <td>Hjelpetekst til punktet</td>
              </tr>
              <tr>
                <td><code>isRequired</code></td>
                <td>boolean</td>
                <td>Nei</td>
                <td>Standard: true</td>
              </tr>
              <tr>
                <td><code>expectedText</code></td>
                <td>tekst</td>
                <td>Nei</td>
                <td>Forventet tekstsvar (for TEXT-type)</td>
              </tr>
              <tr>
                <td><code>expectedNumericMin</code></td>
                <td>desimaltall</td>
                <td>Nei</td>
                <td>Nedre grense (NUMBER / TEMPERATURE)</td>
              </tr>
              <tr>
                <td><code>expectedNumericMax</code></td>
                <td>desimaltall</td>
                <td>Nei</td>
                <td>Øvre grense (NUMBER / TEMPERATURE)</td>
              </tr>
              <tr>
                <td><code>choiceOptionsJson</code></td>
                <td>JSON-streng</td>
                <td>Nei*</td>
                <td>Alternativer for CHOICE-type. *Påkrevd for CHOICE</td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div class="bulk-upload__footer">
        <p v-if="submitAttempted && !canSubmit" class="bulk-upload__footer-error">
          Fyll ut alle påkrevde felt og last opp en fil for å fortsette.
        </p>
        <button
            type="button"
            class="submit-btn"
            :class="{ 'submit-btn--disabled': submitAttempted && !canSubmit }"
            @click="handleSubmit"
        >
          Opprett sjekkliste
        </button>
      </div>

    </div>
  </section>
</template>

<style scoped>
.bulk-upload {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-card);
  overflow: hidden;
}

.bulk-upload__inner {
  display: grid;
}

.bulk-upload__section {
  padding: 1.25rem 1.25rem;
  border-bottom: 1px solid var(--color-border);
}

.bulk-upload__section:last-of-type {
  border-bottom: none;
}

.bulk-upload__section--guide {
  background: var(--color-gray-50);
}

.bulk-upload__section-title {
  margin: 0 0 0.2rem;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-foreground);
}

.bulk-upload__section-desc {
  margin: 0 0 1rem;
  font-size: var(--font-size-sm);
  color: var(--color-gray-500);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.form-group--wide {
  grid-column: 1 / -1;
}

.form-group--checkbox {
  justify-content: flex-end;
}

.form-label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-gray-700);
}

.form-label__required {
  color: var(--color-danger);
  margin-left: 0.15rem;
}

.form-input {
  min-height: 2.5rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-family: inherit;
  color: var(--color-foreground);
  background: var(--color-card);
  transition: border-color var(--transition-fast);
}

.form-input:focus {
  outline: none;
  border-color: var(--color-focus);
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.12);
}

.form-input--error {
  border-color: var(--color-danger);
}

.form-input--textarea {
  min-height: 4rem;
  resize: vertical;
}

.form-error {
  font-size: var(--font-size-xs);
  color: var(--color-danger);
  margin: 0;
}

.form-error--standalone {
  margin-top: 0.5rem;
}

.form-checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: var(--font-size-sm);
  color: var(--color-gray-700);
  cursor: pointer;
  min-height: 2.5rem;
}

.form-checkbox {
  width: 1rem;
  height: 1rem;
  cursor: pointer;
}

.drop-zone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  padding: 2rem 1.5rem;
  border: 1.5px dashed var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-gray-50);
  cursor: pointer;
  transition: border-color var(--transition-fast), background var(--transition-fast);
  text-align: center;
  min-height: 9rem;
}

.drop-zone:hover,
.drop-zone--dragging {
  border-color: var(--color-gray-400);
  background: var(--color-accent);
}

.drop-zone--loaded {
  border-color: var(--color-success);
  background: var(--color-success-bg);
  border-style: solid;
}

.drop-zone--error {
  border-color: var(--color-danger);
  background: var(--color-danger-bg);
  border-style: solid;
}

.drop-zone__input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.drop-zone__icon {
  color: var(--color-gray-400);
  display: flex;
}

.drop-zone__icon--success {
  color: var(--color-success);
}

.drop-zone__icon--error {
  color: var(--color-danger);
}

.drop-zone__primary {
  margin: 0;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-foreground);
}

.drop-zone__primary--error {
  color: var(--color-danger);
}

.drop-zone__secondary {
  margin: 0;
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
}

.drop-zone__link {
  text-decoration: underline;
  color: var(--color-info);
}

.items-preview {
  margin-top: 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.items-preview__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.55rem 0.75rem;
  background: var(--color-gray-50);
  border-bottom: 1px solid var(--color-border);
}

.items-preview__count {
  margin: 0;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  color: var(--color-gray-600);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.items-preview__clear {
  border: none;
  background: none;
  font-size: var(--font-size-xs);
  color: var(--color-danger);
  cursor: pointer;
  padding: 0.2rem 0.4rem;
  border-radius: var(--radius-sm);
}

.items-preview__clear:hover {
  background: var(--color-danger-bg);
}

.items-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 14rem;
  overflow-y: auto;
}

.items-list__row {
  display: grid;
  grid-template-columns: 2rem 1fr auto auto;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  border-bottom: 1px solid var(--color-border);
  font-size: var(--font-size-sm);
}

.items-list__row:last-child {
  border-bottom: none;
}

.items-list__order {
  font-size: var(--font-size-xs);
  color: var(--color-gray-400);
  font-weight: var(--font-weight-semibold);
  text-align: right;
}

.items-list__label {
  color: var(--color-foreground);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.items-list__badge {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  padding: 0.15rem 0.4rem;
  border-radius: var(--radius-sm);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  white-space: nowrap;
}

.items-list__badge--boolean   { background: #dbeafe; color: #1e40af; }
.items-list__badge--text      { background: #ede9fe; color: #5b21b6; }
.items-list__badge--number    { background: #fef3c7; color: #92400e; }
.items-list__badge--temperature { background: #ffedd5; color: #9a3412; }
.items-list__badge--choice    { background: #d1fae5; color: #065f46; }

.items-list__optional {
  font-size: var(--font-size-xs);
  color: var(--color-gray-400);
  font-style: italic;
}

.guide-toggle {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  border: none;
  background: none;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-gray-600);
  cursor: pointer;
  padding: 0;
}

.guide-toggle:hover {
  color: var(--color-foreground);
}

.guide-toggle__icon {
  transition: transform var(--transition-fast);
  flex-shrink: 0;
}

.guide-toggle__icon--open {
  transform: rotate(180deg);
}

.guide-content {
  margin-top: 1rem;
}

.guide-content__intro {
  margin: 0 0 0.75rem;
  font-size: var(--font-size-sm);
  color: var(--color-gray-600);
}

.guide-content__intro code {
  font-family: 'Menlo', 'Consolas', monospace;
  font-size: 0.8em;
  background: var(--color-gray-200);
  border-radius: 2px;
  padding: 0.1rem 0.3rem;
}

.guide-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.guide-block {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.guide-block__header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.45rem 0.75rem;
  background: var(--color-gray-100);
  border-bottom: 1px solid var(--color-border);
}

.guide-block__badge {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
  padding: 0.1rem 0.4rem;
  border-radius: var(--radius-sm);
  background: #fef3c7;
  color: #92400e;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.guide-block__badge--csv {
  background: #d1fae5;
  color: #065f46;
}

.guide-block__filename {
  font-size: var(--font-size-xs);
  color: var(--color-gray-500);
  font-family: 'Menlo', 'Consolas', monospace;
}

.guide-code {
  margin: 0;
  padding: 0.75rem;
  font-size: 11px;
  font-family: 'Menlo', 'Consolas', 'Monaco', monospace;
  color: var(--color-gray-800);
  overflow-x: auto;
  line-height: 1.6;
  background: var(--color-card);
}

.guide-table-wrap {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.guide-table-title {
  margin: 0;
  padding: 0.45rem 0.75rem;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-gray-600);
  background: var(--color-gray-100);
  border-bottom: 1px solid var(--color-border);
}

.guide-table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--font-size-xs);
}

.guide-table th,
.guide-table td {
  padding: 0.5rem 0.75rem;
  text-align: left;
  border-bottom: 1px solid var(--color-border);
  color: var(--color-gray-700);
}

.guide-table thead th {
  background: var(--color-gray-50);
  font-weight: var(--font-weight-semibold);
  color: var(--color-gray-600);
}

.guide-table tbody tr:last-child td {
  border-bottom: none;
}

.guide-table tbody tr:hover td {
  background: var(--color-gray-50);
}

.guide-table code {
  font-family: 'Menlo', 'Consolas', monospace;
  font-size: 0.9em;
  background: var(--color-gray-100);
  border-radius: 2px;
  padding: 0.1rem 0.25rem;
}

.guide-table__req {
  color: var(--color-danger);
  font-weight: var(--font-weight-semibold);
}

.bulk-upload__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  background: var(--color-gray-50);
  border-top: 1px solid var(--color-border);
}

.bulk-upload__footer-error {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-danger);
  flex: 1;
}

.submit-btn {
  min-height: 2.5rem;
  padding: 0 1.25rem;
  border: none;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: var(--color-primary-foreground);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  cursor: pointer;
  transition: opacity var(--transition-fast);
  white-space: nowrap;
}

.submit-btn:hover {
  opacity: 0.88;
}

.submit-btn--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 48rem) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-group--wide {
    grid-column: 1;
  }

  .guide-grid {
    grid-template-columns: 1fr;
  }

  .items-list__row {
    grid-template-columns: 2rem 1fr auto;
  }

  .items-list__optional {
    display: none;
  }

  .bulk-upload__footer {
    flex-direction: column;
    align-items: stretch;
  }

  .submit-btn {
    width: 100%;
    text-align: center;
  }
}
</style>