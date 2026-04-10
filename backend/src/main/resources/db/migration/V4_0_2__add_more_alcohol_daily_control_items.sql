
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
       'Skilting og informasjon om aldersgrense synlig',
       'Bekreft at skilt om 18/20-årsgrense er synlige ved inngang og bar',
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
       'Avvik og hendelser fra forrige skift gjennomgått',
       'Sjekk at tidligere hendelser er lest og fulgt opp før ny servering starter',
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
       'Ansvarsvakt og bemanning bekreftet for skiftet',
       'Bekreft at ansvarlig vakt er satt opp og at bemanningen er tilstrekkelig',
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
       1,
       NULL,
       NULL,
       NULL,
       0,
       'Fullført',
       NOW(),
       NOW()
FROM checklist_run cr
JOIN checklist_template t
  ON t.template_id = cr.template_id
JOIN checklist_template_item cti
  ON cti.template_id = t.template_id
WHERE cr.org_number = 937219997
  AND t.title = 'Daglig alkoholkontroll'
  AND cti.sort_order IN (5, 6, 7)
  AND NOT EXISTS (
    SELECT 1
    FROM checklist_run_item cri
    WHERE cri.run_id = cr.run_id
      AND cri.template_item_id = cti.item_id
  );
