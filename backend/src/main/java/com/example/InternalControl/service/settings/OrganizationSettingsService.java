package com.example.InternalControl.service.settings;

import com.example.InternalControl.dto.settings.OrganizationSettingsRequest;
import com.example.InternalControl.dto.settings.OrganizationSettingsResponse;
import com.example.InternalControl.model.organization.OrganizationSettings;

/**
 * Manages organization-specific settings like timezone, modules, and retention policies.
 *
 * @author TriTacLe
 * @since 1.0
 */
public interface OrganizationSettingsService {

    OrganizationSettingsResponse getSettings(Integer orgNumber);

    OrganizationSettingsResponse updateSettings(Integer orgNumber, OrganizationSettingsRequest request, Long userId);

    OrganizationSettingsResponse createDefaultSettings(Integer orgNumber);
}
