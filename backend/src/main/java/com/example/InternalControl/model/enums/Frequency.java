package com.example.InternalControl.model.enums;

/**
 * How often a checklist should be performed.
 * Maps to database ENUM('daily','weekly','monthly','custom').
 * Used by scheduler for auto-generation.
 *
 * @author TriTacLe
 * @since 1.0
 */
public enum Frequency {
  DAILY,

  WEEKLY,

  MONTHLY,

  CUSTOM
}
