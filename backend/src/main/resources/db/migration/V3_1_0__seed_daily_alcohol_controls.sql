-- Seed additional daily alcohol controls for Everest Sushi

-- ---------------------------------------------------------
-- ALCOHOL TEMPLATE ITEMS
-- ---------------------------------------------------------

INSERT INTO checklist_template_item (
    template_id,
    sort_order,
    label,
    description,
    item_type,
    is_required,
    expected_text,
    expected_numeric_min,
    expected_numeric_max,
    choice_options_json
)
SELECT t.template_id,
       5,
       'Kontroll av minimumsalder (≥ 18 år)',
       'Bekreft at alle gjester som serveres alkohol oppfyller alderskravet',
       'BOOLEAN',
       1,
       NULL,
       NULL,
       NULL,
       NULL
FROM checklist_template t
WHERE t.org_number = 937219997
  AND t.title = 'Daglig alkoholkontroll'
  AND NOT EXISTS (
      SELECT 1
      FROM checklist_template_item cti
      WHERE cti.template_id = t.template_id
        AND cti.sort_order = 5
  );

INSERT INTO checklist_template_item (
    template_id,
    sort_order,
    label,
    description,
    item_type,
    is_required,
    expected_text,
    expected_numeric_min,
    expected_numeric_max,
    choice_options_json
)
SELECT t.template_id,
       6,
       'Lukking av bar: Sluttkontroll og rapportering',
       'Bekreft at baren er kontrollert ved stenging og at rapport er sendt',
       'BOOLEAN',
       1,
       NULL,
       NULL,
       NULL,
       NULL
FROM checklist_template t
WHERE t.org_number = 937219997
  AND t.title = 'Daglig alkoholkontroll'
  AND NOT EXISTS (
      SELECT 1
      FROM checklist_template_item cti
      WHERE cti.template_id = t.template_id
        AND cti.sort_order = 6
  );

INSERT INTO checklist_template_item (
    template_id,
    sort_order,
    label,
    description,
    item_type,
    is_required,
    expected_text,
    expected_numeric_min,
    expected_numeric_max,
    choice_options_json
)
SELECT t.template_id,
       7,
       'Sjekk kjøl',
       'Bekreft at kjøl for drikkevarer holder stabil temperatur og er ryddig',
       'BOOLEAN',
       1,
       NULL,
       NULL,
       NULL,
       NULL
FROM checklist_template t
WHERE t.org_number = 937219997
  AND t.title = 'Daglig alkoholkontroll'
  AND NOT EXISTS (
      SELECT 1
      FROM checklist_template_item cti
      WHERE cti.template_id = t.template_id
        AND cti.sort_order = 7
  );

INSERT INTO checklist_template_item (
    template_id,
    sort_order,
    label,
    description,
    item_type,
    is_required,
    expected_text,
    expected_numeric_min,
    expected_numeric_max,
    choice_options_json
)
SELECT t.template_id,
       8,
       'Sjekk beholdning',
       'Kontroller at beholdning av alkohol stemmer med forventet nivå og registrering',
       'BOOLEAN',
       1,
       NULL,
       NULL,
       NULL,
       NULL
FROM checklist_template t
WHERE t.org_number = 937219997
  AND t.title = 'Daglig alkoholkontroll'
  AND NOT EXISTS (
      SELECT 1
      FROM checklist_template_item cti
      WHERE cti.template_id = t.template_id
        AND cti.sort_order = 8
  );

-- ---------------------------------------------------------
-- ALCOHOL CHECKLIST RUN
-- ---------------------------------------------------------

INSERT INTO checklist_run (
    template_id,
    org_number,
    location_id,
    performed_by_user_id,
    assigned_to_user_id,
    run_date,
    due_at,
    completed_at,
    status,
    notes,
    created_at,
    updated_at
)
SELECT t.template_id,
       937219997,
       (SELECT location_id FROM location WHERE org_number = 937219997 AND name = 'Bar'),
       (SELECT user_id FROM app_user WHERE email = 'harald@oersonligmail.no'),
       (SELECT user_id FROM app_user WHERE email = 'manager@everest-sushi.no'),
       '2026-04-08',
       '2026-04-08 23:59:59',
       NULL,
       'DRAFT',
       'Daglig alkoholkontroll for kveldsskift',
       NOW(),
       NOW()
FROM checklist_template t
WHERE t.org_number = 937219997
  AND t.title = 'Daglig alkoholkontroll'
  AND NOT EXISTS (
      SELECT 1
      FROM checklist_run cr
      WHERE cr.template_id = t.template_id
        AND cr.org_number = 937219997
        AND cr.run_date = '2026-04-08'
        AND cr.status = 'DRAFT'
  );

-- ---------------------------------------------------------
-- ALCOHOL CHECKLIST RUN ITEMS
-- ---------------------------------------------------------

INSERT INTO checklist_run_item (
    run_id,
    template_item_id,
    boolean_value,
    text_value,
    numeric_value,
    selected_choice,
    is_deviation,
    comment_text,
    created_at,
    updated_at
)
SELECT cr.run_id,
       cti.item_id,
       CASE cti.sort_order
           WHEN 1 THEN 1
           WHEN 2 THEN 1
           WHEN 3 THEN 0
           WHEN 4 THEN 1
           WHEN 5 THEN 0
           WHEN 6 THEN 0
           WHEN 7 THEN 1
           WHEN 8 THEN 1
           ELSE NULL
       END,
       NULL,
       NULL,
       NULL,
       CASE cti.sort_order
           WHEN 3 THEN 1
           WHEN 5 THEN 1
           ELSE 0
       END,
       CASE cti.sort_order
           WHEN 1 THEN 'Kontrollert legitimasjon ved åpning. Ingen tegn til beruselse.'
           WHEN 2 THEN 'Alle gjester under 25 år ble bedt om legitimasjon.'
           WHEN 3 THEN 'En gjest måtte følges opp etter tegn på høy promille.'
           WHEN 4 THEN 'Sjenkestopp ble kommunisert tydelig til alle ansatte.'
           WHEN 5 THEN 'To gjester måtte vise ekstra legitimasjon før servering.'
           WHEN 6 THEN 'Sluttkontroll og rapportering gjenstår ved stenging.'
           WHEN 7 THEN 'Kjøl ble kontrollert. Temperatur og plassering av varer var i orden.'
           WHEN 8 THEN 'Beholdning stemmer mot registrert lager ved skiftstart.'
           ELSE 'Daglig kontroll registrert'
       END,
       NOW(),
       NOW()
FROM checklist_run cr
JOIN checklist_template_item cti ON cti.template_id = cr.template_id
WHERE cr.org_number = 937219997
  AND cr.run_date = '2026-04-08'
  AND cr.status = 'DRAFT'
  AND cr.template_id = (
      SELECT template_id
      FROM checklist_template
      WHERE org_number = 937219997
        AND title = 'Daglig alkoholkontroll'
      LIMIT 1
  )
  AND NOT EXISTS (
      SELECT 1
      FROM checklist_run_item cri
      WHERE cri.run_id = cr.run_id
        AND cri.template_item_id = cti.item_id
  );
