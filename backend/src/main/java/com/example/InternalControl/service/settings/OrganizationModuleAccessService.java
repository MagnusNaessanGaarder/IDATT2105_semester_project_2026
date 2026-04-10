package com.example.InternalControl.service.settings;

import com.example.InternalControl.model.enums.ModuleType;
import com.example.InternalControl.model.organization.OrganizationSettings;
import com.example.InternalControl.repository.organization.OrganizationSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Guards access to module-specific functionality based on organization settings.
 */
@Service
@RequiredArgsConstructor
public class OrganizationModuleAccessService {

  private final OrganizationSettingsRepository settingsRepository;

  public void ensureFoodModuleEnabled(Integer orgNumber) {
    ensureModuleEnabled(orgNumber, ModuleType.FOOD);
  }

  public void ensureAlcoholModuleEnabled(Integer orgNumber) {
    ensureModuleEnabled(orgNumber, ModuleType.ALCOHOL);
  }

  public void ensureModuleEnabled(Integer orgNumber, ModuleType moduleType) {
    if (orgNumber == null || moduleType == null) {
      return;
    }

    boolean enabled = settingsRepository.findById(orgNumber)
        .map(settings -> isEnabled(settings, moduleType))
        .orElse(true);

    if (!enabled) {
      throw new AccessDeniedException("Module is disabled for this organization: " + moduleType);
    }
  }

  private boolean isEnabled(OrganizationSettings settings, ModuleType moduleType) {
    return switch (moduleType) {
      case FOOD -> settings.isEnableFoodModule();
      case ALCOHOL -> settings.isEnableAlcoholModule();
    };
  }
}
