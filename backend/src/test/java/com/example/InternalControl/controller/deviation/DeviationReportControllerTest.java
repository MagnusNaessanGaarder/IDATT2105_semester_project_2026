package com.example.InternalControl.controller.deviation;

import com.example.InternalControl.AbstractIntegrationTest;
import com.example.InternalControl.dto.deviation.request.DeviationReportCreateRequest;
import com.example.InternalControl.model.deviation.DeviationReport;
import com.example.InternalControl.model.enums.DeviationStatus;
import com.example.InternalControl.model.enums.Severity;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.service.deviation.DeviationReportService;
import com.example.InternalControl.service.user.UserOrganizationService;
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

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for DeviationReportController using TestContainers.
 *
 * @author TriTacLe
 * @since 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class DeviationReportControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeviationReportService deviationReportService;

    @MockBean
    private UserOrganizationService userOrgService;

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        userDetails = new CustomUserDetails(1L, "test@example.com", "password", Collections.emptyList());
        when(userOrgService.isUserInOrganization(anyLong(), anyInt())).thenReturn(true);
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getReports_WithValidRequest_ReturnsReports() throws Exception {
        // Given
        DeviationReport report = DeviationReport.builder()
                .reportId(1L)
                .title("Test Report")
                .status(DeviationStatus.REPORTED)
                .severity(Severity.MINOR)
                .build();

        when(deviationReportService.getReportsByOrg(123456789))
                .thenReturn(List.of(report));

        // When & Then
        mockMvc.perform(get("/api/deviations")
                        .param("orgNumber", "123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reportId").value(1))
                .andExpect(jsonPath("$[0].title").value("Test Report"));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getReport_WithValidId_ReturnsReport() throws Exception {
        // Given
        DeviationReport report = DeviationReport.builder()
                .reportId(1L)
                .title("Test Report")
                .status(DeviationStatus.REPORTED)
                .build();

        when(deviationReportService.getReport(1L, 123456789))
                .thenReturn(report);

        // When & Then
        mockMvc.perform(get("/api/deviations/1")
                        .param("orgNumber", "123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(1))
                .andExpect(jsonPath("$.title").value("Test Report"));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void createReport_WithValidRequest_ReturnsCreatedReport() throws Exception {
        // Given
        DeviationReportCreateRequest request = DeviationReportCreateRequest.builder()
                .title("New Report")
                .description("Test Description")
                .severity(Severity.MINOR)
                .reportType(com.example.InternalControl.model.enums.ReportType.INCIDENT)
                .build();

        DeviationReport created = DeviationReport.builder()
                .reportId(1L)
                .title("New Report")
                .status(DeviationStatus.REPORTED)
                .build();

        when(deviationReportService.createReport(any(), anyInt(), anyLong()))
                .thenReturn(created);

        // When & Then
        mockMvc.perform(post("/api/deviations")
                        .param("orgNumber", "123456789")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").value(1))
                .andExpect(jsonPath("$.title").value("New Report"));
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void getReportsByStatus_WithValidStatus_ReturnsReports() throws Exception {
        // Given
        DeviationReport report = DeviationReport.builder()
                .reportId(1L)
                .title("Test Report")
                .status(DeviationStatus.REPORTED)
                .build();

        when(deviationReportService.getReportsByStatus(123456789, DeviationStatus.REPORTED))
                .thenReturn(List.of(report));

        // When & Then
        mockMvc.perform(get("/api/deviations/status/REPORTED")
                        .param("orgNumber", "123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("REPORTED"));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getOpenReportCount_ReturnsCount() throws Exception {
        // Given
        when(deviationReportService.getOpenReportCount(123456789))
                .thenReturn(5L);

        // When & Then
        mockMvc.perform(get("/api/deviations/count/open")
                        .param("orgNumber", "123456789"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getReports_WithServiceError_ReturnsNotFound() throws Exception {
        // Given
        when(deviationReportService.getReportsByOrg(123456789))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("Organization not found"));

        // When & Then
        mockMvc.perform(get("/api/deviations")
                        .param("orgNumber", "123456789"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getReports_WithInvalidOrgNumber_ReturnsNotFound() throws Exception {
        // Given
        when(userOrgService.isUserInOrganization(anyLong(), anyInt()))
                .thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/deviations")
                        .param("orgNumber", "999999999"))
                .andExpect(status().isNotFound());
    }
}