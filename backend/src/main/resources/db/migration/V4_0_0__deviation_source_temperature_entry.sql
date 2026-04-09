ALTER TABLE deviation_report
    ADD COLUMN source_temperature_entry_id BIGINT NULL AFTER location_text;

CREATE INDEX ix_deviation_source_temperature_entry
    ON deviation_report (org_number, source_temperature_entry_id);
