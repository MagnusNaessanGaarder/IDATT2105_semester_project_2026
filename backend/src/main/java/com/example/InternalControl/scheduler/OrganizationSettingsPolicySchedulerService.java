package com.example.InternalControl.scheduler;

import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.enums.RunStatus;
import com.example.InternalControl.model.notification.DeliveryChannel;
import com.example.InternalControl.model.notification.DeliveryStatus;
import com.example.InternalControl.model.notification.Notification;
import com.example.InternalControl.model.notification.NotificationDelivery;
import com.example.InternalControl.model.notification.NotificationType;
import com.example.InternalControl.model.notification.RelatedEntityType;
import com.example.InternalControl.model.organization.OrganizationSettings;
import com.example.InternalControl.model.user.AppUser;
import com.example.InternalControl.model.user.UserOrganization;
import com.example.InternalControl.repository.audit.AuditLogRepository;
import com.example.InternalControl.repository.checklist.ChecklistRunRepository;
import com.example.InternalControl.repository.notification.NotificationDeliveryRepository;
import com.example.InternalControl.repository.notification.NotificationRepository;
import com.example.InternalControl.repository.organization.OrganizationSettingsRepository;
import com.example.InternalControl.repository.user.AppUserIdentityRepository;
import com.example.InternalControl.repository.user.AppUserLocalCredentialRepository;
import com.example.InternalControl.repository.user.AppUserRepository;
import com.example.InternalControl.repository.user.UserOrganizationRepository;
import com.example.InternalControl.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationSettingsPolicySchedulerService {

    private static final Set<String> REMINDER_RECIPIENT_ROLES = Set.of("ADMIN", "MANAGER");
    private static final Set<RunStatus> REMINDER_RUN_STATUSES = Set.of(
            RunStatus.DRAFT, RunStatus.IN_PROGRESS, RunStatus.OVERDUE
    );

    private final OrganizationSettingsRepository organizationSettingsRepository;
    private final ChecklistRunRepository checklistRunRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final NotificationService notificationService;
    private final AuditLogRepository auditLogRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final AppUserRepository appUserRepository;
    private final AppUserIdentityRepository appUserIdentityRepository;
    private final AppUserLocalCredentialRepository appUserLocalCredentialRepository;
    private final NotificationEmailDeliverySchedulerService notificationEmailDeliverySchedulerService;

    @Scheduled(cron = "0 15 6 * * ?")
    @Transactional
    public void applyOrganizationPolicies() {
        List<OrganizationSettings> allSettings = organizationSettingsRepository.findAll();
        for (OrganizationSettings settings : allSettings) {
            Integer orgNumber = Math.toIntExact(settings.getOrgNumber());
            applyReminderPolicy(orgNumber, settings);
            applyAuditRetentionPolicy(orgNumber, settings);
            applyUserRetentionPolicy(orgNumber, settings);
        }
    }

    private void applyReminderPolicy(Integer orgNumber, OrganizationSettings settings) {
        if (!settings.isReminderEmailEnabled()) {
            return;
        }
        if (settings.getNotificationEmail() == null || settings.getNotificationEmail().isBlank()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        List<ChecklistRun> runs = checklistRunRepository.findReminderCandidates(orgNumber, REMINDER_RUN_STATUSES, now);
        if (runs.isEmpty()) {
            return;
        }

        List<AppUser> recipients = appUserRepository.findActiveByOrgAndRoles(orgNumber, REMINDER_RECIPIENT_ROLES);
        if (recipients.isEmpty()) {
            return;
        }

        for (ChecklistRun run : runs) {
            for (AppUser recipient : recipients) {
                if (shouldSkipReminder(orgNumber, recipient.getUserId(), run.getRunId(), todayStart)) {
                    continue;
                }
                Notification notification = notificationService.createNotificationWithEntity(
                        orgNumber,
                        recipient.getUserId(),
                        NotificationType.TASK_OVERDUE,
                        "Påminnelse: forfalt sjekkliste",
                        reminderBody(run),
                        RelatedEntityType.CHECKLIST_RUN,
                        run.getRunId()
                );
                queueReminderEmailDelivery(notification.getNotificationId());
            }
        }
    }

    private boolean shouldSkipReminder(Integer orgNumber, Long userId, Long runId, LocalDateTime todayStart) {
        return notificationRepository.existsRecentEntityNotification(
                orgNumber,
                userId,
                NotificationType.TASK_OVERDUE,
                RelatedEntityType.CHECKLIST_RUN,
                runId,
                todayStart
        );
    }

    private String reminderBody(ChecklistRun run) {
        String templateTitle = run.getTemplate() == null ? null : run.getTemplate().getTitle();
        String templatePart = templateTitle == null ? "Sjekklisten" : "Sjekklisten \"" + templateTitle + "\"";
        return templatePart + " er forfalt og krever oppfølging.";
    }

    private void queueReminderEmailDelivery(Long notificationId) {
        NotificationDelivery delivery = NotificationDelivery.builder()
                .notificationId(notificationId)
                .deliveryChannel(DeliveryChannel.EMAIL)
                .deliveryStatus(DeliveryStatus.PENDING)
                .build();
        NotificationDelivery savedDelivery = notificationDeliveryRepository.save(delivery);
        notificationEmailDeliverySchedulerService.processDeliveryImmediately(savedDelivery);
    }

    private void applyAuditRetentionPolicy(Integer orgNumber, OrganizationSettings settings) {
        if (settings.getRetentionAuditMonths() <= 0) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(settings.getRetentionAuditMonths());
        int deleted = auditLogRepository.deleteByOrgNumberAndCreatedAtBefore(orgNumber, cutoff);
        if (deleted > 0) {
            log.info("Deleted {} audit entries for org {} (cutoff {})", deleted, orgNumber, cutoff);
        }
    }

    private void applyUserRetentionPolicy(Integer orgNumber, OrganizationSettings settings) {
        if (settings.getRetentionUserMonths() <= 0) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(settings.getRetentionUserMonths());
        List<UserOrganization> staleMemberships = userOrganizationRepository.findInactiveLeftBefore(orgNumber, cutoff);
        for (UserOrganization membership : staleMemberships) {
            AppUser user = membership.getUser();
            if (user == null || Boolean.TRUE.equals(user.getIsActive())) {
                continue;
            }
            if (userOrganizationRepository.countActiveMemberships(user.getUserId()) > 0) {
                continue;
            }
            if (isAlreadyAnonymized(user)) {
                continue;
            }
            anonymizeUser(user);
            appUserRepository.save(user);
            appUserIdentityRepository.deleteByUserId(user.getUserId());
            appUserLocalCredentialRepository.deleteByUserUserId(user.getUserId());
        }
    }

    private boolean isAlreadyAnonymized(AppUser user) {
        return user.getEmail() == null
                && user.getPhone() == null
                && user.getDisplayName() != null
                && user.getDisplayName().startsWith("Anonymized user ");
    }

    private void anonymizeUser(AppUser user) {
        user.setDisplayName("Anonymized user " + user.getUserId());
        user.setEmail(null);
        user.setPhone(null);
        user.setGlobalLastSeenAt(null);
    }
}
