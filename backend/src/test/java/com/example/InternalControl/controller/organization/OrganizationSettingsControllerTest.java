package com.example.InternalControl.controller.organization;

import com.example.InternalControl.AbstractIntegrationTest;
import com.example.InternalControl.dto.settings.OrganizationSettingsRequest;
import com.example.InternalControl.model.organization.OrganizationSettings;
import com.example.InternalControl.repository.organization.OrganizationSettingsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OrganizationSettingsController using TestContainers.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class OrganizationSettingsControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrganizationSettingsRepository settingsRepository;

    private OrganizationSettings testSettings;

    @BeforeEach
    void setUp() {
        testSettings = OrganizationSettings.builder()
                .orgNumber(937219997)
                .timezoneName("Europe/Oslo")
                .localeCode("nb-NO")
                .enableFoodModule(true)
                .enableAlcoholModule(true)
                .defaultTempMinC(BigDecimal.valueOf(2.0))
                .defaultTempMaxC(BigDecimal.valueOf(8.0))
                .reminderEmailEnabled(true)
                .notificationEmail("admin@everest-sushi.no")
                .retentionUserMonths(24)
                .retentionAuditMonths(36)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getSettings_AsAdmin_ReturnsSettings() throws Exception {
        // Given
        when(settingsRepository.findById(937219997))
                .thenReturn(Optional.of(testSettings));

        // When & Then
        mockMvc.perform(get("/api/admin/organizations/settings")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orgNumber").value(937219997))
                .andExpect(jsonPath("$.timezoneName").value("Europe/Oslo"))
                .andExpect(jsonPath("$.localeCode").value("nb-NO"))
                .andExpect(jsonPath("$.enableFoodModule").value(true))
                .andExpect(jsonPath("$.enableAlcoholModule").value(true))
                .andExpect(jsonPath("$.defaultTempMinC").value(2.0))
                .andExpect(jsonPath("$.defaultTempMaxC").value(8.0))
                .andExpect(jsonPath("$.reminderEmailEnabled").value(true))
                .andExpect(jsonPath("$.notificationEmail").value("admin@everest-sushi.no"));
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void getSettings_AsManager_ReturnsSettings() throws Exception {
        // Given
        when(settingsRepository.findById(937219997))
                .thenReturn(Optional.of(testSettings));

        // When & Then
        mockMvc.perform(get("/api/admin/organizations/settings")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getSettings_AsEmployee_ReturnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/admin/organizations/settings")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getSettings_NotFound_ReturnsNotFound() throws Exception {
        // Given
        when(settingsRepository.findById(937219997))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/admin/organizations/settings")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void updateSettings_AsAdmin_ReturnsUpdatedSettings() throws Exception {
        // Given
        OrganizationSettingsRequest request = OrganizationSettingsRequest.builder()
                .timezoneName("Europe/London")
                .localeCode("en-GB")
                .enableFoodModule(false)
                .enableAlcoholModule(true)
                .defaultTempMinC(BigDecimal.valueOf(0.0))
                .defaultTempMaxC(BigDecimal.valueOf(5.0))
                .reminderEmailEnabled(false)
                .notificationEmail("new@everest-sushi.no")
                .retentionUserMonths(12)
                .retentionAuditMonths(24)
                .build();

        OrganizationSettings updatedSettings = OrganizationSettings.builder()
                .orgNumber(937219997)
                .timezoneName("Europe/London")
                .localeCode("en-GB")
                .enableFoodModule(false)
                .enableAlcoholModule(true)
                .defaultTempMinC(BigDecimal.valueOf(0.0))
                .defaultTempMaxC(BigDecimal.valueOf(5.0))
                .reminderEmailEnabled(false)
                .notificationEmail("new@everest-sushi.no")
                .retentionUserMonths(12)
                .retentionAuditMonths(24)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(settingsRepository.findById(937219997))
                .thenReturn(Optional.of(testSettings));
        when(settingsRepository.save(any(OrganizationSettings.class)))
                .thenReturn(updatedSettings);

        // When & Then
        mockMvc.perform(put("/api/admin/organizations/settings")
                        .param("orgNumber", "937219997")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezoneName").value("Europe/London"))
                .andExpect(jsonPath("$.localeCode").value("en-GB"))
                .andExpect(jsonPath("$.enableFoodModule").value(false))
                .andExpect(jsonPath("$.defaultTempMinC").value(0.0))
                .andExpect(jsonPath("$.notificationEmail").value("new@everest-sushi.no"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void updateSettings_PartialUpdate_ReturnsUpdatedSettings() throws Exception {
        // Given
        OrganizationSettingsRequest request = OrganizationSettingsRequest.builder()
                .timezoneName("America/New_York")
                .localeCode("nb-NO")
                .enableFoodModule(true)
                .enableAlcoholModule(true)
                .reminderEmailEnabled(true)
                .build();

        OrganizationSettings updatedSettings = OrganizationSettings.builder()
                .orgNumber(937219997)
                .timezoneName("America/New_York")
                .localeCode("nb-NO")
                .enableFoodModule(true)
                .enableAlcoholModule(true)
                .reminderEmailEnabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(settingsRepository.findById(937219997))
                .thenReturn(Optional.of(testSettings));
        when(settingsRepository.save(any(OrganizationSettings.class)))
                .thenReturn(updatedSettings);

        // When & Then
        mockMvc.perform(put("/api/admin/organizations/settings")
                        .param("orgNumber", "937219997")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezoneName").value("America/New_York"))
                .andExpect(jsonPath("$.localeCode").value("nb-NO"));
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void updateSettings_AsManager_ReturnsForbidden() throws Exception {
        // Given
        OrganizationSettingsRequest request = OrganizationSettingsRequest.builder()
                .timezoneName("Europe/London")
                .localeCode("en-GB")
                .enableFoodModule(true)
                .enableAlcoholModule(true)
                .reminderEmailEnabled(true)
                .build();

        // When & Then
        mockMvc.perform(put("/api/admin/organizations/settings")
                        .param("orgNumber", "937219997")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void updateSettings_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Given - invalid email format
        String invalidRequest = "{\"notificationEmail\": \"invalid-email\"}";

        // When & Then
        mockMvc.perform(put("/api/admin/organizations/settings")
                        .param("orgNumber", "937219997")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void updateSettings_NotFound_ReturnsNotFound() throws Exception {
        // Given
        OrganizationSettingsRequest request = OrganizationSettingsRequest.builder()
                .timezoneName("Asia/Tokyo")
                .localeCode("ja-JP")
                .enableFoodModule(true)
                .enableAlcoholModule(true)
                .reminderEmailEnabled(true)
                .build();

        when(settingsRepository.findById(937219997))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(put("/api/admin/organizations/settings")
                        .param("orgNumber", "937219997")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void updateSettings_OrganizationNotFound_ReturnsNotFound() throws Exception {
        // Given
        OrganizationSettingsRequest request = OrganizationSettingsRequest.builder()
                .timezoneName("Europe/London")
                .localeCode("en-GB")
                .enableFoodModule(true)
                .enableAlcoholModule(true)
                .reminderEmailEnabled(true)
                .build();

        when(settingsRepository.findById(999999999))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(put("/api/admin/organizations/settings")
                        .param("orgNumber", "999999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
