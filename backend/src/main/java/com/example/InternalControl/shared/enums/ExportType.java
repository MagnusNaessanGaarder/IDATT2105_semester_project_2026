package com.example.InternalControl.shared.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Type of export/report job.
 * Maps to database ENUM('AUDIT_REPORT','CHECKLIST_REPORT','TEMPERATURE_REPORT','DEVIATION_REPORT','TRAINING_REPORT','FULL_COMPLIANCE_REPORT').
 */
public enum ExportType {
    AUDIT_REPORT("AUDIT_REPORT"),
    CHECKLIST_REPORT("CHECKLIST_REPORT"),
    TEMPERATURE_REPORT("TEMPERATURE_REPORT"),
    DEVIATION_REPORT("DEVIATION_REPORT"),
    TRAINING_REPORT("TRAINING_REPORT"),
    FULL_COMPLIANCE_REPORT("FULL_COMPLIANCE_REPORT");

    private final String dbValue;

    ExportType(String dbValue) {
        this.dbValue = dbValue;
    }

    @JsonValue
    public String getValue() {
        return name();
    }

    public String getDbValue() {
        return dbValue;
    }

    @JsonCreator
    public static ExportType fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase();
        for (ExportType t : values()) {
            if (t.dbValue.toLowerCase().equals(normalized) || t.name().toLowerCase().equals(normalized)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown export type: " + value);
    }
}
