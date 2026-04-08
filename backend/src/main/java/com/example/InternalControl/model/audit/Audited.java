package com.example.InternalControl.model.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should be audited.
 * Used with AuditLogAspect to automatically log write operations.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /**
     * The type of action being performed (CREATE, UPDATE, DELETE).
     */
    ActionType action();

    /**
     * The entity type name (e.g., "ChecklistRun", "DeviationReport").
     */
    String entityType();

    /**
     * SpEL expression to extract the entity ID from the return value or arguments.
     * Default empty means no entity ID will be logged.
     */
    String entityId() default "";
}
