package com.example.InternalControl.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Severity level of a deviation.
 * Maps to database ENUM('MINOR','MAJOR','CRITICAL').
 */
public enum Severity {
    MINOR("MINOR"),
    MAJOR("MAJOR"),
    CRITICAL("CRITICAL");

    private final String dbValue;

    Severity(String dbValue) {
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
    public static Severity fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase();
        for (Severity s : values()) {
            if (s.dbValue.toLowerCase().equals(normalized) || s.name().toLowerCase().equals(normalized)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown severity: " + value);
    }
}
