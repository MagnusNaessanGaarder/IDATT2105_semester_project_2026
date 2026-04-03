package com.example.InternalControl.model.notification;

import com.example.InternalControl.shared.enums.DeliveryChannel;
import com.example.InternalControl.shared.enums.DeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "notification_delivery")
public class NotificationDelivery {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "delivery_id")
  private Long deliveryId;

  @Column(name = "notification_id", nullable = false)
  private Long notificationId;

  @Enumerated(EnumType.STRING)
  @Column(name = "delivery_channel", nullable = false)
  private DeliveryChannel deliveryChannel;

  @Enumerated(EnumType.STRING)
  @Column(name = "delivery_status", nullable = false, length = 20)
  private DeliveryStatus deliveryStatus = DeliveryStatus.pending;

  @Column(name = "attempted_at")
  private LocalDateTime attemptedAt;

  @Column(name = "delivered_at")
  private LocalDateTime deliveredAt;

  @Column(name = "failure_reason", columnDefinition = "TEXT")
  private String failureReason;
}
