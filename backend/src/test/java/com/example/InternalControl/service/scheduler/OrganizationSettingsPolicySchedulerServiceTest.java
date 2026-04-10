package com.example.InternalControl.service.scheduler;

import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.checklist.ChecklistTemplate;
import com.example.InternalControl.model.notification.Notification;
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
import com.example.InternalControl.scheduler.NotificationEmailDeliverySchedulerService;
import com.example.InternalControl.scheduler.OrganizationSettingsPolicySchedulerService;
import com.example.InternalControl.service.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationSettingsPolicySchedulerServiceTest {

    @Mock
    private OrganizationSettingsRepository organizationSettingsRepository;
    @Mock
    private ChecklistRunRepository checklistRunRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationDeliveryRepository notificationDeliveryRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private UserOrganizationRepository userOrganizationRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private AppUserIdentityRepository appUserIdentityRepository;
    @Mock
    private AppUserLocalCredentialRepository appUserLocalCredentialRepository;
    @Mock
    private NotificationEmailDeliverySchedulerService notificationEmailDeliverySchedulerService;

    @InjectMocks
    private OrganizationSettingsPolicySchedulerService schedulerService;

    @Test
    void shouldCreateReminderNotificationAndEmailDelivery() {
        OrganizationSettings settings = baseSettings();
        settings.setReminderEmailEnabled(true);
        settings.setNotificationEmail("varsling@example.com");

        ChecklistRun run = new ChecklistRun();
        run.setRunId(55L);
        ChecklistTemplate template = new ChecklistTemplate();
        template.setTitle("Kjølekontroll");
        run.setTemplate(template);

        AppUser recipient = AppUser.builder()
                .userId(10L)
                .displayName("Admin")
                .email("admin@example.com")
                .isActive(true)
                .build();

        Notification created = new Notification();
        created.setNotificationId(99L);

        when(organizationSettingsRepository.findAll()).thenReturn(List.of(settings));
        when(checklistRunRepository.findReminderCandidates(eq(123), any(Set.class), any(LocalDateTime.class)))
                .thenReturn(List.of(run));
        when(appUserRepository.findActiveByOrgAndRoles(eq(123), any(Set.class))).thenReturn(List.of(recipient));
        when(notificationRepository.existsRecentEntityNotification(anyInt(), anyLong(), any(), any(), anyLong(), any()))
                .thenReturn(false);
        when(notificationService.createNotificationWithEntity(anyInt(), anyLong(), any(), anyString(), anyString(), any(), anyLong()))
                .thenReturn(created);
        when(notificationDeliveryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        schedulerService.applyOrganizationPolicies();

        verify(notificationService).createNotificationWithEntity(
                eq(123), eq(10L), any(), anyString(), contains("Kjølekontroll"), any(), eq(55L));
        verify(notificationDeliveryRepository).save(any());
        verify(notificationEmailDeliverySchedulerService).processDeliveryImmediately(any());
    }

    @Test
    void shouldDeleteOldAuditLogsWhenRetentionConfigured() {
        OrganizationSettings settings = baseSettings();
        settings.setRetentionAuditMonths(6);

        when(organizationSettingsRepository.findAll()).thenReturn(List.of(settings));

        schedulerService.applyOrganizationPolicies();

        verify(auditLogRepository).deleteByOrgNumberAndCreatedAtBefore(eq(123), any(LocalDateTime.class));
    }

    @Test
    void shouldAnonymizeInactiveUsersPastRetention() {
        OrganizationSettings settings = baseSettings();
        settings.setRetentionUserMonths(12);

        AppUser staleUser = AppUser.builder()
                .userId(22L)
                .displayName("Old User")
                .email("old@example.com")
                .phone("12345678")
                .isActive(false)
                .build();
        UserOrganization membership = UserOrganization.builder()
                .user(staleUser)
                .isActive(false)
                .leftAt(LocalDateTime.now().minusMonths(13))
                .build();

        when(organizationSettingsRepository.findAll()).thenReturn(List.of(settings));
        when(userOrganizationRepository.findInactiveLeftBefore(eq(123), any(LocalDateTime.class)))
                .thenReturn(List.of(membership));
        when(userOrganizationRepository.countActiveMemberships(22L)).thenReturn(0L);

        schedulerService.applyOrganizationPolicies();

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());
        AppUser savedUser = userCaptor.getValue();
        assertThat(savedUser.getDisplayName()).isEqualTo("Anonymized user 22");
        assertThat(savedUser.getEmail()).isNull();
        assertThat(savedUser.getPhone()).isNull();
        verify(appUserIdentityRepository).deleteByUserId(22L);
        verify(appUserLocalCredentialRepository).deleteByUserUserId(22L);
    }

    private OrganizationSettings baseSettings() {
        return OrganizationSettings.builder()
                .orgNumber(123L)
                .timezoneName("Europe/Oslo")
                .localeCode("nb-NO")
                .enableFoodModule(true)
                .enableAlcoholModule(true)
                .defaultTempMinC(BigDecimal.valueOf(-18))
                .defaultTempMaxC(BigDecimal.valueOf(4))
                .reminderEmailEnabled(false)
                .notificationEmail(null)
                .retentionUserMonths(0)
                .retentionAuditMonths(0)
                .build();
    }
}
