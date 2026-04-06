package com.example.InternalControl.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Different types of locations within an organization.
 * Maps to database ENUM values (UPPERCASE).
 */
public enum LocationType {
    KITCHEN("KITCHEN"),
    BAR("BAR"),
    FREEZER("FREEZER"),
    FRIDGE("FRIDGE"),
    STORAGE("STORAGE"),
    SERVING_AREA("SERVING_AREA"),
    HOT_FOOD("HOT_FOOD"),
    OTHER("OTHER");

    private final String dbValue;

    LocationType(String dbValue) {
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
    public static LocationType fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase();
        for (LocationType t : values()) {
            if (t.dbValue.toLowerCase().equals(normalized) || t.name().toLowerCase().equals(normalized)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown location type: " + value);
    }
}
