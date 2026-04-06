package com.example.InternalControl.service.notification;

import java.util.List;

import com.example.InternalControl.model.notification.Notification;
import com.example.InternalControl.model.notification.NotificationType;
import com.example.InternalControl.model.notification.RelatedEntityType;

public interface NotificationService {

    Notification createNotification(Integer orgNumber, Long userId, NotificationType type,
                                    String title, String body);

    Notification createNotificationWithEntity(Integer orgNumber, Long userId, NotificationType type,
                                             String title, String body, RelatedEntityType entityType, Long entityId);

    List<Notification> getUserNotifications(Long userId);

    List<Notification> getUserUnreadNotifications(Long userId);

    Long getUnreadCount(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);

    void deleteNotification(Long notificationId, Long userId);

    Notification getNotification(Long notificationId, Long userId);
}
