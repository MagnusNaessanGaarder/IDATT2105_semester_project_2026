package com.example.InternalControl.repository;

import com.example.InternalControl.AbstractIntegrationTest;
import com.example.InternalControl.model.notification.Notification;
import com.example.InternalControl.model.notification.NotificationType;
import com.example.InternalControl.model.notification.RelatedEntityType;
import com.example.InternalControl.model.user.AppUser;
import com.example.InternalControl.repository.notification.NotificationRepository;
import com.example.InternalControl.repository.user.AppUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for NotificationRepository.
 * Tests custom query methods for notification management.
 */
@SpringBootTest
@Transactional
@DisplayName("NotificationRepository Integration Tests")
class NotificationRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    @DisplayName("Should find notifications by user ID and organization number ordered by creation date")
    void shouldFindByUserIdAndOrgNumber() {
        // Given
        AppUser user = createAndSaveTestUser("notify@test.com", "Test User");
        Notification notification1 = createTestNotification(user, 999001, "First Notification", false);
        notification1.setCreatedAt(LocalDateTime.now().minusHours(2));
        Notification notification2 = createTestNotification(user, 999001, "Second Notification", false);
        notification2.setCreatedAt(LocalDateTime.now().minusHours(1));
        notificationRepository.save(notification1);
        notificationRepository.save(notification2);

        // When
        List<Notification> notifications = notificationRepository.findByUserIdAndOrgNumber(user.getUserId(), 999001);

        // Then
        assertThat(notifications).hasSize(2);
        assertThat(notifications.get(0).getTitle()).isEqualTo("Second Notification");
        assertThat(notifications.get(1).getTitle()).isEqualTo("First Notification");
    }

    @Test
    @DisplayName("Should find unread notifications by user ID and organization")
    void shouldFindUnreadByUserIdAndOrgNumber() {
        // Given
        AppUser user = createAndSaveTestUser("unread@test.com", "Test User");
        Notification readNotification = createTestNotification(user, 999002, "Read Notification", true);
        Notification unreadNotification = createTestNotification(user, 999002, "Unread Notification", false);
        notificationRepository.save(readNotification);
        notificationRepository.save(unreadNotification);

        // When
        List<Notification> unreadNotifications = notificationRepository.findUnreadByUserIdAndOrgNumber(user.getUserId(), 999002);

        // Then
        assertThat(unreadNotifications).hasSize(1);
        assertThat(unreadNotifications.get(0).getTitle()).isEqualTo("Unread Notification");
        assertThat(unreadNotifications.get(0).getIsRead()).isFalse();
    }

    @Test
    @DisplayName("Should count unread notifications by user ID and organization")
    void shouldCountUnreadByUserIdAndOrgNumber() {
        // Given
        AppUser user = createAndSaveTestUser("count@test.com", "Test User");
        Notification notification1 = createTestNotification(user, 999003, "Unread 1", false);
        Notification notification2 = createTestNotification(user, 999003, "Unread 2", false);
        Notification notification3 = createTestNotification(user, 999003, "Read", true);
        notificationRepository.save(notification1);
        notificationRepository.save(notification2);
        notificationRepository.save(notification3);

        // When
        long unreadCount = notificationRepository.countByUserIdAndOrgNumberAndIsReadFalse(user.getUserId(), 999003);

        // Then
        assertThat(unreadCount).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return empty list when no notifications found for user")
    void shouldReturnEmptyWhenNoNotificationsForUser() {
        // Given
        AppUser user = createAndSaveTestUser("nonotify@test.com", "Test User");

        // When
        List<Notification> notifications = notificationRepository.findByUserIdAndOrgNumber(user.getUserId(), 999999);

        // Then
        assertThat(notifications).isEmpty();
    }

    @Test
    @DisplayName("Should return zero when counting unread notifications for user with no notifications")
    void shouldReturnZeroWhenNoUnreadNotifications() {
        // Given
        AppUser user = createAndSaveTestUser("zeronotify@test.com", "Test User");

        // When
        long unreadCount = notificationRepository.countByUserIdAndOrgNumberAndIsReadFalse(user.getUserId(), 999999);

        // Then
        assertThat(unreadCount).isEqualTo(0);
    }

    @Test
    @DisplayName("Should find notifications with different notification types")
    void shouldHandleDifferentNotificationTypes() {
        // Given
        AppUser user = createAndSaveTestUser("types@test.com", "Test User");
        Notification deviationNotification = createTestNotification(user, 999004, "Deviation Assigned", false);
        deviationNotification.setNotificationType(NotificationType.DEVIATION_ASSIGNED);
        Notification taskNotification = createTestNotification(user, 999004, "Task Overdue", false);
        taskNotification.setNotificationType(NotificationType.TASK_OVERDUE);
        notificationRepository.save(deviationNotification);
        notificationRepository.save(taskNotification);

        // When
        List<Notification> notifications = notificationRepository.findByUserIdAndOrgNumber(user.getUserId(), 999004);

        // Then
        assertThat(notifications).hasSize(2);
        assertThat(notifications).extracting(Notification::getNotificationType)
                .containsExactlyInAnyOrder(NotificationType.DEVIATION_ASSIGNED, NotificationType.TASK_OVERDUE);
    }

    @Test
    @DisplayName("Should find notifications with related entity information")
    void shouldFindNotificationsWithRelatedEntity() {
        // Given
        AppUser user = createAndSaveTestUser("entity@test.com", "Test User");
        Notification notification = createTestNotification(user, 999005, "Deviation Created", false);
        notification.setRelatedEntityType(RelatedEntityType.DEVIATION_REPORT);
        notification.setRelatedEntityId(123L);
        notificationRepository.save(notification);

        // When
        List<Notification> notifications = notificationRepository.findByUserIdAndOrgNumber(user.getUserId(), 999005);

        // Then
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getRelatedEntityType()).isEqualTo(RelatedEntityType.DEVIATION_REPORT);
        assertThat(notifications.get(0).getRelatedEntityId()).isEqualTo(123L);
    }

    private AppUser createAndSaveTestUser(String email, String displayName) {
        AppUser user = AppUser.builder()
                .email(email)
                .displayName(displayName)
                .isActive(true)
                .build();
        return appUserRepository.save(user);
    }

    private Notification createTestNotification(AppUser user, Integer orgNumber, String title, boolean isRead) {
        return Notification.builder()
                .user(user)
                .orgNumber(orgNumber)
                .notificationType(NotificationType.GENERAL)
                .title(title)
                .bodyText("Test notification body for: " + title)
                .isRead(isRead)
                .build();
    }
}
