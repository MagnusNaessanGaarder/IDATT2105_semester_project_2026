package com.example.InternalControl.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Type of deviation report.
 * Maps to database ENUM('INCIDENT','DISCREPANCY').
 */
public enum ReportType {
    INCIDENT("INCIDENT"),
    DISCREPANCY("DISCREPANCY");

    private final String dbValue;

    ReportType(String dbValue) {
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
    public static ReportType fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase();
        for (ReportType t : values()) {
            if (t.dbValue.toLowerCase().equals(normalized) || t.name().toLowerCase().equals(normalized)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown report type: " + value);
    }
}
