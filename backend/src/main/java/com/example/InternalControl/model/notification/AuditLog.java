package com.example.InternalControl.model.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author TriTacLe
 * @since 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "audit_log")
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "audit_log_id")
  private Long auditLogId;

  @Column(name = "org_number")
  private Integer orgNumber;

  @Column(name = "acted_by_user_id")
  private Long actedByUserId;

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

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
