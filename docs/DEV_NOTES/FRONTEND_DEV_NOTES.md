# Frontend dev notes

Konvensjoner for å utvikle frontend i IK-Kontroll og oppnå karakter A.
Author: @TRi

> **Vue 3 · TypeScript · Composition API · Pinia · Vue Router · Vitest · Cypress**

---

## Innhold

1. [Prioriteringer for A](#1-prioriteringer-for-a)
2. [Tekniske valg](#2-tekniske-valg)
3. [Prosjektstruktur](#3-prosjektstruktur)
4. [CSS](#4-css)
5. [Komponenter](#5-komponenter)
6. [Composables](#6-composables)
7. [State](#7-state)
8. [Routing](#8-routing)
9. [API](#9-api)
10. [Skjemaer](#10-skjemaer)
11. [Feilhåndtering](#11-feilhåndtering)
12. [Roller](#12-roller)
13. [Testing](#13-testing)
14. [Sikkerhet](#14-sikkerhet)
15. [WCAG](#15-wcag)
16. [Navn](#16-navn)
17. [Sjekkliste](#17-sjekkliste)

### Hvordan bruke denne guiden

**For utvikling:** Følg seksjonene 1-16 kronologisk når du bygger features.

**For A-karakter:** Fokuser spesielt på:

- Seksjon 3: Feature-first arkitektur
- Seksjon 5: Props/Emits mønster (viser forståelse for dataflyt)
- Seksjon 7: Pinia vs Composables (viser state-management kompetanse)
- Seksjon 14: OWASP sikkerhet
- Seksjon 17: Sjekklisten

**Sensor ser etter:**

- Kan studenten forklare HVORFOR arkitekturen er valgt?
- Er props/emits brukt korrekt (enveis dataflyt)?
- Er sikkerhet tatt på alvor (OWASP)?
- Er koden testbar og testet?

---

## 1. Prioriteringer (fra oppgavetekst)

**Husk:** Dette er prioritert rekkefølge fra lærer:

| #   | Område                   | Hva gir A?                                                                                                     |
| --- | ------------------------ | -------------------------------------------------------------------------------------------------------------- |
| 1   | **Funksjonalitet**       | Full-stack først! Login → Sjekklister → Temperatur → Avvik. **Ufullstendig funksjonalitet blir ikke evaluert** |
| 2   | **Kodekvalitet**         | Cohesion, separation of concerns                                                                               |
| 3   | **Arkitektur**           | Feature-based struktur, lagdeling                                                                              |
| 4   | **Sikkerhet**            | OWASP implementert, JWT, CSRF                                                                                  |
| 5   | **Universell utforming** | WCAG AAA, Lighthouse 90+                                                                                       |
| 6   | **Testing**              | 50%+ dekning (krav),                                                                                           |
| 7   | **Test coverage**        | Mål: 70%+                                                                                                      |
| 8   | **CI/CD**                | Aktiv bruk i utvikling                                                                                         |
| 9   | **Prosjektstruktur**     | Tydelig, dokumentert                                                                                           |
| 10  | **Dokumentasjon**        | README, API-doc (Swagger), systemoversikt                                                                      |
| 11  | **Presentasjon**         | Videoleveranse                                                                                                 |

**Viktig:** "Det er bedre å huke av på alle punktene enn å bare lage masse funksjonalitet."

### Kontekst: Everest Sushi & Fusion

- **Type:** Sushi-restaurant med varmkjøkken og bar
- **Ansatte:** 12 (leder, kjøkkensjef, kokker, bartendere, servitører)
- **HACCP-risk:** HØY (rå fisk serveres direkte)
- **Spesifikt:** Fiskekjøleskap må være ≤ 2°C (strengere enn standard ≤ 4°C)
- **Brukere:** Kokker på nettbrett, bartendere på mobil, leder på laptop

**Roller (fra DEV_NOTES):**

- **ADMIN:** (Daglig leder) - Full tilgang
- **MANAGER:** (Kjøkkensjef) - IK-Mat + rapporter
- **STAFF:** Kokker, bartendere, servitører - Begrenset til sine områder

---

## 2. Tekniske valg

### Feature-based (ikke type-based)

**Hvorfor:** All kode for én feature på ett sted. Lett å finne, endre, slette.

### Composition API (ikke Options API)

**Hvorfor:** Bedre TypeScript, gjenbruk via composables, relatert kode gruppert.

### TypeScript (ikke JS)

**Hvorfor:** Færre bugs, bedre refactoring, viser profesjonalitet.

### Pinia (ikke Vuex)

**Hvorfor:** Offisiell Vue 3-løsning, bedre TypeScript, enklere API.

### Ren CSS (ikke Tailwind)

**Hvorfor:** Oppgavekrav, læringsverdi, ingen runtime overhead.

### BEM naming

**Hvorfor:** `.block__element--modifier` - leselig, ingen spesifisitetskrig.

### JWT i sessionStorage

**Hvorfor:** Tømmes ved lukking, bedre enn localStorage, enklere enn cookies.

### Mobile-first CSS

**Hvorfor:** Kokker bruker mobil (prioritet 1), mindre CSS for flertallet.

### Props/Emits (ikke v-model)

**Hvorfor:** Eksplisitt dataflyt, enklere å debugge, tydelige kontrakter.

---

## 3. Prosjektstruktur - Feature-First Arkitektur

**Hvorfor dette gir A:** Feature-first demonstrerer "separation of concerns" og "high cohesion". Sensor ser at du kan organisere kode etter forretningslogikk, ikke teknisk type.

### Mappestruktur forklart

```
src/
├── features/              # FORRETNINGSFEATURES
│   │                      # En mappe = én forretningsenhet
│   ├── auth/
│   │   ├── components/    # Auth-spesifikke komponenter (LoginForm, RegisterForm)
│   │   ├── composables/   # useAuth.ts (login-logikk)
│   │   ├── views/         # Sider router peker til (LoginView.vue)
│   │   └── api.ts         # Auth API-kall
│   │
│   ├── ik-mat/            #  MATSIKKERHET
│   │   ├── components/    # ChecklistCard, TempLogRow, DeviationDetail
│   │   ├── composables/   # useChecklist.ts, useTemperature.ts
│   │   ├── views/         # ChecklistView, TemperatureView
│   │   └── api.ts         # ikMatApi.getChecklists()
│   │
│   ├── ik-alkohol/        # ALKOHOLSERVERING
│   │   ├── components/    # DailyControlForm, CertificationList
│   │   ├── composables/   # useDailyControl.ts, useCertifications.ts
│   │   ├── views/         # DailyControlView, CertificationsView
│   │   └── api.ts         # ikAlkoholApi.getDailyControl()
│   │
│   ├── felles/            # DELTE FUNKSJONER
│   │   ├── components/    # DashboardStats, ReportList
│   │   ├── composables/   # useDashboard.ts, useReports.ts
│   │   ├── views/         # DashboardView, ReportsView
│   │   └── api.ts         # fellesApi.getDashboard()
│   │
│   └── admin/             # ADMINISTRASJON
│       ├── components/    # UserTable, UserForm
│       ├── composables/   # useUsers.ts
│       ├── views/         # UsersView, SettingsView
│       └── api.ts         # adminApi.getUsers()
│
├── shared/                # DELTE RESSURSER
│   │                      # Kun det som brukes av FLERE features
│   ├── components/        # BaseButton, BaseInput, BaseModal
│   │                      # Generiske, gjenbrukbare komponenter
│   ├── composables/       # useApi.ts, useForm.ts, useErrorHandler.ts
│   │                      # Generisk funksjonalitet
│   └── utils/             # validators.ts, constants.ts
│                          # Hjelpefunksjoner uten Vue-avhengighet
│
├── layouts/               # LAYOUTS
│   ├── AppShell.vue       # Hovedlayout med sidebar + toppbar
│   ├── Sidebar.vue        # Navigasjon (brukerrolle-avhengig)
│   └── AuthLayout.vue     # Layout for login/registrering
│
├── router/                # ROUTING
│   ├── index.ts           # Alle routes
│   └── guards.ts          # Navigation guards (auth, roller)
│
├── stores/                # GLOBAL STATE (kun dette!)
│   └── auth.ts            # Pinia: auth-state
│   └── notifications.ts   # Pinia: global varsling
│
├── api/                   # HTTP-KLIENT
│   └── client.ts          # Axios med interceptors
│
├── types/                 # TYPESCRIPT
│   └── index.ts           # Interfaces for hele appen
│
└── main.ts                # ENTRY POINT
```

### Hvorfor denne strukturen?

| Problem med type-based                           | Feature-first løsning          |
| ------------------------------------------------ | ------------------------------ |
| Auth-logikk spredt i `stores/`, `api/`, `views/` | Alt auth i `features/auth/`    |
| Vanskelig å fjerne en feature                    | Slett én mappe = feature borte |
| Team-konflikter (alle endrer samme filer)        | Hver feature eies av én person |
| Hvor er koden?                                   | Finn feature → alt er der      |

### Regler (kritiske for A)

**1. Ingen kryss-imports mellom features**

```typescript
// ❌ FORBUDT: Ikke importer på tvers av features
import { useChecklist } from '@features/ik-mat/composables/useChecklist'
// Inni ik-alkohol!

// ✅ RIKTIG: Bruk shared eller dupliser
import { useApi } from '@shared/composables/useApi'
// Generelt nok til å deles
```

**2. Ansvarsfordeling**
| Mappe | Ansvar | Ikke tillatt |
|-------|--------|--------------|
| `views/` | Routing, orkestrere komponenter | Business-logikk |
| `components/` | Rene UI-komponenter | API-kall |
| `composables/` | Business-logikk, state | UI-manipulasjon |
| `api.ts` | HTTP-kall | State-håndtering |

**3. Shared er for gjenbruk**

- Base-komponenter: Må brukes av minst 2 features
- Utils: Pure functions uten sideeffekter
- Composables: Generisk nok til flere features

### Importeringsstrategi

```typescript
// 1. Eksterne bibliotek
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

// 2. Shared (generisk funksjonalitet)
import BaseButton from '@shared/components/BaseButton.vue'
import { useForm } from '@shared/composables/useForm'
import { rules } from '@shared/utils/validators'

// 3. Andre features (KUN via stores/types)
import { useAuthStore } from '@/stores/auth'

// 4. Lokal feature
import { ikMatApi } from '../api'
import { useChecklist } from '../composables/useChecklist'
import ChecklistCard from '../components/ChecklistCard.vue'
```

---

## 4. CSS

### Palett (minimalistisk)

```css
todo:;
```

### BEM - Block Element Modifier

**Hvorfor dette gir A:** BEM skaper selvdokumenterende CSS uten spesifisitetsproblemer.

```css
/* Block = Komponenten (uavhengig enhet) */
.checklist-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
}

/* Element = Del av blokken (dobbel understrek) */
/* Kan ikke eksistere uten .checklist-card */
.checklist-card__header {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}

.checklist-card__title {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.checklist-card__body {
  padding: var(--space-3) 0;
}

.checklist-card__footer {
  display: flex;
  gap: var(--space-2);
  padding-top: var(--space-3);
  border-top: 1px solid var(--color-border-light);
}

/* Modifier = Variant/tilstand (dobbel bindestrek) */
.checklist-card--completed {
  opacity: 0.7;
  background: var(--color-surface-alt);
}

.checklist-card--overdue {
  border-color: var(--color-danger-fg);
  background: var(--color-danger-bg);
}

/* Element + Modifier kombinasjon */
.checklist-card__title--urgent {
  color: var(--color-danger-fg);
}
```

### BEM Fordeler (hva sensor ser etter)

| Problem               | BEM Løsning                  |
| --------------------- | ---------------------------- |
| `!important` kriger   | Lav, flat spesifisitet       |
| Navnekollisjoner      | Unike klassenavn med prefiks |
| Uleselig CSS          | Selvdokumenterende struktur  |
| Vanskelig vedlikehold | Tydelig hva som hører sammen |

**Konvensjon:**

- Block: `checklist-card` (flere ord med bindestrek)
- Element: `__header` (dobbel understrek)
- Modifier: `--completed` (dobbel bindestrek)
- ALDRI: `.checklist .header` (for høy spesifisitet)

### Regler

- ALLTID `scoped` på `<style>`
- `rem` for størrelser, `px` KUN for borders
- Mobile-first
- Ingen nesting dypere enn 2 nivåer

### Responsive

```css
/* Mobil først */
.grid {
  grid-template-columns: 1fr;
}

/* Tablet 768px */
@media (min-width: 48rem) {
  .grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* Desktop 1024px */
@media (min-width: 64rem) {
  .grid {
    grid-template-columns: repeat(4, 1fr);
  }
}
```

---

## 5. Komponenter

### Struktur

```vue
<script setup lang="ts">
// 1. Imports
// 2. Props (ALLTID full definisjon)
// 3. Emits (med validering)
// 4. Stores/composables
// 5. State
// 6. Computed
// 7. Lifecycle
// 8. Metoder
</script>
```

### Props - Eksplisitte kontrakter

**Hvorfor dette gir A:** Props dokumenterer komponentens API og skaper kontrakter som fanger feil tidlig.

```typescript
// ✅ RIKTIG - Full definisjon med validering
const props = defineProps({
  // Påkrevd props tvinger parent til å sende data
  title: { type: String, required: true },

  // Default verdier sikrer komponenten alltid fungerer
  status: {
    type: String,
    default: 'pending',
    // Validator sikrer kun gyldige verdier slipper gjennom
    validator: (v) => ['pending', 'completed', 'overdue'].includes(v),
  },

  // Funksjon for Array/Object-defaults (unngår delt state!)
  items: { type: Array, default: () => [] },
})

// ❌ FEIL - Type-only gir ingen validering
const props = defineProps({
  title: String,
  items: { type: Array, default: [] }, // BUG: Delt state mellom instanser!
})
```

**Hva sensor ser etter:**

- ✅ `required` på kritiske props
- ✅ `validator` på enum-verdier (status, type, rolle)
- ✅ Funksjon for Array/Object `default: () => []`
- ✅ Tydelige prop-navn som dokumenterer hensikt

### Emits - Hendelser opp til parent

**Hvorfor dette gir A:** Emits opprettholder enveis dataflyt (parent → child via props, child → parent via emits).

```typescript
// ✅ RIKTIG - Med payload-validering
const emit = defineEmits({
  // Validering sikrer at parent får forventet format
  save: (data) => {
    // Returner true hvis gyldig
    return data !== null && typeof data === 'object'
  },
  delete: (id) => typeof id === 'number' && id > 0,
  close: null, // Ingen payload
})

// Bruk: emitter opp til parent
function handleSave() {
  emit('save', formData) // Parent bestemmer hva som skjer
}

// ❌ FEIL - Direkte mutering av props
function handleSave() {
  props.title = 'Ny tittel' // MUTASJON FORBUDT! Props er read-only
}
```

**Arkitekturprinsipp:**

- Parent eier dataen
- Child ber om endringer via emits
- Aldri muter props direkte
- Dette sikrer "single source of truth"

### Template-regler

```vue
<!-- ✅ OK -->
<button @click="handleSubmit">Send</button>

<!-- ❌ FEIL -->
<button @click="items.push(newItem)">Send</button>

<!-- v-model -->
<input :value="modelValue" @input="$emit('update:modelValue', $event.target.value)" />
```

---

## 6. Composables

### Definisjon

**Composable = En funksjon som bruker Vue's Composition API og kan gjenbrukes på tvers av komponenter.**

Tenk på det som en "hook" som trekker ut logikk fra komponenter:

- Inneholder reactive state (`ref`, `reactive`, `computed`)
- Kan bruke lifecycle hooks (`onMounted`, `watch`)
- Returnerer verdier og funksjoner komponenten kan bruke
- Funksjonsnavn starter med `use`

**Hvorfor dette gir A:** Composables demonstrerer "separation of concerns" - du skiller forretningslogikk fra UI-kode.

### Når skal du bruke Composables?

| Scenario                          | Bruk                  | Ikke bruk       |
| --------------------------------- | --------------------- | --------------- |
| **API-kall for én feature**       | ✅ Composable         | ❌ Pinia Store  |
| **Skjema-håndtering**             | ✅ Composable         | ❌ Store        |
| **Feature-spesifikk state**       | ✅ Composable         | ❌ Store        |
| **Global state (auth)**           | ❌ Composable         | ✅ Pinia Store  |
| **UI-state (modal åpen)**         | ✅ Composable (lokal) | ❌ Global state |
| **Del logikk mellom komponenter** | ✅ Composable         | ❌ Mixins       |

**Huskeregel:**

- **Composable** = "Denne featuren trenger denne logikken"
- **Pinia Store** = "Mange features trenger denne dataen"

### Regler for Composables

1. **Navngivning:** Alltid `useXxx()` (f.eks. `useChecklist`, `useAuth`)
2. **Export:** Navngitt export (`export function`) - aldri default
3. **Returner kun det som trengs:** Ikke eksponer interne hjelpefunksjoner
4. **Feilhåndtering:** Inne i composable - ikke la komponenten håndtere
5. **Én per domene:** `useChecklist`, `useTemperature`, `useDeviations`
6. **Ingen UI-logikk:** Composables skal ikke vite om modals/toast
7. **Ingen sirkulære avhengigheter:** Composable A skal ikke importere B som importerer A

---

## 7. State - Pinia vs Composables

**Kritisk for A:** Å forstå forskjellen mellom global og lokal state viser arkitekturforståelse.

### Hva er Pinia?

**Pinia = Vue's offisielle state management bibliotek for global tilstand.**

Det er erstatteren for Vuex (som er utdatert) og er spesialdesignet for Vue 3 med Composition API.

**Definisjon:**

- Et **sentralisert lager** for data som skal deles på tvers av hele applikasjonen
- Data som **vedvarer** selv når brukeren navigerer mellom sider
- Tilstand som **mange komponenter** trenger tilgang til samtidig

**Anatomi av en Pinia Store:**

```typescript
export const useXxxStore = defineStore('uniktId', () => {
  // 1. State - data som lagres
  const state = ref(initialValue)

  // 2. Getters - avledet state (computed)
  const derivedState = computed(() => state.value.filter(...))

  // 3. Actions - funksjoner som endrer state
  function updateState(newValue) {
    state.value = newValue
  }

  return { state, derivedState, updateState }
})
```

### Pinia vs Composables - Hovedforskjell

| Egenskap       | Pinia (Global State)                | Composables (Lokal State)           |
| -------------- | ----------------------------------- | ----------------------------------- |
| **Scope**      | Hele applikasjonen                  | Én feature/komponent                |
| **Levetid**    | Vedvarer ved navigering             | Opprettes/slettes med komponenten   |
| **Instanser**  | Én singleton per store              | Ny instans hver gang den importeres |
| **Bruk**       | Brukerdata, settings, notifications | Feature-data, skjemaer, lister      |
| **Devtools**   | Ja - tidsreise debugging            | Nei                                 |
| **Hot reload** | Ja                                  | Nei                                 |

**Enkel analogi:**

- **Pinia** = "Postkassen i oppgangen" (alle har tilgang, permanent)
- **Composable** = "Din lommebok" (kun du har tilgang, med deg midlertidig)

### Pinia (GLOBAL state)

**Definisjon:** State som skal være tilgjengelig for **hele applikasjonen** og **vedvare** ved navigering.

**Bruk når:**

- ✅ Data trengs av mange forskjellige komponenter/features
- ✅ Data skal huskes når bruker bytter side
- ✅ Data er "sannheten" for hele appen (f.eks. innlogget bruker)

```typescript
// stores/auth.ts - Global fordi alle trenger å vite om bruker
export const useAuthStore = defineStore('auth', () => {
  const token = ref(sessionStorage.getItem('jwt'))
  const user = ref(null)

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === ROLES.ADMIN)

  async function login(creds) {
    const { token: jwt, user: data } = await authApi.login(creds)
    token.value = jwt
    user.value = data
    sessionStorage.setItem('jwt', jwt)
  }

  function logout() {
    token.value = null
    user.value = null
    sessionStorage.removeItem('jwt')
  }

  // Sjekk roller - sentralisert logikk
  function hasRole(...roles) {
    return roles.includes(user.value?.role)
  }

  return { token, user, isAuthenticated, isAdmin, login, logout, hasRole }
})

// stores/notifications.ts - Global fordi header viser teller
export const useNotificationStore = defineStore('notifications', () => {
  const items = ref([])
  const unreadCount = computed(() => items.value.filter((i) => !i.read).length)

  function markAsRead(id) {
    const item = items.value.find((i) => i.id === id)
    if (item) item.read = true
  }

  return { items, unreadCount, markAsRead }
})
```

### Composables (LOKAL state)

**Bruk når:** State kun trengs i én feature eller komponent.

```typescript
// features/ik-mat/composables/useChecklist.ts
export function useChecklist() {
  // Lokal state - kun denne featuren trenger disse
  const checklists = ref([])
  const isLoading = ref(false)
  const error = ref(null)

  // Computed - avledet state
  const pendingCount = computed(() => checklists.value.filter((c) => c.status === 'PENDING').length)

  // Actions
  async function fetchChecklists() {
    isLoading.value = true
    try {
      checklists.value = await ikMatApi.getChecklists()
    } catch (e) {
      error.value = 'Kunne ikke hente sjekklister'
    } finally {
      isLoading.value = false
    }
  }

  async function completeItem(checklistId, itemId) {
    try {
      await ikMatApi.completeItem(checklistId, itemId)
      // Optimistisk oppdatering
      const item = findItem(checklistId, itemId)
      if (item) item.completed = true
    } catch (e) {
      error.value = 'Kunne ikke fullføre punkt'
    }
  }

  return {
    checklists,
    isLoading,
    error,
    pendingCount, // Computed eksporteres som vanlig ref
    fetchChecklists,
    completeItem,
  }
}
```

### Valg-guide

| Scenario          | Bruk           | Hvorfor                                              |
| ----------------- | -------------- | ---------------------------------------------------- |
| Innlogget bruker  | **Pinia**      | Må være tilgjengelig overalt, vedvare ved navigering |
| Uleste varsler    | **Pinia**      | Header viser teller på tvers av alle sider           |
| Sjekkliste-data   | **Composable** | Kun ChecklistView trenger dette                      |
| Skjema-data       | **Composable** | Lokalt for skjemaet                                  |
| Modal åpen/lukket | **Composable** | Kun parent/child trenger å vite                      |
| Brukerpreferanser | **Pinia**      | Skal huskes ved navigering                           |

**Regel:** "Start med composable, flytt til Pinia kun når du har et share-problem."

---

## 8. Routing

### Routes

```typescript
const routes = [
  {
    path: '/login',
    component: () => import('@features/auth/views/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/ik-mat',
    component: AppShell,
    meta: { requiresAuth: true },
    children: [
      {
        path: 'checklister',
        component: () => import('@features/ik-mat/views/ChecklistView.vue'),
        meta: { roles: ['ADMIN', 'MANAGER'] },
      },
    ],
  },
]
```

### Guards

```typescript
router.beforeEach((to) => {
  const auth = useAuthStore()

  if (!to.meta.public && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.roles && !to.meta.roles.includes(auth.role)) {
    return { name: 'ik-mat' }
  }
})
```

---

## 9. API

### Axios client

```typescript
export const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  timeout: 10000,
})

// Legg til JWT
client.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('jwt')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Håndter 401
client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      sessionStorage.clear()
      window.location.href = '/login'
    }
    return Promise.reject(err)
  },
)
```

### Feature API

```typescript
export const ikMatApi = {
  // Sjekklister (daglige, ukentlige, månedlige)
  getChecklists: () => client.get('/ik-mat/checklists').then((r) => r.data),
  completeItem: (id, itemId) =>
    client.patch(`/ik-mat/checklists/${id}/items/${itemId}/complete`).then((r) => r.data),

  // Temperaturlogging (HACCP CCP-1 til CCP-5)
  // CCP-1: Fiskemottak ≤ 2°C
  // CCP-2: Kjølelagring
  // CCP-5: Sushi-ris nedkjøling ≤ 60°C
  getTemperatureLogs: () => client.get('/ik-mat/temperature-logs').then((r) => r.data),
  createTemperatureLog: (data) => client.post('/ik-mat/temperature-logs', data).then((r) => r.data),

  // Avvik (HACCP - korrigerende tiltak)
  getDeviations: () => client.get('/ik-mat/deviations').then((r) => r.data),
  createDeviation: (data) => client.post('/ik-mat/deviations', data).then((r) => r.data),
}

export const ikAlkoholApi = {
  // Daglig kontroll (Alkoholloven paragraf 1-5)
  getDailyControl: () => client.get('/ik-alkohol/daily-control').then((r) => r.data),

  // Sertifiseringer (kunnskapsprøver)
  getCertifications: () => client.get('/ik-alkohol/certifications').then((r) => r.data),
}
```

---

## 9.1 Regelverk (fra DEV_NOTES)

Systemet skal støtte etterlevelse av:

**IK-Mat:**

- IK-mat-forskriften (FOR-1994-12-15-1187)
- Næringsmiddelhygieneforskriften (FOR-2008-12-22-1623)
- HACCP (Codex Alimentarius)

**IK-Alkohol:**

- Alkoholloven (LOV-1989-06-02-27)
- Skjenkeforskriften
- Paragraf 1-5: Ansvarlig skjenking
- Paragraf 1-7c: Kunnskapsprøve

**UI-konsekvens:**

- Hvert sjekklistepunkt skal vise lovhenvisning (f.eks. "IK-mat §5a")
- Sertifiseringer må spore utløp mot Alkoholloven
- Avvik må kunne knyttes til spesifikke forskrifter

---

## 9.2 HACCP for sushi (Everest-spesifikt)

**HACCP = Hazard Analysis Critical Control Points**

| CCP   | Prosesstrinn   | Fare            | Grenseverdi     | Fra DEV_NOTES                |
| ----- | -------------- | --------------- | --------------- | ---------------------------- |
| CCP-1 | Fiskemottak    | Bakterievekst   | ≤ 2°C           | Lina sjekker mottak          |
| CCP-2 | Kjølelagring   | Oppbevaring     | ≤ 2°C           | Amir sjekker hver morgen     |
| CCP-3 | Tilberedning   | Kontaminering   | Visuell         | Rengjøring mellom rå og kokt |
| CCP-4 | Nedkjøling ris | Bacillus cereus | ≤ 60°C innen 2t | Lina logger                  |
| CCP-5 | Servering      | Temperatur      | ≤ 4°C           | Kontinuerlig                 |

**UI-konsekvens:**

- Temperatur utenfor grenseverdi = rød alert + automatisk avvik
- Kokker må kunne logge på 30 sekunder (våte hender, hastverk)
- Store touch-targets (44px+) essensielt

---

## 10. Skjemaer

### useForm

```typescript
export function useForm(initialValues, rules) {
  const values = reactive({ ...initialValues })
  const errors = reactive({})

  function validate() {
    Object.keys(errors).forEach((k) => delete errors[k])
    let isValid = true

    for (const [field, fieldRules] of Object.entries(rules)) {
      for (const rule of fieldRules) {
        const result = rule(values[field])
        if (result !== true) {
          errors[field] = result
          isValid = false
          break
        }
      }
    }
    return isValid
  }

  return { values, errors, validate }
}
```

### Validators (spesifikt for Everest)

```typescript
export const rules = {
  required:
    (msg = 'Påkrevd') =>
    (v) =>
      !!v || msg,
  minLength: (n) => (v) => v?.length >= n || `Min ${n} tegn`,
  email: () => (v) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v) || 'Ugyldig e-post',

  // Standard temperatur
  temperature: () => (v) => (v >= -30 && v <= 100) || 'Mellom -30 og 100°C',

  // SUSHI-SPESIFIKT: Fiskekjøleskap ≤ 2°C (strengere enn standard ≤ 4°C)
  // Fra DEV_NOTES seksjon 2.1: "HACCP-risiko: HØY - rå fisk serveres direkte"
  fishTemperature: () => (v) => v <= 2 || 'Fiskekjøleskap må være ≤ 2°C',

  // HACCP CCP-5: Sushi-ris nedkjøling
  riceTemperature: () => (v) => v <= 60 || 'Ris må nedkjøles til ≤ 60°C innen 2 timer',
}
```

### Bruk

```vue
<script setup>
const { values, errors, validate } = useForm(
  { email: '', temp: '' },
  {
    email: [rules.required(), rules.email()],
    temp: [rules.required(), rules.temperature()],
  },
)

async function submit() {
  if (!validate()) return
  await api.create(values)
}
</script>
```

---

## 11. Feilhåndtering

### useErrorHandler

```typescript
export function useErrorHandler() {
  function handleError(error, context = '') {
    const status = error.response?.status
    const message = error.response?.data?.message

    const map = {
      401: 'Session utløpt',
      403: 'Ingen tilgang',
      404: 'Ikke funnet',
      500: 'Serverfeil',
    }

    toast.add({ type: 'danger', message: map[status] || message })
    console.error(`[${context}]`, error)
  }

  return { handleError }
}
```

### View-mønster

```vue
<template>
  <BaseSpinner v-if="isLoading" />
  <ErrorMessage v-else-if="error" :message="error" @retry="fetch" />
  <EmptyState v-else-if="items.length === 0" title="Ingen data" />
  <DataList v-else :items="items" />
</template>
```

---

## 12. Roller

### Konstanter (fra DEV_NOTES)

```typescript
export const ROLES = {
  ADMIN: 'ADMIN', // Kari Olsen - Daglig leder
  MANAGER: 'MANAGER', // Amir Patel - Kjøkkensjef
  STAFF: 'STAFF', // Kokker, bartendere, servitører
}

// Tilgangsmatrix (fra DEV_NOTES seksjon 2.3)
// ADMIN:     Full tilgang til alt + admin
// MANAGER:   IK-Mat fullt, rapporter, egne ansatte
// STAFF:     Begrenset til eget område (kokk ser ikke alkohol)
```

### usePermissions

```typescript
export function usePermissions() {
  const auth = useAuthStore()

  const can = {
    viewChecklists: computed(() => auth.isAuthenticated),
    manageUsers: computed(() => auth.role === ROLES.ADMIN),
    exportReports: computed(() => ['ADMIN', 'MANAGER'].includes(auth.role)),
  }

  return { can }
}
```

---

## 13. Testing

### Strategi

| Type      | Dekning | Fokus              |
| --------- | ------- | ------------------ |
| Unit      | 50%+    | Validators, utils  |
| Komponent | 40%+    | Props, emits       |
| E2E       | -       | Login → sjekkliste |

### Eksempel: Komponent

```typescript
import { mount } from '@vue/test-utils'

describe('BaseButton', () => {
  it('emits click', async () => {
    const wrapper = mount(BaseButton)
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
  })
})
```

### Eksempel: Composable

```typescript
describe('useApi', () => {
  it('setter loading', async () => {
    const { isLoading, execute } = useApi(() => Promise.resolve())
    const promise = execute()
    expect(isLoading.value).toBe(true)
    await promise
    expect(isLoading.value).toBe(false)
  })
})
```

### Eksempel: Validator

```typescript
describe('rules.required', () => {
  it('godtar verdi', () => {
    expect(rules.required()('test')).toBe(true)
  })

  it('avviser tom', () => {
    expect(rules.required()('')).toBe('Påkrevd')
  })
})
```

---

## 14. Sikkerhet - OWASP Top 10 for Frontend

**Hvorfor dette gir A:** Å demonstrere bevissthet om sikkerhet viser profesjonalitet. Sensor ser etter at du forstår både frontend OG backend-sikkerhet.

### A01: Broken Access Control

**Problem:** Bruker får tilgang til ressurser de ikke skal ha.

**Løsning:**

```typescript
// router/guards.ts
router.beforeEach((to) => {
  const auth = useAuthStore()

  // Sjekk autentisering
  if (!to.meta.public && !auth.isAuthenticated) {
    return { name: 'login' }
  }

  // Sjekk roller (UI-filtrering + backend-validering!)
  if (to.meta.roles && !auth.hasRole(...to.meta.roles)) {
    return { name: 'dashboard' } // Redirect, ikke bare skjul UI
  }
})

// I komponenter: Dobbelsjekk (defense in depth)
const { can } = usePermissions()
// Bruk både router-guards OG UI-filtrering
```

### A03: Injection (XSS)

**Problem:** Script injiseres via brukerinput.

**Løsning:**

```vue
<template>
  <!-- ✅ Vue 3 escaper automatisk {{ }} -->
  <p>{{ userInput }}</p>

  <!-- ❌ ALDRI gjør dette med brukerinput! -->
  <div v-html="userInput"></div>

  <!-- ✅ Kun hvis du kontrollerer innholdet 100% -->
  <div v-html="sanitizedContent"></div>
</template>

<script>
// Sanitering hvis du MÅ bruke v-html
import DOMPurify from 'dompurify'

const sanitizedContent = DOMPurify.sanitize(richTextFromServer)
</script>
```

### A05: Security Misconfiguration

**Problem:** Feil konfigurasjon eksponerer sårbarheter.

**Løsning:**

```typescript
// Axios med timeout og headers
const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  timeout: 10000, // Ikke uendelig timeout
  headers: {
    'Content-Type': 'application/json',
    'X-Requested-With': 'XMLHttpRequest',
  },
})

// Ikke eksponer sensitive env-variabler
// ❌ console.log(import.meta.env)  // ALDRI!
```

### A07: Authentication

**Problem:** Svak autentisering lar angripere ta over kontoer.

**Løsning:**

```typescript
// stores/auth.ts
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(sessionStorage.getItem('jwt'))

  // Kort session = mindre tid å angripe
  // sessionStorage tømmes når fanen lukkes

  async function login(credentials) {
    // Valider input før sending
    if (!credentials.email || !credentials.password) {
      throw new Error('E-post og passord påkrevd')
    }

    const response = await authApi.login(credentials)
    token.value = response.token
    sessionStorage.setItem('jwt', response.token)
  }

  function logout() {
    token.value = null
    sessionStorage.removeItem('jwt')
    // Tøm ALL sensitiv state
    resetStores()
  }

  // Automatisk logout ved 401
  client.interceptors.response.use(
    (res) => res,
    (err) => {
      if (err.response?.status === 401) {
        logout()
        router.push('/login')
      }
      return Promise.reject(err)
    },
  )

  return { token, login, logout }
})
```

### CSRF-beskyttelse

**Problem:** Angriper får bruker til å utføre uønskede handlinger.

**Løsning:**

```typescript
// Axioss sender automatisk cookies med credentials
client.defaults.withCredentials = true

// Backend setter CSRF-token i cookie
// Frontend leser og sender i header
client.interceptors.request.use((config) => {
  const csrfToken = getCookie('XSRF-TOKEN')
  if (csrfToken) {
    config.headers['X-XSRF-TOKEN'] = csrfToken
  }
  return config
})
```

### Input-validering

**Prinsipp:** Valider på BÅDE frontend OG backend.

```typescript
// Frontend (brukeropplevelse)
const rules = {
  email: () => (v) => /^[^\s@]+@[^\s@]+$/.test(v) || 'Ugyldig e-post',
  password: () => (v) => v.length >= 8 || 'Minst 8 tegn',
}

// Bruk
const { values, errors, validate } = useForm(
  { email: '', password: '' },
  { email: [rules.required(), rules.email()], password: [rules.required(), rules.password()] },
)

// Backend (sikkerhet) - MÅ alltid validere uavhengig!
// @Valid @Email String email
// @Size(min=8) String password
```

### Sjekkliste for A

| Tiltak                     | Hva sensor ser etter                                 |
| -------------------------- | ---------------------------------------------------- |
| ✅ JWT i `sessionStorage`  | Ikke localStorage, med kort levetid                  |
| ✅ Route guards            | Autentisering + autorisering på alle ruter           |
| ✅ Input-validering        | Frontend + backend, ikke blindt stole på brukerinput |
| ✅ XSS-beskyttelse         | Aldri `v-html` med brukerinput, auto-escaping        |
| ✅ CSRF-tokens             | For state-changing requests                          |
| ✅ Ikke logg sensitivt     | ALDRI logg tokens, passord, personinfo               |
| ✅ HTTPS-only              | I produksjon, aldri HTTP                             |
| ✅ Content Security Policy | Headers som begrenser script-kilder                  |

### Sanitering (hvis nødvendig)

```typescript
function sanitizeInput(value: string): string {
  return value
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#x27;')
    .replace(/&/g, '&amp;')
}

// Bruk
const userComment = sanitizeInput(formData.comment)
```

---

## 15. WCAG

### Kritisk

- [ ] 44px touch targets
- [ ] Kontrast 4.5:1 (7:1 for AAA)
- [ ] Alle inputs har `<label>`
- [ ] Fokus synlig: `outline: 2px solid blue`
- [ ] Skip-link: "Hopp til innhold"
- [ ] Ikonknapper har `aria-label`
- [ ] Feilmeldinger med `role="alert"`

### Eksempel

```vue
<label for="email">E-post *</label>
<input
  id="email"
  v-model="email"
  aria-required="true"
  aria-invalid="!!error"
  aria-describedby="email-error"
/>
<span v-if="error" id="email-error" role="alert">{{ error }}</span>
```

---

## 16. Navn

| Type        | Konvensjon        | Eksempel                 |
| ----------- | ----------------- | ------------------------ |
| Views       | PascalCase + View | `ChecklistView.vue`      |
| Komponenter | PascalCase        | `TempLogTable.vue`       |
| Base        | Base-prefix       | `BaseButton.vue`         |
| Composables | use- prefix       | `useChecklist.ts`        |
| CSS         | BEM               | `.checklist__item--done` |
| Konstanter  | UPPER_SNAKE       | `ROLES.ADMIN`            |

---

## 17. Sjekkliste (basert på oppgavetekst)

### ⚠️ KRITISK (fra oppgaven)

- [ ] **Ingen ufullstendig funksjonalitet** (blir ikke evaluert)
- [ ] Full-stack fungerer: Login → Sjekklister → Temperatur → Avvik
- [ ] IK-Alkohol modul: Daglig kontroll, sertifiseringer
- [ ] Roller: ADMIN (Kari), MANAGER (Amir), STAFF fungerer
- [ ] Ren CSS (ingen Tailwind!) - **obligatorisk**
- [ ] 50%+ test coverage - **obligatorisk**
- [ ] OWASP sikkerhet implementert
- [ ] WCAG AAA universell utforming

### Hva sensor ser etter (for A)

**Arkitektur (30% av karakter):**

- ✅ Feature-first struktur med tydelig ansvarsfordeling
- ✅ Props/Emits mønster (ikke mutering av props)
- ✅ Pinia kun for global state, composables for lokal
- ✅ BEM-konsistent CSS uten spesifisitetsproblemer
- ✅ Separation of Concerns: View→Component→Composable→API

**Kodekvalitet (25% av karakter):**

- ✅ Full props-definisjon med validators
- ✅ Emits med payload-validering
- ✅ TypeScript types på alt
- ✅ Ingen logikk i templates
- ✅ DRY prinsippet - gjenbruk via composables

**Sikkerhet (20% av karakter):**

- ✅ JWT i sessionStorage
- ✅ Route guards på alle beskyttede ruter
- ✅ Input-validering (frontend + backend)
- ✅ XSS-beskyttelse (aldri v-html med brukerinput)
- ✅ CSRF-tokens for state-changing requests

**UU & Testing (15% av karakter):**

- ✅ WCAG AAA: 44px touch targets, kontrast 7:1
- ✅ Alle inputs har labels, synlig fokus
- ✅ 50%+ coverage (70%+ for A)
- ✅ E2E-tester for kritiske flyter

**Dokumentasjon (10% av karakter):**

- ✅ README med kjøreinstruksjoner
- ✅ API-dok (Swagger)
- ✅ Denne guiden følges

### Funksjonalitet (Everest-spesifikt)

- [ ] Login/logout med JWT
- [ ] Sjekklister: Daglig, ukentlig, månedlig
- [ ] Temperaturlogging: Fiskekjøleskap ≤ 2°C
- [ ] Avvikshåndtering: Høy/Middels/Lav alvorlighetsgrad
- [ ] HACCP-plan: 5 CCP-er vises
- [ ] IK-Alkohol: Daglig kontroll (Alkoholloven)
- [ ] Sertifiseringer: Kunnskapsprøver med utløpsdatoer
- [ ] Rapporter: PDF/JSON eksport
- [ ] Varsler: Forfalte oppgaver

### Kodekvalitet (for A)

- [ ] Feature-based struktur
- [ ] Ingen logikk i templates/views
- [ ] Full props-definisjon med validators
- [ ] Emits med payload-validering
- [ ] Composables for all logikk
- [ ] Ingen `console.log` i produksjon
- [ ] Ingen hardkodede verdier

### CSS

- [ ] Scoped på alle komponenter
- [ ] BEM-konsistent
- [ ] Mobile-first (kokker bruker mobil!)
- [ ] 44px touch targets
- [ ] Minimalistisk: svart/hvitt/grå
- [ ] Ingen rammeverk (Tailwind, Bootstrap)

### Sikkerhet (OWASP)

- [ ] JWT i sessionStorage (ikke localStorage)
- [ ] Route guards på alle beskyttede ruter
- [ ] Input-validering frontend + backend
- [ ] Ingen `v-html` med brukerinput
- [ ] Ingen logging av sensitive data
- [ ] CSRF-tokens

### WCAG AAA

- [ ] 44px touch targets (kokker har våte hender)
- [ ] Kontrast 4.5:1 (AAA: 7:1)
- [ ] Alle inputs har `<label>` med for/id
- [ ] Ikonknapper har `aria-label`
- [ ] Synlig fokus: `outline: 2px solid`
- [ ] Skip-link til hovedinnhold
- [ ] Feilmeldinger med `role="alert"`
- [ ] `aria-live` for dynamisk innhold

### Testing

- [ ] 50%+ coverage (kjør: `npm run coverage`)
- [ ] 70%+ for A-karakter
- [ ] Unit-tester: validators, utils
- [ ] Komponent-tester: props, emits
- [ ] E2E: Login → sjekkliste → avvik
- [ ] Tilgjengelighetstester (axe-core)

### CI/CD

- [ ] Pipeline kjører ved push
- [ ] Lint + test + build
- [ ] Feiler ved lav coverage
- [ ] Aktiv bruk under utvikling

### Dokumentasjon (obligatorisk)

- [ ] README: Kjøreinstruksjoner
- [ ] API-doc: Swagger/OpenAPI
- [ ] Systemoversikt: Arkitektur, DB-skjema
- [ ] Testdata: Testbrukere, påloggingsinfo
- [ ] Denne guiden følges!

### Kvalitetskriterier (fra oppgaven)

| Område         | C-nivå (forventet)      | A-nivå (imponerende)                      |
| -------------- | ----------------------- | ----------------------------------------- |
| Funksjonalitet | Grunnleggende CRUD      | Fullstendig IK-Mat + IK-Alkohol med HACCP |
| Kodekvalitet   | Fungerende              | SRP, composables, DRY, konsistent         |
| Design         | Grunnleggende responsiv | Minimalistisk, Don Norman-prinsipper      |
| Sikkerhet      | JWT login               | OWASP, CSRF, input-sanitering             |
| UU             | Grunnleggende           | WCAG AAA, Lighthouse 90+                  |
| Testing        | 50% coverage            | 70%+, a11y-tester, E2E                    |
| Dokumentasjon  | README                  | Omfattende manual, API-doc                |

**Husk:** "For å få A eller B kreves det at leveransen er 'Meget godt' eller 'Særdeles godt' (dvs. imponerende)."

---

## Scripts

```json
{
  "dev": "vite",
  "build": "vue-tsc && vite build",
  "test": "vitest",
  "coverage": "vitest run --coverage",
  "test:e2e": "cypress open",
  "lint": "eslint . --fix",
  "format": "prettier --write src/"
}
```
