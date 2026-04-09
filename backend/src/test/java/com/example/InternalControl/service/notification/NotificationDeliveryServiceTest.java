package com.example.InternalControl.service.notification;

import com.example.InternalControl.dto.notification.NotificationDeliveryResponse;
import com.example.InternalControl.model.notification.DeliveryChannel;
import com.example.InternalControl.model.notification.DeliveryStatus;
import com.example.InternalControl.model.notification.NotificationDelivery;
import com.example.InternalControl.repository.notification.NotificationDeliveryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationDeliveryService.
 */
@ExtendWith(MockitoExtension.class)
class NotificationDeliveryServiceTest {

    @Mock
    private NotificationDeliveryRepository deliveryRepository;

    @InjectMocks
    private NotificationDeliveryServiceImpl notificationDeliveryService;

    private static final Long DELIVERY_ID = 1L;
    private static final Long NOTIFICATION_ID = 100L;

    private NotificationDelivery testDelivery;

    @BeforeEach
    void setUp() {
        testDelivery = createTestDelivery(DELIVERY_ID, NOTIFICATION_ID, DeliveryChannel.IN_APP, DeliveryStatus.SENT);
    }

    // ==================== GET DELIVERIES BY NOTIFICATION TESTS ====================

    @Test
    void shouldGetDeliveriesByNotificationId() {
        // Given
        NotificationDelivery emailDelivery = createTestDelivery(2L, NOTIFICATION_ID, DeliveryChannel.EMAIL, DeliveryStatus.SENT);
        when(deliveryRepository.findByNotificationId(NOTIFICATION_ID))
                .thenReturn(List.of(testDelivery, emailDelivery));

        // When
        List<NotificationDeliveryResponse> result = notificationDeliveryService.getDeliveriesByNotificationId(NOTIFICATION_ID);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDeliveryChannel()).isEqualTo(DeliveryChannel.IN_APP);
        assertThat(result.get(1).getDeliveryChannel()).isEqualTo(DeliveryChannel.EMAIL);
    }

    @Test
    void shouldReturnEmptyListWhenNoDeliveries() {
        // Given
        when(deliveryRepository.findByNotificationId(NOTIFICATION_ID))
                .thenReturn(Collections.emptyList());

        // When
        List<NotificationDeliveryResponse> result = notificationDeliveryService.getDeliveriesByNotificationId(NOTIFICATION_ID);

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== GET DELIVERY BY CHANNEL TESTS ====================

    @Test
    void shouldGetDeliveryByChannel() {
        // Given
        when(deliveryRepository.findByNotificationIdAndDeliveryChannel(NOTIFICATION_ID, DeliveryChannel.IN_APP))
                .thenReturn(Optional.of(testDelivery));

        // When
        NotificationDeliveryResponse result = notificationDeliveryService.getDeliveryByChannel(
                NOTIFICATION_ID, DeliveryChannel.IN_APP);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDeliveryId()).isEqualTo(DELIVERY_ID);
        assertThat(result.getNotificationId()).isEqualTo(NOTIFICATION_ID);
        assertThat(result.getDeliveryChannel()).isEqualTo(DeliveryChannel.IN_APP);
    }

    @Test
    void shouldThrowWhenDeliveryNotFoundForChannel() {
        // Given
        when(deliveryRepository.findByNotificationIdAndDeliveryChannel(NOTIFICATION_ID, DeliveryChannel.SMS))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> notificationDeliveryService.getDeliveryByChannel(NOTIFICATION_ID, DeliveryChannel.SMS))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Delivery not found");
    }

    // ==================== RETRY DELIVERIES TESTS ====================

    @Test
    void shouldRetryFailedDeliveries() {
        // Given
        NotificationDelivery failedDelivery = createTestDelivery(2L, NOTIFICATION_ID, DeliveryChannel.EMAIL, DeliveryStatus.FAILED);
        failedDelivery.setFailureReason("Connection timeout");
        failedDelivery.setAttemptedAt(LocalDateTime.now());

        when(deliveryRepository.findByNotificationIdAndDeliveryStatus(NOTIFICATION_ID, DeliveryStatus.FAILED))
                .thenReturn(List.of(failedDelivery));
        when(deliveryRepository.save(any(NotificationDelivery.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        notificationDeliveryService.retryDeliveries(NOTIFICATION_ID, null);

        // Then
        assertThat(failedDelivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(failedDelivery.getAttemptedAt()).isNull();
        assertThat(failedDelivery.getDeliveredAt()).isNull();
        assertThat(failedDelivery.getFailureReason()).isNull();
        verify(deliveryRepository).save(failedDelivery);
    }

    @Test
    void shouldRetryFailedDeliveriesForSpecificChannel() {
        // Given
        NotificationDelivery failedEmail = createTestDelivery(2L, NOTIFICATION_ID, DeliveryChannel.EMAIL, DeliveryStatus.FAILED);
        NotificationDelivery failedSms = createTestDelivery(3L, NOTIFICATION_ID, DeliveryChannel.SMS, DeliveryStatus.FAILED);

        when(deliveryRepository.findByNotificationIdAndDeliveryStatus(NOTIFICATION_ID, DeliveryStatus.FAILED))
                .thenReturn(List.of(failedEmail, failedSms));
        when(deliveryRepository.save(any(NotificationDelivery.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        notificationDeliveryService.retryDeliveries(NOTIFICATION_ID, DeliveryChannel.EMAIL);

        // Then
        assertThat(failedEmail.getDeliveryStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(failedSms.getDeliveryStatus()).isEqualTo(DeliveryStatus.FAILED); // Should not change
        verify(deliveryRepository, times(1)).save(any());
    }

    @Test
    void shouldHandleNoFailedDeliveries() {
        // Given
        when(deliveryRepository.findByNotificationIdAndDeliveryStatus(NOTIFICATION_ID, DeliveryStatus.FAILED))
                .thenReturn(Collections.emptyList());

        // When
        notificationDeliveryService.retryDeliveries(NOTIFICATION_ID, null);

        // Then
        verify(deliveryRepository, never()).save(any());
    }

    // ==================== GET PENDING DELIVERIES TESTS ====================

    @Test
    void shouldGetPendingDeliveries() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        NotificationDelivery pending = createTestDelivery(1L, 101L, DeliveryChannel.EMAIL, DeliveryStatus.PENDING);
        Page<NotificationDelivery> page = new PageImpl<>(List.of(pending));

        when(deliveryRepository.findByDeliveryStatus(DeliveryStatus.PENDING, pageable))
                .thenReturn(page);

        // When
        Page<NotificationDeliveryResponse> result = notificationDeliveryService.getPendingDeliveries(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeliveryStatus()).isEqualTo(DeliveryStatus.PENDING);
    }

    // ==================== GET FAILED DELIVERIES TESTS ====================

    @Test
    void shouldGetFailedDeliveries() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        NotificationDelivery failed = createTestDelivery(1L, 102L, DeliveryChannel.EMAIL, DeliveryStatus.FAILED);
        failed.setFailureReason("SMTP error");
        Page<NotificationDelivery> page = new PageImpl<>(List.of(failed));

        when(deliveryRepository.findByDeliveryStatus(DeliveryStatus.FAILED, pageable))
                .thenReturn(page);

        // When
        Page<NotificationDeliveryResponse> result = notificationDeliveryService.getFailedDeliveries(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDeliveryStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(result.getContent().get(0).getFailureReason()).isEqualTo("SMTP error");
    }

    // ==================== HELPER METHODS ====================

    private NotificationDelivery createTestDelivery(Long id, Long notificationId, DeliveryChannel channel, DeliveryStatus status) {
        return NotificationDelivery.builder()
                .deliveryId(id)
                .notificationId(notificationId)
                .deliveryChannel(channel)
                .deliveryStatus(status)
                .deliveredAt(status == DeliveryStatus.SENT ? LocalDateTime.now() : null)
                .build();
    }
}
