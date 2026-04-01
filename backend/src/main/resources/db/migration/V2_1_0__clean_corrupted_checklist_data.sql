-- Clean up corrupted checklist data with lowercase enum values
-- This fixes the issue where test data was created with 'daily' instead of 'DAILY'

-- Delete all checklist run items (child records)
DELETE FROM checklist_run_item WHERE run_id IN (
    SELECT run_id FROM checklist_run WHERE template_id IN (
        SELECT template_id FROM checklist_template 
        WHERE frequency NOT IN ('DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')
    )
);

-- Delete all checklist runs with corrupted templates
DELETE FROM checklist_run WHERE template_id IN (
    SELECT template_id FROM checklist_template 
    WHERE frequency NOT IN ('DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')
);

-- Delete corrupted checklist template items
DELETE FROM checklist_template_item WHERE template_id IN (
    SELECT template_id FROM checklist_template 
    WHERE frequency NOT IN ('DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')
);

-- Delete corrupted checklist templates
DELETE FROM checklist_template 
WHERE frequency NOT IN ('DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY');
