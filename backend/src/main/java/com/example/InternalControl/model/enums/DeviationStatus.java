package com.example.InternalControl.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Lifecycle states of a deviation report.
 * Maps to database ENUM values.
 */
public enum DeviationStatus {
    DRAFT("DRAFT"),
    REPORTED("REPORTED"),
    UNDER_INVESTIGATION("UNDER_INVESTIGATION"),
    CORRECTIVE_ACTION_PLANNED("CORRECTIVE_ACTION_PLANNED"),
    CORRECTIVE_ACTION_COMPLETED("CORRECTIVE_ACTION_COMPLETED"),
    CLOSED("CLOSED");

    private final String dbValue;

    DeviationStatus(String dbValue) {
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
    public static DeviationStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase();
        for (DeviationStatus s : values()) {
            if (s.dbValue.toLowerCase().equals(normalized) || s.name().toLowerCase().equals(normalized)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown deviation status: " + value);
    }
}
