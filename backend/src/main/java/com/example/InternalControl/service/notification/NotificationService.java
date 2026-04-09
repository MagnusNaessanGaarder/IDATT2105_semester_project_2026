package com.example.InternalControl.service.notification;

import java.util.List;

import com.example.InternalControl.model.notification.Notification;
import com.example.InternalControl.model.notification.NotificationType;
import com.example.InternalControl.model.notification.RelatedEntityType;

/**
 * Service interface for managing user notifications.
 * <p>
 * Notifications inform users about important events such as:
 * <ul>
 *   <li>Checklist assignments and deadlines</li>
 *   <li>Deviation reports requiring attention</li>
 *   <li>Temperature alerts</li>
 *   <li>Training expirations</li>
 *   <li>System updates</li>
 * </ul>
 * <p>
 * Notifications support in-app delivery and can be linked to specific
 * entities for direct navigation from the notification to the relevant item.
 *
 * @author TriTacLe
 * @version 1.0
 * @since 1.0
 */
public interface NotificationService {

    /**
     * Creates a notification for a user.
     * <p>
     * The notification is created in UNREAD status and automatically
     * creates a delivery record. Notifications are scoped to an organization.
     *
     * @param orgNumber the organization number for scoping
     * @param userId    the ID of the user to notify
     * @param type      the notification type (classification)
     * @param title     the notification title (short, descriptive)
     * @param body      the notification body (detailed message)
     * @return the created Notification with generated ID
     * @throws jakarta.persistence.EntityNotFoundException if user or organization not found
     */
    Notification createNotification(Integer orgNumber, Long userId, NotificationType type,
                                    String title, String body);

    /**
     * Creates a notification linked to a specific entity.
     * <p>
     * Entity-linked notifications allow users to navigate directly
     * to the related item (e.g., a specific deviation report or checklist)
     * when clicking the notification.
     *
     * @param orgNumber  the organization number for scoping
     * @param userId     the ID of the user to notify
     * @param type       the notification type (classification)
     * @param title      the notification title
     * @param body       the notification body
     * @param entityType the type of entity linked (e.g., DEVIATION_REPORT, CHECKLIST_RUN)
     * @param entityId   the ID of the linked entity
     * @return the created Notification with entity reference
     * @throws jakarta.persistence.EntityNotFoundException if user, organization, or entity not found
     */
    Notification createNotificationWithEntity(Integer orgNumber, Long userId, NotificationType type,
                                             String title, String body, RelatedEntityType entityType, Long entityId);

    /**
     * Retrieves all notifications for a user in an organization.
     * <p>
     * Returns both read and unread notifications, sorted by creation
     * date with newest first. Use {@link #getUserUnreadNotifications}
     * to get only unread items.
     *
     * @param userId    the ID of the user
     * @param orgNumber the organization number for scoping
     * @return list of all notifications for the user
     */
    List<Notification> getUserNotifications(Long userId, Integer orgNumber);

    /**
     * Retrieves only unread notifications for a user.
     * <p>
     * Useful for notification badges and "new items" indicators.
     *
     * @param userId    the ID of the user
     * @param orgNumber the organization number for scoping
     * @return list of unread notifications
     */
    List<Notification> getUserUnreadNotifications(Long userId, Integer orgNumber);

    /**
     * Counts unread notifications for a user.
     * <p>
     * Efficient method for displaying notification badges
     * without fetching all notification data.
     *
     * @param userId    the ID of the user
     * @param orgNumber the organization number for scoping
     * @return count of unread notifications
     */
    long getUnreadCount(Long userId, Integer orgNumber);

    /**
     * Marks a single notification as read.
     * <p>
     * Sets the read timestamp and updates status to READ.
     * No-op if notification is already read.
     *
     * @param notificationId the ID of the notification
     * @param userId         the ID of the user (for ownership validation)
     * @throws jakarta.persistence.EntityNotFoundException if notification not found
     * @throws org.springframework.security.access.AccessDeniedException if user doesn't own the notification
     */
    void markAsRead(Long notificationId, Long userId);

    /**
     * Marks all unread notifications as read for a user.
     * <p>
     * Bulk operation for "mark all as read" functionality.
     * More efficient than calling {@link #markAsRead} for each notification.
     *
     * @param userId    the ID of the user
     * @param orgNumber the organization number for scoping
     */
    void markAllAsRead(Long userId, Integer orgNumber);

    /**
     * Deletes a notification.
     * <p>
     * Permanently removes the notification from the database.
     * Users can only delete their own notifications.
     *
     * @param notificationId the ID of the notification to delete
     * @param userId         the ID of the user (for ownership validation)
     * @throws jakarta.persistence.EntityNotFoundException if notification not found
     * @throws org.springframework.security.access.AccessDeniedException if user doesn't own the notification
     */
    void deleteNotification(Long notificationId, Long userId);

    /**
     * Retrieves a specific notification by ID.
     *
     * @param notificationId the ID of the notification
     * @param userId         the ID of the user (for ownership validation)
     * @return the Notification
     * @throws jakarta.persistence.EntityNotFoundException if notification not found
     * @throws org.springframework.security.access.AccessDeniedException if user doesn't own the notification
     */
    Notification getNotification(Long notificationId, Long userId);
}
