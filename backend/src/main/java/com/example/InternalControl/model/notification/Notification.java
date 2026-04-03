package com.example.InternalControl.model.notification;

import com.example.InternalControl.shared.enums.NotificationType;
import com.example.InternalControl.shared.enums.RelatedEntityType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "notification")
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "notification_id")
  private Long notificationId;

  @Column(name = "org_number")
  private Integer orgNumber;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "notification_type", nullable = false)
  private NotificationType notificationType;

  @Column(name = "title", nullable = false, length = 255)
  private String title;

  @Column(name = "body_text", nullable = false, columnDefinition = "TEXT")
  private String bodyText;

  @Enumerated(EnumType.STRING)
  @Column(name = "related_entity_type")
  private RelatedEntityType relatedEntityType;

  @Column(name = "related_entity_id")
  private Long relatedEntityId;

  @Column(name = "is_read", nullable = false)
  private Boolean isRead = false;

  @Column(name = "read_at")
  private LocalDateTime readAt;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
