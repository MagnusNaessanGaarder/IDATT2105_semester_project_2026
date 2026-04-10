-- Sample notifications for testing - includes all types, both read and unread

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


INSERT INTO notification
(org_number, user_id, notification_type, title, body_text,
 related_entity_type, related_entity_id, is_read, read_at, created_at)
SELECT 937219997,
       u.user_id,
       'TEMPERATURE_ALERT',
       'Temperaturavvik – Kjøleskap 1',
       'Kjøleskap 1 (fersk fisk) registrerte 7,2 °C kl. 06:45 – over øvre grense på 4 °C. Kontroller umiddelbart.',
       'TEMPERATURE_LOG_ENTRY',
       1,
       0,
       NULL,
       NOW() - INTERVAL 25 MINUTE
FROM app_user u
WHERE u.email = 'admin@everest-sushi.no';

INSERT INTO notification
(org_number, user_id, notification_type, title, body_text,
 related_entity_type, related_entity_id, is_read, read_at, created_at)
SELECT 937219997,
       u.user_id,
       'DEVIATION_ASSIGNED',
       'Avvik tildelt deg – Emballasje',
       'Avviket «Dårlig pakking av varer ved mottak» er nå tildelt deg for oppfølging.',
       'DEVIATION_REPORT',
       1,
       0,
       NULL,
       NOW() - INTERVAL 2 HOUR
FROM app_user u
WHERE u.email = 'admin@everest-sushi.no';

INSERT INTO notification
(org_number, user_id, notification_type, title, body_text,
 related_entity_type, related_entity_id, is_read, read_at, created_at)
SELECT 937219997,
       u.user_id,
       'DOCUMENT_UPLOADED',
       'Nytt dokument tilgjengelig',
       'HACCP-plan 2024 (v2.1) er lastet opp og klar for gjennomgang.',
       'ORGANIZATION_DOCUMENT',
       1,
       1,
       NOW() - INTERVAL 1 HOUR,
       NOW() - INTERVAL 3 HOUR
FROM app_user u
WHERE u.email = 'admin@everest-sushi.no';

INSERT INTO notification
(org_number, user_id, notification_type, title, body_text,
 related_entity_type, related_entity_id, is_read, read_at, created_at)
SELECT 937219997,
       u.user_id,
       'TASK_OVERDUE',
       'Forfalt sjekkliste – Daglig renholdskontroll',
       'Sjekklisten «Daglig renholdskontroll» for i går er ikke fullført. Registrer status snarest.',
       'CHECKLIST_RUN',
       1,
       0,
       NULL,
       NOW() - INTERVAL 6 HOUR
FROM app_user u
WHERE u.email = 'admin@everest-sushi.no';


INSERT INTO notification
(org_number, user_id, notification_type, title, body_text,
 related_entity_type, related_entity_id, is_read, read_at, created_at)
SELECT 937219997,
       u.user_id,
       'TRAINING_EXPIRING',
       'Kunnskapsprøve utløper om 30 dager',
       'Sertifikatet for «Ansvarlig alkoholutskjenking» utløper 08.05.2026. Forny i god tid.',
       'TRAINING_RECORD',
       1,
       0,
       NULL,
       NOW() - INTERVAL 1 DAY
FROM app_user u
WHERE u.email = 'manager@everest-sushi.no';

INSERT INTO notification
(org_number, user_id, notification_type, title, body_text,
 related_entity_type, related_entity_id, is_read, read_at, created_at)
SELECT 937219997,
       u.user_id,
       'DEVIATION_STATUS_CHANGED',
       'Avviksstatus oppdatert',
       'Avviket «Temperaturlogg manglet signatur» er nå merket som løst.',
       'DEVIATION_REPORT',
       2,
       1,
       NOW() - INTERVAL 12 HOUR,
       NOW() - INTERVAL 2 DAY
FROM app_user u
WHERE u.email = 'manager@everest-sushi.no';

INSERT INTO notification
(org_number, user_id, notification_type, title, body_text,
 related_entity_type, related_entity_id, is_read, read_at, created_at)
SELECT 937219997,
       u.user_id,
       'GENERAL',
       'Velkommen til IK-Kontroll',
       'Systemet er konfigurert for Everest Sushi & Fusion AS. Ta kontakt med administrator ved spørsmål.',
       NULL,
       NULL,
       1,
       NOW() - INTERVAL 5 DAY,
       NOW() - INTERVAL 7 DAY
FROM app_user u
WHERE u.email = 'manager@everest-sushi.no';


INSERT INTO notification
(org_number, user_id, notification_type, title, body_text,
 related_entity_type, related_entity_id, is_read, read_at, created_at)
SELECT 937219997,
       u.user_id,
       'TASK_OVERDUE',
       'Oppgave forfalt – Temperaturlogg fryseren',
       'Temperaturloggen for fryseren mangler registrering for de siste 4 timene.',
       'TEMPERATURE_LOG_ENTRY',
       2,
       0,
       NULL,
       NOW() - INTERVAL 4 HOUR
FROM app_user u
WHERE u.email = 'anine@personligmail.com';

INSERT INTO notification
(org_number, user_id, notification_type, title, body_text,
 related_entity_type, related_entity_id, is_read, read_at, created_at)
SELECT 937219997,
       u.user_id,
       'DEVIATION_ASSIGNED',
       'Nytt avvik tildelt',
       'Du er satt som ansvarlig for avviket «Allergenmerking manglet på dessertbuffet».',
       'DEVIATION_REPORT',
       3,
       0,
       NULL,
       NOW() - INTERVAL 45 MINUTE
FROM app_user u
WHERE u.email = 'anine@personligmail.com';

INSERT INTO notification
(org_number, user_id, notification_type, title, body_text,
 related_entity_type, related_entity_id, is_read, read_at, created_at)
SELECT 937219997,
       u.user_id,
       'GENERAL',
       'Ny prosedyre publisert',
       'Rengjøringsprosedyren er oppdatert. Les gjennom og bekreft at du har lest den.',
       'ORGANIZATION_DOCUMENT',
       3,
       0,
       NULL,
       NOW() - INTERVAL 3 HOUR
FROM app_user u
WHERE u.email = 'surya@personligmai.com';

-- One IN_APP delivery record per notification (mirrors what NotificationServiceImpl creates).

INSERT INTO notification_delivery
(notification_id, delivery_channel, delivery_status, attempted_at, delivered_at)
SELECT n.notification_id,
       'IN_APP',
       'SENT',
       n.created_at,
       n.created_at
FROM notification n
WHERE n.org_number = 937219997
  AND NOT EXISTS (SELECT 1
                  FROM notification_delivery d
                  WHERE d.notification_id = n.notification_id);

SET FOREIGN_KEY_CHECKS = 1;
