package com.example.InternalControl.model.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests for DeviationStatus enum.
 */
class DeviationStatusTest {

  @Test
  void shouldGetValue() {
    assertEquals("DRAFT", DeviationStatus.DRAFT.getValue());
    assertEquals("REPORTED", DeviationStatus.REPORTED.getValue());
    assertEquals("CLOSED", DeviationStatus.CLOSED.getValue());
  }

  @Test
  void shouldGetDbValue() {
    assertEquals("DRAFT", DeviationStatus.DRAFT.getDbValue());
    assertEquals("REPORTED", DeviationStatus.REPORTED.getDbValue());
    assertEquals("CLOSED", DeviationStatus.CLOSED.getDbValue());
  }

  @Test
  void shouldConvertFromValue() {
    assertEquals(DeviationStatus.DRAFT, DeviationStatus.fromValue("DRAFT"));
    assertEquals(DeviationStatus.DRAFT, DeviationStatus.fromValue("draft"));
    assertEquals(DeviationStatus.REPORTED, DeviationStatus.fromValue("REPORTED"));
    assertEquals(DeviationStatus.REPORTED, DeviationStatus.fromValue("reported"));
    assertEquals(DeviationStatus.CLOSED, DeviationStatus.fromValue("CLOSED"));
    assertEquals(DeviationStatus.CLOSED, DeviationStatus.fromValue("closed"));
  }

  @Test
  void shouldReturnNullForNullValue() {
    assertNull(DeviationStatus.fromValue(null));
  }

  @Test
  void shouldThrowForUnknownValue() {
    assertThrows(IllegalArgumentException.class, () -> DeviationStatus.fromValue("UNKNOWN"));
  }

  @Test
  void shouldHaveAllStatuses() {
    DeviationStatus[] statuses = DeviationStatus.values();
    assertEquals(6, statuses.length);
  }
}
