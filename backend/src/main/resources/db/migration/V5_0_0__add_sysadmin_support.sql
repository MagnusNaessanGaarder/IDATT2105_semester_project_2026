-- V5_0_0: Add sysadmin support
-- Adds is_sysadmin flag to app_user and creates the platform sysadmin account.
-- Password hash is for 'Test1234!'

ALTER TABLE app_user
    ADD COLUMN is_sysadmin TINYINT(1) NOT NULL DEFAULT 0 AFTER is_active;

INSERT INTO app_user (display_name, email, is_active, is_sysadmin, created_at, updated_at)
VALUES ('System Administrator', 'sysadmin@ik-kontroll.no', 1, 1, NOW(), NOW());

INSERT INTO app_user_local_credential (user_id, password_hash, created_at, updated_at)
SELECT user_id,
       '$2a$10$/Z.0lo0byi4feb4kAdcXjOZfC.4mKPQVt.8Xr4LalKlt34X2vHLXm',
       NOW(),
       NOW()
FROM app_user
WHERE email = 'sysadmin@ik-kontroll.no';