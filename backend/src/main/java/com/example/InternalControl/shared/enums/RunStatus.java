package com.example.InternalControl.shared.enums;

/**
 * Lifecycle states of a checklist run.
 * Controls editing permissions and reporting.
 *
 * @author TriTacLe
 * @since 1.0
 */
public enum RunStatus {
  DRAFT,

  IN_PROGRESS,

  COMPLETED,

  OVERDUE,

  CANCELLED
}
