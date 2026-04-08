package com.example.InternalControl.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Lifecycle states of a checklist run.
 * Maps to database ENUM('DRAFT','IN_PROGRESS','COMPLETED','OVERDUE','CANCELLED').
 * Controls editing permissions and reporting.
 */
public enum RunStatus {
    /** Initial state, can edit everything */
    DRAFT("DRAFT"),

    /** Currently being filled out */
    IN_PROGRESS("IN_PROGRESS"),

    /** All items answered, checklist complete */
    COMPLETED("COMPLETED"),

    /** Past due date without completion */
    OVERDUE("OVERDUE"),

    /** Cancelled by user/manager */
    CANCELLED("CANCELLED");

    private final String dbValue;

    RunStatus(String dbValue) {
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
    public static RunStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase();
        for (RunStatus s : values()) {
            if (s.dbValue.toLowerCase().equals(normalized) || s.name().toLowerCase().equals(normalized)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown run status: " + value);
    }
}
