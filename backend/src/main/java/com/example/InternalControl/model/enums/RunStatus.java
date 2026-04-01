package com.example.InternalControl.model.enums;

/**
 * Lifecycle states of a checklist run.
 * Maps to database ENUM('draft','in_progress','completed','overdue','cancelled').
 * Controls editing permissions and reporting.
 *
 * @author TriTacLe
 * @since 1.0
 */
public enum RunStatus {
    /** Initial state, can edit everything */
    DRAFT,

    /** Currently being filled out */
    IN_PROGRESS,

    /** All items answered, checklist complete */
    COMPLETED,

    /** Past due date without completion */
    OVERDUE,

    /** Cancelled by user/manager */
    CANCELLED
}
