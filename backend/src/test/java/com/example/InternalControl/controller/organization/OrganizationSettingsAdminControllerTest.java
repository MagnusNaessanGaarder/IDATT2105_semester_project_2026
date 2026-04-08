package com.example.InternalControl.controller.organization;

import com.example.InternalControl.AbstractIntegrationTest;
import com.example.InternalControl.dto.organization.OrganizationSettingsRequest;
import com.example.InternalControl.model.organization.Organization;
import com.example.InternalControl.model.organization.OrganizationSettings;
import com.example.InternalControl.repository.organization.OrganizationRepository;
import com.example.InternalControl.repository.organization.OrganizationSettingsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
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

    @Autowired
    private OrganizationSettingsRepository settingsRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private static final Integer ORG_NUMBER = 937219997;
    private static final String BASE_URL = "/api/admin/organizations/settings";

    @BeforeEach
    void setUp() {
        // Setup mocks for repositories if needed
        Organization org = new Organization();
        org.setOrgNumber(ORG_NUMBER);
        org.setLegalName("Test Organization");
        
        when(organizationRepository.findById(ORG_NUMBER)).thenReturn(Optional.of(org));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getSettings_AsAdmin_ReturnsOk() throws Exception {
        // When & Then
        mockMvc.perform(get(BASE_URL)
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void getSettings_AsManager_ReturnsOk() throws Exception {
        // When & Then
        mockMvc.perform(get(BASE_URL)
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getSettings_AsEmployee_ReturnsForbidden() throws Exception {
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

        // When & Then
        mockMvc.perform(put(BASE_URL)
                        .param("orgNumber", ORG_NUMBER.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezoneName").value("Europe/London"));
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void updateSettings_AsManager_ReturnsForbidden() throws Exception {
        // Given
        OrganizationSettingsRequest request = new OrganizationSettingsRequest();
        request.setTimezoneName("Europe/London");

        // When & Then - only ADMIN can update, MANAGER can only view
        mockMvc.perform(put(BASE_URL)
                        .param("orgNumber", ORG_NUMBER.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
