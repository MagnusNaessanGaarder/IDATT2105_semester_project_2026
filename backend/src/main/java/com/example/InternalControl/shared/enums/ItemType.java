package com.example.InternalControl.shared.enums;

/**
 * Types of questions/items in a checklist.
 * Determines validation rules and UI component.
 *
 * @author TriTacLe
 * @since 1.0
 */
public enum ItemType {
  /** Yes/No answer */
  BOOLEAN,

  TEXT,

  NUMBER,

  /** Temperature with unit */
  TEMPERATURE,

  /** Multiple choice */
  CHOICE
}
