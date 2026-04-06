package com.example.InternalControl.model.export;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Status of an export job.
 * Maps to database ENUM('PENDING','RUNNING','COMPLETED','FAILED').
 */
public enum ExportStatus {
    PENDING("PENDING"),
    RUNNING("RUNNING"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");

    private final String dbValue;

    ExportStatus(String dbValue) {
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
    public static ExportStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase();
        for (ExportStatus s : values()) {
            if (s.dbValue.toLowerCase().equals(normalized) || s.name().toLowerCase().equals(normalized)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown export status: " + value);
    }
}
