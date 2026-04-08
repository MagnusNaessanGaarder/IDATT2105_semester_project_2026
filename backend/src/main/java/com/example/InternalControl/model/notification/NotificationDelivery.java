package com.example.InternalControl.model.notification;

import java.time.LocalDateTime;

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
@Table(name = "notification_delivery", indexes = {
    @Index(name = "ix_notification_delivery_notification", columnList = "notification_id"),
    @Index(name = "ix_notification_delivery_status", columnList = "delivery_status, attempted_at")
})
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
    @Column(name = "delivery_status", nullable = false)
    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

    @Column(name = "attempted_at")
    private LocalDateTime attemptedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
}
