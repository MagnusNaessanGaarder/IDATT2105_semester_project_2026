SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- V3.0.0: COMPREHENSIVE TEST DATA SEED
-- 
-- This migration provides realistic test data for all features:
-- - Locations (8 types including storage, dry goods, receiving)
-- - Temperature logs (multiple users, various readings)
-- - Checklists (FOOD and ALCOHOL modules)
-- - Deviations (various severities and statuses)
-- - Documents (with mock Azure blob references)
-- - Training records (multiple types and expiry dates)
-- - Notifications (unread and read)
-- - Audit logs (tracking data changes)
-- ============================================================

-- ---------------------------------------------------------
-- LOCATIONS (8 different types for comprehensive testing)
-- ---------------------------------------------------------

INSERT INTO location (org_number, name, description, location_type, temp_min_c, temp_max_c, is_active, created_at, updated_at)
VALUES
  (937219997, 'Kjøleskap 1 (fersk fisk)', 'Kjøleskap for frisk fisk og sjømat', 'FRIDGE', 0, 4, 1, NOW(), NOW()),
  (937219997, 'Kjøleskap 2 (kjøtt/fjærkre)', 'Kjøleskap for kjøtt og fjærkre', 'FRIDGE', 0, 4, 1, NOW(), NOW()),
  (937219997, 'Fryseren', 'Industrifrysing for langsiktig lagring', 'FREEZER', -20, -15, 1, NOW(), NOW()),
  (937219997, 'Serveringsbuffet', 'Varmholdt serveringsbuffet', 'HOT_FOOD', 60, 70, 1, NOW(), NOW()),
  (937219997, 'Kjøkken', 'Hovedkjøkken', 'KITCHEN', NULL, NULL, 1, NOW(), NOW()),
  (937219997, 'Bar', 'Barområde', 'BAR', NULL, NULL, 1, NOW(), NOW()),
  (937219997, 'Tørrlager', 'Lager for tørrvarer', 'STORAGE', NULL, NULL, 1, NOW(), NOW()),
  (937219997, 'Mottak', 'Varemottak og kontroll', 'RECEIVING', NULL, NULL, 1, NOW(), NOW());

-- ---------------------------------------------------------
-- TEMPERATURE LOG POINTS / ENTRIES (Multiple users, dates)
-- ---------------------------------------------------------

INSERT INTO temperature_log_point (org_number, location_id, name, is_active, created_at, updated_at)
SELECT 937219997, l.location_id, l.name, 1, NOW(), NOW()
FROM location l
WHERE l.org_number = 937219997;

-- Temperature entries with relative dates (last 7 days)
INSERT INTO temperature_log_entry (org_number, log_point_id, recorded_by_user_id, measured_at, temperature_c, is_alert, note_text, created_at)
SELECT
  937219997,
  lp.log_point_id,
  (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'),
  DATE_SUB(CURDATE(), INTERVAL 1 DAY) + INTERVAL 8 HOUR,
  2.00,
  0,
  'Normal temperatur - morgenmåling',
  NOW()
FROM temperature_log_point lp
WHERE lp.org_number = 937219997 AND lp.name = 'Kjøleskap 1 (fersk fisk)'
UNION ALL
SELECT
  937219997,
  lp.log_point_id,
  (SELECT user_id FROM app_user WHERE email = 'anine@personligmail.com'),
  DATE_SUB(CURDATE(), INTERVAL 1 DAY) + INTERVAL 16 HOUR,
  3.50,
  0,
  'Ettermiddagsmåling - OK',
  NOW()
FROM temperature_log_point lp
WHERE lp.org_number = 937219997 AND lp.name = 'Kjøleskap 1 (fersk fisk)'
UNION ALL
SELECT
  937219997,
  lp.log_point_id,
  (SELECT user_id FROM app_user WHERE email = 'anine@personligmail.com'),
  DATE_SUB(CURDATE(), INTERVAL 1 DAY) + INTERVAL 8 HOUR,
  3.00,
  0,
  'Kjøtt/fjærkre - morgenmåling',
  NOW()
FROM temperature_log_point lp
WHERE lp.org_number = 937219997 AND lp.name = 'Kjøleskap 2 (kjøtt/fjærkre)'
UNION ALL
SELECT
  937219997,
  lp.log_point_id,
  (SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'),
  DATE_SUB(CURDATE(), INTERVAL 2 DAY) + INTERVAL 14 HOUR,
  -18.00,
  0,
  'Fryser temperatur stabil',
  NOW()
FROM temperature_log_point lp
WHERE lp.org_number = 937219997 AND lp.name = 'Fryseren'
UNION ALL
SELECT
  937219997,
  lp.log_point_id,
  (SELECT user_id FROM app_user WHERE email = 'surya@personligmai.com'),
  DATE_SUB(CURDATE(), INTERVAL 1 DAY) + INTERVAL 12 HOUR,
  65.00,
  0,
  'Buffet temperatur OK',
  NOW()
FROM temperature_log_point lp
WHERE lp.org_number = 937219997 AND lp.name = 'Serveringsbuffet';

-- Alert entry (high temperature)
INSERT INTO temperature_log_entry (org_number, log_point_id, recorded_by_user_id, measured_at, temperature_c, is_alert, note_text, created_at)
SELECT
  937219997,
  lp.log_point_id,
  (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'),
  DATE_SUB(CURDATE(), INTERVAL 3 DAY) + INTERVAL 10 HOUR,
  7.50,
  1,
  'ALARM: Høy temperatur oppdaget! Kjøleskapet må sjekkes.',
  NOW()
FROM temperature_log_point lp
WHERE lp.org_number = 937219997 AND lp.name = 'Kjøleskap 1 (fersk fisk)';

-- ---------------------------------------------------------
-- CHECKLIST TEMPLATES (ik-mat.json + ik-alkohol.json)
-- ---------------------------------------------------------

INSERT INTO checklist_template (org_number, module_type, title, description, frequency, is_active, created_by_user_id, created_at, updated_at)
VALUES
  (937219997, 'FOOD', 'Daglig kjøleskap kontroll', 'Kontroll av kjøleskapstemperatur morgen og kveld', 'DAILY', 1, (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'), NOW(), NOW()),
  (937219997, 'FOOD', 'Ukentlig HACCP-gjennomgang', 'Gjennomgang av HACCP-planen og kritiske kontrollpunkter', 'WEEKLY', 1, (SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'), NOW(), NOW()),
  (937219997, 'FOOD', 'Månedlig hygienekontroll', 'Grundig hygienekontroll av kjøkken og serveringsområder', 'MONTHLY', 1, (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'), NOW(), NOW()),
  (937219997, 'ALCOHOL', 'Daglig alkoholkontroll', 'Daglig kontroll av alderssjekk, beruselse og sjenketider', 'DAILY', 1, (SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'), NOW(), NOW()),
  (937219997, 'FOOD', 'Mottakskontroll varer', 'Kontroll av temperatur og kvalitet ved varemottak', 'DAILY', 1, (SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'), NOW(), NOW()),
  (937219997, 'FOOD', 'Rengjøring og vedlikehold', 'Daglig sjekk av renhold og utstyr', 'DAILY', 1, (SELECT user_id FROM app_user WHERE email = 'anine@personligmail.com'), NOW(), NOW());

INSERT INTO checklist_template_item (template_id, sort_order, label, description, item_type, is_required, expected_text, expected_numeric_min, expected_numeric_max, choice_options_json)
SELECT t.template_id, 1, 'Sjekk kjøleskapstemperatur', '2-4°C, normal drift', 'BOOLEAN', 1, NULL, NULL, NULL, NULL
FROM checklist_template t WHERE t.org_number = 937219997 AND t.title = 'Daglig kjøleskap kontroll'
UNION ALL
SELECT t.template_id, 2, 'Kontroller for frysing av varer', 'Ingen tegn på frysing observert', 'BOOLEAN', 1, NULL, NULL, NULL, NULL
FROM checklist_template t WHERE t.org_number = 937219997 AND t.title = 'Daglig kjøleskap kontroll'
UNION ALL
SELECT t.template_id, 3, 'Sjekk pakninger for skader', 'Rapporter skader umiddelbart', 'BOOLEAN', 1, NULL, NULL, NULL, NULL
FROM checklist_template t WHERE t.org_number = 937219997 AND t.title = 'Daglig kjøleskap kontroll'
UNION ALL
SELECT t.template_id, 1, 'Kontroller CCP 1: Mottakskontroll', 'Temp ok, kvalitet godkjent', 'BOOLEAN', 1, NULL, NULL, NULL, NULL
FROM checklist_template t WHERE t.org_number = 937219997 AND t.title = 'Ukentlig HACCP-gjennomgang'
UNION ALL
SELECT t.template_id, 2, 'Kontroller CCP 2: Lagertemperatur', 'Fryseren på -18°C', 'BOOLEAN', 1, NULL, NULL, NULL, NULL
FROM checklist_template t WHERE t.org_number = 937219997 AND t.title = 'Ukentlig HACCP-gjennomgang'
UNION ALL
SELECT t.template_id, 3, 'Kontroller CCP 3: Tilberedning', 'Prosedyrer fulgt', 'BOOLEAN', 1, NULL, NULL, NULL, NULL
FROM checklist_template t WHERE t.org_number = 937219997 AND t.title = 'Ukentlig HACCP-gjennomgang'
UNION ALL
SELECT t.template_id, 1, 'Sjekk rengjøring av kjøkkenflater', NULL, 'BOOLEAN', 1, NULL, NULL, NULL, NULL
FROM checklist_template t WHERE t.org_number = 937219997 AND t.title = 'Månedlig hygienekontroll'
UNION ALL
SELECT t.template_id, 2, 'Kontroller personlig hygiene prosedyrer', NULL, 'BOOLEAN', 1, NULL, NULL, NULL, NULL
FROM checklist_template t WHERE t.org_number = 937219997 AND t.title = 'Månedlig hygienekontroll'
UNION ALL
SELECT t.template_id, 1, 'Alderskontroll ved åpning', 'Kontrollert legitimasjon og alder ved åpning', 'BOOLEAN', 1, NULL, NULL, NULL, NULL
FROM checklist_template t WHERE t.org_number = 937219997 AND t.title = 'Daglig alkoholkontroll'
UNION ALL
SELECT t.template_id, 2, 'Legitimasjon sjekket (alle gjester under 25 år)', 'Sjekk legitimasjon for alle under 25', 'BOOLEAN', 1, NULL, NULL, NULL, NULL
FROM checklist_template t WHERE t.org_number = 937219997 AND t.title = 'Daglig alkoholkontroll'
UNION ALL
SELECT t.template_id, 3, 'Beruselseskontroll: Observasjon av gjester', 'Ingen overstadig berusede gjester', 'BOOLEAN', 1, NULL, NULL, NULL, NULL
FROM checklist_template t WHERE t.org_number = 937219997 AND t.title = 'Daglig alkoholkontroll'
UNION ALL
SELECT t.template_id, 4, 'Sjenketid overholdt (senest 01:00)', 'Kontroll av skjenkestopp', 'BOOLEAN', 1, NULL, NULL, NULL, NULL
FROM checklist_template t WHERE t.org_number = 937219997 AND t.title = 'Daglig alkoholkontroll'
UNION ALL
SELECT t.template_id, 1, 'Temperatur ved mottak', 'Sjekk at varer er innen temperaturkrav', 'NUMERIC', 1, NULL, -20, 4, NULL
FROM checklist_template t WHERE t.org_number = 937219997 AND t.title = 'Mottakskontroll varer'
UNION ALL
SELECT t.template_id, 2, 'Emballasjekontroll', 'Ingen skader på emballasje', 'BOOLEAN', 1, NULL, NULL, NULL, NULL
FROM checklist_template t WHERE t.org_number = 937219997 AND t.title = 'Mottakskontroll varer'
UNION ALL
SELECT t.template_id, 1, 'Gulv og overflater rengjort', 'Alle overflater er rengjort og desinfisert', 'BOOLEAN', 1, NULL, NULL, NULL, NULL
FROM checklist_template t WHERE t.org_number = 937219997 AND t.title = 'Rengjøring og vedlikehold'
UNION ALL
SELECT t.template_id, 2, 'Utstyr fungerer og er rent', 'Alt utstyr er rengjort og i drift', 'BOOLEAN', 1, NULL, NULL, NULL, NULL
FROM checklist_template t WHERE t.org_number = 937219997 AND t.title = 'Rengjøring og vedlikehold';

-- ---------------------------------------------------------
-- CHECKLIST RUNS (Multiple users including surya and anine)
-- ---------------------------------------------------------

INSERT INTO checklist_run (template_id, org_number, location_id, performed_by_user_id, assigned_to_user_id, run_date, due_at, completed_at, status, notes, created_at, updated_at)
SELECT
  t.template_id,
  937219997,
  (SELECT location_id FROM location WHERE org_number = 937219997 AND name = 'Kjøkken'),
  (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'),
  (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'),
  DATE_SUB(CURDATE(), INTERVAL 1 DAY),
  DATE_SUB(CURDATE(), INTERVAL 1 DAY) + INTERVAL 23 HOUR + INTERVAL 59 MINUTE,
  DATE_SUB(CURDATE(), INTERVAL 1 DAY) + INTERVAL 8 HOUR + INTERVAL 30 MINUTE,
  'COMPLETED',
  'Daglig sjekk fullført av admin',
  NOW(),
  NOW()
FROM checklist_template t
WHERE t.org_number = 937219997 AND t.title = 'Daglig kjøleskap kontroll'
UNION ALL
SELECT
  t.template_id,
  937219997,
  (SELECT location_id FROM location WHERE org_number = 937219997 AND name = 'Kjøkken'),
  (SELECT user_id FROM app_user WHERE email = 'anine@personligmail.com'),
  (SELECT user_id FROM app_user WHERE email = 'anine@personligmail.com'),
  DATE_SUB(CURDATE(), INTERVAL 1 DAY),
  DATE_SUB(CURDATE(), INTERVAL 1 DAY) + INTERVAL 23 HOUR + INTERVAL 59 MINUTE,
  DATE_SUB(CURDATE(), INTERVAL 1 DAY) + INTERVAL 16 HOUR,
  'COMPLETED',
  'Ettermiddags sjekk fullført av Anine',
  NOW(),
  NOW()
FROM checklist_template t
WHERE t.org_number = 937219997 AND t.title = 'Daglig kjøleskap kontroll'
UNION ALL
SELECT
  t.template_id,
  937219997,
  (SELECT location_id FROM location WHERE org_number = 937219997 AND name = 'Kjøkken'),
  (SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'),
  (SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'),
  DATE_SUB(CURDATE(), INTERVAL 7 DAY),
  DATE_SUB(CURDATE(), INTERVAL 7 DAY) + INTERVAL 23 HOUR + INTERVAL 59 MINUTE,
  DATE_SUB(CURDATE(), INTERVAL 7 DAY) + INTERVAL 14 HOUR,
  'COMPLETED',
  'Ukentlig HACCP gjennomgang fullført',
  NOW(),
  NOW()
FROM checklist_template t
WHERE t.org_number = 937219997 AND t.title = 'Ukentlig HACCP-gjennomgang'
UNION ALL
SELECT
  t.template_id,
  937219997,
  (SELECT location_id FROM location WHERE org_number = 937219997 AND name = 'Bar'),
  (SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'),
  (SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'),
  DATE_SUB(CURDATE(), INTERVAL 1 DAY),
  DATE_SUB(CURDATE(), INTERVAL 1 DAY) + INTERVAL 23 HOUR + INTERVAL 59 MINUTE,
  DATE_SUB(CURDATE(), INTERVAL 1 DAY) + INTERVAL 18 HOUR,
  'COMPLETED',
  'Daglig alkoholkontroll gjennomført',
  NOW(),
  NOW()
FROM checklist_template t
WHERE t.org_number = 937219997 AND t.title = 'Daglig alkoholkontroll'
UNION ALL
SELECT
  t.template_id,
  937219997,
  (SELECT location_id FROM location WHERE org_number = 937219997 AND name = 'Mottak'),
  (SELECT user_id FROM app_user WHERE email = 'surya@personligmai.com'),
  (SELECT user_id FROM app_user WHERE email = 'surya@personligmai.com'),
  DATE_SUB(CURDATE(), INTERVAL 1 DAY),
  DATE_SUB(CURDATE(), INTERVAL 1 DAY) + INTERVAL 23 HOUR + INTERVAL 59 MINUTE,
  DATE_SUB(CURDATE(), INTERVAL 1 DAY) + INTERVAL 9 HOUR,
  'COMPLETED',
  'Mottakskontroll fullført av Surya',
  NOW(),
  NOW()
FROM checklist_template t
WHERE t.org_number = 937219997 AND t.title = 'Mottakskontroll varer'
UNION ALL
SELECT
  t.template_id,
  937219997,
  (SELECT location_id FROM location WHERE org_number = 937219997 AND name = 'Kjøkken'),
  (SELECT user_id FROM app_user WHERE email = 'anine@personligmail.com'),
  (SELECT user_id FROM app_user WHERE email = 'anine@personligmail.com'),
  DATE_SUB(CURDATE(), INTERVAL 1 DAY),
  DATE_SUB(CURDATE(), INTERVAL 1 DAY) + INTERVAL 23 HOUR + INTERVAL 59 MINUTE,
  DATE_SUB(CURDATE(), INTERVAL 1 DAY) + INTERVAL 20 HOUR,
  'COMPLETED',
  'Rengjøring og vedlikehold fullført',
  NOW(),
  NOW()
FROM checklist_template t
WHERE t.org_number = 937219997 AND t.title = 'Rengjøring og vedlikehold'
UNION ALL
-- Overdue checklist (for testing overdue notifications)
SELECT
  t.template_id,
  937219997,
  (SELECT location_id FROM location WHERE org_number = 937219997 AND name = 'Kjøkken'),
  (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'),
  (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'),
  DATE_SUB(CURDATE(), INTERVAL 1 DAY),
  DATE_SUB(CURDATE(), INTERVAL 1 DAY) + INTERVAL 23 HOUR + INTERVAL 59 MINUTE,
  NULL,
  'PENDING',
  'Månedlig hygienekontroll - VENTER',
  NOW(),
  NOW()
FROM checklist_template t
WHERE t.org_number = 937219997 AND t.title = 'Månedlig hygienekontroll';

-- Checklist run items with some deviations
INSERT INTO checklist_run_item (run_id, template_item_id, boolean_value, text_value, numeric_value, selected_choice, is_deviation, comment_text, created_at, updated_at)
SELECT
  cr.run_id,
  cti.item_id,
  CASE
    WHEN cr.notes LIKE '%Daglig sjekk%' AND cti.sort_order = 3 THEN 0
    WHEN cr.notes LIKE '%alkoholkontroll%' AND cti.sort_order = 3 THEN 0
    WHEN cr.notes LIKE '%Mottak%' AND cti.sort_order = 2 THEN 0
    ELSE 1
  END,
  NULL,
  CASE
    WHEN cr.notes LIKE '%Mottak%' AND cti.sort_order = 1 THEN 3.5
    ELSE NULL
  END,
  NULL,
  CASE
    WHEN cr.notes LIKE '%Daglig sjekk%' AND cti.sort_order = 3 THEN 1
    WHEN cr.notes LIKE '%alkoholkontroll%' AND cti.sort_order = 3 THEN 1
    WHEN cr.notes LIKE '%Mottak%' AND cti.sort_order = 2 THEN 1
    ELSE 0
  END,
  CASE
    WHEN cr.notes LIKE '%Daglig sjekk%' AND cti.sort_order = 3 THEN 'Pakninger ikke kontrollert - dette må følges opp'
    WHEN cr.notes LIKE '%alkoholkontroll%' AND cti.sort_order = 3 THEN 'Trenger oppfølging ved skiftbytte'
    WHEN cr.notes LIKE '%Mottak%' AND cti.sort_order = 2 THEN 'Emballasje skadet på én leveranse'
    ELSE 'Fullført uten avvik'
  END,
  NOW(),
  NOW()
FROM checklist_run cr
JOIN checklist_template_item cti ON cti.template_id = cr.template_id
WHERE cr.org_number = 937219997 AND cr.status = 'COMPLETED';

-- ---------------------------------------------------------
-- DEVIATION REPORTS (Various severities and statuses)
-- ---------------------------------------------------------

INSERT INTO deviation_report (
  org_number, reported_by_user_id, report_type, severity, title, description, location_id, location_text, occurred_date, occurred_time, report_date, immediate_action_text, corrective_action_text, status, created_at, updated_at, closed_at
)
VALUES
  (
    937219997,
    (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'),
    'DISCREPANCY',
    'MAJOR',
    'Kjøleskap temperaturen var høy',
    'Kjøleskapet på kjøkkenet registrerte 7C i stedet for 2-4C. Dette kan føre til matsikkerhetsrisiko.',
    (SELECT location_id FROM location WHERE org_number = 937219997 AND name = 'Kjøleskap 1 (fersk fisk)'),
    'Kjøleskap 1 (fersk fisk)',
    DATE_SUB(CURDATE(), INTERVAL 3 DAY),
    '14:30:00',
    DATE_SUB(CURDATE(), INTERVAL 3 DAY),
    'Lukket kjøleskapet straks og justerte temperaturinnstillingen. Kontaktet vedlikehold.',
    'Kjøleskapet ble reparert og kalibrert. Ny rutine for daglig dobbeltsjekk etablert.',
    'CLOSED',
    NOW(),
    NOW(),
    DATE_SUB(CURDATE(), INTERVAL 1 DAY)
  ),
  (
    937219997,
    (SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'),
    'DISCREPANCY',
    'MINOR',
    'Pakking med skader funnet ved mottak',
    'En pakke med ferskvare viste tegn på lekkasje. Varen ble ikke tatt i bruk.',
    (SELECT location_id FROM location WHERE org_number = 937219997 AND name = 'Kjøleskap 2 (kjøtt/fjærkre)'),
    'Kjøleskap 2 (kjøtt/fjærkre)',
    DATE_SUB(CURDATE(), INTERVAL 5 DAY),
    '09:15:00',
    DATE_SUB(CURDATE(), INTERVAL 5 DAY),
    'Kastet den skadede pakken og dokumenterte. Kontaktet leverandør.',
    'Hever oppmerksomheten på pakningskvalitet ved mottak. Nye rutiner for inspeksjon.',
    'CORRECTIVE_ACTION_PLANNED',
    NOW(),
    NOW(),
    NULL
  ),
  (
    937219997,
    (SELECT user_id FROM app_user WHERE email = 'harald@oersonligmail.no'),
    'DISCREPANCY',
    'CRITICAL',
    'Alderskontroll ikke gjennomført ved høy pågang',
    'Under en travel lørdagskveld ble ikke alle gjester under 25 sjekket for legitimasjon før alkoholservering.',
    (SELECT location_id FROM location WHERE org_number = 937219997 AND name = 'Bar'),
    'Bar',
    DATE_SUB(CURDATE(), INTERVAL 2 DAY),
    '23:45:00',
    DATE_SUB(CURDATE(), INTERVAL 2 DAY),
    'Stoppet alkoholservering umiddelbart. Gjennomførte ekstra kontroller.',
    'Trenger opplæring i håndtering av høy pågang. Vurderer ekstra bemanning ved behov.',
    'UNDER_REVIEW',
    NOW(),
    NOW(),
    NULL
  );

-- ---------------------------------------------------------
-- DOCUMENTS (With mock Azure blob references)
-- NOTE: These are mock blob references for testing only.
-- Real document downloads require Azure Storage configuration.
-- ---------------------------------------------------------

INSERT INTO organization_document (org_number, document_type, title, description, current_version, is_active, created_by_user_id, created_at, updated_at)
VALUES
  (937219997, 'REPORT_EXPORT', 'Månedlig kontrollrapport - ' || DATE_FORMAT(CURDATE(), '%M'), 'Månedlig rapport med kontrollresultater og avvik. 94% samsvar med 2 løste avvik.', 1, 1, (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'), NOW(), NOW()),
  (937219997, 'REPORT_EXPORT', 'Avvik og korreksjoner - ' || YEAR(CURDATE()) || ' Q' || QUARTER(CURDATE()), 'Kvartalsrapport: 6 avvik rapportert, 5 løst, 1 under behandling', 1, 1, (SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'), NOW(), NOW()),
  (937219997, 'POLICY', 'Internkontrollplan for matsikkerhet', 'Organisasjonens IK-MAT plan i henhold til Matloven §13', 1, 1, (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'), NOW(), NOW()),
  (937219997, 'POLICY', 'Alkoholloven - Retningslinjer og prosedyrer', 'Organisasjonens retningslinjer for alkoholutskjenking i henhold til alkoholloven', 1, 1, (SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'), NOW(), NOW()),
  (937219997, 'CERTIFICATE', 'HACCP Sertifisering', 'Årlig HACCP sertifisering og revisjonsrapport', 1, 1, (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'), NOW(), NOW());

-- Document versions with mock Azure blob references
-- NOTE: These are placeholder blob paths. Real Azure Storage setup required for actual file downloads.
INSERT INTO organization_document_version (
  document_id, version_number, azure_container, azure_blob_name, original_filename, mime_type, file_size_bytes, blob_etag, uploaded_by_user_id, uploaded_at, valid_from, valid_to, checksum_sha256
)
SELECT
  d.document_id,
  1,
  'documents',
  CONCAT('org-937219997/', DATE_FORMAT(CURDATE(), '%Y%m%d'), '/', REPLACE(LOWER(d.title), ' ', '-'), '.pdf'),
  CONCAT(d.title, '.pdf'),
  'application/pdf',
  FLOOR(50000 + RAND() * 100000), -- Random file size between 50KB and 150KB
  NULL,
  d.created_by_user_id,
  NOW(),
  DATE_SUB(CURDATE(), INTERVAL 30 DAY),
  NULL,
  NULL
FROM organization_document d
WHERE d.org_number = 937219997;

-- ---------------------------------------------------------
-- TRAINING RECORDS (Multiple users and training types)
-- ---------------------------------------------------------

INSERT INTO training_record (
  user_id, org_number, training_type, title, completed_at, expires_at, status, certificate_document_id, notes, created_at
)
VALUES
  -- Admin user training
  ((SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'), 937219997, 'FOOD_HYGIENE', 'Grunnleggende hygieneopplæring', '2024-01-15 10:00:00', '2027-01-15 00:00:00', 'COMPLETED', NULL, 'Oppdatert opplæring fullført', NOW()),
  ((SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'), 937219997, 'TEMPERATURE_CONTROL', 'Temperaturkontroll og logging', '2024-02-10 14:00:00', '2026-02-10 00:00:00', 'COMPLETED', NULL, 'Praktisk kurs med sertifisering', NOW()),
  
  -- Manager training
  ((SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'), 937219997, 'RESPONSIBLE_ALCOHOL_SERVICE', 'Kunnskapsprøve alkoholloven', '2024-01-20 10:00:00', '2026-01-20 00:00:00', 'COMPLETED', NULL, 'Sertifisert skjenkemester', NOW()),
  ((SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'), 937219997, 'ALLERGEN_HANDLING', 'Allergen håndtering', '2024-03-05 09:00:00', '2027-03-05 00:00:00', 'COMPLETED', NULL, 'Obligatorisk for kjøkkenledelse', NOW()),
  
  -- Harald (bartender) training
  ((SELECT user_id FROM app_user WHERE email = 'harald@oersonligmail.no'), 937219997, 'RESPONSIBLE_ALCOHOL_SERVICE', 'Kunnskapsprøve alkoholloven', '2024-02-01 10:00:00', '2026-02-01 00:00:00', 'COMPLETED', NULL, 'Fullført med bestått', NOW()),
  ((SELECT user_id FROM app_user WHERE email = 'harald@oersonligmail.no'), 937219997, 'AGE_VERIFICATION', 'Alderskontroll og legitimasjon', '2024-02-01 11:00:00', '2026-02-01 00:00:00', 'COMPLETED', NULL, 'Del av alkoholsertifisering', NOW()),
  
  -- Anine (cook) training
  ((SELECT user_id FROM app_user WHERE email = 'anine@personligmail.com'), 937219997, 'FOOD_HYGIENE', 'Grunnleggende hygieneopplæring', '2024-03-10 09:00:00', '2027-03-10 00:00:00', 'COMPLETED', NULL, 'Kjøkkenpersonell opplæring', NOW()),
  ((SELECT user_id FROM app_user WHERE email = 'anine@personligmail.com'), 937219997, 'TEMPERATURE_CONTROL', 'Temperaturkontroll ved matlaging', '2024-03-15 13:00:00', '2026-03-15 00:00:00', 'COMPLETED', NULL, 'Praktisk opplæring', NOW()),
  
  -- Surya (waiter) training
  ((SELECT user_id FROM app_user WHERE email = 'surya@personligmai.com'), 937219997, 'FOOD_HYGIENE', 'Grunnleggende hygieneopplæring', '2024-04-01 10:00:00', '2027-04-01 00:00:00', 'COMPLETED', NULL, 'Serveringspersonell opplæring', NOW()),
  ((SELECT user_id FROM app_user WHERE email = 'surya@personligmai.com'), 937219997, 'CLEANING_ROUTINES', 'Rengjøringsrutiner', '2024-04-05 14:00:00', '2026-04-05 00:00:00', 'COMPLETED', NULL, 'Daglige renholdsrutiner', NOW()),
  
  -- Training expiring soon (for testing notifications)
  ((SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'), 937219997, 'OTHER', 'Brannsikkerhet og nødprosedyrer', DATE_SUB(CURDATE(), INTERVAL 2 YEAR), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 'COMPLETED', NULL, 'Utløper snart - må fornyes', NOW()),
  
  -- Assigned training not completed
  ((SELECT user_id FROM app_user WHERE email = 'harald@oersonligmail.no'), 937219997, 'ALLERGEN_HANDLING', 'Allergen håndtering og merking', NULL, NULL, 'ASSIGNED', NULL, 'Påkrevd opplæring - må fullføres innen 30 dager', NOW());

-- ---------------------------------------------------------
-- NOTIFICATIONS (Unread and read, various types)
-- ---------------------------------------------------------

INSERT INTO notification (user_id, org_number, notification_type, title, body_text, is_read, read_at, related_entity_type, related_entity_id, created_at)
VALUES
  -- Unread notifications for admin
  ((SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'), 937219997, 'DEVIATION_ASSIGNED', 'Nytt avvik tildelt deg', 'Et avvik krever din oppmerksomhet: Alderskontroll ikke gjennomført ved høy pågang', 0, NULL, 'DEVIATION_REPORT', 3, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
  ((SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'), 937219997, 'TASK_OVERDUE', 'Oppgave forfalt', 'Månedlig hygienekontroll har passert forfallsdato', 0, NULL, 'CHECKLIST_RUN', 7, DATE_SUB(NOW(), INTERVAL 5 HOUR)),
  ((SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'), 937219997, 'TEMPERATURE_ALERT', 'Temperaturalarm', 'Kjøleskap 1 registrerte høy temperatur (7.5°C)', 0, NULL, 'TEMPERATURE_LOG_ENTRY', 6, DATE_SUB(NOW(), INTERVAL 1 DAY)),
  
  -- Unread notifications for manager
  ((SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'), 937219997, 'TRAINING_EXPIRING', 'Opplæring utløper snart', 'Din brannsikkerhet-opplæring utløper om 14 dager', 0, NULL, 'TRAINING_RECORD', 11, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
  ((SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'), 937219997, 'DEVIATION_STATUS_CHANGED', 'Avvik status oppdatert', 'Avvik #2 har blitt oppdatert med korrigerende tiltak', 0, NULL, 'DEVIATION_REPORT', 2, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
  
  -- Read notifications (for testing history)
  ((SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'), 937219997, 'GENERAL', 'Velkommen til IK-Kontroll', 'Din konto er nå aktiv. Ta kontakt med support ved spørsmål.', 1, DATE_SUB(NOW(), INTERVAL 5 DAY), NULL, NULL, DATE_SUB(NOW(), INTERVAL 7 DAY)),
  ((SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'), 937219997, 'GENERAL', 'Velkommen til IK-Kontroll', 'Din konto er nå aktiv. Ta kontakt med support ved spørsmål.', 1, DATE_SUB(NOW(), INTERVAL 5 DAY), NULL, NULL, DATE_SUB(NOW(), INTERVAL 7 DAY)),
  ((SELECT user_id FROM app_user WHERE email = 'harald@oersonligmail.no'), 937219997, 'TRAINING_EXPIRING', 'Ny opplæring tildelt', 'Allergen håndtering opplæring er nå tilgjengelig', 0, NULL, 'TRAINING_RECORD', 12, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ---------------------------------------------------------
-- AUDIT LOGS (Tracking data changes)
-- ---------------------------------------------------------

INSERT INTO audit_log (org_number, action_type, entity_type, entity_id, performed_by_user_id, change_summary, ip_address, user_agent, created_at)
VALUES
  (937219997, 'CREATE', 'DEVIATION_REPORT', 1, (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'), 'Opprettet avvik: Kjøleskap temperaturen var høy', '192.168.1.100', 'Mozilla/5.0', DATE_SUB(NOW(), INTERVAL 3 DAY)),
  (937219997, 'UPDATE', 'DEVIATION_REPORT', 1, (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'), 'Oppdatert status til CLOSED', '192.168.1.100', 'Mozilla/5.0', DATE_SUB(NOW(), INTERVAL 1 DAY)),
  (937219997, 'CREATE', 'DEVIATION_REPORT', 2, (SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'), 'Opprettet avvik: Pakking med skader funnet', '192.168.1.101', 'Mozilla/5.0', DATE_SUB(NOW(), INTERVAL 5 DAY)),
  (937219997, 'CREATE', 'DEVIATION_REPORT', 3, (SELECT user_id FROM app_user WHERE email = 'harald@oersonligmail.no'), 'Opprettet avvik: Alderskontroll ikke gjennomført', '192.168.1.102', 'Mozilla/5.0', DATE_SUB(NOW(), INTERVAL 2 DAY)),
  (937219997, 'UPDATE', 'CHECKLIST_RUN', 1, (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'), 'Fullførte sjekkliste: Daglig kjøleskap kontroll', '192.168.1.100', 'Mozilla/5.0', DATE_SUB(NOW(), INTERVAL 1 DAY)),
  (937219997, 'CREATE', 'TEMPERATURE_LOG_ENTRY', 6, (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'), 'Registrerte temperatur med ALERT: 7.5°C', '192.168.1.100', 'Mozilla/5.0', DATE_SUB(NOW(), INTERVAL 3 DAY)),
  (937219997, 'LOGIN', 'APP_USER', (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'), (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'), 'Bruker logget inn', '192.168.1.100', 'Mozilla/5.0', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
  (937219997, 'LOGIN', 'APP_USER', (SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'), (SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'), 'Bruker logget inn', '192.168.1.101', 'Mozilla/5.0', DATE_SUB(NOW(), INTERVAL 2 HOUR));

-- ---------------------------------------------------------
-- ORG SETTINGS (Updated with comprehensive defaults)
-- ---------------------------------------------------------

UPDATE organization_settings
SET
  enable_food_module = 1,
  enable_alcohol_module = 1,
  default_temp_min_c = 0,
  default_temp_max_c = 4,
  reminder_email_enabled = 1,
  notification_email = 'kari@everest-sushi.no',
  timezone_name = 'Europe/Oslo',
  locale_code = 'nb-NO',
  updated_at = NOW()
WHERE org_number = 937219997;

SET FOREIGN_KEY_CHECKS = 1;
