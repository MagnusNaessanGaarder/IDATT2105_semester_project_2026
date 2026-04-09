package com.example.InternalControl.model.audit;

/**
 * Types of actions that can be logged in the audit trail.
 *
 * @author TriTacLe
 * @since 1.0
 */
public enum ActionType {
    CREATE,
    UPDATE,
    DELETE,
    LOGIN,
    LOGOUT,
    EXPORT,
    VIEW
}
