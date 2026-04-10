package com.example.InternalControl.service.settings;

import com.example.InternalControl.dto.settings.OrganizationSettingsRequest;
import com.example.InternalControl.dto.settings.OrganizationSettingsResponse;
import com.example.InternalControl.model.organization.OrganizationSettings;
import com.example.InternalControl.repository.organization.OrganizationSettingsRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrganizationSettingsService.
 */
@ExtendWith(MockitoExtension.class)
class OrganizationSettingsServiceTest {

    @Mock
    private OrganizationSettingsRepository settingsRepository;

    @InjectMocks
    private OrganizationSettingsServiceImpl settingsService;

    private static final Integer ORG_NUMBER = 123;
    private static final Long USER_ID = 1L;

    private OrganizationSettings testSettings;

    @BeforeEach
    void setUp() {
        testSettings = createTestSettings(ORG_NUMBER);
    }

    // ==================== GET SETTINGS TESTS ====================

    @Test
    void shouldGetSettings() {
        // Given
        when(settingsRepository.findById(ORG_NUMBER)).thenReturn(Optional.of(testSettings));

        // When
        OrganizationSettingsResponse result = settingsService.getSettings(ORG_NUMBER);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrgNumber()).isEqualTo(ORG_NUMBER);
        assertThat(result.getTimezoneName()).isEqualTo("Europe/Oslo");
        assertThat(result.getLocaleCode()).isEqualTo("nb-NO");
        assertThat(result.isEnableFoodModule()).isTrue();
        assertThat(result.isEnableAlcoholModule()).isTrue();
    }

    @Test
    void shouldThrowWhenSettingsNotFound() {
        // Given
        when(settingsRepository.findById(ORG_NUMBER)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> settingsService.getSettings(ORG_NUMBER))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Organization settings not found for org");
    }

    // ==================== UPDATE SETTINGS TESTS ====================

    @Test
    void shouldUpdateSettings() {
        // Given
        OrganizationSettingsRequest request = createUpdateRequest();
        when(settingsRepository.findById(ORG_NUMBER)).thenReturn(Optional.of(testSettings));
        when(settingsRepository.save(any(OrganizationSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        OrganizationSettingsResponse result = settingsService.updateSettings(ORG_NUMBER, request, USER_ID);

        // Then
        assertThat(result.getTimezoneName()).isEqualTo("Europe/London");
        assertThat(result.getLocaleCode()).isEqualTo("en-GB");
        assertThat(result.isEnableFoodModule()).isFalse();
        assertThat(result.isEnableAlcoholModule()).isFalse();
        assertThat(result.getDefaultTempMinC()).isEqualTo(new BigDecimal("2.00"));
        assertThat(result.getDefaultTempMaxC()).isEqualTo(new BigDecimal("8.00"));
        assertThat(result.isReminderEmailEnabled()).isFalse();
        assertThat(result.getNotificationEmail()).isEqualTo("admin@example.com");
        assertThat(result.getRetentionUserMonths()).isEqualTo(24);
        assertThat(result.getRetentionAuditMonths()).isEqualTo(36);

        verify(settingsRepository).save(any(OrganizationSettings.class));
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentSettings() {
        // Given
        OrganizationSettingsRequest request = createUpdateRequest();
        when(settingsRepository.findById(ORG_NUMBER)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> settingsService.updateSettings(ORG_NUMBER, request, USER_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Organization settings not found for org");

        verify(settingsRepository, never()).save(any());
    }

    @Test
    void shouldUpdateSettingsWithPartialValues() {
        // Given
        OrganizationSettingsRequest request = new OrganizationSettingsRequest();
        request.setTimezoneName("America/New_York");
        request.setLocaleCode("en-US");
        request.setEnableFoodModule(true);
        request.setEnableAlcoholModule(true);
        request.setReminderEmailEnabled(true);
        // Leave other fields null

        when(settingsRepository.findById(ORG_NUMBER)).thenReturn(Optional.of(testSettings));
        when(settingsRepository.save(any(OrganizationSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        OrganizationSettingsResponse result = settingsService.updateSettings(ORG_NUMBER, request, USER_ID);

        // Then
        assertThat(result.getTimezoneName()).isEqualTo("America/New_York");
        assertThat(result.getLocaleCode()).isEqualTo("en-US");
        assertThat(result.getDefaultTempMinC()).isNull();
        assertThat(result.getDefaultTempMaxC()).isNull();
        assertThat(result.getNotificationEmail()).isNull();
    }

    // ==================== CREATE DEFAULT SETTINGS TESTS ====================

    @Test
    void shouldCreateDefaultSettings() {
        // Given
        when(settingsRepository.existsById(ORG_NUMBER)).thenReturn(false);
        when(settingsRepository.save(any(OrganizationSettings.class))).thenAnswer(inv -> {
            OrganizationSettings settings = inv.getArgument(0);
            settings.setCreatedAt(LocalDateTime.now());
            return settings;
        });

        // When
        OrganizationSettingsResponse result = settingsService.createDefaultSettings(ORG_NUMBER);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrgNumber()).isEqualTo(ORG_NUMBER);
        assertThat(result.getTimezoneName()).isEqualTo("Europe/Oslo");
        assertThat(result.getLocaleCode()).isEqualTo("nb-NO");
        assertThat(result.isEnableFoodModule()).isTrue();
        assertThat(result.isEnableAlcoholModule()).isTrue();
        assertThat(result.isReminderEmailEnabled()).isTrue();

        verify(settingsRepository).save(any(OrganizationSettings.class));
    }

    @Test
    void shouldReturnExistingSettingsWhenAlreadyExist() {
        // Given
        when(settingsRepository.existsById(ORG_NUMBER)).thenReturn(true);
        when(settingsRepository.findById(ORG_NUMBER)).thenReturn(Optional.of(testSettings));

        // When
        OrganizationSettingsResponse result = settingsService.createDefaultSettings(ORG_NUMBER);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrgNumber()).isEqualTo(ORG_NUMBER);
        verify(settingsRepository, never()).save(any());
    }

    @Test
    void shouldCreateSettingsWithCorrectDefaults() {
        // Given
        when(settingsRepository.existsById(ORG_NUMBER)).thenReturn(false);
        when(settingsRepository.save(any(OrganizationSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        OrganizationSettingsResponse result = settingsService.createDefaultSettings(ORG_NUMBER);

        // Then
        assertThat(result.isEnableFoodModule()).isTrue();
        assertThat(result.isEnableAlcoholModule()).isTrue();
        assertThat(result.isReminderEmailEnabled()).isTrue();
        assertThat(result.getTimezoneName()).isEqualTo("Europe/Oslo");
        assertThat(result.getLocaleCode()).isEqualTo("nb-NO");
    }

    // ==================== HELPER METHODS ====================

    private OrganizationSettings createTestSettings(Integer orgNumber) {
        return OrganizationSettings.builder()
                .orgNumber(orgNumber)
                .timezoneName("Europe/Oslo")
                .localeCode("nb-NO")
                .enableFoodModule(true)
                .enableAlcoholModule(true)
                .reminderEmailEnabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private OrganizationSettingsRequest createUpdateRequest() {
        OrganizationSettingsRequest request = new OrganizationSettingsRequest();
        request.setTimezoneName("Europe/London");
        request.setLocaleCode("en-GB");
        request.setEnableFoodModule(false);
        request.setEnableAlcoholModule(false);
        request.setDefaultTempMinC(new BigDecimal("2.00"));
        request.setDefaultTempMaxC(new BigDecimal("8.00"));
        request.setReminderEmailEnabled(false);
        request.setNotificationEmail("admin@example.com");
        request.setRetentionUserMonths(24);
        request.setRetentionAuditMonths(36);
        return request;
    }
}
