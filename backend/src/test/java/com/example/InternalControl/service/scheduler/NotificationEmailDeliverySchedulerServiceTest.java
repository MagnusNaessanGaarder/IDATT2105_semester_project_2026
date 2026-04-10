package com.example.InternalControl.service.scheduler;

import com.example.InternalControl.model.notification.DeliveryChannel;
import com.example.InternalControl.model.notification.DeliveryStatus;
import com.example.InternalControl.model.notification.Notification;
import com.example.InternalControl.model.notification.NotificationDelivery;
import com.example.InternalControl.model.organization.OrganizationSettings;
import com.example.InternalControl.repository.notification.NotificationDeliveryRepository;
import com.example.InternalControl.repository.notification.NotificationRepository;
import com.example.InternalControl.repository.organization.OrganizationSettingsRepository;
import com.example.InternalControl.scheduler.NotificationEmailDeliverySchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEmailDeliverySchedulerServiceTest {

    @Mock
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private OrganizationSettingsRepository organizationSettingsRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationEmailDeliverySchedulerService schedulerService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(schedulerService, "fromEmail", "noreply@test.local");
    }

    @Test
    void shouldSendPendingEmailAndMarkSent() {
        NotificationDelivery delivery = NotificationDelivery.builder()
                .deliveryId(1L)
                .notificationId(11L)
                .deliveryChannel(DeliveryChannel.EMAIL)
                .deliveryStatus(DeliveryStatus.PENDING)
                .build();
        Notification notification = Notification.builder()
                .notificationId(11L)
                .orgNumber(123)
                .title("Påminnelse")
                .bodyText("Sjekklisten er forfalt")
                .build();
        OrganizationSettings settings = OrganizationSettings.builder()
                .orgNumber(123L)
                .reminderEmailEnabled(true)
                .notificationEmail("notify@example.com")
                .build();

        when(notificationDeliveryRepository.findTop100ByDeliveryChannelAndDeliveryStatusOrderByDeliveryIdAsc(
                DeliveryChannel.EMAIL, DeliveryStatus.PENDING)).thenReturn(List.of(delivery));
        when(notificationRepository.findById(11L)).thenReturn(Optional.of(notification));
        when(organizationSettingsRepository.findById(123)).thenReturn(Optional.of(settings));

        schedulerService.processPendingEmailDeliveries();

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getTo()).containsExactly("notify@example.com");
        assertThat(sentMessage.getSubject()).isEqualTo("Påminnelse");
        assertThat(sentMessage.getText()).isEqualTo("Sjekklisten er forfalt");
        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.SENT);
        assertThat(delivery.getAttemptedAt()).isNotNull();
        assertThat(delivery.getDeliveredAt()).isNotNull();
        verify(notificationDeliveryRepository).save(delivery);
    }

    @Test
    void shouldMarkFailedWhenNotificationMissing() {
        NotificationDelivery delivery = NotificationDelivery.builder()
                .deliveryId(2L)
                .notificationId(22L)
                .deliveryChannel(DeliveryChannel.EMAIL)
                .deliveryStatus(DeliveryStatus.PENDING)
                .build();

        when(notificationDeliveryRepository.findTop100ByDeliveryChannelAndDeliveryStatusOrderByDeliveryIdAsc(
                DeliveryChannel.EMAIL, DeliveryStatus.PENDING)).thenReturn(List.of(delivery));
        when(notificationRepository.findById(22L)).thenReturn(Optional.empty());

        schedulerService.processPendingEmailDeliveries();

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(delivery.getFailureReason()).contains("Notification not found");
        verify(notificationDeliveryRepository).save(delivery);
    }

    @Test
    void shouldMarkFailedWhenMailSendFails() {
        NotificationDelivery delivery = NotificationDelivery.builder()
                .deliveryId(3L)
                .notificationId(33L)
                .deliveryChannel(DeliveryChannel.EMAIL)
                .deliveryStatus(DeliveryStatus.PENDING)
                .build();
        Notification notification = Notification.builder()
                .notificationId(33L)
                .orgNumber(123)
                .title("Varsel")
                .bodyText("Innhold")
                .build();
        OrganizationSettings settings = OrganizationSettings.builder()
                .orgNumber(123L)
                .reminderEmailEnabled(true)
                .notificationEmail("notify@example.com")
                .build();

        when(notificationDeliveryRepository.findTop100ByDeliveryChannelAndDeliveryStatusOrderByDeliveryIdAsc(
                DeliveryChannel.EMAIL, DeliveryStatus.PENDING)).thenReturn(List.of(delivery));
        when(notificationRepository.findById(33L)).thenReturn(Optional.of(notification));
        when(organizationSettingsRepository.findById(123)).thenReturn(Optional.of(settings));
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

        schedulerService.processPendingEmailDeliveries();

        assertThat(delivery.getDeliveryStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(delivery.getAttemptedAt()).isNotNull();
        assertThat(delivery.getDeliveredAt()).isNull();
        assertThat(delivery.getFailureReason()).contains("Email send failed");
        verify(notificationDeliveryRepository).save(delivery);
    }
}
