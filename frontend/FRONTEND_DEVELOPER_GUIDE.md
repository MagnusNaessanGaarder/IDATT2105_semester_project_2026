# Frontend Developer Guide

This guide documents the entire frontend application and is the primary onboarding and maintenance reference for contributors.

It covers:
- architecture and folder ownership
- routing and access control
- state, API, and data patterns
- UI system and styleguide compliance
- testing and quality gates
- feature-level behavior and extension workflows

---

## 1. Frontend Snapshot

- Framework: Vue 3 + TypeScript
- Build: Vite
- Router: Vue Router
- Global state: Pinia
- HTTP client: Axios
- Motion: @vueuse/motion
- Unit tests: Vitest
- E2E tests: Cypress
- Styling: base styles + design tokens + scoped feature styles

Frontend goals:
- predictable feature-first structure
- strong typing and explicit data contracts
- consistent UI language across modules
- accessibility-safe defaults
- easy migration between dummy data and API-backed data

---

## 2. Getting Started

### 2.1 Requirements

- Node: `^20.19.0 || >=22.12.0`
- npm (or compatible package manager)

### 2.2 Install and Run

```bash
npm install
npm run dev
```

### 2.3 Build and Validate

```bash
npm run type-check
npm run build
```

### 2.4 Test

```bash
npm run test:unit
npm run test:e2e
```

### 2.5 Useful Scripts

- `npm run preview` runs production preview on port 5173
- `npm run coverage` runs unit tests with coverage
- `npm run lint` runs oxlint + eslint fixes
- `npm run format` runs Prettier over `src/`

---

## 3. Application Boot and Runtime Flow

### 3.1 Entry Point

`src/main.ts` bootstraps:
- `createPinia()`
- `router`
- `MotionPlugin`
- global style layers:
  - `src/assets/styles/variables.css`
  - `src/assets/styles/base.css`
  - `src/assets/styles/components.css`
  - `src/assets/css/main.css`

### 3.2 Root Component

`src/App.vue` is intentionally thin and renders router output.

### 3.3 Authenticated Shell

`src/layouts/AppShell.vue` provides:
- sidebar and mobile navigation shell
- top-level route outlet for authenticated sections
- shared layout behavior (including responsive concerns)

### 3.4 Sysadmin Shell

`src/layouts/SysadminLayout.vue` is a dedicated shell for sysadmin-only routes.

---

## 4. Source Tree and Ownership

```text
src/
  api/                # Shared API client and low-level HTTP configuration
  assets/             # Global style tokens and base styles
  components/         # App-level reusable components
  data/               # Dummy data sources for feature iteration
  features/           # Feature-first modules (auth, admin, ik-mat, ik-alkohol, etc.)
  layouts/            # Route layout shells
  router/             # Route table and navigation guards
  shared/             # Cross-feature components, composables, utils
  stores/             # Pinia stores (auth is primary)
  types/              # Shared TypeScript contracts
```

Ownership rule:
- Keep domain logic inside `src/features/<feature>`.
- Move only truly cross-feature behavior to `src/shared`.

---

## 5. Routing, Roles, and Access Control

Routing is centralized in `src/router/index.ts`.

### 5.1 Route Groups

- Public:
  - `/login`
  - `/register`
- Sysadmin:
  - `/sysadmin`
- Authenticated app shell:
  - `/` dashboard and domain modules

### 5.2 Guards and Meta Contract

The global `beforeEach` guard handles:
- document title from `meta.title`
- auth recheck on cold start
- sysadmin vs non-sysadmin access separation
- inactive/no-organization routing
- role checks via `meta.allowedRoles`
- module gating via organization settings (`meta.moduleKey`)

Supported role model in routes:
- `ADMIN`
- `MANAGER`
- `EMPLOYEE`

---

## 6. Authentication and Session Model

Primary store: `src/stores/auth.ts`.

### 6.1 Store Responsibilities

- login/register/logout lifecycle
- token and user session persistence in `sessionStorage`
- role helpers (`hasRole`, `isAdmin`, `isSysadmin`)
- token expiry check + refresh flow

### 6.2 Session Keys

Current storage keys include:
- `accessToken`
- `refreshToken`
- `email`
- `role`
- `organizations`
- `orgNumber`
- `selectedOrgNumber`
- `currentOrgNumber`

### 6.3 Auth API

`src/features/auth/api` handles backend auth endpoints.
Keep store interface stable if auth payloads evolve.

---

## 7. API Layer and Error Handling

Shared client: `src/api/client.ts`.

Expected behavior:
- attach auth token to requests
- centralize response/error handling
- keep endpoint paths feature-relative

Recommended practice:
- wrap asynchronous UI actions with shared loading/error patterns
- convert backend errors to user-readable messages at feature boundaries
- avoid repeating raw axios calls directly in views

---

## 8. Data Strategy: Dummy Data and Migration

Dummy data under `src/data` supports fast feature iteration.

Feature composables should:
- cast source data once
- expose typed models
- provide derived values (`computed`) and helper formatters
- hide source details from views

Migration path from dummy to backend:
1. preserve composable public interface
2. swap internal source from JSON to API calls
3. keep view template contracts unchanged
4. add loading and error states without redesigning page structure

---

## 9. Feature Documentation

### 9.1 Auth (`src/features/auth`)

Contains authentication views and auth API integration.
Responsibilities:
- sign-in/register UX
- handoff to auth store
- error display and session transition

### 9.2 Dashboard/Felles (`src/features/dashboard`)

Contains shared operational pages:
- dashboard
- reports
- documents
- notifications
- forbidden/not-found fallback views

Responsibilities:
- cross-domain overview screens
- high-level list/table interactions
- non-module-specific user workflows

### 9.3 IK-Mat (`src/features/ik-mat`)

Contains food-compliance flows:
- checklists
- temperature logging
- deviations
- HACCP

Responsibilities:
- operational compliance interactions
- status/badge-heavy data presentation
- mobile + desktop task flows

### 9.4 IK-Alkohol (`src/features/ik-alkohol`)

Contains alcohol-compliance flows:
- dashboard
- daily control
- certifications
- regulations

Responsibilities:
- module-specific process views
- compliance documentation workflows
- role-aware action paths

### 9.5 Admin (`src/features/admin`)

Contains admin and manager operations:
- users
- locations
- settings
- audit log interactions

Role policy highlights:
- users page: ADMIN only
- settings/locations: ADMIN + MANAGER

### 9.6 Sysadmin (`src/features/sysadmin`)

Contains cross-organization operational control for sysadmin users.

---

## 10. Shared Layer Documentation

### 10.1 Shared Components (`src/shared/components`)

Use for domain-agnostic primitives and reusable flows:
- base modal/input/button/spinner
- shared form sections
- shared error and state widgets

### 10.2 Shared Composables (`src/shared/composables`)

Use for behavior that is reused across features, e.g.:
- API request wrappers
- common form-state logic
- permission checks

### 10.3 Shared Utils (`src/shared/utils`)

Use for pure helpers and mappers:
- organization settings resolution and cache handling
- formatters and constants

---

## 11. UI System and Styleguide Compliance

The frontend must follow both:
- base style tokens and utilities in `src/assets/styles`
- IK brand/styleguide rules from project docs

### 11.1 Base Style Layers

- `variables.css`: tokens (color, spacing, typography, shadow, radius)
- `base.css`: global reset + native element baseline
- `components.css`: shared component-level utility classes

### 11.2 Required Styling Rules

- use token variables, avoid hardcoded hex values in feature styles
- use semantic tokens for state (`--color-danger`, `--color-success-bg`, `--color-focus`)
- preserve contrast-safe foreground/background pairings
- preserve focus-visible outlines on all interactive elements
- keep spacing aligned to 8px rhythm (`--spacing-*` and compatible rem values)

### 11.3 Typography Rules

- headings and UI labels: `var(--font-family-display)` / `var(--font-family-ui)`
- body text and long-form content: `var(--font-family)`
- avoid introducing ad hoc font stacks in feature files

### 11.4 Buttons and Inputs

Prefer base/shared variants:
- primary actions use primary token backgrounds
- danger actions use danger tokens
- ghost/secondary actions use border + neutral surfaces
- focus ring must remain visible and tokenized

---

## 12. Accessibility Baseline

Minimum expectations for all new UI:
- keyboard navigable interactive controls
- visible `:focus-visible` styles
- clear text contrast against surface color
- logical heading order and semantic structure
- touch targets appropriate for mobile interaction

When changing existing views, do not regress accessibility behavior.

---

## 13. Testing Strategy

### 13.1 Unit Tests (Vitest)

Use unit tests for:
- composables
- helper utilities
- data transformations
- edge-case formatting and mapping logic

### 13.2 E2E Tests (Cypress)

Use e2e for:
- auth and navigation flow
- route guard behavior
- high-value user journeys (forms/tables/modals)
- regressions in cross-feature workflows

### 13.3 Recommended Local Verification Before PR

```bash
npm run type-check
npm run lint
npm run test:unit
npm run build
```

Run e2e when your change affects end-to-end behavior.

---

## 14. Contribution Workflow

### 14.1 Adding a New Feature

1. Create `src/features/<new-feature>/`
2. Add `views`, `components`, `composables`, optional `api`
3. Register routes and `meta` rules in router
4. Add navigation entry where relevant
5. Define typed data contracts
6. Use tokenized styling and shared components first
7. Add tests for critical behavior

### 14.2 Extending Existing Features

- preserve public composable signatures where possible
- avoid mixing domain logic into presentational components
- keep role checks explicit in route meta and UI behavior
- document new assumptions and edge cases in this guide

---

## 15. Review Checklist (Frontend)

Before merging, verify:
- no route guard regressions
- no role access leakage
- no hardcoded style drift from tokens
- no inaccessible focus/contrast regressions
- no duplicated business logic already available in shared/composables
- no breaking of existing API/composable contracts

---

## 16. Troubleshooting

### 16.1 Router Redirect Loops

Check:
- `meta.requiresAuth`
- `meta.allowedRoles`
- `isSysadmin` and organization status conditions

### 16.2 Auth Appears Logged Out After Refresh

Check:
- storage keys are written correctly in auth store
- `checkAuth` refresh flow
- backend refresh token validity

### 16.3 Style Inconsistency Between Pages

Check:
- token usage vs hardcoded values
- local scoped style collisions
- whether a shared component already exists for that UI pattern

### 16.4 API Works in Postman but Not in UI

Check:
- base URL/env configuration
- auth header interceptor
- route-level org/module requirements

---

## 17. Definition of Done for Frontend Changes

A frontend change is done when:
- behavior is implemented and role-safe
- UI follows tokenized base styling and styleguide rules
- focus/contrast accessibility is preserved
- affected tests and validation commands pass
- documentation in this guide remains accurate

---

## 18. Living Document Policy

This document is intentionally maintained as a living guide.
When architecture, routing, shared patterns, or style rules change, update this file in the same PR.

---

## 19. Composable Index

Use this section as the canonical index of existing composables before creating new ones.

### 19.1 Shared Composables (`src/shared/composables`)

| Composable | Path | Primary responsibility |
|---|---|---|
| `useApi` | `src/shared/composables/useApi.ts` | Shared async loading/error wrapper pattern |
| `useErrorHandler` | `src/shared/composables/useErrorHandler.ts` | Normalize and present errors consistently |
| `useForm` | `src/shared/composables/useForm.ts` | Common form state and validation flow |
| `usePermissions` | `src/shared/composables/usePermissions.ts` | Permission helper checks across views |

### 19.2 Feature Composables

| Feature | Composable | Path | Primary responsibility |
|---|---|---|---|
| Auth | `useAuth` | `src/features/auth/composables/useAuth.ts` | Auth-specific page helper logic |
| Dashboard | `useFellesData` | `src/features/dashboard/composables/useFellesData.ts` | Dashboard shared data mapping |
| Dashboard | `useDocuments` | `src/features/dashboard/composables/useDocuments.ts` | Documents feature state, filtering, API flow |
| Dashboard | `useNotifications` | `src/features/dashboard/composables/useNotifications.ts` | Notifications state/actions |
| Admin | `useAdminData` | `src/features/admin/composables/useAdminData.ts` | Admin settings and management data composition |
| Admin | `useAuditLog` | `src/features/admin/composables/useAuditLog.ts` | Audit log retrieval/formatting |
| Admin | `useSettingsValidation` | `src/features/admin/composables/useSettingsValidation.ts` | Settings form validation rules |
| Admin | `useUsers` | `src/features/admin/composables/useUsers.ts` | Users data CRUD and UI state |
| IK-Mat | `useIkMatData` | `src/features/ik-mat/composables/useIkMatData.ts` | Primary IK-Mat data orchestration |
| IK-Mat | `useChecklists` | `src/features/ik-mat/composables/useChecklists.ts` | Checklist-specific state/actions |
| IK-Mat | `useChecklistViewState` | `src/features/ik-mat/composables/useChecklistViewState.ts` | Checklist view-mode/UI state |
| IK-Mat | `useIkMatFormatters` | `src/features/ik-mat/composables/useIkMatFormatters.ts` | IK-Mat formatting helpers |
| IK-Alkohol | `useAlkoholData` | `src/features/ik-alkohol/composables/useAlkoholData.ts` | Main IK-Alkohol data mapping |
| IK-Alkohol | `useCertifications` | `src/features/ik-alkohol/composables/useCertifications.ts` | Certification-specific flow/state |
| Export | `useExport` | `src/features/export/composables/useExport.ts` | Export payload and flow orchestration |

### 19.3 Composable Design Rules

Before adding a new composable:
1. Check this index and reuse existing composables where possible.
2. Keep composables focused on state and logic, not template concerns.
3. Keep side effects explicit (`fetch`, `save`, `submit` actions).
4. Expose typed interfaces and computed derivations.
5. Keep feature composables feature-local unless reused cross-feature.

Naming convention:
- use `useXxx` with domain nouns (`useUsers`, `useDocuments`)
- for view-only state, include `ViewState` suffix
- for pure formatting/mapping helpers, include `Formatters` suffix

---

## 20. API Endpoint-to-Feature Matrix

Use this matrix to quickly find where frontend endpoint calls are owned.

### 20.1 Auth Endpoints

| Endpoint | Method | Frontend owner |
|---|---|---|
| `/auth/login` | POST | `src/features/auth/api.ts` |
| `/auth/register` | POST | `src/features/auth/api.ts` |
| `/auth/refresh` | POST | `src/features/auth/api.ts` |
| `/auth/logout` | POST | `src/features/auth/api.ts` |
| `/auth/me` | GET | `src/features/auth/api.ts` |

### 20.2 User and Admin Endpoints

| Endpoint | Method | Frontend owner |
|---|---|---|
| `/users` | GET | `src/features/admin/api/users.ts` |
| `/users/{id}` | GET | `src/features/admin/api/users.ts` |
| `/users` | POST | `src/features/admin/api/users.ts` |
| `/users/{id}` | PUT | `src/features/admin/api/users.ts` |
| `/users/{id}` | DELETE | `src/features/admin/api/users.ts` |
| `/organizations/{orgNumber}/settings` | GET | `src/features/admin/api/settingsApi.ts` |
| `/organizations/{orgNumber}/settings` | PUT | `src/features/admin/api/settingsApi.ts` |
| `/admin/audit-log` | GET | `src/features/admin/api/auditLogApi.ts` |
| `/admin/audit-log/action/{actionType}` | GET | `src/features/admin/api/auditLogApi.ts` |
| `/admin/audit-log/date-range` | GET | `src/features/admin/api/auditLogApi.ts` |
| `/admin/audit-log/entity/{entityType}/{entityId}` | GET | `src/features/admin/api/auditLogApi.ts` |

### 20.3 IK-Mat Endpoints

| Endpoint | Method | Frontend owner |
|---|---|---|
| `/locations/{id}` | DELETE | `src/features/ik-mat/api/ikMatApi.ts`, `src/features/admin/views/LocationsView.vue` |
| `/checklists/templates/{id}` | DELETE | `src/features/ik-mat/api/ikMatApi.ts` |
| `/temperature/points/{pointId}` | DELETE | `src/features/ik-mat/api/ikMatApi.ts` |
| `/temperature/points/{pointId}/entries` | DELETE | `src/features/ik-mat/api/ikMatApi.ts` |
| `/deviations/{id}` | DELETE | `src/features/ik-mat/api/ikMatApi.ts` |
| `/deviations` | POST | `src/features/ik-mat/views/TemperatureView.vue`, `src/features/ik-mat/views/DeviationsView.vue` |
| `/files/upload` | POST | `src/features/ik-mat/api/ikMatApi.ts` |
| `/files/{documentId}` | DELETE | `src/features/ik-mat/api/ikMatApi.ts` |
| `/api/v1/checklists/templates` | GET/POST | `src/features/ik-mat/api/checklists.ts` |
| `/api/v1/checklists/templates/{templateId}` | GET/PUT/DELETE | `src/features/ik-mat/api/checklists.ts` |
| `/api/v1/checklists/runs` | GET/POST | `src/features/ik-mat/api/checklists.ts` |
| `/api/v1/checklists/runs/{runId}/complete` | PUT | `src/features/ik-mat/api/checklists.ts` |
| `/api/v1/checklists/runs/{runId}/items/{itemId}` | PUT | `src/features/ik-mat/api/checklists.ts` |

### 20.4 IK-Alkohol Endpoints

| Endpoint | Method | Frontend owner |
|---|---|---|
| `/training/{id}` | DELETE | `src/features/ik-alkohol/api/certifications.ts` |

### 20.5 Dashboard and Shared Endpoints

| Endpoint | Method | Frontend owner |
|---|---|---|
| `/notifications/{notificationId}/read` | PUT | `src/features/dashboard/composables/useFellesData.ts` |
| `/notifications/read-all` | PUT | `src/features/dashboard/composables/useFellesData.ts` |
| `/notifications/{notificationId}` | DELETE | `src/features/dashboard/composables/useFellesData.ts` |
| `/sysadmin/organizations/{orgNumber}` | DELETE | `src/features/sysadmin/views/SysadminOrgsView.vue` |

### 20.6 Endpoint Documentation Rules

When adding or changing API calls:
1. Update this matrix in the same PR.
2. Prefer endpoint wrappers in feature `api` modules over calling `client` directly in views.
3. Keep endpoint paths relative to shared client base config.
4. For temporary direct calls in views, add a TODO and migrate to an `api` module.

---

## 19. Composable Index

Use this section as the canonical index of existing composables before creating new ones.

### 19.1 Shared Composables (`src/shared/composables`)

| Composable | Path | Primary responsibility |
|---|---|---|
| `useApi` | `src/shared/composables/useApi.ts` | shared async loading/error wrapper pattern |
| `useErrorHandler` | `src/shared/composables/useErrorHandler.ts` | normalize and present errors consistently |
| `useForm` | `src/shared/composables/useForm.ts` | common form state and validation flow |
| `usePermissions` | `src/shared/composables/usePermissions.ts` | permission helper checks across views |

### 19.2 Feature Composables

| Feature | Composable | Path | Primary responsibility |
|---|---|---|---|
| Auth | `useAuth` | `src/features/auth/composables/useAuth.ts` | auth-specific page helper logic |
| Dashboard | `useFellesData` | `src/features/dashboard/composables/useFellesData.ts` | dashboard shared data mapping |
| Dashboard | `useDocuments` | `src/features/dashboard/composables/useDocuments.ts` | documents feature state, filtering, API flow |
| Dashboard | `useNotifications` | `src/features/dashboard/composables/useNotifications.ts` | notifications state/actions |
| Admin | `useAdminData` | `src/features/admin/composables/useAdminData.ts` | admin settings and management data composition |
| Admin | `useAuditLog` | `src/features/admin/composables/useAuditLog.ts` | audit log retrieval/formatting |
| Admin | `useSettingsValidation` | `src/features/admin/composables/useSettingsValidation.ts` | settings form validation rules |
| Admin | `useUsers` | `src/features/admin/composables/useUsers.ts` | users data CRUD and UI state |
| IK-Mat | `useIkMatData` | `src/features/ik-mat/composables/useIkMatData.ts` | primary IK-Mat data orchestration |
| IK-Mat | `useChecklists` | `src/features/ik-mat/composables/useChecklists.ts` | checklist-specific state/actions |
| IK-Mat | `useChecklistViewState` | `src/features/ik-mat/composables/useChecklistViewState.ts` | checklist view-mode/UI state |
| IK-Mat | `useIkMatFormatters` | `src/features/ik-mat/composables/useIkMatFormatters.ts` | IK-Mat formatting helpers |
| IK-Alkohol | `useAlkoholData` | `src/features/ik-alkohol/composables/useAlkoholData.ts` | main IK-Alkohol data mapping |
| IK-Alkohol | `useCertifications` | `src/features/ik-alkohol/composables/useCertifications.ts` | certification-specific flow/state |
| Export | `useExport` | `src/features/export/composables/useExport.ts` | export payload and flow orchestration |

### 19.3 Composable Design Rules

Before adding a new composable:
1. Check this index and reuse existing composables where possible.
2. Keep composables focused on state and logic, not template concerns.
3. Keep side effects explicit (`fetch`, `save`, `submit` actions).
4. Expose typed interfaces and computed derivations.
5. Keep feature composables feature-local unless reused cross-feature.

Naming convention:
- use `useXxx` with domain nouns (`useUsers`, `useDocuments`).
- for view-only state, include `ViewState` suffix.
- for pure formatting/mapping helpers, include `Formatters` suffix.
