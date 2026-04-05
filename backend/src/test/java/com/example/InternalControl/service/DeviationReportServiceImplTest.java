package com.example.InternalControl.service;

import com.example.InternalControl.dto.deviation.request.*;
import com.example.InternalControl.model.user.AppUser;
import com.example.InternalControl.model.deviation.DeviationReport;
import com.example.InternalControl.model.organization.Location;
import com.example.InternalControl.model.enums.DeviationStatus;
import com.example.InternalControl.model.enums.ReportType;
import com.example.InternalControl.model.enums.Severity;
import com.example.InternalControl.repository.user.AppUserRepository;
import com.example.InternalControl.repository.deviation.DeviationReportRepository;
import com.example.InternalControl.repository.organization.LocationRepository;
import com.example.InternalControl.repository.organization.OrganizationRepository;
import com.example.InternalControl.service.deviation.DeviationReportServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DeviationReportServiceImpl.
 *
 * @author TriTacLe
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class DeviationReportServiceImplTest {

    @Mock
    private DeviationReportRepository deviationReportRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private DeviationReportServiceImpl deviationReportService;

    private static final Integer ORG_NUMBER = 123;
    private static final Long USER_ID = 1L;
    private static final Long REPORT_ID = 100L;

    private AppUser testUser;
    private Location testLocation;
    private DeviationReport testReport;

    @BeforeEach
    void setUp() {
        testUser = createTestUser(USER_ID, "Test User");
        testLocation = createTestLocation(1L, "Kitchen");
        testReport = createTestReport(REPORT_ID, DeviationStatus.REPORTED);
    }

    // ==================== CREATE REPORT TESTS ====================

    @Test
    void shouldCreateReport() {
        // Given
        DeviationReportCreateRequest request = DeviationReportCreateRequest.builder()
                .reportType(ReportType.INCIDENT)
                .severity(Severity.MAJOR)
                .title("Test Incident")
                .description("Test description")
                .locationId(1L)
                .build();

        when(organizationRepository.existsById(ORG_NUMBER)).thenReturn(true);
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(locationRepository.findByLocationIdAndOrgNumber(1L, ORG_NUMBER))
                .thenReturn(Optional.of(testLocation));
        when(deviationReportRepository.save(any(DeviationReport.class))).thenAnswer(inv -> {
            DeviationReport saved = inv.getArgument(0);
            saved.setReportId(REPORT_ID);
            return saved;
        });

        // When
        DeviationReport result = deviationReportService.createReport(request, ORG_NUMBER, USER_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getReportId()).isEqualTo(REPORT_ID);
        assertThat(result.getTitle()).isEqualTo("Test Incident");
        assertThat(result.getSeverity()).isEqualTo(Severity.MAJOR);
        assertThat(result.getStatus()).isEqualTo(DeviationStatus.REPORTED);
        verify(deviationReportRepository).save(any(DeviationReport.class));
    }

    @Test
    void shouldThrowWhenOrgNotFound() {
        // Given
        DeviationReportCreateRequest request = DeviationReportCreateRequest.builder()
                .reportType(ReportType.INCIDENT)
                .severity(Severity.MINOR)
                .title("Test")
                .description("Test")
                .build();

        when(organizationRepository.existsById(ORG_NUMBER)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> deviationReportService.createReport(request, ORG_NUMBER, USER_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Organization not found");

        verify(deviationReportRepository, never()).save(any());
    }

    // ==================== GET REPORT TESTS ====================

    @Test
    void shouldGetReport() {
        // Given
        when(deviationReportRepository.findByReportIdAndOrgNumber(REPORT_ID, ORG_NUMBER))
                .thenReturn(Optional.of(testReport));

        // When
        DeviationReport result = deviationReportService.getReport(REPORT_ID, ORG_NUMBER);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getReportId()).isEqualTo(REPORT_ID);
        assertThat(result.getTitle()).isEqualTo("Test Report");
    }

    @Test
    void shouldThrowWhenReportNotFound() {
        // Given
        when(deviationReportRepository.findByReportIdAndOrgNumber(REPORT_ID, ORG_NUMBER))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> deviationReportService.getReport(REPORT_ID, ORG_NUMBER))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Deviation report not found");
    }

    // ==================== UPDATE REPORT TESTS ====================

    @Test
    void shouldUpdateReport() {
        // Given
        DeviationReportUpdateRequest request = DeviationReportUpdateRequest.builder()
                .title("Updated Title")
                .build();

        when(deviationReportRepository.findByReportIdAndOrgNumber(REPORT_ID, ORG_NUMBER))
                .thenReturn(Optional.of(testReport));
        when(deviationReportRepository.save(any(DeviationReport.class))).thenReturn(testReport);

        // When
        DeviationReport result = deviationReportService.updateReport(REPORT_ID, request, ORG_NUMBER);

        // Then
        assertThat(result.getTitle()).isEqualTo("Updated Title");
        verify(deviationReportRepository).save(testReport);
    }

    @Test
    void shouldNotUpdateClosedReport() {
        // Given
        testReport.setStatus(DeviationStatus.CLOSED);
        DeviationReportUpdateRequest request = DeviationReportUpdateRequest.builder()
                .title("Updated Title")
                .build();

        when(deviationReportRepository.findByReportIdAndOrgNumber(REPORT_ID, ORG_NUMBER))
                .thenReturn(Optional.of(testReport));

        // When/Then
        assertThatThrownBy(() -> deviationReportService.updateReport(REPORT_ID, request, ORG_NUMBER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot edit report in status: CLOSED");
    }

    // ==================== DELETE REPORT TESTS ====================

    @Test
    void shouldDeleteReport() {
        // Given
        when(deviationReportRepository.findByReportIdAndOrgNumber(REPORT_ID, ORG_NUMBER))
                .thenReturn(Optional.of(testReport));

        // When
        deviationReportService.deleteReport(REPORT_ID, ORG_NUMBER);

        // Then
        verify(deviationReportRepository).delete(testReport);
    }

    // ==================== STATUS UPDATE TESTS ====================

    @Test
    void shouldUpdateStatusFromReportedToInvestigation() {
        // Given
        testReport.setStatus(DeviationStatus.REPORTED);
        when(deviationReportRepository.findByReportIdAndOrgNumber(REPORT_ID, ORG_NUMBER))
                .thenReturn(Optional.of(testReport));
        when(deviationReportRepository.save(any(DeviationReport.class))).thenReturn(testReport);

        // When
        DeviationReport result = deviationReportService.updateStatus(
                REPORT_ID, DeviationStatus.UNDER_INVESTIGATION, ORG_NUMBER, USER_ID);

        // Then
        assertThat(result.getStatus()).isEqualTo(DeviationStatus.UNDER_INVESTIGATION);
    }

    @Test
    void shouldCloseReport() {
        // Given
        testReport.setStatus(DeviationStatus.CORRECTIVE_ACTION_COMPLETED);
        when(deviationReportRepository.findByReportIdAndOrgNumber(REPORT_ID, ORG_NUMBER))
                .thenReturn(Optional.of(testReport));
        when(deviationReportRepository.save(any(DeviationReport.class))).thenReturn(testReport);

        // When
        DeviationReport result = deviationReportService.updateStatus(
                REPORT_ID, DeviationStatus.CLOSED, ORG_NUMBER, USER_ID);

        // Then
        assertThat(result.getStatus()).isEqualTo(DeviationStatus.CLOSED);
        assertThat(result.getClosedAt()).isNotNull();
    }

    @Test
    void shouldRejectInvalidStatusTransition() {
        // Given
        testReport.setStatus(DeviationStatus.CLOSED);
        when(deviationReportRepository.findByReportIdAndOrgNumber(REPORT_ID, ORG_NUMBER))
                .thenReturn(Optional.of(testReport));

        // When/Then
        assertThatThrownBy(() -> deviationReportService.updateStatus(
                REPORT_ID, DeviationStatus.REPORTED, ORG_NUMBER, USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid status transition");
    }

    // ==================== WORKFLOW ACTION TESTS ====================

    @Test
    void shouldAddImmediateAction() {
        // Given
        testReport.setStatus(DeviationStatus.UNDER_INVESTIGATION);
        DeviationActionRequest request = DeviationActionRequest.builder()
                .actionText("Immediate action taken")
                .build();

        when(deviationReportRepository.findByReportIdAndOrgNumber(REPORT_ID, ORG_NUMBER))
                .thenReturn(Optional.of(testReport));
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(deviationReportRepository.save(any(DeviationReport.class))).thenReturn(testReport);

        // When
        DeviationReport result = deviationReportService.addImmediateAction(
                REPORT_ID, request, ORG_NUMBER, USER_ID);

        // Then
        assertThat(result.getImmediateActionText()).isEqualTo("Immediate action taken");
        assertThat(result.getImmediateActionSignedBy()).isEqualTo(testUser);
    }

    @Test
    void shouldNotAddImmediateActionWhenNotInInvestigation() {
        // Given
        testReport.setStatus(DeviationStatus.REPORTED);
        DeviationActionRequest request = DeviationActionRequest.builder()
                .actionText("Immediate action")
                .build();

        when(deviationReportRepository.findByReportIdAndOrgNumber(REPORT_ID, ORG_NUMBER))
                .thenReturn(Optional.of(testReport));

        // When/Then
        assertThatThrownBy(() -> deviationReportService.addImmediateAction(
                REPORT_ID, request, ORG_NUMBER, USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Immediate action can only be added during investigation");
    }

    @Test
    void shouldAdvanceStatusWhenBothActionsProvided() {
        // Given
        testReport.setStatus(DeviationStatus.UNDER_INVESTIGATION);
        testReport.setImmediateActionText("Already added");
        testReport.setImmediateActionSignedBy(testUser);

        DeviationActionRequest request = DeviationActionRequest.builder()
                .actionText("Root cause analysis")
                .build();

        when(deviationReportRepository.findByReportIdAndOrgNumber(REPORT_ID, ORG_NUMBER))
                .thenReturn(Optional.of(testReport));
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(deviationReportRepository.save(any(DeviationReport.class))).thenReturn(testReport);

        // When
        DeviationReport result = deviationReportService.addCauseAnalysis(
                REPORT_ID, request, ORG_NUMBER, USER_ID);

        // Then
        assertThat(result.getStatus()).isEqualTo(DeviationStatus.CORRECTIVE_ACTION_PLANNED);
    }

    // ==================== ASSIGNMENT TESTS ====================

    @Test
    void shouldAssignReport() {
        // Given
        Long assignedToId = 2L;
        AppUser assignedTo = createTestUser(assignedToId, "Assigned User");

        when(deviationReportRepository.findByReportIdAndOrgNumber(REPORT_ID, ORG_NUMBER))
                .thenReturn(Optional.of(testReport));
        when(appUserRepository.findById(assignedToId)).thenReturn(Optional.of(assignedTo));
        when(deviationReportRepository.save(any(DeviationReport.class))).thenReturn(testReport);

        // When
        DeviationReport result = deviationReportService.assignReport(
                REPORT_ID, assignedToId, ORG_NUMBER, USER_ID);

        // Then
        assertThat(result.getAssignedTo()).isEqualTo(assignedTo);
    }

    @Test
    void shouldNotAssignClosedReport() {
        // Given
        testReport.setStatus(DeviationStatus.CLOSED);

        when(deviationReportRepository.findByReportIdAndOrgNumber(REPORT_ID, ORG_NUMBER))
                .thenReturn(Optional.of(testReport));

        // When/Then
        assertThatThrownBy(() -> deviationReportService.assignReport(
                REPORT_ID, 2L, ORG_NUMBER, USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot assign closed report");
    }

    // ==================== LISTING TESTS ====================

    @Test
    void shouldGetReportsByOrg() {
        // Given
        List<DeviationReport> reports = Arrays.asList(
                createTestReport(1L, DeviationStatus.REPORTED),
                createTestReport(2L, DeviationStatus.CLOSED)
        );
        when(deviationReportRepository.findByOrgNumber(ORG_NUMBER)).thenReturn(reports);

        // When
        List<DeviationReport> result = deviationReportService.getReportsByOrg(ORG_NUMBER);

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    void shouldGetReportsByStatus() {
        // Given
        List<DeviationReport> reports = Collections.singletonList(
                createTestReport(1L, DeviationStatus.REPORTED)
        );
        when(deviationReportRepository.findByOrgNumberAndStatus(ORG_NUMBER, DeviationStatus.REPORTED))
                .thenReturn(reports);

        // When
        List<DeviationReport> result = deviationReportService.getReportsByStatus(
                ORG_NUMBER, DeviationStatus.REPORTED);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(DeviationStatus.REPORTED);
    }

    @Test
    void shouldGetReportsBySeverity() {
        // Given
        List<DeviationReport> reports = Collections.singletonList(
                createTestReport(1L, DeviationStatus.REPORTED)
        );
        when(deviationReportRepository.findByOrgNumberAndSeverity(ORG_NUMBER, Severity.CRITICAL))
                .thenReturn(reports);

        // When
        List<DeviationReport> result = deviationReportService.getReportsBySeverity(
                ORG_NUMBER, Severity.CRITICAL);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void shouldCountOpenReports() {
        // Given
        when(deviationReportRepository.countOpenByOrgNumber(ORG_NUMBER)).thenReturn(5L);

        // When
        Long result = deviationReportService.getOpenReportCount(ORG_NUMBER);

        // Then
        assertThat(result).isEqualTo(5L);
    }

    // ==================== HELPER METHODS ====================

    private AppUser createTestUser(Long userId, String name) {
        AppUser user = new AppUser();
        user.setUserId(userId);
        user.setDisplayName(name);
        return user;
    }

    private Location createTestLocation(Long locationId, String name) {
        Location location = new Location();
        location.setLocationId(locationId);
        location.setName(name);
        location.setOrgNumber(ORG_NUMBER);
        return location;
    }

    private DeviationReport createTestReport(Long reportId, DeviationStatus status) {
        return DeviationReport.builder()
                .reportId(reportId)
                .orgNumber(ORG_NUMBER)
                .reportType(ReportType.INCIDENT)
                .severity(Severity.MAJOR)
                .title("Test Report")
                .description("Test description")
                .reportDate(LocalDate.now())
                .reportedBy(testUser)
                .status(status)
                .build();
    }
}
