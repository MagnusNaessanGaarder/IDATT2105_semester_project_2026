SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------
-- LOCATIONS
-- ---------------------------------------------------------

INSERT INTO location (org_number, name, description, location_type, temp_min_c, temp_max_c, is_active, created_at, updated_at)
VALUES
  (937219997, 'Kjøleskap 1 (fersk fisk)', 'Kjøleskap for frisk fisk og sjømat', 'FRIDGE', 0, 4, 1, NOW(), NOW()),
  (937219997, 'Kjøleskap 2 (kjøtt/fjærkre)', 'Kjøleskap for kjøtt og fjærkre', 'FRIDGE', 0, 4, 1, NOW(), NOW()),
  (937219997, 'Fryseren', 'Industrifrysing for langsiktig lagring', 'FREEZER', -20, -15, 1, NOW(), NOW()),
  (937219997, 'Serveringsbuffet', 'Varmholdt serveringsbuffet', 'HOT_FOOD', 60, 70, 1, NOW(), NOW()),
  (937219997, 'Kjøkken', 'Hovedkjøkken', 'KITCHEN', NULL, NULL, 1, NOW(), NOW()),
  (937219997, 'Bar', 'Barområde', 'BAR', NULL, NULL, 1, NOW(), NOW());

-- ---------------------------------------------------------
-- TEMPERATURE LOG POINTS / ENTRIES (ik-mat.json)
-- ---------------------------------------------------------

INSERT INTO temperature_log_point (org_number, location_id, name, is_active, created_at, updated_at)
SELECT 937219997, l.location_id, l.name, 1, NOW(), NOW()
FROM location l
WHERE l.org_number = 937219997;

INSERT INTO temperature_log_entry (org_number, log_point_id, recorded_by_user_id, measured_at, temperature_c, is_alert, note_text, created_at)
SELECT
  937219997,
  lp.log_point_id,
  (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'),
  '2024-06-01 08:30:00',
  2.00,
  0,
  'Kjøleskap 1 (fersk fisk)',
  NOW()
FROM temperature_log_point lp
WHERE lp.org_number = 937219997 AND lp.name = 'Kjøleskap 1 (fersk fisk)'
UNION ALL
SELECT
  937219997,
  lp.log_point_id,
  (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'),
  '2024-06-01 08:30:00',
  3.00,
  0,
  'Kjøleskap 2 (kjøtt/fjærkre)',
  NOW()
FROM temperature_log_point lp
WHERE lp.org_number = 937219997 AND lp.name = 'Kjøleskap 2 (kjøtt/fjærkre)'
UNION ALL
SELECT
  937219997,
  lp.log_point_id,
  (SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'),
  '2024-06-01 14:00:00',
  -18.00,
  0,
  'Fryseren',
  NOW()
FROM temperature_log_point lp
WHERE lp.org_number = 937219997 AND lp.name = 'Fryseren'
UNION ALL
SELECT
  937219997,
  lp.log_point_id,
  (SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'),
  '2024-06-01 12:00:00',
  65.00,
  0,
  'Serveringsbuffet',
  NOW()
FROM temperature_log_point lp
WHERE lp.org_number = 937219997 AND lp.name = 'Serveringsbuffet';

-- ---------------------------------------------------------
-- CHECKLIST TEMPLATES (ik-mat.json + ik-alkohol.json)
-- ---------------------------------------------------------

INSERT INTO checklist_template (org_number, module_type, title, description, frequency, is_active, created_by_user_id, created_at, updated_at)
VALUES
  (937219997, 'FOOD', 'Daglig kjøleskap kontroll', 'Kontroll av kjøleskapstemperatur morgen og kveld', 'DAILY', 1, (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'), NOW(), NOW()),
  (937219997, 'FOOD', 'Ukentlig HACCP-gjennomgang', 'Gjennomgang av HACCP-planen og kritiske kontrollpunkter', 'WEEKLY', 1, (SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'), NOW(), NOW()),
  (937219997, 'FOOD', 'Månedlig hygienekontroll', 'Grundig hygienekontroll av kjøkken og serveringsområder', 'MONTHLY', 1, (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'), NOW(), NOW()),
  (937219997, 'ALCOHOL', 'Daglig alkoholkontroll', 'Daglig kontroll av alderssjekk, beruselse og sjenketider', 'DAILY', 1, (SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'), NOW(), NOW());

INSERT INTO checklist_template_item (template_id, sort_order, label, description, item_type, is_required, expected_text, expected_numeric_min, expected_numeric_max, choice_options_json)
SELECT t.template_id, 1, 'Sjekk kjøleskapstemperatur', '2-4°C, normal drift', 'BOOLEAN', 1, NULL, NULL, NULL, NULL
FROM checklist_template t WHERE t.org_number = 937219997 AND t.title = 'Daglig kjøleskap kontroll'
UNION ALL
SELECT t.template_id, 2, 'Kontroller for frysing av varer', 'Ingen tegn på frysing observert', 'BOOLEAN', 1, NULL, NULL, NULL, NULL
FROM checklist_template t WHERE t.org_number = 937219997 AND t.title = 'Daglig kjøleskap kontroll'
UNION ALL
SELECT t.template_id, 3, 'Sjekk pakninger for skader', NULL, 'BOOLEAN', 1, NULL, NULL, NULL, NULL
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
FROM checklist_template t WHERE t.org_number = 937219997 AND t.title = 'Daglig alkoholkontroll';

-- ---------------------------------------------------------
-- CHECKLIST RUNS / ITEMS
-- ---------------------------------------------------------

INSERT INTO checklist_run (template_id, org_number, location_id, performed_by_user_id, assigned_to_user_id, run_date, due_at, completed_at, status, notes, created_at, updated_at)
SELECT
  t.template_id,
  937219997,
  (SELECT location_id FROM location WHERE org_number = 937219997 AND name = 'Kjøkken'),
  (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'),
  (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'),
  '2024-06-01',
  '2024-06-01 23:59:59',
  '2024-06-01 08:30:00',
  'COMPLETED',
  'Daglig sjekk fullført',
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
  '2024-05-29',
  '2024-05-29 23:59:59',
  '2024-05-29 14:00:00',
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
  '2024-06-01',
  '2024-06-01 23:59:59',
  '2024-06-01 18:00:00',
  'COMPLETED',
  'Daglig alkoholkontroll gjennomført',
  NOW(),
  NOW()
FROM checklist_template t
WHERE t.org_number = 937219997 AND t.title = 'Daglig alkoholkontroll';

INSERT INTO checklist_run_item (run_id, template_item_id, boolean_value, text_value, numeric_value, selected_choice, is_deviation, comment_text, created_at, updated_at)
SELECT
  cr.run_id,
  cti.item_id,
  CASE
    WHEN cr.notes LIKE '%Daglig sjekk%' AND cti.sort_order = 3 THEN 0
    WHEN cr.notes LIKE '%alkoholkontroll%' AND cti.sort_order IN (3) THEN 0
    ELSE 1
  END,
  NULL,
  NULL,
  NULL,
  CASE
    WHEN cr.notes LIKE '%Daglig sjekk%' AND cti.sort_order = 3 THEN 1
    WHEN cr.notes LIKE '%alkoholkontroll%' AND cti.sort_order IN (3) THEN 1
    ELSE 0
  END,
  CASE
    WHEN cr.notes LIKE '%Daglig sjekk%' AND cti.sort_order = 3 THEN 'Pakninger ikke kontrollert enda'
    WHEN cr.notes LIKE '%alkoholkontroll%' AND cti.sort_order = 3 THEN 'Trenger oppfølging ved skiftbytte'
    ELSE 'Fullført'
  END,
  NOW(),
  NOW()
FROM checklist_run cr
JOIN checklist_template_item cti ON cti.template_id = cr.template_id
WHERE cr.org_number = 937219997;

-- ---------------------------------------------------------
-- DEVIATION REPORTS (ik-mat.json)
-- ---------------------------------------------------------

INSERT INTO deviation_report (
  org_number,
  reported_by_user_id,
  report_type,
  severity,
  title,
  description,
  location_id,
  location_text,
  occurred_date,
  occurred_time,
  report_date,
  immediate_action_text,
  corrective_action_text,
  status,
  created_at,
  updated_at,
  closed_at
)
VALUES
  (
    937219997,
    (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'),
    'DISCREPANCY',
    'MAJOR',
    'Kjøleskap temperaturen var høy',
    'Kjøleskapet på kjøkkenet registrerte 7C i stedet for 2-4C.',
    (SELECT location_id FROM location WHERE org_number = 937219997 AND name = 'Kjøleskap 1 (fersk fisk)'),
    'Kjøleskap 1 (fersk fisk)',
    '2024-05-28',
    '14:30:00',
    '2024-05-28',
    'Lukket kjøleskapet straks og justerte temperaturinnstillingen',
    'Kjøleskapet ble reparert og kalibrert',
    'CLOSED',
    NOW(),
    NOW(),
    NOW()
  ),
  (
    937219997,
    (SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'),
    'DISCREPANCY',
    'MINOR',
    'Pakking med skader funnet',
    'En pakke med ferskvare viste tegn på lekkasje.',
    (SELECT location_id FROM location WHERE org_number = 937219997 AND name = 'Kjøleskap 2 (kjøtt/fjærkre)'),
    'Kjøleskap 2 (kjøtt/fjærkre)',
    '2024-05-27',
    '09:15:00',
    '2024-05-27',
    'Kastet den skadede pakken',
    'Hever oppmerksomheten på pakningskvalitet ved mottak',
    'CORRECTIVE_ACTION_PLANNED',
    NOW(),
    NOW(),
    NULL
  );

-- ---------------------------------------------------------
-- DOCUMENTS (felles.json)
-- ---------------------------------------------------------

INSERT INTO organization_document (org_number, document_type, title, description, current_version, is_active, created_by_user_id, created_at, updated_at)
VALUES
  (937219997, 'REPORT_EXPORT', 'Månedlig kontrollrapport - Mai', 'Mai hadde 94 prosent samsvar. 2 avvik rapportert og løst.', 1, 1, (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'), NOW(), NOW()),
  (937219997, 'REPORT_EXPORT', 'Avvik og korreksjoner - Q2', '6 avvik rapportert i Q2, 5 løst, 1 under behandling', 1, 1, (SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'), NOW(), NOW()),
  (937219997, 'REPORT_EXPORT', 'HACCP Revisjon', 'Årlig HACCP revisjon og oppdatering av planen', 1, 1, (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'), NOW(), NOW()),
  (937219997, 'POLICY', 'Internkontrollplan for matsikkerhet', 'Organisasjonens IK-MAT plan i henhold til Matloven', 1, 1, (SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'), NOW(), NOW()),
  (937219997, 'POLICY', 'Alkoholloven - Retningslinjer og prosedyrer', 'Organisasjonens retningslinjer for alkoholutskjenking', 1, 1, (SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'), NOW(), NOW());

INSERT INTO organization_document_version (
  document_id,
  version_number,
  azure_container,
  azure_blob_name,
  original_filename,
  mime_type,
  file_size_bytes,
  blob_etag,
  uploaded_by_user_id,
  uploaded_at,
  valid_from,
  valid_to,
  checksum_sha256
)
SELECT
  d.document_id,
  1,
  'documents',
  CONCAT('org-937219997/', REPLACE(LOWER(d.title), ' ', '-'), '.pdf'),
  CONCAT(d.title, '.pdf'),
  'application/pdf',
  102400,
  NULL,
  d.created_by_user_id,
  NOW(),
  '2024-01-01',
  NULL,
  NULL
FROM organization_document d
WHERE d.org_number = 937219997;

-- ---------------------------------------------------------
-- TRAINING RECORDS (admin.json + ik-alkohol.json)
-- ---------------------------------------------------------

INSERT INTO training_record (
  user_id,
  org_number,
  training_type,
  title,
  completed_at,
  expires_at,
  status,
  certificate_document_id,
  notes,
  created_at
)
VALUES
  ((SELECT user_id FROM app_user WHERE email = 'admin@everest-sushi.no'), 937219997, 'FOOD_HYGIENE', 'Grunnleggende hygieneopplæring', '2024-01-15 10:00:00', '2027-03-01 00:00:00', 'COMPLETED', NULL, 'Fra opplæringsplan', NOW()),
  ((SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'), 937219997, 'RESPONSIBLE_ALCOHOL_SERVICE', 'Kunnskapsprøve alkoholloven', '2024-01-20 10:00:00', '2026-06-01 00:00:00', 'COMPLETED', NULL, 'Fra sertifiseringer', NOW()),
  ((SELECT user_id FROM app_user WHERE email = 'harald@oersonligmail.no'), 937219997, 'RESPONSIBLE_ALCOHOL_SERVICE', 'Kunnskapsprøve alkoholloven', '2024-02-01 10:00:00', '2029-12-12 00:00:00', 'COMPLETED', NULL, 'Fra sertifiseringer', NOW());

-- ---------------------------------------------------------
-- ORG SETTINGS
-- ---------------------------------------------------------

UPDATE organization_settings
SET
  enable_food_module = 1,
  enable_alcohol_module = 1,
  default_temp_min_c = 0,
  default_temp_max_c = 4,
  reminder_email_enabled = 1,
  notification_email = 'kari@everest-sushi.no'
WHERE org_number = 937219997;

SET FOREIGN_KEY_CHECKS = 1;
