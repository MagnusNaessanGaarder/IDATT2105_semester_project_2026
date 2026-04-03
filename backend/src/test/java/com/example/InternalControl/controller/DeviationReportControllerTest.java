package com.example.InternalControl.controller;

import com.example.InternalControl.dto.deviation.DeviationReportDto;
import com.example.InternalControl.dto.deviation.request.DeviationReportCreateRequest;
import com.example.InternalControl.service.deviation.mapper.DeviationReportMapper;
import com.example.InternalControl.model.deviation.DeviationReport;
import com.example.InternalControl.shared.enums.DeviationStatus;
import com.example.InternalControl.shared.enums.ReportType;
import com.example.InternalControl.shared.enums.Severity;
import com.example.InternalControl.security.AuthenticationFacade;
import com.example.InternalControl.service.deviation.DeviationReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.InternalControl.controller.deviation.DeviationReportController;

/**
 * Unit tests for DeviationReportController.
 *
 * @author TriTacLe
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class DeviationReportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DeviationReportService deviationReportService;

    @Mock
    private DeviationReportMapper deviationReportMapper;

    @Mock
    private AuthenticationFacade authenticationFacade;

    @InjectMocks
    private DeviationReportController controller;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    void getReports_WithValidRequest_ReturnsList() throws Exception {
        // Given
        Long userId = 1L;
        Integer orgNumber = 123456789;
        DeviationReport report = createTestReport();
        DeviationReportDto dto = createTestDto();

        when(authenticationFacade.extractAndValidateUser(any(), eq(orgNumber))).thenReturn(userId);
        when(deviationReportService.getReportsByOrg(orgNumber)).thenReturn(List.of(report));
        when(deviationReportMapper.toDto(report)).thenReturn(dto);

        // When & Then
        mockMvc.perform(get("/api/deviations")
                        .param("orgNumber", orgNumber.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].title").value("Test Report"));
    }

    @Test
    void getReportById_WithValidId_ReturnsReport() throws Exception {
        // Given
        Long userId = 1L;
        Integer orgNumber = 123456789;
        Long reportId = 1L;
        DeviationReport report = createTestReport();
        DeviationReportDto dto = createTestDto();

        when(authenticationFacade.extractAndValidateUser(any(), eq(orgNumber))).thenReturn(userId);
        when(deviationReportService.getReport(reportId, orgNumber)).thenReturn(report);
        when(deviationReportMapper.toDto(report)).thenReturn(dto);

        // When & Then
        mockMvc.perform(get("/api/deviations/{id}", reportId)
                        .param("orgNumber", orgNumber.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Report"));
    }

    @Test
    void createReport_WithValidRequest_ReturnsCreated() throws Exception {
        // Given
        Long userId = 1L;
        Integer orgNumber = 123456789;
        DeviationReportCreateRequest request = createTestCreateRequest();
        DeviationReport created = createTestReport();
        DeviationReportDto dto = createTestDto();

        when(authenticationFacade.extractAndValidateUser(any(), eq(orgNumber))).thenReturn(userId);
        when(deviationReportService.createReport(any(), eq(orgNumber), eq(userId))).thenReturn(created);
        when(deviationReportMapper.toDto(created)).thenReturn(dto);

        // When & Then
        mockMvc.perform(post("/api/deviations")
                        .param("orgNumber", orgNumber.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.title").value("Test Report"));
    }

    @Test
    void createReport_WithNullTitle_ReturnsBadRequest() throws Exception {
        // Given
        Integer orgNumber = 123456789;
        DeviationReportCreateRequest invalidRequest = DeviationReportCreateRequest.builder()
                .reportType(ReportType.INCIDENT)
                .severity(Severity.MAJOR)
                .title(null)
                .description("Test description")
                .build();

        // When & Then
        mockMvc.perform(post("/api/deviations")
                        .param("orgNumber", orgNumber.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    private DeviationReport createTestReport() {
        DeviationReport report = new DeviationReport();
        report.setTitle("Test Report");
        report.setDescription("Test Description");
        report.setReportType(ReportType.INCIDENT);
        report.setSeverity(Severity.MAJOR);
        report.setStatus(DeviationStatus.DRAFT);
        report.setReportDate(LocalDate.now());
        return report;
    }

    private DeviationReportDto createTestDto() {
        return DeviationReportDto.builder()
                .title("Test Report")
                .description("Test Description")
                .reportType(ReportType.INCIDENT)
                .severity(Severity.MAJOR)
                .status(DeviationStatus.DRAFT)
                .reportDate(LocalDate.now())
                .build();
    }

    private DeviationReportCreateRequest createTestCreateRequest() {
        return DeviationReportCreateRequest.builder()
                .reportType(ReportType.INCIDENT)
                .severity(Severity.MAJOR)
                .title("Test Report")
                .description("Test Description")
                .build();
    }
}
