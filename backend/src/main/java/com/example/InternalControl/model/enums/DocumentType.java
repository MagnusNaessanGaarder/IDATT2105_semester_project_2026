package com.example.InternalControl.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Document types for organization documents.
 * Maps to database ENUM('POLICY','PROCEDURE','TRAINING_MATERIAL','CERTIFICATE','ATTACHMENT','REPORT_EXPORT','OTHER').
 */
public enum DocumentType {
    POLICY("POLICY"),
    PROCEDURE("PROCEDURE"),
    TRAINING_MATERIAL("TRAINING_MATERIAL"),
    CERTIFICATE("CERTIFICATE"),
    ATTACHMENT("ATTACHMENT"),
    REPORT_EXPORT("REPORT_EXPORT"),
    OTHER("OTHER");

    private final String dbValue;

    DocumentType(String dbValue) {
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
    public static DocumentType fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase();
        for (DocumentType t : values()) {
            if (t.dbValue.toLowerCase().equals(normalized) || t.name().toLowerCase().equals(normalized)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown document type: " + value);
    }
}
