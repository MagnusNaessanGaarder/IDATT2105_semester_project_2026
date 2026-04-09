package com.example.InternalControl.model.notification;

/**
 * Types of entities that can be linked to a notification for context.
 *
 * @author TriTacLe
 * @since 1.0
 */
public enum RelatedEntityType {
    CHECKLIST_RUN,
    TEMPERATURE_LOG_ENTRY,
    DEVIATION_REPORT,
    TRAINING_RECORD,
    ORGANIZATION_DOCUMENT,
    EXPORT_JOB,
    OTHER
}
