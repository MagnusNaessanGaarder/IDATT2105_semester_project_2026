package com.example.InternalControl.service.notification;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationDeliveryRepository deliveryRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private static final Long NOTIFICATION_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final Integer ORG_NUMBER = 123;

    private AppUser testUser;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testUser = createTestUser(USER_ID, "test@example.com");
        testNotification = createTestNotification(NOTIFICATION_ID, testUser, "Test Notification");
    }

    // ==================== CREATE NOTIFICATION TESTS ====================

    @Test
    void shouldCreateNotification() {
        // Given
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setNotificationId(NOTIFICATION_ID);
            return n;
        });
        when(deliveryRepository.save(any(NotificationDelivery.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Notification result = notificationService.createNotification(
                ORG_NUMBER, USER_ID, NotificationType.GENERAL, "Test Title", "Test Body");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getNotificationId()).isEqualTo(NOTIFICATION_ID);
        assertThat(result.getTitle()).isEqualTo("Test Title");
        assertThat(result.getBodyText()).isEqualTo("Test Body");
        assertThat(result.getNotificationType()).isEqualTo(NotificationType.GENERAL);
        assertThat(result.getIsRead()).isFalse();
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.getOrgNumber()).isEqualTo(ORG_NUMBER);

        verify(notificationRepository).save(any(Notification.class));
        verify(deliveryRepository).save(any(NotificationDelivery.class));
    }

    @Test
    void shouldCreateNotificationWithEntity() {
        // Given
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setNotificationId(NOTIFICATION_ID);
            return n;
        });
        when(deliveryRepository.save(any(NotificationDelivery.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Notification result = notificationService.createNotificationWithEntity(
                ORG_NUMBER, USER_ID, NotificationType.DEVIATION_ASSIGNED,
                "Deviation Assigned", "A deviation has been assigned to you",
                RelatedEntityType.DEVIATION_REPORT, 100L);

        // Then
        assertThat(result.getRelatedEntityType()).isEqualTo(RelatedEntityType.DEVIATION_REPORT);
        assertThat(result.getRelatedEntityId()).isEqualTo(100L);
    }

    @Test
    void shouldThrowWhenCreatingNotificationForNonExistentUser() {
        // Given
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> notificationService.createNotification(
                ORG_NUMBER, USER_ID, NotificationType.GENERAL, "Title", "Body"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void shouldCreateDeliveryRecordWhenCreatingNotification() {
        // Given
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setNotificationId(NOTIFICATION_ID);
            return n;
        });
        when(deliveryRepository.save(any(NotificationDelivery.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        notificationService.createNotification(ORG_NUMBER, USER_ID, NotificationType.GENERAL, "Title", "Body");

        // Then
        ArgumentCaptor<NotificationDelivery> deliveryCaptor = ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(deliveryRepository).save(deliveryCaptor.capture());

        NotificationDelivery savedDelivery = deliveryCaptor.getValue();
        assertThat(savedDelivery.getNotificationId()).isEqualTo(NOTIFICATION_ID);
        assertThat(savedDelivery.getDeliveryChannel()).isEqualTo(DeliveryChannel.IN_APP);
        assertThat(savedDelivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.SENT);
        assertThat(savedDelivery.getDeliveredAt()).isNotNull();
    }

    // ==================== GET NOTIFICATION TESTS ====================

    @Test
    void shouldGetUserNotifications() {
        // Given
        List<Notification> notifications = List.of(
                createTestNotification(1L, testUser, "Notification 1"),
                createTestNotification(2L, testUser, "Notification 2")
        );
        when(notificationRepository.findByUserIdAndOrgNumber(USER_ID, ORG_NUMBER))
                .thenReturn(notifications);

        // When
        List<Notification> result = notificationService.getUserNotifications(USER_ID, ORG_NUMBER);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Notification 1");
    }

    @Test
    void shouldGetUserUnreadNotifications() {
        // Given
        Notification unread = createTestNotification(1L, testUser, "Unread");
        unread.setIsRead(false);
        when(notificationRepository.findUnreadByUserIdAndOrgNumber(USER_ID, ORG_NUMBER))
                .thenReturn(List.of(unread));

        // When
        List<Notification> result = notificationService.getUserUnreadNotifications(USER_ID, ORG_NUMBER);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsRead()).isFalse();
    }

    @Test
    void shouldGetUnreadCount() {
        // Given
        when(notificationRepository.countByUserIdAndOrgNumberAndIsReadFalse(USER_ID, ORG_NUMBER))
                .thenReturn(5L);

        // When
        long result = notificationService.getUnreadCount(USER_ID, ORG_NUMBER);

        // Then
        assertThat(result).isEqualTo(5L);
    }

    @Test
    void shouldGetNotification() {
        // Given
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(testNotification));

        // When
        Notification result = notificationService.getNotification(NOTIFICATION_ID, USER_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getNotificationId()).isEqualTo(NOTIFICATION_ID);
    }

    @Test
    void shouldThrowWhenGettingNotificationNotOwnedByUser() {
        // Given
        AppUser otherUser = createTestUser(2L, "other@example.com");
        Notification otherNotification = createTestNotification(NOTIFICATION_ID, otherUser, "Other");
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(otherNotification));

        // When/Then
        assertThatThrownBy(() -> notificationService.getNotification(NOTIFICATION_ID, USER_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Notification not found or not owned by user");
    }

    // ==================== MARK AS READ TESTS ====================

    @Test
    void shouldMarkAsRead() {
        // Given
        testNotification.setIsRead(false);
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // When
        notificationService.markAsRead(NOTIFICATION_ID, USER_ID);

        // Then
        assertThat(testNotification.getIsRead()).isTrue();
        assertThat(testNotification.getReadAt()).isNotNull();
        verify(notificationRepository).save(testNotification);
    }

    @Test
    void shouldThrowWhenMarkingNotificationNotOwnedByUser() {
        // Given
        AppUser otherUser = createTestUser(2L, "other@example.com");
        Notification otherNotification = createTestNotification(NOTIFICATION_ID, otherUser, "Other");
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(otherNotification));

        // When/Then
        assertThatThrownBy(() -> notificationService.markAsRead(NOTIFICATION_ID, USER_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Notification not found or not owned by user");
    }

    @Test
    void shouldMarkAllAsRead() {
        // Given
        Notification unread1 = createTestNotification(1L, testUser, "Unread 1");
        unread1.setIsRead(false);
        Notification unread2 = createTestNotification(2L, testUser, "Unread 2");
        unread2.setIsRead(false);

        when(notificationRepository.findUnreadByUserIdAndOrgNumber(USER_ID, ORG_NUMBER))
                .thenReturn(List.of(unread1, unread2));
        when(notificationRepository.saveAll(anyList())).thenReturn(List.of(unread1, unread2));

        // When
        notificationService.markAllAsRead(USER_ID, ORG_NUMBER);

        // Then
        assertThat(unread1.getIsRead()).isTrue();
        assertThat(unread2.getIsRead()).isTrue();
        assertThat(unread1.getReadAt()).isNotNull();
        assertThat(unread2.getReadAt()).isNotNull();
        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    void shouldHandleMarkAllAsReadWhenNoUnreadNotifications() {
        // Given
        when(notificationRepository.findUnreadByUserIdAndOrgNumber(USER_ID, ORG_NUMBER))
                .thenReturn(Collections.emptyList());
        when(notificationRepository.saveAll(Collections.emptyList())).thenReturn(Collections.emptyList());

        // When
        notificationService.markAllAsRead(USER_ID, ORG_NUMBER);

        // Then
        verify(notificationRepository).saveAll(Collections.emptyList());
    }

    // ==================== DELETE NOTIFICATION TESTS ====================

    @Test
    void shouldDeleteNotification() {
        // Given
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(testNotification));

        // When
        notificationService.deleteNotification(NOTIFICATION_ID, USER_ID);

        // Then
        verify(notificationRepository).delete(testNotification);
    }

    @Test
    void shouldThrowWhenDeletingNotificationNotOwnedByUser() {
        // Given
        AppUser otherUser = createTestUser(2L, "other@example.com");
        Notification otherNotification = createTestNotification(NOTIFICATION_ID, otherUser, "Other");
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(otherNotification));

        // When/Then
        assertThatThrownBy(() -> notificationService.deleteNotification(NOTIFICATION_ID, USER_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Notification not found or not owned by user");

        verify(notificationRepository, never()).delete(any());
    }

    @Test
    void shouldThrowWhenDeletingNonExistentNotification() {
        // Given
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> notificationService.deleteNotification(NOTIFICATION_ID, USER_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Notification not found");
    }

    // ==================== HELPER METHODS ====================

    private AppUser createTestUser(Long userId, String email) {
        AppUser user = new AppUser();
        user.setUserId(userId);
        user.setEmail(email);
        user.setDisplayName("Test User");
        return user;
    }

    private Notification createTestNotification(Long id, AppUser user, String title) {
        return Notification.builder()
                .notificationId(id)
                .user(user)
                .orgNumber(ORG_NUMBER)
                .notificationType(NotificationType.GENERAL)
                .title(title)
                .bodyText("Test body")
                .isRead(false)
                .build();
    }
}
