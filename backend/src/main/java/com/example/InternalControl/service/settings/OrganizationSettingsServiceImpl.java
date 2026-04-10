package com.example.InternalControl.service.settings;

import com.example.InternalControl.dto.settings.OrganizationSettingsRequest;
import com.example.InternalControl.dto.settings.OrganizationSettingsResponse;
import com.example.InternalControl.model.audit.ActionType;
import com.example.InternalControl.model.audit.Audited;
import com.example.InternalControl.model.organization.OrganizationSettings;
import com.example.InternalControl.repository.organization.OrganizationSettingsRepository;
import com.example.InternalControl.service.audit.AuditLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public OrganizationSettingsResponse getSettings(Integer orgNumber) {
        return settingsRepository.findById(orgNumber)
                .map(this::mapToResponse)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Organization settings not found for org: " + orgNumber));
    }

    @Override
    @Transactional
    public OrganizationSettingsResponse updateSettings(Integer orgNumber, OrganizationSettingsRequest request, Long userId) {
        OrganizationSettings settings = settingsRepository.findById(orgNumber)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Organization settings not found for org: " + orgNumber));
        OrganizationSettingsResponse oldValues = mapToResponse(settings);

        if (request.getTimezoneName() != null) {
            settings.setTimezoneName(request.getTimezoneName());
        }
        if (request.getLocaleCode() != null) {
            settings.setLocaleCode(request.getLocaleCode());
        }
        if (request.getEnableFoodModule() != null) {
            settings.setEnableFoodModule(request.getEnableFoodModule());
        }
        if (request.getEnableAlcoholModule() != null) {
            settings.setEnableAlcoholModule(request.getEnableAlcoholModule());
        }
        if (request.getDefaultTempMinC() != null) {
            settings.setDefaultTempMinC(request.getDefaultTempMinC());
        }
        if (request.getDefaultTempMaxC() != null) {
            settings.setDefaultTempMaxC(request.getDefaultTempMaxC());
        }
        if (request.getReminderEmailEnabled() != null) {
            settings.setReminderEmailEnabled(request.getReminderEmailEnabled());
        }
        if (request.getNotificationEmail() != null) {
            settings.setNotificationEmail(request.getNotificationEmail());
        }
        if (request.getDisplayName() != null) {
            settings.setDisplayName(request.getDisplayName());
        }
        if (request.getLegalName() != null) {
            settings.setLegalName(request.getLegalName());
        }
        if (request.getContactEmail() != null) {
            settings.setContactEmail(request.getContactEmail());
        }
        if (request.getContactPhone() != null) {
            settings.setContactPhone(request.getContactPhone());
        }
        if (request.getRetentionUserMonths() != null) {
            settings.setRetentionUserMonths(request.getRetentionUserMonths());
        }
        if (request.getRetentionAuditMonths() != null) {
            settings.setRetentionAuditMonths(request.getRetentionAuditMonths());
        }

        OrganizationSettings updated = settingsRepository.save(settings);
        log.info("Updated organization settings for org: {} by user: {}", orgNumber, userId);

        OrganizationSettingsResponse response = mapToResponse(updated);
        logSettingsUpdateAudit(orgNumber, userId, oldValues, response);
        return response;
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
                .displayName(settings.getDisplayName())
                .legalName(settings.getLegalName())
                .contactEmail(settings.getContactEmail())
                .contactPhone(settings.getContactPhone())
                .retentionUserMonths(Math.toIntExact(settings.getRetentionUserMonths()))
                .retentionAuditMonths(Math.toIntExact(settings.getRetentionAuditMonths()))
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    private void logSettingsUpdateAudit(Integer orgNumber,
                                        Long userId,
                                        OrganizationSettingsResponse oldValues,
                                        OrganizationSettingsResponse newValues) {
        try {
            String oldValuesJson = objectMapper.writeValueAsString(oldValues);
            String newValuesJson = objectMapper.writeValueAsString(newValues);
            auditLogService.logAction(
                    orgNumber,
                    userId,
                    ActionType.UPDATE,
                    "OrganizationSettings",
                    orgNumber != null ? orgNumber.longValue() : null,
                    oldValuesJson,
                    newValuesJson,
                    null,
                    null);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize organization settings audit payload for org: {}", orgNumber, e);
        }
    }
}
