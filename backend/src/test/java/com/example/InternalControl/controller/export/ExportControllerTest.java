package com.example.InternalControl.controller.export;

import com.example.InternalControl.AbstractIntegrationTest;
import com.example.InternalControl.dto.export.request.ExportRequest;
import com.example.InternalControl.dto.export.response.ExportResponse;
import com.example.InternalControl.model.export.ExportFormat;
import com.example.InternalControl.model.export.ExportStatus;
import com.example.InternalControl.model.export.ExportType;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.service.export.ExportService;
import com.example.InternalControl.service.user.UserOrganizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ExportController using TestContainers.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ExportControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExportService exportService;

    @MockBean
    private UserOrganizationService userOrgService;

    private static final Integer ORG_NUMBER = 123456789;
    private static final String BASE_URL = "/api/v1/exports";

    @BeforeEach
    void setUp() {
        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
        if (existingAuth != null) {
            CustomUserDetails userDetails = new CustomUserDetails(
                    1L, existingAuth.getName(), "password", existingAuth.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
        }
        when(userOrgService.isUserInOrganization(anyLong(), anyInt())).thenReturn(true);
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void createExport_AsEmployee_ReturnsCreated() throws Exception {
        // Given
        ExportRequest request = new ExportRequest();
        request.setExportType(ExportType.CHECKLIST_REPORT);
        request.setFormat(ExportFormat.PDF);

        ExportResponse response = ExportResponse.builder()
                .exportJobId(1L)
                .status(ExportStatus.PENDING)
                .build();

        when(exportService.createExportJob(any(), eq(ORG_NUMBER), anyLong())).thenReturn(response);

        // When & Then
        mockMvc.perform(post(BASE_URL)
                        .param("orgNumber", ORG_NUMBER.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exportJobId").value(1));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getExportStatus_AsEmployee_ReturnsOk() throws Exception {
        // Given
        ExportResponse response = ExportResponse.builder()
                .exportJobId(1L)
                .status(ExportStatus.COMPLETED)
                .build();

        when(exportService.getExportStatus(1L, ORG_NUMBER)).thenReturn(response);

        // When & Then
        mockMvc.perform(get(BASE_URL + "/1")
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportJobId").value(1));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getDownloadUrl_AsEmployee_ReturnsOk() throws Exception {
        // Given
        when(exportService.getDownloadUrl(1L, ORG_NUMBER)).thenReturn("https://example.com/download/1");

        // When & Then
        mockMvc.perform(get(BASE_URL + "/1/download")
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isOk());
    }
}
