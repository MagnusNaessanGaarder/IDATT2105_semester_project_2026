package com.example.InternalControl.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * How often a checklist should be performed.
 * Maps to database ENUM('DAILY','WEEKLY','MONTHLY','CUSTOM').
 * Used by scheduler for auto-generation.
 */
public enum Frequency {
  DAILY("DAILY"),
  WEEKLY("WEEKLY"),
  MONTHLY("MONTHLY"),
  CUSTOM("CUSTOM");

  private final String dbValue;

  Frequency(String dbValue) {
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
  public static Frequency fromValue(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.toLowerCase();
    for (Frequency f : values()) {
      if (f.dbValue.toLowerCase().equals(normalized) || f.name().toLowerCase().equals(normalized)) {
        return f;
      }
    }
    throw new IllegalArgumentException("Unknown frequency: " + value);
  }
}
