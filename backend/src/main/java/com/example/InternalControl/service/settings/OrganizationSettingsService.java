package com.example.InternalControl.service.settings;

import com.example.InternalControl.dto.settings.OrganizationSettingsRequest;
import com.example.InternalControl.dto.settings.OrganizationSettingsResponse;

public interface OrganizationSettingsService {

    OrganizationSettingsResponse getSettings(Integer orgNumber);

    OrganizationSettingsResponse updateSettings(Integer orgNumber, OrganizationSettingsRequest request, Long userId);

    OrganizationSettingsResponse createDefaultSettings(Integer orgNumber);
}
