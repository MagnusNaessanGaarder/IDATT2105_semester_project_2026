package com.example.InternalControl.dto.notification;

import com.example.InternalControl.model.notification.DeliveryChannel;
import com.example.InternalControl.model.notification.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for notification delivery response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDeliveryResponse {

    private Long deliveryId;
    private Long notificationId;
    private DeliveryChannel deliveryChannel;
    private DeliveryStatus deliveryStatus;
    private LocalDateTime attemptedAt;
    private LocalDateTime deliveredAt;
    private String failureReason;
}
