package com.example.InternalControl.model.enums;

/**
 * Lifecycle states of a deviation report.
 *
 * @author TriTacLe
 * @since 1.0
 */
public enum DeviationStatus {
    draft,
    reported,
    under_investigation,
    corrective_action_planned,
    corrective_action_completed,
    closed
}
