package com.example.InternalControl.controller.organization;

import com.example.InternalControl.AbstractIntegrationTest;
import com.example.InternalControl.dto.settings.OrganizationSettingsRequest;
import com.example.InternalControl.dto.settings.OrganizationSettingsResponse;
import com.example.InternalControl.service.settings.OrganizationSettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OrganizationSettingsAdminController using TestContainers.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class OrganizationSettingsAdminControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrganizationSettingsService settingsService;

    private static final Integer ORG_NUMBER = 123456789;
    private static final String BASE_URL = "/api/v1/admin/organization-settings";

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getSettings_AsAdmin_ReturnsOk() throws Exception {
        // Given
        OrganizationSettingsResponse settings = OrganizationSettingsResponse.builder()
                .orgNumber(ORG_NUMBER)
                .timezoneName("Europe/Oslo")
                .build();

        when(settingsService.getSettings(ORG_NUMBER)).thenReturn(settings);

        // When & Then
        mockMvc.perform(get(BASE_URL)
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orgNumber").value(ORG_NUMBER));
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void getSettings_AsManager_ReturnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get(BASE_URL)
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void updateSettings_AsAdmin_ReturnsOk() throws Exception {
        // Given
        OrganizationSettingsRequest request = new OrganizationSettingsRequest();
        request.setTimezoneName("Europe/London");

        OrganizationSettingsResponse settings = OrganizationSettingsResponse.builder()
                .orgNumber(ORG_NUMBER)
                .timezoneName("Europe/London")
                .build();

        when(settingsService.updateSettings(eq(ORG_NUMBER), any(), anyLong()))
                .thenReturn(settings);

        // When & Then
        mockMvc.perform(put(BASE_URL)
                        .param("orgNumber", ORG_NUMBER.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezoneName").value("Europe/London"));
    }
}
