package com.example.InternalControl.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Types of questions/items in a checklist.
 * Maps to database ENUM('BOOLEAN','TEXT','NUMBER','TEMPERATURE','CHOICE').
 * Determines validation rules and UI component.
 */
public enum ItemType {
    /** Yes/No answer */
    BOOLEAN("BOOLEAN"),

    TEXT("TEXT"),

    NUMBER("NUMBER"),

    /** Temperature with unit */
    TEMPERATURE("TEMPERATURE"),

    /** Multiple choice */
    CHOICE("CHOICE");

    private final String dbValue;

    ItemType(String dbValue) {
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
    public static ItemType fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase();
        for (ItemType t : values()) {
            if (t.dbValue.toLowerCase().equals(normalized) || t.name().toLowerCase().equals(normalized)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown item type: " + value);
    }
}
