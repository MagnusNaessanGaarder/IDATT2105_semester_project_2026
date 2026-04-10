package com.example.InternalControl.service.settings;

import com.example.InternalControl.dto.settings.OrganizationSettingsRequest;
import com.example.InternalControl.dto.settings.OrganizationSettingsResponse;
import com.example.InternalControl.model.audit.ActionType;
import com.example.InternalControl.model.audit.Audited;
import com.example.InternalControl.model.organization.OrganizationSettings;
import com.example.InternalControl.repository.organization.OrganizationSettingsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author TriTacLe
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationSettingsServiceImpl implements OrganizationSettingsService {

    private final OrganizationSettingsRepository settingsRepository;

    @Override
    @Transactional(readOnly = true)
    public OrganizationSettingsResponse getSettings(Integer orgNumber) {
        return settingsRepository.findById(orgNumber)
                .map(this::mapToResponse)
                .orElseGet(() -> {
                    log.info("No settings found for org: {}. Creating defaults.", orgNumber);
                    return createDefaultSettings(orgNumber);
                });
    }

    @Override
    @Transactional
    @Audited(action = ActionType.UPDATE, entityType = "OrganizationSettings")
    public OrganizationSettingsResponse updateSettings(Integer orgNumber, OrganizationSettingsRequest request, Long userId) {
        OrganizationSettings settings = settingsRepository.findById(orgNumber)
                .orElseGet(() -> {
                    log.info("No settings found for org: {}. Creating defaults before update.", orgNumber);
                    return OrganizationSettings.builder()
                            .orgNumber(orgNumber)
                            .timezoneName("Europe/Oslo")
                            .localeCode("nb-NO")
                            .enableFoodModule(true)
                            .enableAlcoholModule(true)
                            .reminderEmailEnabled(true)
                            .build();
                });

        settings.setTimezoneName(request.getTimezoneName());
        settings.setLocaleCode(request.getLocaleCode());
        settings.setEnableFoodModule(request.getEnableFoodModule());
        settings.setEnableAlcoholModule(request.getEnableAlcoholModule());
        settings.setDefaultTempMinC(request.getDefaultTempMinC());
        settings.setDefaultTempMaxC(request.getDefaultTempMaxC());
        settings.setReminderEmailEnabled(request.getReminderEmailEnabled());
        settings.setNotificationEmail(request.getNotificationEmail());
        settings.setRetentionUserMonths(request.getRetentionUserMonths());
        settings.setRetentionAuditMonths(request.getRetentionAuditMonths());

        OrganizationSettings updated = settingsRepository.save(settings);
        log.info("Updated organization settings for org: {} by user: {}", orgNumber, userId);

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    @Audited(action = ActionType.CREATE, entityType = "OrganizationSettings")
    public OrganizationSettingsResponse createDefaultSettings(Integer orgNumber) {
        // Check if settings already exist
        if (settingsRepository.existsById(orgNumber)) {
            log.debug("Settings already exist for org: {}", orgNumber);
            return getSettings(orgNumber);
        }

        OrganizationSettings settings = OrganizationSettings.builder()
                .orgNumber(orgNumber)
                .timezoneName("Europe/Oslo")
                .localeCode("nb-NO")
                .enableFoodModule(true)
                .enableAlcoholModule(true)
                .reminderEmailEnabled(true)
                .build();

        OrganizationSettings saved = settingsRepository.save(settings);
        log.info("Created default organization settings for org: {}", orgNumber);

        return mapToResponse(saved);
    }

    private OrganizationSettingsResponse mapToResponse(OrganizationSettings settings) {
        return OrganizationSettingsResponse.builder()
                .orgNumber(Math.toIntExact(settings.getOrgNumber()))
                .timezoneName(settings.getTimezoneName())
                .localeCode(settings.getLocaleCode())
                .enableFoodModule(settings.isEnableFoodModule())
                .enableAlcoholModule(settings.isEnableAlcoholModule())
                .defaultTempMinC(settings.getDefaultTempMinC())
                .defaultTempMaxC(settings.getDefaultTempMaxC())
                .reminderEmailEnabled(settings.isReminderEmailEnabled())
                .notificationEmail(settings.getNotificationEmail())
                .retentionUserMonths(Math.toIntExact(settings.getRetentionUserMonths()))
                .retentionAuditMonths(Math.toIntExact(settings.getRetentionAuditMonths()))
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}
