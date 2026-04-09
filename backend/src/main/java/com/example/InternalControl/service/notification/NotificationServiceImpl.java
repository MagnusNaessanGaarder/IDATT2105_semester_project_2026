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
import com.example.InternalControl.model.user.AppUser;
import com.example.InternalControl.repository.notification.NotificationDeliveryRepository;
import com.example.InternalControl.repository.notification.NotificationRepository;
import com.example.InternalControl.repository.user.AppUserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles creation and management of user notifications with delivery tracking.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final AppUserRepository appUserRepository;

    @Override
    public Notification createNotification(Integer orgNumber, Long userId, NotificationType type,
                                          String title, String body) {
        return createNotificationWithEntity(orgNumber, userId, type, title, body, null, null);
    }

    @Override
    public Notification createNotificationWithEntity(Integer orgNumber, Long userId, NotificationType type,
                                                    String title, String body, RelatedEntityType entityType,
                                                    Long entityId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        Notification notification = new Notification();
        notification.setOrgNumber(orgNumber);
        notification.setUser(user);
        notification.setNotificationType(type);
        notification.setTitle(title);
        notification.setBodyText(body);
        notification.setRelatedEntityType(entityType);
        notification.setRelatedEntityId(entityId);
        notification.setIsRead(false);

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
    public List<Notification> getUserNotifications(Long userId, Integer orgNumber) {
        return notificationRepository.findByUserIdAndOrgNumber(userId, orgNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUserUnreadNotifications(Long userId, Integer orgNumber) {
        return notificationRepository.findUnreadByUserIdAndOrgNumber(userId, orgNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId, Integer orgNumber) {
        return notificationRepository.countByUserIdAndOrgNumberAndIsReadFalse(userId, orgNumber);
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));

        if (!notification.getUser().getUserId().equals(userId)) {
            throw new EntityNotFoundException("Notification not found or not owned by user");
        }

        notification.setIsRead(true);
        notification.setReadAt(java.time.LocalDateTime.now());
        notificationRepository.save(notification);
        log.debug("Marked notification {} as read for user {}", notificationId, userId);
    }

    @Override
    public void markAllAsRead(Long userId, Integer orgNumber) {
        List<Notification> unreadNotifications =
                notificationRepository.findUnreadByUserIdAndOrgNumber(userId, orgNumber);
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
            notification.setReadAt(java.time.LocalDateTime.now());
        }
        notificationRepository.saveAll(unreadNotifications);
        log.info("Marked {} notifications as read for user {}", unreadNotifications.size(), userId);
    }

    @Override
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));

        if (!notification.getUser().getUserId().equals(userId)) {
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

        if (!notification.getUser().getUserId().equals(userId)) {
            throw new EntityNotFoundException("Notification not found or not owned by user");
        }

        return notification;
    }
}
