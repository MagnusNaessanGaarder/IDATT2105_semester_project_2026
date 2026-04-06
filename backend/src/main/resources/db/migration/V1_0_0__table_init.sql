SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------
-- CLEANUP
-- ---------------------------------------------------------

DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS export_job;
DROP TABLE IF EXISTS notification_delivery;
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS deviation_report_document;
DROP TABLE IF EXISTS deviation_report;
DROP TABLE IF EXISTS temperature_log_entry;
DROP TABLE IF EXISTS temperature_log_point;
DROP TABLE IF EXISTS checklist_run_item;
DROP TABLE IF EXISTS checklist_run;
DROP TABLE IF EXISTS checklist_template_item;
DROP TABLE IF EXISTS checklist_template;
DROP TABLE IF EXISTS training_record;
DROP TABLE IF EXISTS organization_document_version;
DROP TABLE IF EXISTS organization_document;
DROP TABLE IF EXISTS user_organization_role;
DROP TABLE IF EXISTS role_permission;
DROP TABLE IF EXISTS permission;
DROP TABLE IF EXISTS role;
DROP TABLE IF EXISTS user_organization;
DROP TABLE IF EXISTS app_user_refresh_token;
DROP TABLE IF EXISTS app_user_local_credential;
DROP TABLE IF EXISTS app_user_identity;
DROP TABLE IF EXISTS app_user;
DROP TABLE IF EXISTS organization_settings;
DROP TABLE IF EXISTS location;
DROP TABLE IF EXISTS organization;

-- ---------------------------------------------------------
-- ORGANIZATION / TENANCY
-- ---------------------------------------------------------

CREATE TABLE organization
(
    org_number    INT UNSIGNED NOT NULL PRIMARY KEY,
    legal_name    VARCHAR(255) NOT NULL,
    display_name  VARCHAR(255) NULL,
    contact_email VARCHAR(255) NULL,
    contact_phone VARCHAR(50)  NULL,
    is_active     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE organization_settings
(
    org_number             INT UNSIGNED NOT NULL PRIMARY KEY,
    timezone_name          VARCHAR(100) NOT NULL DEFAULT 'Europe/Oslo',
    locale_code            VARCHAR(20)  NOT NULL DEFAULT 'nb-NO',
    enable_food_module     TINYINT(1)   NOT NULL DEFAULT 1,
    enable_alcohol_module  TINYINT(1)   NOT NULL DEFAULT 1,
    default_temp_min_c     DECIMAL(5,2) NULL,
    default_temp_max_c     DECIMAL(5,2) NULL,
    reminder_email_enabled TINYINT(1)   NOT NULL DEFAULT 1,
    notification_email     VARCHAR(255) NULL,
    retention_user_months  INT UNSIGNED NULL,
    retention_audit_months INT UNSIGNED NULL,
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_org_settings_org FOREIGN KEY (org_number) REFERENCES organization (org_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE location
(
    location_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    org_number    INT UNSIGNED    NOT NULL,
    name          VARCHAR(100)    NOT NULL,
    description   VARCHAR(255)    NULL,
    location_type ENUM('KITCHEN', 'BAR', 'FREEZER', 'FRIDGE', 'STORAGE', 'SERVING_AREA', 'HOT_FOOD', 'OTHER') NOT NULL DEFAULT 'OTHER',
    temp_min_c    DECIMAL(5,2)    NULL,
    temp_max_c    DECIMAL(5,2)    NULL,
    is_active     TINYINT(1)      NOT NULL DEFAULT 1,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_location_org      FOREIGN KEY (org_number) REFERENCES organization (org_number),
    CONSTRAINT uq_location_org_name UNIQUE (org_number, name),
    INDEX ix_location_org (org_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------
-- USER / IDENTITY / MEMBERSHIP
-- ---------------------------------------------------------

CREATE TABLE app_user
(
    user_id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    display_name        VARCHAR(255)    NOT NULL,
    email               VARCHAR(255)    NULL,
    phone               VARCHAR(50)     NULL,
    is_active           TINYINT(1)      NOT NULL DEFAULT 1,
    global_last_seen_at DATETIME        NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_app_user_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE app_user_local_credential
(
    credential_id   BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT UNSIGNED  NOT NULL,
    password_hash   VARCHAR(255)     NOT NULL,           -- bcrypt / argon2, format is self-describing
    must_change_pw  TINYINT(1)       NOT NULL DEFAULT 0, -- force reset on first login / admin resets
    last_changed_at DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    failed_attempts TINYINT UNSIGNED NOT NULL DEFAULT 0,
    locked_until    DATETIME         NULL,               -- brute-force lockout
    created_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_local_cred_user FOREIGN KEY (user_id) REFERENCES app_user (user_id),
    CONSTRAINT uq_local_cred_user UNIQUE (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE app_user_refresh_token
(
    token_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT UNSIGNED NOT NULL,
    token_hash CHAR(64)        NOT NULL, -- SHA-256 of the raw token; never store raw
    issued_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME        NOT NULL,
    revoked_at DATETIME        NULL,
    ip_address VARBINARY(16)   NULL,     -- IPv4 or IPv6; consistent with audit_log

    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES app_user (user_id),
    INDEX ix_refresh_token_hash (token_hash),
    INDEX ix_refresh_token_user (user_id, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE app_user_identity
(
    identity_id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT UNSIGNED NOT NULL,
    provider_name    VARCHAR(50)     NOT NULL, -- e.g. 'vipps', 'google'
    provider_user_id VARCHAR(255)    NOT NULL,
    provider_email   VARCHAR(255)    NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_identity_user     FOREIGN KEY (user_id) REFERENCES app_user (user_id),
    CONSTRAINT uq_provider_identity UNIQUE (provider_name, provider_user_id),
    INDEX ix_identity_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_organization
(
    user_id      BIGINT UNSIGNED NOT NULL,
    org_number   INT UNSIGNED    NOT NULL,
    is_active    TINYINT(1)      NOT NULL DEFAULT 1,
    joined_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at      DATETIME        NULL,
    last_seen_at DATETIME        NULL,

    PRIMARY KEY (user_id, org_number),
    CONSTRAINT fk_user_org_user FOREIGN KEY (user_id)    REFERENCES app_user    (user_id),
    CONSTRAINT fk_user_org_org  FOREIGN KEY (org_number) REFERENCES organization (org_number),
    INDEX ix_user_org_org            (org_number),
    INDEX ix_user_org_last_seen      (org_number, last_seen_at),
    INDEX ix_user_org_user_last_seen (user_id, last_seen_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------
-- ROLES / PERMISSIONS
-- ---------------------------------------------------------

CREATE TABLE role
(
    role_id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_name      VARCHAR(50)     NOT NULL,
    description    VARCHAR(255)    NULL,
    is_system_role TINYINT(1)      NOT NULL DEFAULT 0,

    CONSTRAINT uq_role_name UNIQUE (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE permission
(
    permission_id  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    permission_key VARCHAR(100)    NOT NULL,
    description    VARCHAR(255)    NULL,

    CONSTRAINT uq_permission_key UNIQUE (permission_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE role_permission
(
    role_id       BIGINT UNSIGNED NOT NULL,
    permission_id BIGINT UNSIGNED NOT NULL,

    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_perm_role       FOREIGN KEY (role_id)       REFERENCES role       (role_id),
    CONSTRAINT fk_role_perm_permission FOREIGN KEY (permission_id) REFERENCES permission (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_organization_role
(
    user_id             BIGINT UNSIGNED NOT NULL,
    org_number          INT UNSIGNED    NOT NULL,
    role_id             BIGINT UNSIGNED NOT NULL,
    assigned_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by_user_id BIGINT UNSIGNED NULL,

    PRIMARY KEY (user_id, org_number, role_id),
    CONSTRAINT fk_uor_membership  FOREIGN KEY (user_id, org_number) REFERENCES user_organization (user_id, org_number),
    CONSTRAINT fk_uor_role        FOREIGN KEY (role_id)             REFERENCES role              (role_id),
    CONSTRAINT fk_uor_assigned_by FOREIGN KEY (assigned_by_user_id) REFERENCES app_user          (user_id),
    INDEX ix_uor_role (role_id),
    INDEX ix_uor_org  (org_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------
-- DOCUMENT STORAGE
-- ---------------------------------------------------------

CREATE TABLE organization_document
(
    document_id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    org_number         INT UNSIGNED    NOT NULL,
    document_type      ENUM('POLICY', 'PROCEDURE', 'TRAINING_MATERIAL', 'CERTIFICATE', 'ATTACHMENT', 'REPORT_EXPORT', 'OTHER') NOT NULL DEFAULT 'OTHER',
    title              VARCHAR(255)    NOT NULL,
    description        TEXT            NULL,
    current_version    INT UNSIGNED    NOT NULL DEFAULT 1,
    is_active          TINYINT(1)      NOT NULL DEFAULT 1,
    created_by_user_id BIGINT UNSIGNED NULL,
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_org_document_org        FOREIGN KEY (org_number)         REFERENCES organization (org_number),
    CONSTRAINT fk_org_document_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user     (user_id),
    INDEX ix_org_document_org  (org_number),
    INDEX ix_org_document_type (org_number, document_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE organization_document_version
(
    document_version_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    document_id         BIGINT UNSIGNED NOT NULL,
    version_number      INT UNSIGNED    NOT NULL,
    azure_container     VARCHAR(63)     NOT NULL,
    azure_blob_name     VARCHAR(1024)   NOT NULL,
    original_filename   VARCHAR(255)    NOT NULL,
    mime_type           VARCHAR(100)    NOT NULL,
    file_size_bytes     BIGINT UNSIGNED NOT NULL,
    blob_etag           VARCHAR(100)    NULL,
    uploaded_by_user_id BIGINT UNSIGNED NULL,
    uploaded_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_from          DATE            NULL,
    valid_to            DATE            NULL,
    checksum_sha256     CHAR(64)        NULL,

    CONSTRAINT fk_doc_version_document    FOREIGN KEY (document_id)         REFERENCES organization_document (document_id),
    CONSTRAINT fk_doc_version_uploaded_by FOREIGN KEY (uploaded_by_user_id) REFERENCES app_user              (user_id),
    CONSTRAINT uq_doc_version             UNIQUE (document_id, version_number),
    UNIQUE KEY uq_blob_location (azure_container, azure_blob_name(512)),
    INDEX ix_doc_version_document (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ---------------------------------------------------------
-- TRAINING
-- ---------------------------------------------------------

CREATE TABLE training_record
(
    training_record_id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id                 BIGINT UNSIGNED NOT NULL,
    org_number              INT UNSIGNED    NOT NULL,
    training_type           ENUM('FOOD_HYGIENE', 'ALLERGEN_HANDLING', 'TEMPERATURE_CONTROL', 'CLEANING_ROUTINES', 'RESPONSIBLE_ALCOHOL_SERVICE', 'AGE_VERIFICATION', 'OTHER') NOT NULL,
    title                   VARCHAR(255)    NOT NULL,
    completed_at            DATETIME        NULL,
    expires_at              DATETIME        NULL,
    status                  ENUM('ASSIGNED', 'COMPLETED', 'EXPIRED') NOT NULL DEFAULT 'ASSIGNED',
    certificate_document_id BIGINT UNSIGNED NULL,
    notes                   TEXT            NULL,
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_training_membership  FOREIGN KEY (user_id, org_number)     REFERENCES user_organization     (user_id, org_number),
    CONSTRAINT fk_training_certificate FOREIGN KEY (certificate_document_id) REFERENCES organization_document (document_id),
    INDEX ix_training_org     (org_number),
    INDEX ix_training_user    (user_id),
    INDEX ix_training_status  (org_number, status),
    INDEX ix_training_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------
-- CHECKLISTS
-- ---------------------------------------------------------

CREATE TABLE checklist_template
(
    template_id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    org_number         INT UNSIGNED    NOT NULL,
    module_type        ENUM('FOOD', 'ALCOHOL') NOT NULL,
    title              VARCHAR(255)    NOT NULL,
    description        TEXT            NULL,
    frequency          ENUM('DAILY', 'WEEKLY', 'MONTHLY', 'CUSTOM') NOT NULL,
    is_active          TINYINT(1)      NOT NULL DEFAULT 1,
    created_by_user_id BIGINT UNSIGNED NULL,
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_checklist_template_org        FOREIGN KEY (org_number)         REFERENCES organization (org_number),
    CONSTRAINT fk_checklist_template_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user     (user_id),
    INDEX ix_checklist_template_org       (org_number),
    INDEX ix_checklist_template_module    (org_number, module_type),
    INDEX ix_checklist_template_frequency (org_number, frequency)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE checklist_template_item
(
    item_id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    template_id          BIGINT UNSIGNED NOT NULL,
    sort_order           INT             NOT NULL,
    label                VARCHAR(255)    NOT NULL,
    description          VARCHAR(500)    NULL,
    item_type            ENUM('BOOLEAN', 'TEXT', 'NUMBER', 'TEMPERATURE', 'CHOICE') NOT NULL,
    is_required          TINYINT(1)      NOT NULL DEFAULT 1,
    expected_text        VARCHAR(255)    NULL,
    expected_numeric_min DECIMAL(10,2)   NULL,
    expected_numeric_max DECIMAL(10,2)   NULL,
    choice_options_json  JSON            NULL,

    CONSTRAINT fk_checklist_item_template FOREIGN KEY (template_id) REFERENCES checklist_template (template_id),
    CONSTRAINT uq_checklist_item_order    UNIQUE (template_id, sort_order),
    INDEX ix_checklist_item_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE checklist_run
(
    run_id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    template_id          BIGINT UNSIGNED NOT NULL,
    org_number           INT UNSIGNED    NOT NULL,
    location_id          BIGINT UNSIGNED NULL,
    performed_by_user_id BIGINT UNSIGNED NOT NULL,
    assigned_to_user_id  BIGINT UNSIGNED NULL,
    run_date             DATE            NOT NULL,
    due_at               DATETIME        NULL,
    completed_at         DATETIME        NULL,
    status               ENUM('DRAFT', 'IN_PROGRESS', 'COMPLETED', 'OVERDUE', 'CANCELLED') NOT NULL DEFAULT 'DRAFT',
    notes                TEXT            NULL,
    created_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_checklist_run_template     FOREIGN KEY (template_id)                      REFERENCES checklist_template (template_id),
    CONSTRAINT fk_checklist_run_org          FOREIGN KEY (org_number)                       REFERENCES organization       (org_number),
    CONSTRAINT fk_checklist_run_location     FOREIGN KEY (location_id)                      REFERENCES location           (location_id),
    CONSTRAINT fk_checklist_run_performed_by FOREIGN KEY (performed_by_user_id, org_number) REFERENCES user_organization  (user_id, org_number),
    CONSTRAINT fk_checklist_run_assigned_to  FOREIGN KEY (assigned_to_user_id, org_number)  REFERENCES user_organization  (user_id, org_number),
    INDEX ix_checklist_run_org_date (org_number, run_date),
    INDEX ix_checklist_run_status   (org_number, status),
    INDEX ix_checklist_run_due      (due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE checklist_run_item
(
    run_item_id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    run_id           BIGINT UNSIGNED NOT NULL,
    template_item_id BIGINT UNSIGNED NOT NULL,
    boolean_value    TINYINT(1)      NULL,
    text_value       TEXT            NULL,
    numeric_value    DECIMAL(10,2)   NULL,
    selected_choice  VARCHAR(255)    NULL,
    is_deviation     TINYINT(1)      NOT NULL DEFAULT 0,
    comment_text     TEXT            NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_checklist_run_item_run           FOREIGN KEY (run_id)           REFERENCES checklist_run           (run_id),
    CONSTRAINT fk_checklist_run_item_template_item FOREIGN KEY (template_item_id) REFERENCES checklist_template_item (item_id),
    CONSTRAINT uq_run_template_item                UNIQUE (run_id, template_item_id),
    INDEX ix_checklist_run_item_run       (run_id),
    INDEX ix_checklist_run_item_deviation (is_deviation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------
-- TEMPERATURE LOGGING
-- ---------------------------------------------------------

CREATE TABLE temperature_log_point
(
    log_point_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    org_number   INT UNSIGNED    NOT NULL,
    location_id  BIGINT UNSIGNED NOT NULL,
    name         VARCHAR(100)    NOT NULL,
    is_active    TINYINT(1)      NOT NULL DEFAULT 1,
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_temp_point_org      FOREIGN KEY (org_number)  REFERENCES organization (org_number),
    CONSTRAINT fk_temp_point_location FOREIGN KEY (location_id) REFERENCES location     (location_id),
    CONSTRAINT uq_temp_point_org_name UNIQUE (org_number, name),
    INDEX ix_temp_point_org (org_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE temperature_log_entry
(
    entry_id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    org_number          INT UNSIGNED    NOT NULL,
    log_point_id        BIGINT UNSIGNED NOT NULL,
    recorded_by_user_id BIGINT UNSIGNED NOT NULL,
    measured_at         DATETIME        NOT NULL,
    temperature_c       DECIMAL(5,2)    NOT NULL,
    is_alert            TINYINT(1)      NOT NULL DEFAULT 0,
    note_text           TEXT            NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_temp_entry_org         FOREIGN KEY (org_number)                      REFERENCES organization          (org_number),
    CONSTRAINT fk_temp_entry_point       FOREIGN KEY (log_point_id)                    REFERENCES temperature_log_point (log_point_id),
    CONSTRAINT fk_temp_entry_recorded_by FOREIGN KEY (recorded_by_user_id, org_number) REFERENCES user_organization     (user_id, org_number),
    INDEX ix_temp_entry_org_time   (org_number, measured_at),
    INDEX ix_temp_entry_point_time (log_point_id, measured_at),
    INDEX ix_temp_entry_alert      (org_number, is_alert, measured_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------
-- DEVIATION / INCIDENT MANAGEMENT
-- ---------------------------------------------------------

CREATE TABLE deviation_report
(
    report_id                           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    org_number                          INT UNSIGNED    NOT NULL,
    reported_by_user_id                 BIGINT UNSIGNED NOT NULL,
    report_type                         ENUM('INCIDENT', 'DISCREPANCY') NOT NULL,
    severity                            ENUM('MINOR', 'MAJOR', 'CRITICAL') NOT NULL,
    title                               VARCHAR(255)    NOT NULL,
    description                         TEXT            NOT NULL,
    location_id                         BIGINT UNSIGNED NULL,
    location_text                       VARCHAR(100)    NULL,
    occurred_date                       DATE            NULL,
    occurred_time                       TIME            NULL,
    report_date                         DATE            NOT NULL,
    discovered_by_user_id               BIGINT UNSIGNED NULL,
    discovered_by_name                  VARCHAR(255)    NULL,
    reported_to_user_id                 BIGINT UNSIGNED NULL,
    reported_to_name                    VARCHAR(255)    NULL,
    assigned_to_user_id                 BIGINT UNSIGNED NULL,
    immediate_action_text               TEXT            NULL,
    immediate_action_signed_by_user_id  BIGINT UNSIGNED NULL,
    cause_analysis_text                 TEXT            NULL,
    cause_analysis_signed_by_user_id    BIGINT UNSIGNED NULL,
    corrective_action_text              TEXT            NULL,
    corrective_action_signed_by_user_id BIGINT UNSIGNED NULL,
    completion_text                     TEXT            NULL,
    completion_signed_by_user_id        BIGINT UNSIGNED NULL,
    status                              ENUM('DRAFT', 'REPORTED', 'UNDER_INVESTIGATION', 'CORRECTIVE_ACTION_PLANNED', 'CORRECTIVE_ACTION_COMPLETED', 'CLOSED') NOT NULL DEFAULT 'REPORTED',
    created_at                          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    closed_at                           DATETIME        NULL,

    CONSTRAINT fk_deviation_org                    FOREIGN KEY (org_number)                          REFERENCES organization      (org_number),
    CONSTRAINT fk_deviation_reported_by_membership FOREIGN KEY (reported_by_user_id, org_number)     REFERENCES user_organization (user_id, org_number),
    CONSTRAINT fk_deviation_location               FOREIGN KEY (location_id)                         REFERENCES location          (location_id),
    CONSTRAINT fk_deviation_discovered_by          FOREIGN KEY (discovered_by_user_id)               REFERENCES app_user          (user_id),
    CONSTRAINT fk_deviation_reported_to            FOREIGN KEY (reported_to_user_id)                 REFERENCES app_user          (user_id),
    CONSTRAINT fk_deviation_assigned_to_membership FOREIGN KEY (assigned_to_user_id, org_number)     REFERENCES user_organization (user_id, org_number),
    CONSTRAINT fk_deviation_immediate_sign         FOREIGN KEY (immediate_action_signed_by_user_id)  REFERENCES app_user          (user_id),
    CONSTRAINT fk_deviation_cause_sign             FOREIGN KEY (cause_analysis_signed_by_user_id)    REFERENCES app_user          (user_id),
    CONSTRAINT fk_deviation_corrective_sign        FOREIGN KEY (corrective_action_signed_by_user_id) REFERENCES app_user          (user_id),
    CONSTRAINT fk_deviation_completion_sign        FOREIGN KEY (completion_signed_by_user_id)        REFERENCES app_user          (user_id),
    INDEX ix_deviation_org         (org_number),
    INDEX ix_deviation_status      (org_number, status),
    INDEX ix_deviation_severity    (org_number, severity),
    INDEX ix_deviation_report_date (org_number, report_date),
    INDEX ix_deviation_assigned_to (assigned_to_user_id),
    INDEX ix_deviation_location    (location_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE deviation_report_document
(
    report_id   BIGINT UNSIGNED NOT NULL,
    document_id BIGINT UNSIGNED NOT NULL,

    PRIMARY KEY (report_id, document_id),
    CONSTRAINT fk_deviation_doc_report   FOREIGN KEY (report_id)   REFERENCES deviation_report      (report_id),
    CONSTRAINT fk_deviation_doc_document FOREIGN KEY (document_id) REFERENCES organization_document (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------
-- NOTIFICATIONS
-- ---------------------------------------------------------

CREATE TABLE notification
(
    notification_id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    org_number          INT UNSIGNED    NULL,
    user_id             BIGINT UNSIGNED NOT NULL,
    notification_type   ENUM('TASK_OVERDUE', 'TEMPERATURE_ALERT', 'DEVIATION_ASSIGNED', 'DEVIATION_STATUS_CHANGED', 'TRAINING_EXPIRING', 'DOCUMENT_UPLOADED', 'GENERAL') NOT NULL,
    title               VARCHAR(255)    NOT NULL,
    body_text           TEXT            NOT NULL,
    related_entity_type ENUM('CHECKLIST_RUN', 'TEMPERATURE_LOG_ENTRY', 'DEVIATION_REPORT', 'TRAINING_RECORD', 'ORGANIZATION_DOCUMENT', 'EXPORT_JOB', 'OTHER') NULL,
    related_entity_id   BIGINT UNSIGNED NULL,
    is_read             TINYINT(1)      NOT NULL DEFAULT 0,
    read_at             DATETIME        NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notification_org  FOREIGN KEY (org_number) REFERENCES organization (org_number),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id)    REFERENCES app_user     (user_id),
    INDEX ix_notification_user_read (user_id, is_read, created_at),
    INDEX ix_notification_org       (org_number, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notification_delivery
(
    delivery_id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    notification_id  BIGINT UNSIGNED NOT NULL,
    delivery_channel ENUM('IN_APP', 'EMAIL', 'SMS', 'PUSH') NOT NULL,
    delivery_status  ENUM('PENDING', 'SENT', 'FAILED')       NOT NULL DEFAULT 'PENDING',
    attempted_at     DATETIME        NULL,
    delivered_at     DATETIME        NULL,
    failure_reason   TEXT            NULL,

    CONSTRAINT fk_notification_delivery_notification FOREIGN KEY (notification_id) REFERENCES notification (notification_id),
    INDEX ix_notification_delivery_notification (notification_id),
    INDEX ix_notification_delivery_status       (delivery_status, attempted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------
-- EXPORTS / REPORTING
-- ---------------------------------------------------------

CREATE TABLE export_job
(
    export_job_id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    org_number           INT UNSIGNED    NOT NULL,
    requested_by_user_id BIGINT UNSIGNED NOT NULL,
    export_type          ENUM('AUDIT_REPORT', 'CHECKLIST_REPORT', 'TEMPERATURE_REPORT', 'DEVIATION_REPORT', 'TRAINING_REPORT', 'FULL_COMPLIANCE_REPORT') NOT NULL,
    format               ENUM('PDF', 'JSON') NOT NULL,
    status               ENUM('PENDING', 'RUNNING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    parameters_json      JSON            NULL,
    result_document_id   BIGINT UNSIGNED NULL,
    requested_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at         DATETIME        NULL,
    failure_reason       TEXT            NULL,

    CONSTRAINT fk_export_job_org             FOREIGN KEY (org_number)                       REFERENCES organization          (org_number),
    CONSTRAINT fk_export_job_requested_by    FOREIGN KEY (requested_by_user_id, org_number) REFERENCES user_organization     (user_id, org_number),
    CONSTRAINT fk_export_job_result_document FOREIGN KEY (result_document_id)               REFERENCES organization_document (document_id),
    INDEX ix_export_job_org_status (org_number, status, requested_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------
-- AUDIT LOG
-- ---------------------------------------------------------

CREATE TABLE audit_log
(
    audit_log_id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    org_number       INT UNSIGNED    NULL,
    acted_by_user_id BIGINT UNSIGNED NULL,
    action_type      VARCHAR(100)    NOT NULL,
    entity_type      VARCHAR(100)    NOT NULL,
    entity_id        BIGINT UNSIGNED NULL,
    old_values_json  JSON            NULL,
    new_values_json  JSON            NULL,
    user_agent       VARCHAR(500)    NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_log_org  FOREIGN KEY (org_number)       REFERENCES organization (org_number),
    CONSTRAINT fk_audit_log_user FOREIGN KEY (acted_by_user_id) REFERENCES app_user     (user_id),
    INDEX ix_audit_log_org_time (org_number, created_at),
    INDEX ix_audit_log_entity   (entity_type, entity_id),
    INDEX ix_audit_log_action   (action_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
