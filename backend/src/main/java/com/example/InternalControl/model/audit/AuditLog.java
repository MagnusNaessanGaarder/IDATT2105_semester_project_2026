package com.example.InternalControl.model.audit;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "audit_log", indexes = {
    @Index(name = "ix_audit_log_org_time", columnList = "org_number, created_at"),
    @Index(name = "ix_audit_log_entity", columnList = "entity_type, entity_id"),
    @Index(name = "ix_audit_log_action", columnList = "action_type, created_at")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id")
    private Long auditLogId;

    @Column(name = "org_number")
    private Integer orgNumber;

    @Column(name = "acted_by_user_id")
    private Long actedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 100)
    private ActionType actionType;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "old_values_json", columnDefinition = "JSON")
    private String oldValuesJson;

    @Column(name = "new_values_json", columnDefinition = "JSON")
    private String newValuesJson;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
