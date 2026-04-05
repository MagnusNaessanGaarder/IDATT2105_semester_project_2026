# Database Schema Reference

Table summaries are written by AI (chatGPT) and controlled for accuracy by the DB creator.
Tables are organized by their category, and each subheading is a table name.
Each table has a description and a table with foreign keys.

## Organization / Tenancy

### `organization`

The top-level tenant. Every piece of data in the system belongs to an organization identified by its Norwegian
organization number.

_No foreign keys._

---

### `organization_settings`

Per-organization configuration: timezone, locale, which modules are enabled, email notification settings, and data
retention periods.

| Column       | References                |
|--------------|---------------------------|
| `org_number` | `organization.org_number` |

---

### `location`

A named physical area within an organization (e.g. kitchen, fridge, bar). Carries optional permitted temperature ranges
used for alert evaluation in temperature logging.

| Column       | References                |
|--------------|---------------------------|
| `org_number` | `organization.org_number` |

---

## User / Identity / Membership

### `app_user`

A person who can log in to the system. Not tied to any specific organization - membership is managed separately via
`user_organization`.

_No foreign keys._

---

### `app_user_local_credential`

Stores the hashed password and brute-force lockout state for users authenticating with JWT login.

| Column    | References         |
|-----------|--------------------|
| `user_id` | `app_user.user_id` |

---

### `app_user_refresh_token`

Issued refresh tokens (stored as SHA-256 hashes) used to mint new JWTs without re-authentication. Supports revocation
and expiry.

| Column    | References         |
|-----------|--------------------|
| `user_id` | `app_user.user_id` |

---

### `app_user_identity`

Federated identity records for future third-party providers (e.g. Vipps). Each row links a provider subject ID to an
`app_user`.

| Column    | References         |
|-----------|--------------------|
| `user_id` | `app_user.user_id` |

---

### `user_organization`

Membership join table. Records when a user joined or left an organization and tracks their last activity within it.

| Column       | References                |
|--------------|---------------------------|
| `user_id`    | `app_user.user_id`        |
| `org_number` | `organization.org_number` |

---

## Roles / Permissions

### `role`

Named roles (e.g. admin, manager, staff). System roles are seeded and cannot be deleted.

_No foreign keys._

---

### `permission`

Discrete permission keys (e.g. `checklist:write`, `deviation:close`) that can be grouped into roles.

_No foreign keys._

---

### `role_permission`

Many-to-many join between roles and permissions.

| Column          | References                 |
|-----------------|----------------------------|
| `role_id`       | `role.role_id`             |
| `permission_id` | `permission.permission_id` |

---

### `user_organization_role`

Assigns one or more roles to a user within a specific organization. Tracks who made the assignment.

| Column                  | References                                |
|-------------------------|-------------------------------------------|
| `(user_id, org_number)` | `user_organization.(user_id, org_number)` |
| `role_id`               | `role.role_id`                            |
| `assigned_by_user_id`   | `app_user.user_id`                        |

---

## Document Storage

### `organization_document`

Metadata record for a document belonging to an organization. Tracks document type, title, and current version number.
Actual file bytes live in Azure Blob Storage.

| Column               | References                |
|----------------------|---------------------------|
| `org_number`         | `organization.org_number` |
| `created_by_user_id` | `app_user.user_id`        |

---

### `organization_document_version`

Each uploaded version of a document. Stores the Azure container and blob name, file metadata, and a SHA-256 checksum for
integrity verification.

| Column                | References                          |
|-----------------------|-------------------------------------|
| `document_id`         | `organization_document.document_id` |
| `uploaded_by_user_id` | `app_user.user_id`                  |

---

## Training

### `training_record`

Tracks a training assignment for a user within an organization. Records completion, expiry, status, and optionally links
to a certificate document.

| Column                    | References                                |
|---------------------------|-------------------------------------------|
| `(user_id, org_number)`   | `user_organization.(user_id, org_number)` |
| `certificate_document_id` | `organization_document.document_id`       |

---

## Checklists

### `checklist_template`

A reusable checklist definition scoped to either the food or alcohol compliance module, with a configured frequency (
daily, weekly, etc.).

| Column               | References                |
|----------------------|---------------------------|
| `org_number`         | `organization.org_number` |
| `created_by_user_id` | `app_user.user_id`        |

---

### `checklist_template_item`

An individual question or task within a checklist template. Supports multiple answer types (boolean, text, number,
temperature, choice) with optional validation ranges.

| Column        | References                       |
|---------------|----------------------------------|
| `template_id` | `checklist_template.template_id` |

---

### `checklist_run`

A single execution of a checklist template at a given date and location. Tracks who performed it, who it was assigned
to, and its completion status.

| Column                               | References                                |
|--------------------------------------|-------------------------------------------|
| `template_id`                        | `checklist_template.template_id`          |
| `org_number`                         | `organization.org_number`                 |
| `location_id`                        | `location.location_id`                    |
| `(performed_by_user_id, org_number)` | `user_organization.(user_id, org_number)` |
| `(assigned_to_user_id, org_number)`  | `user_organization.(user_id, org_number)` |

---

### `checklist_run_item`

The recorded answer to a single template item within a checklist run. Flags deviations and supports free-text comments.

| Column             | References                        |
|--------------------|-----------------------------------|
| `run_id`           | `checklist_run.run_id`            |
| `template_item_id` | `checklist_template_item.item_id` |

---

## Temperature Logging

### `temperature_log_point`

A named measurement point (e.g. "Back fridge thermometer") attached to a location. The permitted temperature range is
inherited from the parent location.

| Column        | References                |
|---------------|---------------------------|
| `org_number`  | `organization.org_number` |
| `location_id` | `location.location_id`    |

---

### `temperature_log_entry`

A single temperature reading at a log point. Automatically flagged as an alert if the value falls outside the parent
location's permitted range.

| Column                              | References                                |
|-------------------------------------|-------------------------------------------|
| `org_number`                        | `organization.org_number`                 |
| `log_point_id`                      | `temperature_log_point.log_point_id`      |
| `(recorded_by_user_id, org_number)` | `user_organization.(user_id, org_number)` |

---

## Deviation / Incident Management

### `deviation_report`

Records a compliance incident or discrepancy through its full lifecycle: reporting → investigation → corrective action →
closure. Each workflow stage has a free-text field and a sign-off user.

| Column                                | References                                |
|---------------------------------------|-------------------------------------------|
| `org_number`                          | `organization.org_number`                 |
| `(reported_by_user_id, org_number)`   | `user_organization.(user_id, org_number)` |
| `location_id`                         | `location.location_id`                    |
| `discovered_by_user_id`               | `app_user.user_id`                        |
| `reported_to_user_id`                 | `app_user.user_id`                        |
| `(assigned_to_user_id, org_number)`   | `user_organization.(user_id, org_number)` |
| `immediate_action_signed_by_user_id`  | `app_user.user_id`                        |
| `cause_analysis_signed_by_user_id`    | `app_user.user_id`                        |
| `corrective_action_signed_by_user_id` | `app_user.user_id`                        |
| `completion_signed_by_user_id`        | `app_user.user_id`                        |

---

### `deviation_report_document`

Many-to-many join attaching supporting documents (photos, certificates, etc.) to a deviation report.

| Column        | References                          |
|---------------|-------------------------------------|
| `report_id`   | `deviation_report.report_id`        |
| `document_id` | `organization_document.document_id` |

---

## Notifications

### `notification`

An in-system notification for a user, optionally linked to a specific entity (e.g. an overdue checklist or a temperature
alert).

| Column       | References                |
|--------------|---------------------------|
| `org_number` | `organization.org_number` |
| `user_id`    | `app_user.user_id`        |

---

### `notification_delivery`

Tracks the delivery status of a notification across one or more channels (in-app, email, SMS, push). Supports retry
visibility via `failure_reason`.

| Column            | References                     |
|-------------------|--------------------------------|
| `notification_id` | `notification.notification_id` |

---

## Exports / Reporting

### `export_job`

An asynchronous report generation request. Tracks type, format (PDF/JSON), status, and links to the resulting document
once complete.

| Column                               | References                                |
|--------------------------------------|-------------------------------------------|
| `org_number`                         | `organization.org_number`                 |
| `(requested_by_user_id, org_number)` | `user_organization.(user_id, org_number)` |
| `result_document_id`                 | `organization_document.document_id`       |

---

## Audit Log

### `audit_log`

Append-only log of all write actions in the system. Stores before/after JSON snapshots of changed entities for
traceability and inspection compliance.

| Column             | References                |
|--------------------|---------------------------|
| `org_number`       | `organization.org_number` |
| `acted_by_user_id` | `app_user.user_id`        |
