package com.example.InternalControl.service.notification;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.InternalControl.model.notification.DeliveryChannel;
import com.example.InternalControl.model.notification.DeliveryStatus;
import com.example.InternalControl.model.notification.Notification;
import com.example.InternalControl.model.notification.NotificationDelivery;
import com.example.InternalControl.model.notification.NotificationType;
import com.example.InternalControl.model.notification.RelatedEntityType;
import com.example.InternalControl.repository.notification.NotificationDeliveryRepository;
import com.example.InternalControl.repository.notification.NotificationRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository deliveryRepository;

    @Override
    public Notification createNotification(Integer orgNumber, Long userId, NotificationType type,
                                          String title, String body) {
        return createNotificationWithEntity(orgNumber, userId, type, title, body, null, null);
    }

    @Override
    public Notification createNotificationWithEntity(Integer orgNumber, Long userId, NotificationType type,
                                                    String title, String body, RelatedEntityType entityType, Long entityId) {
        Notification notification = Notification.builder()
                .orgNumber(orgNumber)
                .userId(userId)
                .notificationType(type)
                .title(title)
                .bodyText(body)
                .relatedEntityType(entityType)
                .relatedEntityId(entityId)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Created notification {} for user {}: {}", saved.getNotificationId(), userId, title);

        // Create in-app delivery record
        NotificationDelivery delivery = NotificationDelivery.builder()
                .notificationId(saved.getNotificationId())
                .deliveryChannel(DeliveryChannel.IN_APP)
                .deliveryStatus(DeliveryStatus.SENT)
                .deliveredAt(java.time.LocalDateTime.now())
                .build();
        deliveryRepository.save(delivery);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUserUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        int updated = notificationRepository.markAsRead(notificationId, userId);
        if (updated == 0) {
            throw new EntityNotFoundException("Notification not found or not owned by user");
        }
        log.debug("Marked notification {} as read for user {}", notificationId, userId);
    }

    @Override
    public void markAllAsRead(Long userId) {
        int updated = notificationRepository.markAllAsRead(userId);
        log.info("Marked {} notifications as read for user {}", updated, userId);
    }

    @Override
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            throw new EntityNotFoundException("Notification not found or not owned by user");
        }

        notificationRepository.delete(notification);
        log.info("Deleted notification {} for user {}", notificationId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Notification getNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            throw new EntityNotFoundException("Notification not found or not owned by user");
        }

        return notification;
    }
}
