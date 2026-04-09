package com.example.InternalControl.service.notification;

import java.util.List;

import com.example.InternalControl.model.notification.Notification;
import com.example.InternalControl.model.notification.NotificationType;
import com.example.InternalControl.model.notification.RelatedEntityType;

/**
 * Service interface for notification management.
 * Handles creation, retrieval, and status updates of user notifications.
 *
 * @author TriTacLe
 * @since 1.0
 */
public interface NotificationService {

    Notification createNotification(Integer orgNumber, Long userId, NotificationType type,
                                    String title, String body);

    Notification createNotificationWithEntity(Integer orgNumber, Long userId, NotificationType type,
                                             String title, String body, RelatedEntityType entityType, Long entityId);

    List<Notification> getUserNotifications(Long userId, Integer orgNumber);

    List<Notification> getUserUnreadNotifications(Long userId, Integer orgNumber);

    long getUnreadCount(Long userId, Integer orgNumber);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId, Integer orgNumber);

    void deleteNotification(Long notificationId, Long userId);

    Notification getNotification(Long notificationId, Long userId);
}
