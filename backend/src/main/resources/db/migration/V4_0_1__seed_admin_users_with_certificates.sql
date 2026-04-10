-- More test users from admin.json with training certificates

-- Insert users from admin.json
INSERT INTO app_user (display_name, email, phone, is_active, created_at, updated_at) VALUES
('Anna Nielsen', 'anna.nielsen@everest.no', NULL, 1, '2023-01-15 00:00:00', NOW()),
('Per Hansen', 'per.hansen@everest.no', NULL, 1, '2023-02-01 00:00:00', NOW()),
('Maria Bergersen', 'maria.b@everest.no', NULL, 1, '2023-03-10 00:00:00', NOW()),
('Ola Nilsen', 'ola.n@everest.no', NULL, 1, '2023-04-20 00:00:00', NOW()),
('Kari Larsen', 'kari.l@everest.no', NULL, 1, '2023-05-05 00:00:00', NOW()),
('Lars Pettersen', 'lars.p@everest.no', NULL, 1, '2023-06-12 00:00:00', NOW()),
('Karin Olsen', 'karin.o@everest.no', NULL, 0, '2023-07-01 00:00:00', NOW()),
('Rune Solberg', 'rune.s@everest.no', NULL, 1, '2024-01-10 00:00:00', NOW());

-- Insert credentials (Password: Test1234!)
-- Same bcrypt hash as V2_0_0__seed_test_users.sql
INSERT INTO app_user_local_credential (user_id, password_hash, must_change_pw, last_changed_at, failed_attempts, created_at, updated_at)
SELECT user_id,
       '$2a$10$/Z.0lo0byi4feb4kAdcXjOZfC.4mKPQVt.8Xr4LalKlt34X2vHLXm',
       0, NOW(), 0, NOW(), NOW()
FROM app_user
WHERE email IN ('anna.nielsen@everest.no', 'per.hansen@everest.no', 'maria.b@everest.no',
                'ola.n@everest.no', 'kari.l@everest.no', 'lars.p@everest.no',
                'karin.o@everest.no', 'rune.s@everest.no');

-- Insert organization membership (all users belong to Everest Sushi org 937219997)
INSERT INTO user_organization (user_id, org_number, is_active, joined_at)
SELECT user_id, 937219997, is_active, created_at
FROM app_user
WHERE email IN ('anna.nielsen@everest.no', 'per.hansen@everest.no', 'maria.b@everest.no',
                'ola.n@everest.no', 'kari.l@everest.no', 'lars.p@everest.no',
                'karin.o@everest.no', 'rune.s@everest.no');

-- Insert role assignments based on role from admin.json
INSERT INTO user_organization_role (user_id, org_number, role_id, assigned_at, assigned_by_user_id)
SELECT u.user_id, 937219997, r.role_id, NOW(), u.user_id
FROM app_user u
CROSS JOIN role r
WHERE (u.email = 'anna.nielsen@everest.no' AND r.role_name = 'ADMIN')
   OR (u.email = 'per.hansen@everest.no' AND r.role_name = 'MANAGER')
   OR (u.email IN ('maria.b@everest.no', 'ola.n@everest.no', 'kari.l@everest.no',
                   'lars.p@everest.no', 'karin.o@everest.no', 'rune.s@everest.no')
       AND r.role_name = 'EMPLOYEE');

-- Insert training records (certificates) for users with certifications
-- Map certification names to training types:
-- Valid for 5 years from created_date, status COMPLETED for users with certifications_valid=true

-- Anna Nielsen: Kunnskapsprøve alkoholloven + Grunnleggende hygieneopplæring
INSERT INTO training_record (user_id, org_number, training_type, title, completed_at, expires_at, status, notes, created_at)
SELECT u.user_id, 937219997, 'RESPONSIBLE_ALCOHOL_SERVICE', 'Kunnskapsprøve alkoholloven',
       u.created_at, DATE_ADD(u.created_at, INTERVAL 5 YEAR), 'COMPLETED', 'Valid certificate', NOW()
FROM app_user u WHERE u.email = 'anna.nielsen@everest.no';

INSERT INTO training_record (user_id, org_number, training_type, title, completed_at, expires_at, status, notes, created_at)
SELECT u.user_id, 937219997, 'FOOD_HYGIENE', 'Grunnleggende hygieneopplæring',
       u.created_at, DATE_ADD(u.created_at, INTERVAL 5 YEAR), 'COMPLETED', 'Valid certificate', NOW()
FROM app_user u WHERE u.email = 'anna.nielsen@everest.no';

-- Per Hansen: Grunnleggende hygieneopplæring
INSERT INTO training_record (user_id, org_number, training_type, title, completed_at, expires_at, status, notes, created_at)
SELECT u.user_id, 937219997, 'FOOD_HYGIENE', 'Grunnleggende hygieneopplæring',
       u.created_at, DATE_ADD(u.created_at, INTERVAL 5 YEAR), 'COMPLETED', 'Valid certificate', NOW()
FROM app_user u WHERE u.email = 'per.hansen@everest.no';

-- Maria Bergersen: Kunnskapsprøve alkoholloven + Grunnleggende hygieneopplæring
INSERT INTO training_record (user_id, org_number, training_type, title, completed_at, expires_at, status, notes, created_at)
SELECT u.user_id, 937219997, 'RESPONSIBLE_ALCOHOL_SERVICE', 'Kunnskapsprøve alkoholloven',
       u.created_at, DATE_ADD(u.created_at, INTERVAL 5 YEAR), 'COMPLETED', 'Valid certificate', NOW()
FROM app_user u WHERE u.email = 'maria.b@everest.no';

INSERT INTO training_record (user_id, org_number, training_type, title, completed_at, expires_at, status, notes, created_at)
SELECT u.user_id, 937219997, 'FOOD_HYGIENE', 'Grunnleggende hygieneopplæring',
       u.created_at, DATE_ADD(u.created_at, INTERVAL 5 YEAR), 'COMPLETED', 'Valid certificate', NOW()
FROM app_user u WHERE u.email = 'maria.b@everest.no';

-- Ola Nilsen: Kunnskapsprøve alkoholloven
INSERT INTO training_record (user_id, org_number, training_type, title, completed_at, expires_at, status, notes, created_at)
SELECT u.user_id, 937219997, 'RESPONSIBLE_ALCOHOL_SERVICE', 'Kunnskapsprøve alkoholloven',
       u.created_at, DATE_ADD(u.created_at, INTERVAL 5 YEAR), 'COMPLETED', 'Valid certificate', NOW()
FROM app_user u WHERE u.email = 'ola.n@everest.no';

-- Kari Larsen: Grunnleggende hygieneopplæring
INSERT INTO training_record (user_id, org_number, training_type, title, completed_at, expires_at, status, notes, created_at)
SELECT u.user_id, 937219997, 'FOOD_HYGIENE', 'Grunnleggende hygieneopplæring',
       u.created_at, DATE_ADD(u.created_at, INTERVAL 5 YEAR), 'COMPLETED', 'Valid certificate', NOW()
FROM app_user u WHERE u.email = 'kari.l@everest.no';

-- Lars Pettersen: Kunnskapsprøve alkoholloven + Grunnleggende hygieneopplæring
INSERT INTO training_record (user_id, org_number, training_type, title, completed_at, expires_at, status, notes, created_at)
SELECT u.user_id, 937219997, 'RESPONSIBLE_ALCOHOL_SERVICE', 'Kunnskapsprøve alkoholloven',
       u.created_at, DATE_ADD(u.created_at, INTERVAL 5 YEAR), 'COMPLETED', 'Valid certificate', NOW()
FROM app_user u WHERE u.email = 'lars.p@everest.no';

INSERT INTO training_record (user_id, org_number, training_type, title, completed_at, expires_at, status, notes, created_at)
SELECT u.user_id, 937219997, 'FOOD_HYGIENE', 'Grunnleggende hygieneopplæring',
       u.created_at, DATE_ADD(u.created_at, INTERVAL 5 YEAR), 'COMPLETED', 'Valid certificate', NOW()
FROM app_user u WHERE u.email = 'lars.p@everest.no';

