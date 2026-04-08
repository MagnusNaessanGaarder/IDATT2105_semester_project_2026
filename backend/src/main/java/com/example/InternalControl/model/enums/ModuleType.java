package com.example.InternalControl.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Maps to database ENUM('FOOD','ALCOHOL').
 */
public enum ModuleType {
    FOOD("FOOD"),
    ALCOHOL("ALCOHOL");

    private final String dbValue;

    ModuleType(String dbValue) {
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
    public static ModuleType fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase();
        for (ModuleType m : values()) {
            if (m.dbValue.toLowerCase().equals(normalized) || m.name().toLowerCase().equals(normalized)) {
                return m;
            }
        }
        throw new IllegalArgumentException("Unknown module type: " + value);
    }
}
