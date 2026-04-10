-- Extra demo data for presentations - more checklists, tasks, deviations, temps

-- Get the first org number and admin user
SET @org_number = (SELECT org_number FROM organization LIMIT 1);
SET @admin_user = (SELECT user_id FROM app_user LIMIT 1);

-- Insert new IK-MAT checklists
INSERT INTO checklist_template (org_number, module_type, title, description, frequency, is_active, created_by_user_id) VALUES
(@org_number, 'FOOD', 'Daglig rengjøringskontroll', 'Sjekkliste for daglig rengjøring av kjøkken og produksjonsområder', 'DAILY', 1, @admin_user),
(@org_number, 'FOOD', 'Ukentlig varemottak', 'Kontroll av temperatur og kvalitet ved varemottak', 'WEEKLY', 1, @admin_user),
(@org_number, 'FOOD', 'Daglig allergen-kontroll', 'Sjekk av allergenmerking og separering', 'DAILY', 1, @admin_user),
(@org_number, 'FOOD', 'Ukentlig vedlikeholdsrunde', 'Sjekk av utstyr og rapportering av behov', 'WEEKLY', 1, @admin_user),
(@org_number, 'ALCOHOL', 'Ukentlig alkoholbeholdning', 'Opptelling og kontroll av alkoholbeholdning', 'WEEKLY', 1, @admin_user),
(@org_number, 'ALCOHOL', 'Månedlig skjenkekontroll', 'Kontroll av skjenkebevilling og aldersgrenser', 'MONTHLY', 1, @admin_user);

-- Get the ID of the first inserted checklist
SET @rengjoring_id = LAST_INSERT_ID();

-- Add tasks to Daglig rengjøringskontroll
INSERT INTO checklist_template_item (template_id, sort_order, label, description, item_type, is_required) VALUES
(@rengjoring_id, 1, 'Gulv rengjort og tørt', 'Sjekk at gulvet er rengjort og tørt', 'BOOLEAN', 1),
(@rengjoring_id, 2, 'Benkeflater desinfisert', 'Alle benkeflater må være desinfisert', 'BOOLEAN', 1),
(@rengjoring_id, 3, 'Oppvaskmaskin rengjort', 'Filter og kammer rengjort', 'BOOLEAN', 1),
(@rengjoring_id, 4, 'Søppel tømt og ny pose i', 'Søppel tømt, ny pose i alle bøtter', 'BOOLEAN', 1),
(@rengjoring_id, 5, 'Håndvask fylt med såpe', 'Såpedispenser fylt opp', 'BOOLEAN', 0),
(@rengjoring_id, 6, 'Avløp renset', 'Avløp i gulv og vask renset', 'BOOLEAN', 0);

-- Tasks for varemottak
SET @varemottak_id = @rengjoring_id + 1;
INSERT INTO checklist_template_item (template_id, sort_order, label, description, item_type, is_required) VALUES
(@varemottak_id, 1, 'Kjølevarer under 4°C', 'Mål temperatur på kjølevarer', 'TEMPERATURE', 1),
(@varemottak_id, 2, 'Frysevarer under -18°C', 'Mål temperatur på frysevarer', 'TEMPERATURE', 1),
(@varemottak_id, 3, 'Emballasje intakt', 'Ingen skader på emballasje', 'BOOLEAN', 1),
(@varemottak_id, 4, 'Datoer sjekket', 'Sjekk at varer er innen holdbarhet', 'BOOLEAN', 1),
(@varemottak_id, 5, 'Antall stemmer med følgeseddel', 'Kontroller antall mot følgeseddel', 'BOOLEAN', 1);

-- Tasks for allergen-kontroll
SET @allergen_id = @rengjoring_id + 2;
INSERT INTO checklist_template_item (template_id, sort_order, label, description, item_type, is_required) VALUES
(@allergen_id, 1, 'Allergenmerking synlig', 'Sjekk at all mat er korrekt merket', 'BOOLEAN', 1),
(@allergen_id, 2, 'Separering av allergener', 'Allergener lagres separat', 'BOOLEAN', 1),
(@allergen_id, 3, 'Rengjøringsprosedyrer', 'Rengjøring etter allergenprotokoll', 'BOOLEAN', 1),
(@allergen_id, 4, 'Personell informert', 'Nytt personell informert om allergenrutiner', 'BOOLEAN', 0);

-- Tasks for vedlikeholdsrunde
SET @vedlikehold_id = @rengjoring_id + 3;
INSERT INTO checklist_template_item (template_id, sort_order, label, description, item_type, is_required) VALUES
(@vedlikehold_id, 1, 'Kniver og skjærebrett', 'Sjekk skarphet og slitasje', 'BOOLEAN', 1),
(@vedlikehold_id, 2, 'Panner og gryter', 'Sjekk håndtak og belegg', 'BOOLEAN', 1),
(@vedlikehold_id, 3, 'Kjøkkenmaskiner', 'Sjekk at alt fungerer', 'BOOLEAN', 1),
(@vedlikehold_id, 4, 'Lysarmaturer', 'Bytt ødelagte pærer', 'BOOLEAN', 0);

-- ============================================
-- Deviation Reports (Avvik)
-- ============================================

INSERT INTO deviation_report (org_number, reported_by_user_id, report_type, severity, title, description, location_text, status, report_date, occurred_date) VALUES
(@org_number, @admin_user, 'INCIDENT', 'CRITICAL', 'Kjøleskap for varmt', 'Kjøleskap 2 viste 6°C ved morgenrunde. Termostat justert ned.', 'Kjøkken', 'REPORTED', CURDATE(), CURDATE() - INTERVAL 2 DAY),
(@org_number, @admin_user, 'DISCREPANCY', 'MINOR', 'Manglende hansker', 'Oppdaget at hanskebeholder ved prep-benk var tom ved start av vakt.', 'Prep-avdeling', 'CLOSED', CURDATE(), CURDATE() - INTERVAL 5 DAY),
(@org_number, @admin_user, 'DISCREPANCY', 'MINOR', 'Rengjøring etter lukketid', 'Gulvet i kokeavdeling ikke tørt ved inspeksjon etter stengetid.', 'Kokeavdeling', 'REPORTED', CURDATE(), CURDATE() - INTERVAL 1 DAY),
(@org_number, @admin_user, 'INCIDENT', 'MAJOR', 'Vare med kort dato', 'Ost levering med kun 2 dager igjen til best før dato.', 'Lager', 'CLOSED', CURDATE(), CURDATE() - INTERVAL 3 DAY),
(@org_number, @admin_user, 'INCIDENT', 'CRITICAL', 'Termometer viser feil', 'Håndholdt termometer avviker 2 grader fra stasjonært termometer.', 'Kjøkken', 'REPORTED', CURDATE(), CURDATE() - INTERVAL 1 DAY);

-- ============================================
-- Temperature Logs
-- ============================================

-- Get location
SET @location_id = (SELECT location_id FROM location WHERE org_number = @org_number LIMIT 1);

-- Create temperature log points
INSERT INTO temperature_log_point (org_number, location_id, name, is_active) VALUES
(@org_number, @location_id, 'Kjøleskap hoved', 1),
(@org_number, @location_id, 'Kjøleskap prep', 1),
(@org_number, @location_id, 'Fryser lager', 1),
(@org_number, @location_id, 'Fryser dessert', 1),
(@org_number, @location_id, 'Varmeskap buffet', 1);

-- Get the first point ID
SET @point1 = LAST_INSERT_ID();
SET @point2 = @point1 + 1;
SET @point3 = @point1 + 2;
SET @point4 = @point1 + 3;
SET @point5 = @point1 + 4;

-- Add temperature readings
INSERT INTO temperature_log_entry (org_number, log_point_id, recorded_by_user_id, measured_at, temperature_c, is_alert, note_text) VALUES
(@org_number, @point1, @admin_user, NOW() - INTERVAL 6 DAY, 2.5, 0, 'Morgenrunde - OK'),
(@org_number, @point1, @admin_user, NOW() - INTERVAL 5 DAY, 3.1, 0, 'Morgenrunde - OK'),
(@org_number, @point1, @admin_user, NOW() - INTERVAL 4 DAY, 2.8, 0, 'Morgenrunde - OK'),
(@org_number, @point1, @admin_user, NOW() - INTERVAL 3 DAY, 3.5, 0, 'Morgenrunde - OK'),
(@org_number, @point1, @admin_user, NOW() - INTERVAL 2 DAY, 2.9, 0, 'Morgenrunde - OK'),
(@org_number, @point1, @admin_user, NOW() - INTERVAL 1 DAY, 3.2, 0, 'Morgenrunde - OK'),
(@org_number, @point2, @admin_user, NOW() - INTERVAL 6 DAY, 3.2, 0, 'Morgenrunde - OK'),
(@org_number, @point2, @admin_user, NOW() - INTERVAL 5 DAY, 2.9, 0, 'Morgenrunde - OK'),
(@org_number, @point2, @admin_user, NOW() - INTERVAL 4 DAY, 6.2, 1, 'AVVIK: Dør stått åpen - justert'),
(@org_number, @point2, @admin_user, NOW() - INTERVAL 3 DAY, 3.1, 0, 'Morgenrunde - OK'),
(@org_number, @point2, @admin_user, NOW() - INTERVAL 2 DAY, 2.7, 0, 'Morgenrunde - OK'),
(@org_number, @point2, @admin_user, NOW() - INTERVAL 1 DAY, 3.0, 0, 'Morgenrunde - OK'),
(@org_number, @point3, @admin_user, NOW() - INTERVAL 6 DAY, -19.5, 0, 'Morgenrunde - OK'),
(@org_number, @point3, @admin_user, NOW() - INTERVAL 5 DAY, -20.1, 0, 'Morgenrunde - OK'),
(@org_number, @point3, @admin_user, NOW() - INTERVAL 4 DAY, -19.8, 0, 'Morgenrunde - OK'),
(@org_number, @point3, @admin_user, NOW() - INTERVAL 3 DAY, -20.3, 0, 'Morgenrunde - OK'),
(@org_number, @point3, @admin_user, NOW() - INTERVAL 2 DAY, -19.7, 0, 'Morgenrunde - OK'),
(@org_number, @point3, @admin_user, NOW() - INTERVAL 1 DAY, -20.0, 0, 'Morgenrunde - OK');

-- Notifications
INSERT INTO notification (org_number, user_id, notification_type, title, body_text, related_entity_type, is_read) VALUES
(@org_number, @admin_user, 'DEVIATION_ASSIGNED', 'Nytt avvik registrert', 'Kjøleskap for varmt avvik oppdaget og registrert', 'DEVIATION_REPORT', 0),
(@org_number, @admin_user, 'TASK_OVERDUE', 'Sjekkliste pågår', 'Daglig rengjøringskontroll påbegynt', 'CHECKLIST_RUN', 0),
(@org_number, @admin_user, 'TEMPERATURE_ALERT', 'Temperaturalert', 'Kjøleskap prep var over grenseverdi (6.2°C)', 'TEMPERATURE_LOG_ENTRY', 0);
