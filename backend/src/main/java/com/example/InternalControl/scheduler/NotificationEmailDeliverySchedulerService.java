package com.example.InternalControl.scheduler;

import com.example.InternalControl.model.notification.DeliveryChannel;
import com.example.InternalControl.model.notification.DeliveryStatus;
import com.example.InternalControl.model.notification.Notification;
import com.example.InternalControl.model.notification.NotificationDelivery;
import com.example.InternalControl.model.organization.OrganizationSettings;
import com.example.InternalControl.repository.notification.NotificationDeliveryRepository;
import com.example.InternalControl.repository.notification.NotificationRepository;
import com.example.InternalControl.repository.organization.OrganizationSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEmailDeliverySchedulerService {

    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final NotificationRepository notificationRepository;
    private final OrganizationSettingsRepository organizationSettingsRepository;
    private final JavaMailSender mailSender;

    @Value("${application.mail.from:no-reply@internal-control.local}")
    private String fromEmail;

    @Scheduled(cron = "0 */5 * * * ?")
    @Transactional
    public void processPendingEmailDeliveries() {
        List<NotificationDelivery> pendingDeliveries =
                notificationDeliveryRepository.findTop100ByDeliveryChannelAndDeliveryStatusOrderByDeliveryIdAsc(
                        DeliveryChannel.EMAIL,
                        DeliveryStatus.PENDING
                );
        for (NotificationDelivery delivery : pendingDeliveries) {
            processDeliveryImmediately(delivery);
        }
    }

    public void processDeliveryImmediately(NotificationDelivery delivery) {
        if (delivery == null
                || delivery.getDeliveryChannel() != DeliveryChannel.EMAIL
                || delivery.getDeliveryStatus() != DeliveryStatus.PENDING) {
            return;
        }
        processDelivery(delivery);
        notificationDeliveryRepository.save(delivery);
    }

    private void processDelivery(NotificationDelivery delivery) {
        Optional<Notification> notificationOpt = notificationRepository.findById(delivery.getNotificationId());
        if (notificationOpt.isEmpty()) {
            markFailed(delivery, "Notification not found");
            return;
        }

        Notification notification = notificationOpt.get();
        if (notification.getOrgNumber() == null) {
            markFailed(delivery, "Notification missing org scope");
            return;
        }

        Optional<OrganizationSettings> settingsOpt = organizationSettingsRepository.findById(notification.getOrgNumber());
        if (settingsOpt.isEmpty()) {
            markFailed(delivery, "Organization settings not found");
            return;
        }

        OrganizationSettings settings = settingsOpt.get();
        if (!settings.isReminderEmailEnabled()) {
            markFailed(delivery, "Reminder email disabled in organization settings");
            return;
        }

        String toAddress = settings.getNotificationEmail();
        if (toAddress == null || toAddress.isBlank()) {
            markFailed(delivery, "Notification email not configured");
            return;
        }

        try {
            sendEmail(toAddress, notification);
            LocalDateTime now = LocalDateTime.now();
            delivery.setDeliveryStatus(DeliveryStatus.SENT);
            delivery.setAttemptedAt(now);
            delivery.setDeliveredAt(now);
            delivery.setFailureReason(null);
        } catch (MailException ex) {
            markFailed(delivery, "Email send failed: " + ex.getMessage());
            log.warn("Failed to send email delivery {}: {}", delivery.getDeliveryId(), ex.getMessage());
        }
    }

    private void sendEmail(String toAddress, Notification notification) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toAddress);
        message.setSubject(notification.getTitle());
        message.setText(notification.getBodyText());
        mailSender.send(message);
    }

    private void markFailed(NotificationDelivery delivery, String reason) {
        delivery.setDeliveryStatus(DeliveryStatus.FAILED);
        delivery.setAttemptedAt(LocalDateTime.now());
        delivery.setDeliveredAt(null);
        delivery.setFailureReason(reason);
    }
}
