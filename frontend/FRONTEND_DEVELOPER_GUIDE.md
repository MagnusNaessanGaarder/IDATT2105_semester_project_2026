# Frontend Developer Guide

This guide is for developers who are new to the frontend in this repository.
It explains the ecosystem, architecture, feature sections, coding patterns, and how to safely extend the project.

---

## 1. Project Snapshot

- Framework: Vue 3 + TypeScript
- Build tool: Vite
- Routing: Vue Router
- State management: Pinia
- HTTP client: Axios
- Animations: @vueuse/motion (plus motion libraries installed)
- Testing: Vitest (unit), Cypress (e2e)
- Styling: custom CSS with design tokens and component-level scoped styles

Core goals of this frontend:

- Feature-first code organization
- Predictable data flow
- Strong TypeScript support
- Good accessibility baseline
- Fast onboarding for new contributors

---

## 2. Quick Start

## 2.1 Requirements

- Node: `^20.19.0 || >=22.12.0`
- npm (or compatible package manager)

## 2.2 Install

```bash
npm install
```

## 2.3 Run locally

```bash
npm run dev
```

## 2.4 Build and verify

```bash
npm run type-check
npm run build
```

## 2.5 Test

```bash
npm run test:unit
npm run test:e2e
```

---

## 3. Ecosystem and Tooling

## 3.1 Vue 3 + Composition API

The codebase uses script setup and composables for logic extraction.
This means most business logic should live in composables, while views are responsible for page orchestration and rendering.

## 3.2 TypeScript

Type safety is used heavily in:

- route metadata and auth rules
- shared domain models under src/types
- feature-level composables for dummy data and transformation helpers

## 3.3 Vite

Vite is used for fast dev server startup and efficient bundling in production.
The build script runs type-check and build in parallel for fast validation.

## 3.4 Vue Router

Routing is centralized in src/router/index.ts.
Access control is handled with a global beforeEach guard that:

- sets document title from route meta
- verifies auth state
- enforces role-based access
- redirects unauthenticated users to login

## 3.5 Pinia

Pinia is currently used primarily for auth state in src/stores/auth.ts.
Use Pinia for truly global state, not for local page state.

## 3.6 Axios client + interceptors

The shared Axios client in src/api/client.ts:

- reads JWT token from sessionStorage
- attaches Authorization header
- handles 401 globally by clearing session and redirecting to login

---

## 4. Frontend Architecture

The frontend follows a feature-first structure under src/features.
Each feature is a domain module with views, local components, and optional composables/api files.

```text
src/
  features/
    auth/
    felles/
    ik-mat/
    ik-alkohol/
    admin/
  layouts/
  router/
  shared/
  stores/
  api/
  data/
  types/
```

---

## 5. App Entry and Boot Flow

## 5.1 Entry point

`src/main.ts` bootstraps the app and registers:

- Pinia
- Router
- Motion plugin
- Global CSS layers:
  - variables.css
  - base.css
  - components.css

## 5.2 Root component

`src/App.vue` is intentionally minimal and renders RouterView only.

## 5.3 Shell layout

`src/layouts/AppShell.vue` provides shared app chrome:

- sidebar + mobile backdrop
- skip link for accessibility
- animated route transitions in main content

This is where all authenticated pages are rendered via nested routes.

---

## 6. Routing and Access Control

Routes are grouped under:

- Public route: /login
- Protected app shell: /
  - felles routes (dashboard, reports, documents, notifications)
  - ik-mat routes
  - ik-alkohol routes
  - admin routes

Role restrictions are declared in route meta.allowedRoles.

Current role model in the router:

- ADMIN
- MANAGER
- EMPLOYEE

Important: Some dummy JSON files still use STAFF in feature-level data models.
When extending auth and API integration, keep role naming consistent across router, store, backend, and feature data models.

---

## 7. State Management and Session Model

## 7.1 Global auth state

`src/stores/auth.ts` controls:

- current user
- authentication flag
- role helper getters
- login/logout/checkAuth lifecycle

Session persistence uses sessionStorage keys:

- jwt_token
- user

## 7.2 Local page state

Most views use local `ref`/`computed` state for filters, search, tabs, and UI toggles.
Keep local state local unless multiple distant screens need the same data.

---

## 8. Data Sources and Dummy Data Pattern

Dummy data files are under src/data.
Features often use composables that wrap these files and expose:

- typed interfaces
- sorted lists
- label mapping helpers
- formatting helpers

Examples:

- felles: useFellesData
- admin: useAdminData
- ik-mat: useIkMatData
- ik-alkohol: useAlkoholData

### Recommended pattern when adding new dummy-data-backed sections

1. Define interfaces in a feature composable.
2. Cast raw JSON once in the composable.
3. Expose precomputed derivations (counts, sorted lists, labels).
4. Keep views mostly presentational.

---

## 9. How Each Feature Section Works

## 9.1 Auth (src/features/auth)

Responsibility:

- login flow
- auth API calls
- integration with auth store

Typical flow:

- user submits credentials
- auth API returns token + user
- store saves session data
- router guard allows protected routes

## 9.2 Felles (src/features/felles)

Responsibility:

- cross-domain operational overview
- reports/documents/notifications
- general dashboard and shared operational cards

Current architecture:

- views are page-level orchestration
- useFellesData provides typed dummy data and display helpers

## 9.3 IK-Mat (src/features/ik-mat)

Responsibility:

- checklists
- temperature control
- deviations
- HACCP-related views

Data access:

- useIkMatData composable provides reusable typed data and helper functions

## 9.4 IK-Alkohol (src/features/ik-alkohol)

Responsibility:

- daily control
- certifications
- regulations

Data access:

- useAlkoholData composable exposes normalized data and helpers

## 9.5 Admin (src/features/admin)

Responsibility:

- users management views
- settings panels
- audit log visibility

Data access:

- useAdminData composable wraps admin.json and provides role/status/date helpers

---

## 10. Shared Layer

## 10.1 Shared components

`src/shared/components` contains generic, reusable building blocks:

- inputs/buttons/modal/spinner/error components

Use shared components when behavior is domain-agnostic.

## 10.2 Shared composables

Examples:

- useApi: loading/error wrapper for async calls
- useForm: validation and submit flow
- usePermissions, useErrorHandler

Use shared composables for cross-feature patterns.

## 10.3 Shared utils

Common constants and validators live in shared utils.
Keep this layer framework-light where possible.

---

## 11. Styling System

Styling is built with custom CSS tokens and scoped component styles.

Main layers:

- variables.css: design tokens (colors, spacing, typography, shadows)
- base.css: reset, global element styles, focus behavior
- components.css: shared page-level utility classes

### Current style direction

Recent work aligns multiple views with a dense admin/wireframe-inspired design language:

- compact cards
- toolbar + filter controls
- table-first data presentation
- semantic status pills

When adding new pages, follow these established UI patterns for consistency.

---

## 12. Data Flow Guidelines

Preferred flow:

1. source data (API or dummy JSON)
2. composable transforms and computes display-ready models
3. view composes sections and handles user interactions
4. reusable components render small parts

Avoid:

- heavy logic directly in templates
- repeating role/status/date mapping in each view
- mutating raw imported JSON directly

---

## 13. Working With Backend Integration

Current frontend supports dummy data and API-based flows.
When migrating a section from dummy data to backend:

1. Keep existing composable interfaces stable.
2. Replace JSON source with API calls inside composable or feature api.ts.
3. Preserve field mapping helpers so views stay unchanged.
4. Handle loading and error states with useApi.
5. Add optimistic updates only when needed.

This minimizes churn and keeps UI stable during integration.

---

## 14. Adding a New Feature Section

Use this checklist:

1. Create `src/features/<new-feature>/`
2. Add:
   - views/
   - components/
   - composables/
   - api/ (optional)
3. Add route entries in router/index.ts with proper meta.
4. Add navigation entry in layout/sidebar structures.
5. Create or extend dummy JSON in src/data during early iteration.
6. Add typed composable to normalize data and expose helpers.
7. Add unit tests for core logic.
8. Validate with `npm run type-check` and `npm run build`.

---

## 15. Coding and Review Conventions

- Use script setup + TypeScript.
- Prefer computed properties for derived values.
- Keep template conditions simple and expressive.
- Keep component styles scoped unless intentionally global.
- Use explicit emits for component events.
- Keep naming in Norwegian where the existing feature uses Norwegian UI text.

For code reviews, prioritize:

- behavioral regressions
- auth/role leaks
- accessibility and focus behavior
- data consistency between router/store/types/backend

---