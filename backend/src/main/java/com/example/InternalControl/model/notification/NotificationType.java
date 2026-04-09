package com.example.InternalControl.model.notification;

/**
 * Categories of notifications that can be sent to users.
 *
 * @author TriTacLe
 * @since 1.0
 */
public enum NotificationType {
    TASK_OVERDUE,
    TEMPERATURE_ALERT,
    DEVIATION_ASSIGNED,
    DEVIATION_STATUS_CHANGED,
    TRAINING_EXPIRING,
    DOCUMENT_UPLOADED,
    GENERAL
}
