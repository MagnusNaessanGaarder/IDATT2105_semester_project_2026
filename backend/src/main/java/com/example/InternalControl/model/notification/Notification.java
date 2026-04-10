package com.example.InternalControl.model.notification;

import com.example.InternalControl.model.user.AppUser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * JPA Entity mapping to notification table.
 * Represents system notifications for users.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "notification_id")
  private Long notificationId;

  @Column(name = "org_number")
  private Integer orgNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  @JsonIgnore
  private AppUser user;

  @Column(name = "notification_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private NotificationType notificationType;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "body_text", nullable = false, columnDefinition = "TEXT")
  private String bodyText;

  @Column(name = "related_entity_type")
  @Enumerated(EnumType.STRING)
  private RelatedEntityType relatedEntityType;

  @Column(name = "related_entity_id")
  private Long relatedEntityId;

  @Column(name = "is_read", nullable = false)
  @Builder.Default
  private Boolean isRead = false;

  @Column(name = "read_at")
  private LocalDateTime readAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
