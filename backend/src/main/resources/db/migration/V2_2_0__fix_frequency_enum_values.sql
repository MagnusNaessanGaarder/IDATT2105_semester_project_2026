-- Fix corrupted frequency enum values in checklist templates
-- Convert lowercase 'daily' to uppercase 'DAILY'

-- First, delete all related child records (runs and items)
DELETE FROM checklist_run_item WHERE run_id IN (
    SELECT run_id FROM checklist_run WHERE template_id IN (
        SELECT template_id FROM checklist_template WHERE LOWER(frequency) = 'daily'
    )
);

DELETE FROM checklist_run WHERE template_id IN (
    SELECT template_id FROM checklist_template WHERE LOWER(frequency) = 'daily'
);

DELETE FROM checklist_template_item WHERE template_id IN (
    SELECT template_id FROM checklist_template WHERE LOWER(frequency) = 'daily'
);

-- Now delete the corrupted templates
DELETE FROM checklist_template WHERE LOWER(frequency) = 'daily';
