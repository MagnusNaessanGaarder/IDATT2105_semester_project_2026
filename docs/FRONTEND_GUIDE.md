# Frontend Developer Guide - IDATT2105

**Project:** Internal Control System (IK-Kontroll)  
**Stack:** Vue 3, TypeScript, Pinia, Vue Router, Vite  
**Architecture:** Feature-based with Composition API  
**Last Updated:** 2026-04-02

---

## 1. Architecture Overview

### 1.1 Feature-Based Structure

```
src/
├── features/
│   ├── auth/
│   │   ├── api.ts           # API calls
│   │   ├── composables/     # useAuth.ts
│   │   ├── components/      # LoginForm.vue
│   │   └── views/           # LoginView.vue
│   ├── checklist/
│   ├── deviation/
│   ├── location/
│   └── admin/
├── shared/
│   ├── components/          # BaseButton, BaseModal
│   ├── composables/         # useApi, useForm
│   └── utils/
├── stores/                  # Global Pinia stores
├── router/                  # Vue Router config
├── types/                   # TypeScript interfaces
└── layouts/                 # AppShell, Sidebar
```

**Rule:** A feature can import from `shared/`, but never from another feature directly.

### 1.2 Composition API

Always use `<script setup lang="ts">`:

```vue
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import type { ChecklistTemplate } from '@/types'

// Reactive state
const templates = ref<ChecklistTemplate[]>([])
const isLoading = ref(false)
const error = ref<string | null>(null)

// Computed
const hasTemplates = computed(() => templates.value.length > 0)

// Lifecycle
onMounted(async () => {
  await fetchTemplates()
})

// Methods
async function fetchTemplates() {
  isLoading.value = true
  error.value = null
  try {
    templates.value = await checklistApi.getAll()
  } catch (e) {
    error.value = 'Failed to load templates'
  } finally {
    isLoading.value = false
  }
}
</script>
```

---

## 2. Project Standards

### 2.1 Naming Conventions

**Files:**
- Components: `PascalCase.vue` (e.g., `ChecklistCard.vue`)
- Composables: `camelCase.ts` (e.g., `useAuth.ts`)
- Views: `PascalCaseView.vue` (e.g., `ChecklistListView.vue`)
- Types: `PascalCase` (e.g., `ChecklistTemplate`)

**Variables:**
- camelCase: `checklistTemplate`
- Constants: `API_BASE_URL`
- Boolean: `isLoading`, `hasError`

**CSS Classes (BEM):**
```css
.checklist-card           /* Block */
.checklist-card__header   /* Element */
.checklist-card--active   /* Modifier */
```

### 2.2 Component Structure

```vue
<template>
  <div class="checklist-card">
    <header class="checklist-card__header">
      <h3>{{ template.title }}</h3>
      <BaseButton 
        variant="primary"
        :loading="isLoading"
        @click="handleStart"
      >
        Start
      </BaseButton>
    </header>
  </div>
</template>

<script setup lang="ts">
// 1. Imports
import { ref } from 'vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import type { ChecklistTemplate } from '@/types'

// 2. Props & Emits
const props = defineProps<{
  template: ChecklistTemplate
  isReadOnly?: boolean
}>()

const emit = defineEmits<{
  start: [templateId: number]
}>()

// 3. Reactive state
const isLoading = ref(false)

// 4. Methods
async function handleStart() {
  emit('start', props.template.id)
}
</script>

<style scoped>
.checklist-card {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 16px;
}

.checklist-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
```

---

## 3. State Management

### 3.1 Pinia Stores

**Global state** (used by multiple features):
```ts
// stores/auth.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  // State
  const token = ref<string | null>(null)
  const user = ref<User | null>(null)
  
  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  
  // Actions
  async function login(email: string, password: string) {
    const response = await authApi.login(email, password)
    token.value = response.token
    user.value = response.user
    sessionStorage.setItem('token', response.token)
  }
  
  function logout() {
    token.value = null
    user.value = null
    sessionStorage.removeItem('token')
  }
  
  return { token, user, isLoggedIn, isAdmin, login, logout }
})
```

**Feature state** (use composables):
```ts
// features/checklist/composables/useChecklists.ts
export function useChecklists() {
  const checklists = ref<Checklist[]>([])
  const isLoading = ref(false)
  
  async function fetchChecklists() {
    isLoading.value = true
    checklists.value = await checklistApi.getAll()
    isLoading.value = false
  }
  
  return { checklists, isLoading, fetchChecklists }
}
```

### 3.2 Store Decomposition

**Critical:** Use `storeToRefs` when destructuring:
```ts
// WRONG - Loses reactivity
const { user, isLoggedIn } = useAuthStore()

// CORRECT - Keeps reactivity
const { user, isLoggedIn } = storeToRefs(useAuthStore())
const { login, logout } = useAuthStore()  // Actions can destructure directly
```

---

## 4. API Integration

### 4.1 Axios Setup

```ts
// shared/api/client.ts
import axios from 'axios'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  timeout: 10000
})

// Request interceptor - Add JWT
apiClient.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response interceptor - Handle 401
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      sessionStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default apiClient
```

### 4.2 Feature API Files

```ts
// features/checklist/api.ts
import apiClient from '@/shared/api/client'
import type { ChecklistTemplate, CreateTemplateRequest } from '@/types'

export const checklistApi = {
  async getAll(): Promise<ChecklistTemplate[]> {
    const response = await apiClient.get('/checklists/templates')
    return response.data
  },
  
  async create(data: CreateTemplateRequest): Promise<ChecklistTemplate> {
    const response = await apiClient.post('/checklists/templates', data)
    return response.data
  },
  
  async delete(id: number): Promise<void> {
    await apiClient.delete(`/checklists/templates/${id}`)
  }
}
```

---

## 5. Routing

### 5.1 Route Configuration

```ts
// router/index.ts
const routes = [
  {
    path: '/',
    component: DashboardView,
    meta: { requiresAuth: true }
  },
  {
    path: '/checklists',
    component: ChecklistListView,
    meta: { requiresAuth: true, roles: ['ADMIN', 'MANAGER'] }
  },
  {
    path: '/login',
    component: LoginView,
    meta: { requiresAuth: false }
  }
]
```

### 5.2 Navigation Guards

```ts
// router/guards.ts
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  
  // Check authentication
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return next('/login')
  }
  
  // Check role
  if (to.meta.roles && !to.meta.roles.includes(authStore.userRole)) {
    return next('/forbidden')
  }
  
  next()
})
```

---

## 6. Error Handling

### 6.1 Three-State Pattern

Always implement: loading, success, error

```vue
<template>
  <div>
    <!-- Loading state -->
    <div v-if="isLoading">Loading...</div>
    
    <!-- Error state -->
    <div v-else-if="error" class="error">
      {{ error }}
      <button @click="retry">Retry</button>
    </div>
    
    <!-- Empty state -->
    <div v-else-if="!items.length">No items found</div>
    
    <!-- Success state -->
    <div v-else>
      <ItemCard v-for="item in items" :key="item.id" :item="item" />
    </div>
  </div>
</template>
```

### 6.2 Form Validation

```vue
<template>
  <form @submit.prevent="handleSubmit">
    <div>
      <label for="email">Email</label>
      <input 
        id="email"
        v-model="form.email"
        type="email"
        required
        aria-describedby="email-error"
      />
      <span id="email-error" role="alert" v-if="errors.email">
        {{ errors.email }}
      </span>
    </div>
    
    <button type="submit" :disabled="isSubmitting">
      {{ isSubmitting ? 'Submitting...' : 'Submit' }}
    </button>
  </form>
</template>
```

---

## 7. Testing

### 7.1 Component Tests (Vitest)

```ts
// features/checklist/__tests__/ChecklistCard.spec.ts
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ChecklistCard from '../components/ChecklistCard.vue'

describe('ChecklistCard', () => {
  const mockTemplate = {
    id: 1,
    title: 'Daily Cleaning',
    type: 'CLEANING'
  }
  
  it('renders template title', () => {
    const wrapper = mount(ChecklistCard, {
      props: { template: mockTemplate }
    })
    
    expect(wrapper.text()).toContain('Daily Cleaning')
  })
  
  it('emits start event when button clicked', async () => {
    const wrapper = mount(ChecklistCard, {
      props: { template: mockTemplate }
    })
    
    await wrapper.find('button').trigger('click')
    
    expect(wrapper.emitted('start')).toBeTruthy()
    expect(wrapper.emitted('start')[0]).toEqual([1])
  })
})
```

### 7.2 Store Tests

```ts
// stores/__tests__/auth.spec.ts
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../auth'

describe('Auth Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })
  
  it('sets user after login', async () => {
    const store = useAuthStore()
    
    await store.login('test@example.com', 'password')
    
    expect(store.isLoggedIn).toBe(true)
    expect(store.user).toBeDefined()
  })
})
```

---

## 8. Security

### 8.1 OWASP Compliance

| # | Vulnerability | Protection |
|---|---------------|------------|
| A1 | Injection | Never use `v-html` with user data |
| A2 | Broken Auth | JWT in sessionStorage, auto-logout on 401 |
| A3 | XSS | Vue auto-escapes `{{ }}` templates |
| A4 | Insecure References | Route guards check permissions |
| A5 | Misconfiguration | No sensitive data in client |
| A6 | Data Exposure | No passwords in code |
| A7 | Access Control | Router guards + role checks |
| A8 | CSRF | JWT in sessionStorage (not cookies) |

### 8.2 JWT Handling

```ts
// Always use sessionStorage, never localStorage
token.value = response.data.token
sessionStorage.setItem('token', token.value)

// Interceptor adds token to every request
config.headers.Authorization = `Bearer ${token}`
```

---

## 9. Accessibility (WCAG)

**Required for course:**

```vue
<!-- Semantic HTML -->
<nav>, <main>, <form>, <button> (not <div>)

<!-- Labels -->
<label for="email">Email</label>
<input id="email" type="email" />

<!-- Error messages -->
<span id="email-error" role="alert">Invalid email</span>

<!-- Alt text -->
<img src="logo.png" alt="Company Logo" />

<!-- Keyboard accessible -->
<button @keydown.enter="submit">Submit</button>

<!-- Color contrast -->
<!-- Use accessible color palette -->
```

---

## 10. Checklist for New Features

When adding a new feature (e.g., "Temperature"):

- [ ] Create folder: `features/temperature/`
- [ ] Create `api.ts` for backend calls
- [ ] Create composable in `composables/`
- [ ] Create components in `components/`
- [ ] Create views in `views/`
- [ ] Add routes in `router/index.ts`
- [ ] Add navigation link
- [ ] Create types in `types/index.ts`
- [ ] Write component tests
- [ ] Write composable tests
- [ ] Test manually in browser

---

## 11. Common Mistakes

**Don't:**
- Use `any` type (always use proper types)
- Put business logic in components (use composables)
- Use Options API (use Composition API)
- Skip error states
- Hardcode strings (use constants)
- Use `v-html` with user data (XSS risk)
- Create giant components (>200 lines)

**Do:**
- Use TypeScript strictly
- Implement all three states (loading, success, error)
- Use semantic HTML
- Write tests for critical paths
- Keep components small and focused
- Use feature-based organization

---

## 12. Quick Commands

```bash
# Install dependencies
npm install

# Run dev server
npm run dev

# Run unit tests
npm run test:unit

# Run E2E tests
npm run test:e2e

# Build for production
npm run build

# Type check
npm run type-check

# Lint
npm run lint
```

---

**Remember:** Quality over quantity. A well-tested, accessible feature beats 3 untested ones.
