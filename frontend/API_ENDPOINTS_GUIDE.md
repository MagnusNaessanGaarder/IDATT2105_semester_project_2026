# Frontend API Endpoints Guide

**This document maps backend endpoints to frontend features and issues.**

## Overview

This guide shows how each frontend feature should use the backend API endpoints. All endpoints require authentication (JWT token in Authorization header) and an `orgNumber` query parameter for multi-tenancy.

---

## 1. Authentication (features/auth)

**Base Path:** `/api/v1/auth`

| Endpoint | Method | Description | Frontend Usage |
|----------|--------|-------------|----------------|
| `/api/v1/auth/register` | POST | Register new user | `RegisterView.vue` - Create account |
| `/api/v1/auth/login` | POST | Login with credentials | `LoginView.vue` - Authenticate user |
| `/api/v1/auth/refresh` | POST | Refresh access token | `auth.ts` store - Token renewal |

**Related Issues:** #40 (MVP), #22 (OWASP Security)

---

## 2. Checklists - IK-Mat & IK-Alkohol (features/ik-mat, features/ik-alkohol)

**Base Path:** `/api/v1/checklists`

### Templates (Admin/Manager Configuration)

| Endpoint | Method | Roles | Description | Frontend Usage |
|----------|--------|-------|-------------|----------------|
| `/api/v1/checklists/templates?orgNumber={orgNumber}` | GET | All | List all templates | Template management view |
| `/api/v1/checklists/templates/{id}?orgNumber={orgNumber}` | GET | All | Get template details | Edit template modal |
| `/api/v1/checklists/templates/module/{moduleType}?orgNumber={orgNumber}` | GET | All | Get by module (FOOD/ALCOHOL) | Filter templates by module |
| `/api/v1/checklists/templates/active?orgNumber={orgNumber}` | GET | All | Get active templates | Today's checklist selection |
| `/api/v1/checklists/templates?orgNumber={orgNumber}` | POST | ADMIN/MANAGER | Create template | Add new checklist template |
| `/api/v1/checklists/templates/{id}?orgNumber={orgNumber}` | PUT | ADMIN/MANAGER | Update template | Edit checklist template |
| `/api/v1/checklists/templates/{id}?orgNumber={orgNumber}` | DELETE | ADMIN/MANAGER | Delete template | Remove template |

### Runs (User Performing Checklists)

| Endpoint | Method | Roles | Description | Frontend Usage |
|----------|--------|-------|-------------|----------------|
| `/api/v1/checklists/runs?orgNumber={orgNumber}` | GET | All | List all runs | Checklist dashboard |
| `/api/v1/checklists/runs?orgNumber={orgNumber}&status={status}` | GET | All | List by status | Filter by DRAFT/IN_PROGRESS/COMPLETED |
| `/api/v1/checklists/runs/{id}?orgNumber={orgNumber}` | GET | All | Get run details | Checklist detail view |
| `/api/v1/checklists/runs/{id}/items?orgNumber={orgNumber}` | GET | All | Get run items | Display checklist questions |
| `/api/v1/checklists/runs?orgNumber={orgNumber}` | POST | ADMIN/MANAGER | Create run from template | Start new checklist |
| `/api/v1/checklists/runs/{runId}/items/{itemId}?orgNumber={orgNumber}` | PUT | All | Answer/update item | Save checkbox/text/numeric answers |
| `/api/v1/checklists/runs/{id}/complete?orgNumber={orgNumber}` | PUT | All | Complete run | Finish checklist |

**Related Issues:** #64 (Checklist UI), #17 (IK-Mat checklist), #25 (IK-Alkohol daily control), #98 (Admin edit menu)

**Example Flow:**
1. User opens "Daglige oppgaver" → `GET /api/v1/checklists/runs?status=IN_PROGRESS`
2. User clicks a checklist → `GET /api/v1/checklists/runs/{id}` + `GET /api/v1/checklists/runs/{id}/items`
3. User answers question → `PUT /api/v1/checklists/runs/{runId}/items/{itemId}`
4. User completes checklist → `PUT /api/v1/checklists/runs/{id}/complete`

---

## 3. Temperature Logging (features/ik-mat)

**Base Path:** `/api/v1/temperature`

### Log Points (Configuration)

| Endpoint | Method | Roles | Description | Frontend Usage |
|----------|--------|-------|-------------|----------------|
| `/api/v1/temperature/points?orgNumber={orgNumber}` | GET | All | List all log points | Location selector |
| `/api/v1/temperature/points/active?orgNumber={orgNumber}` | GET | All | List active points | Temperature logging form |
| `/api/v1/temperature/points/{pointId}?orgNumber={orgNumber}` | GET | All | Get specific point | Point details/edit |
| `/api/v1/temperature/points?orgNumber={orgNumber}` | POST | ADMIN/MANAGER | Create log point | Add new temperature location |
| `/api/v1/temperature/points/{pointId}?orgNumber={orgNumber}` | PUT | ADMIN/MANAGER | Update log point | Edit location settings |
| `/api/v1/temperature/points/{pointId}?orgNumber={orgNumber}` | DELETE | ADMIN/MANAGER | Delete log point | Remove location |

### Temperature Entries (Readings)

| Endpoint | Method | Roles | Description | Frontend Usage |
|----------|--------|-------|-------------|----------------|
| `/api/v1/temperature/entries?orgNumber={orgNumber}` | GET | All | List all entries | Temperature history view |
| `/api/v1/temperature/entries/paginated?orgNumber={orgNumber}` | GET | All | Paginated entries | Large history tables |
| `/api/v1/temperature/entries/by-point/{pointId}?orgNumber={orgNumber}` | GET | All | Entries for location | Filtered history |
| `/api/v1/temperature/entries/by-date?orgNumber={orgNumber}&from={date}&to={date}` | GET | All | Entries by date range | Date-filtered reports |
| `/api/v1/temperature/entries/{entryId}?orgNumber={orgNumber}` | GET | All | Get specific entry | Entry details |
| `/api/v1/temperature/entries?orgNumber={orgNumber}` | POST | All | Record temperature | Log temperature reading |
| `/api/v1/temperature/alerts?orgNumber={orgNumber}` | GET | ADMIN/MANAGER | Get out-of-range alerts | Alert notifications |

**Related Issues:** #18 (Temperature logging), #98 (Admin edit menu - temperature items)

**Example Flow:**
1. User opens temperature logging → `GET /api/v1/temperature/points/active`
2. User submits temperature → `POST /api/v1/temperature/entries`
3. If out of range, backend auto-creates deviation
4. User views history → `GET /api/v1/temperature/entries`

---

## 4. Deviation/Avvik Handling (features/felles or features/ik-mat)

**Base Path:** `/api/v1/deviations`

| Endpoint | Method | Roles | Description | Frontend Usage |
|----------|--------|-------|-------------|----------------|
| `/api/v1/deviations?orgNumber={orgNumber}` | GET | All | List all deviations | Deviation list view |
| `/api/v1/deviations/search?orgNumber={orgNumber}&status={status}&severity={severity}&fromDate={date}&toDate={date}` | GET | All | Search with filters | Filtered deviation list |
| `/api/v1/deviations/status/{status}?orgNumber={orgNumber}` | GET | All | Get by status | Filter by OPEN/IN_PROGRESS/CLOSED |
| `/api/v1/deviations/severity/{severity}?orgNumber={orgNumber}` | GET | All | Get by severity | Filter by HIGH/MEDIUM/LOW |
| `/api/v1/deviations/assigned/{assignedToId}?orgNumber={orgNumber}` | GET | All | Get assigned to user | "My deviations" view |
| `/api/v1/deviations/{id}?orgNumber={orgNumber}` | GET | All | Get specific deviation | Deviation detail view |
| `/api/v1/deviations?orgNumber={orgNumber}` | POST | All | Create deviation | Report new deviation |
| `/api/v1/deviations/{id}?orgNumber={orgNumber}` | PUT | All | Update deviation | Edit deviation details |
| `/api/v1/deviations/{id}?orgNumber={orgNumber}` | DELETE | ADMIN | Delete deviation | Admin remove deviation |
| `/api/v1/deviations/{id}/status?orgNumber={orgNumber}` | PUT | ADMIN/MANAGER | Update status | Change deviation status |
| `/api/v1/deviations/{id}/assign?orgNumber={orgNumber}&assignedToUserId={userId}` | POST | ADMIN/MANAGER | Assign to user | Assign responsibility |
| `/api/v1/deviations/{id}/immediate-action?orgNumber={orgNumber}` | POST | All | Add immediate action | Document quick fix |
| `/api/v1/deviations/{id}/cause-analysis?orgNumber={orgNumber}` | POST | All | Add cause analysis | Root cause documentation |
| `/api/v1/deviations/{id}/corrective-action?orgNumber={orgNumber}` | POST | All | Add corrective action | Long-term fix |
| `/api/v1/deviations/{id}/complete?orgNumber={orgNumber}` | POST | All | Complete deviation | Mark as resolved |
| `/api/v1/deviations/{id}/close?orgNumber={orgNumber}` | POST | ADMIN/MANAGER | Close deviation | Final closure |
| `/api/v1/deviations/count/open?orgNumber={orgNumber}` | GET | All | Count open deviations | Dashboard badge |

**Related Issues:** #19 (Deviation workflow), #18 (Auto-create from temperature)

**Example Flow:**
1. User opens deviations → `GET /api/v1/deviations?orgNumber={orgNumber}`
2. Temperature breach auto-creates deviation (backend)
3. User views deviation → `GET /api/v1/deviations/{id}`
4. User adds corrective action → `POST /api/v1/deviations/{id}/corrective-action`
5. Manager closes deviation → `POST /api/v1/deviations/{id}/close`

---

## 5. User Management (features/admin)

**Base Path:** `/api/users`

| Endpoint | Method | Roles | Description | Frontend Usage |
|----------|--------|-------|-------------|----------------|
| `/api/users?orgNumber={orgNumber}` | GET | ADMIN/MANAGER | List all users | User management table |
| `/api/users/{userId}?orgNumber={orgNumber}` | GET | ADMIN/MANAGER/Self | Get user details | User profile/edit |
| `/api/users` | POST | ADMIN | Create user | Add new employee |
| `/api/users/{userId}?orgNumber={orgNumber}` | PUT | ADMIN/Self | Update user | Edit user details |
| `/api/users/{userId}?orgNumber={orgNumber}` | DELETE | ADMIN | Deactivate user | Remove employee (soft delete) |

**Related Issues:** #99 (Manage Users and Settings)

---

## 6. Organization Settings (features/admin)

**Base Path:** `/api/v1/organizations/{orgNumber}/settings`

| Endpoint | Method | Roles | Description | Frontend Usage |
|----------|--------|-------|-------------|----------------|
| `/api/v1/organizations/{orgNumber}/settings` | GET | All | Get settings | Load current settings |
| `/api/v1/organizations/{orgNumber}/settings` | PUT | ADMIN/MANAGER | Update settings | Save configuration changes |

**Related Issues:** #99 (Manage Users and Settings)

---

## 7. Locations (features/admin)

**Base Path:** `/api/v1/locations`

| Endpoint | Method | Roles | Description | Frontend Usage |
|----------|--------|-------|-------------|----------------|
| `/api/v1/locations?orgNumber={orgNumber}` | GET | All | List locations | Location selector |
| `/api/v1/locations/{id}?orgNumber={orgNumber}` | GET | All | Get location | Location details |
| `/api/v1/locations?orgNumber={orgNumber}` | POST | ADMIN/MANAGER | Create location | Add new area/room |
| `/api/v1/locations/{id}?orgNumber={orgNumber}` | PUT | ADMIN/MANAGER | Update location | Edit location |
| `/api/v1/locations/{id}?orgNumber={orgNumber}` | DELETE | ADMIN/MANAGER | Delete location | Remove location |

**Related Issues:** #98 (Admin edit menu - locations)

---

## 8. File/Documents (features/felles)

**Base Path:** `/api/v1/files`

| Endpoint | Method | Roles | Description | Frontend Usage |
|----------|--------|-------|-------------|----------------|
| `/api/v1/files?orgNumber={orgNumber}` | GET | All | List documents | Document list view |
| `/api/v1/files?orgNumber={orgNumber}&category={category}` | GET | All | List by category | Filtered documents |
| `/api/v1/files/upload?orgNumber={orgNumber}` | POST | All | Upload file | Document upload |
| `/api/v1/files/download/{documentId}?orgNumber={orgNumber}` | GET | All | Download file | Document download |

**Related Issues:** #41 (Content for Menu Sections - Documents)

---

## 9. Export/Reports (features/felles)

**Base Path:** `/api/v1/exports`

| Endpoint | Method | Roles | Description | Frontend Usage |
|----------|--------|-------|-------------|----------------|
| `/api/v1/exports?orgNumber={orgNumber}` | POST | All | Create export job | Request PDF/JSON export |
| `/api/v1/exports/{exportJobId}?orgNumber={orgNumber}` | GET | All | Get export status | Poll for completion |
| `/api/v1/exports/{exportJobId}/download?orgNumber={orgNumber}` | GET | All | Get download URL | Download completed export |
| `/api/v1/exports?orgNumber={orgNumber}` | GET | MANAGER/ADMIN | List exports | Export history |

**Related Issues:** #26 (PDF/JSON export)

**Example Flow:**
1. User requests export → `POST /api/v1/exports` with type (PDF/JSON)
2. Frontend polls → `GET /api/v1/exports/{exportJobId}` until status is COMPLETED
3. Download → `GET /api/v1/exports/{exportJobId}/download`

---

## 10. Notifications (features/felles)

**Base Path:** `/api/v1/notifications`

| Endpoint | Method | Roles | Description | Frontend Usage |
|----------|--------|-------|-------------|----------------|
| `/api/v1/notifications` | GET | All | Get user notifications | Notification list |
| `/api/v1/notifications/unread-count` | GET | All | Get unread count | Header badge |
| `/api/v1/notifications/{id}` | GET | All | Get specific notification | Notification detail |
| `/api/v1/notifications/{id}/read` | PUT | All | Mark as read | Read notification |
| `/api/v1/notifications/read-all` | PUT | All | Mark all as read | Clear all notifications |
| `/api/v1/notifications/{id}` | DELETE | All | Delete notification | Remove notification |

**Related Issues:** #41 (Content for Menu Sections - Notifications)

---

## Frontend Implementation Pattern

### 1. Create Feature API File

```typescript
// features/ik-mat/api.ts
import { client } from '@/api/client'

export const checklistApi = {
  // Templates
  getTemplates: (orgNumber: number) => 
    client.get(`/api/v1/checklists/templates?orgNumber=${orgNumber}`),
  
  // Runs
  getRuns: (orgNumber: number, status?: string) => 
    client.get(`/api/v1/checklists/runs?orgNumber=${orgNumber}${status ? `&status=${status}` : ''}`),
  
  getRun: (id: number, orgNumber: number) => 
    client.get(`/api/v1/checklists/runs/${id}?orgNumber=${orgNumber}`),
  
  updateItem: (runId: number, itemId: number, data: any, orgNumber: number) => 
    client.put(`/api/v1/checklists/runs/${runId}/items/${itemId}?orgNumber=${orgNumber}`, data),
  
  completeRun: (id: number, orgNumber: number) => 
    client.put(`/api/v1/checklists/runs/${id}/complete?orgNumber=${orgNumber}`),
}
```

### 2. Create Composable

```typescript
// features/ik-mat/composables/useChecklist.ts
import { ref, computed } from 'vue'
import { checklistApi } from '../api'

export function useChecklist() {
  const runs = ref([])
  const isLoading = ref(false)
  const error = ref(null)

  const pendingRuns = computed(() => 
    runs.value.filter(r => r.status === 'IN_PROGRESS')
  )

  async function fetchRuns(orgNumber: number, status?: string) {
    isLoading.value = true
    try {
      const response = await checklistApi.getRuns(orgNumber, status)
      runs.value = response.data
    } catch (e) {
      error.value = e.message
    } finally {
      isLoading.value = false
    }
  }

  async function completeItem(runId: number, itemId: number, data: any, orgNumber: number) {
    await checklistApi.updateItem(runId, itemId, data, orgNumber)
    // Optimistic update
    const run = runs.value.find(r => r.runId === runId)
    const item = run?.items.find(i => i.runItemId === itemId)
    if (item) {
      Object.assign(item, data)
    }
  }

  return {
    runs,
    isLoading,
    error,
    pendingRuns,
    fetchRuns,
    completeItem,
  }
}
```

### 3. Use in View

```vue
<script setup lang="ts">
import { useChecklist } from '../composables/useChecklist'

const { runs, isLoading, fetchRuns, completeItem } = useChecklist()

onMounted(() => {
  fetchRuns(123456789) // orgNumber from auth store
})
</script>
```

---

## Issue-to-Endpoint Mapping Summary

| Issue | Feature | Endpoints |
|-------|---------|-----------|
| #17 | IK-Mat Checklist | `GET/POST/PUT /api/v1/checklists/*` |
| #18 | Temperature Logging | `GET/POST /api/v1/temperature/*` |
| #19 | Deviation Workflow | `GET/POST/PUT /api/v1/deviations/*` |
| #25 | IK-Alkohol Daily Control | `GET/PUT /api/v1/checklists/runs/*` (module=ALCOHOL) |
| #26 | PDF/JSON Export | `POST/GET /api/v1/exports/*` |
| #41 | Menu Content | All endpoints for data fetching |
| #64 | Checklist UI | `GET/POST/PUT /api/v1/checklists/*` |
| #98 | Admin Edit Menu | `POST/PUT/DELETE /api/v1/checklists/templates/*`, `/api/v1/temperature/points/*` |
| #99 | User/Settings Management | `GET/POST/PUT/DELETE /api/users/*`, `GET/PUT /api/v1/organizations/{orgNumber}/settings` |

---

## Common Request Pattern

All endpoints follow this pattern:

1. **Authentication:** JWT token in `Authorization: Bearer {token}` header
2. **Multi-tenancy:** `orgNumber` query parameter required
3. **Roles:** Backend validates role access (ADMIN, MANAGER, EMPLOYEE)
4. **Error Handling:** Standard HTTP status codes (401, 403, 404, 500)

**Example Request:**
```http
GET /api/v1/checklists/runs?orgNumber=123456789&status=IN_PROGRESS
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## Swagger Documentation

Full API documentation available at:
```
http://localhost:8080/swagger-ui/index.html
```

---

*Last updated: April 2026*
