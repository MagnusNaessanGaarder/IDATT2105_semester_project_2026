package com.example.InternalControl.controller.organization;

import com.example.InternalControl.dto.organization.OrganizationSettingsRequest;
import com.example.InternalControl.dto.organization.OrganizationSettingsResponse;
import com.example.InternalControl.model.organization.Organization;
import com.example.InternalControl.model.organization.OrganizationSettings;
import com.example.InternalControl.repository.organization.OrganizationRepository;
import com.example.InternalControl.repository.organization.OrganizationSettingsRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST controller for organization settings management.
 *
 * @author TriTacLe
 * @since 1.0
 */
@RestController
@RequestMapping("/api/admin/organizations/settings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Organization Settings Admin", description = "Administrative endpoints for managing organization settings")
@SecurityRequirement(name = "bearerAuth")
public class OrganizationSettingsAdminController {

    private final OrganizationSettingsRepository settingsRepository;
    private final OrganizationRepository organizationRepository;

    /**
     * Get settings for an organization.
     *
     * @param orgNumber the organization number
     * @return the organization settings
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get organization settings", description = "Retrieve settings for a specific organization")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved settings"),
            @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    public ResponseEntity<OrganizationSettingsResponse> getSettings(
            @RequestParam Integer orgNumber) {
        requireAnyRole("ROLE_ADMIN", "ROLE_MANAGER");
        log.info("Getting settings for organization: {}", orgNumber);

        OrganizationSettings settings = settingsRepository.findById(orgNumber)
                .orElseGet(() -> createDefaultSettings(orgNumber));

        return ResponseEntity.ok(mapToResponse(settings));
    }

    /**
     * Update settings for an organization.
     *
     * @param orgNumber the organization number
     * @param request the settings update request
     * @return the updated settings
     */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update organization settings", description = "Update settings for a specific organization (ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully updated settings"),
            @ApiResponse(responseCode = "404", description = "Organization not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<OrganizationSettingsResponse> updateSettings(
            @RequestParam Integer orgNumber,
            @Valid @RequestBody OrganizationSettingsRequest request) {
        requireAnyRole("ROLE_ADMIN");
        log.info("Updating settings for organization: {}", orgNumber);

        OrganizationSettings settings = settingsRepository.findById(orgNumber)
                .orElseGet(() -> createDefaultSettings(orgNumber));

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
        if (request.getRetentionUserMonths() != null) {
            settings.setRetentionUserMonths(request.getRetentionUserMonths());
        }
        if (request.getRetentionAuditMonths() != null) {
            settings.setRetentionAuditMonths(request.getRetentionAuditMonths());
        }

        settings.setUpdatedAt(LocalDateTime.now());
        settings = settingsRepository.save(settings);

        return ResponseEntity.ok(mapToResponse(settings));
    }

    private void requireAnyRole(String... roles) {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            throw new AccessDeniedException("Missing authentication");
        }

        for (String role : roles) {
            boolean hasRole = authentication.getAuthorities().stream()
                    .anyMatch(authority -> role.equals(authority.getAuthority()));
            if (hasRole) {
                return;
            }
        }

        throw new AccessDeniedException("Insufficient permissions");
    }

    private OrganizationSettings createDefaultSettings(Integer orgNumber) {
        Organization organization = organizationRepository.findById(orgNumber)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + orgNumber));

        OrganizationSettings settings = OrganizationSettings.builder()
                .orgNumber(orgNumber)
                .organization(organization)
                .timezoneName("Europe/Oslo")
                .localeCode("nb-NO")
                .enableFoodModule(true)
                .enableAlcoholModule(true)
                .reminderEmailEnabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return settingsRepository.save(settings);
    }

    private OrganizationSettingsResponse mapToResponse(OrganizationSettings settings) {
        return OrganizationSettingsResponse.builder()
                .orgNumber(settings.getOrgNumber())
                .timezoneName(settings.getTimezoneName())
                .localeCode(settings.getLocaleCode())
                .enableFoodModule(settings.getEnableFoodModule())
                .enableAlcoholModule(settings.getEnableAlcoholModule())
                .defaultTempMinC(settings.getDefaultTempMinC())
                .defaultTempMaxC(settings.getDefaultTempMaxC())
                .reminderEmailEnabled(settings.getReminderEmailEnabled())
                .notificationEmail(settings.getNotificationEmail())
                .retentionUserMonths(settings.getRetentionUserMonths())
                .retentionAuditMonths(settings.getRetentionAuditMonths())
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}
