package com.example.InternalControl.model.audit;

import com.example.InternalControl.model.user.AppUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * JPA Entity mapping to audit_log table.
 * Tracks all significant actions in the system for compliance.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id")
    private Long auditLogId;

    @Column(name = "org_number")
    private Integer orgNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acted_by_user_id")
    private AppUser actedByUser;

    @Column(name = "action_type", nullable = false, length = 100)
    private String actionType;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "old_values_json", columnDefinition = "JSON")
    private String oldValuesJson;

    @Column(name = "new_values_json", columnDefinition = "JSON")
    private String newValuesJson;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}