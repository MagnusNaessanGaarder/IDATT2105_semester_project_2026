package com.example.InternalControl.model.export;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Format for export files.
 * Maps to database ENUM('PDF','JSON').
 */
public enum ExportFormat {
    PDF("PDF"),
    JSON("JSON");

    private final String dbValue;

    ExportFormat(String dbValue) {
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
    public static ExportFormat fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase();
        for (ExportFormat f : values()) {
            if (f.dbValue.toLowerCase().equals(normalized) || f.name().toLowerCase().equals(normalized)) {
                return f;
            }
        }
        throw new IllegalArgumentException("Unknown export format: " + value);
    }
}
