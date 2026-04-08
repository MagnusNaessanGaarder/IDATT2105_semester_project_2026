package com.example.InternalControl.service.notification;

import com.example.InternalControl.dto.notification.NotificationDeliveryResponse;
import com.example.InternalControl.model.notification.DeliveryChannel;
import com.example.InternalControl.model.notification.DeliveryStatus;
import com.example.InternalControl.model.notification.NotificationDelivery;
import com.example.InternalControl.repository.notification.NotificationDeliveryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of NotificationDeliveryService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationDeliveryServiceImpl implements NotificationDeliveryService {

    private final NotificationDeliveryRepository deliveryRepository;

    @Override
    public List<NotificationDeliveryResponse> getDeliveriesByNotificationId(Long notificationId) {
        log.debug("Getting deliveries for notification: {}", notificationId);
        return deliveryRepository.findByNotificationId(notificationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public NotificationDeliveryResponse getDeliveryByChannel(Long notificationId, DeliveryChannel channel) {
        log.debug("Getting delivery for notification {} and channel {}", notificationId, channel);
        NotificationDelivery delivery = deliveryRepository.findByNotificationIdAndDeliveryChannel(notificationId, channel)
                .orElseThrow(() -> new EntityNotFoundException("Delivery not found for notification " + notificationId + " and channel " + channel));
        return mapToResponse(delivery);
    }

    @Override
    @Transactional
    public void retryDeliveries(Long notificationId, DeliveryChannel channel) {
        log.info("Retrying deliveries for notification: {}, channel: {}", notificationId, channel);

        List<NotificationDelivery> deliveries;
        if (channel != null) {
            deliveries = deliveryRepository.findByNotificationIdAndDeliveryStatus(notificationId, DeliveryStatus.FAILED)
                    .stream()
                    .filter(d -> d.getDeliveryChannel() == channel)
                    .collect(Collectors.toList());
        } else {
            deliveries = deliveryRepository.findByNotificationIdAndDeliveryStatus(notificationId, DeliveryStatus.FAILED);
        }

        for (NotificationDelivery delivery : deliveries) {
            delivery.setDeliveryStatus(DeliveryStatus.PENDING);
            delivery.setAttemptedAt(null);
            delivery.setDeliveredAt(null);
            delivery.setFailureReason(null);
            deliveryRepository.save(delivery);
            log.debug("Reset delivery {} to PENDING status", delivery.getDeliveryId());
        }

        log.info("Retried {} deliveries for notification {}", deliveries.size(), notificationId);
    }

    @Override
    public Page<NotificationDeliveryResponse> getPendingDeliveries(Pageable pageable) {
        log.debug("Getting pending deliveries");
        return deliveryRepository.findByDeliveryStatus(DeliveryStatus.PENDING, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<NotificationDeliveryResponse> getFailedDeliveries(Pageable pageable) {
        log.debug("Getting failed deliveries");
        return deliveryRepository.findByDeliveryStatus(DeliveryStatus.FAILED, pageable)
                .map(this::mapToResponse);
    }

    private NotificationDeliveryResponse mapToResponse(NotificationDelivery delivery) {
        return NotificationDeliveryResponse.builder()
                .deliveryId(delivery.getDeliveryId())
                .notificationId(delivery.getNotificationId())
                .deliveryChannel(delivery.getDeliveryChannel())
                .deliveryStatus(delivery.getDeliveryStatus())
                .attemptedAt(delivery.getAttemptedAt())
                .deliveredAt(delivery.getDeliveredAt())
                .failureReason(delivery.getFailureReason())
                .build();
    }
}
