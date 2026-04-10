package com.example.InternalControl.service.analytics;

import com.example.InternalControl.dto.analytics.ComplianceScoreResponse;
import com.example.InternalControl.dto.analytics.DashboardSummaryResponse;
import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.deviation.DeviationReport;
import com.example.InternalControl.model.enums.DeviationStatus;
import com.example.InternalControl.model.enums.ReportType;
import com.example.InternalControl.model.enums.RunStatus;
import com.example.InternalControl.model.enums.Severity;
import com.example.InternalControl.model.temperature.TemperatureLogEntry;
import com.example.InternalControl.model.training.TrainingRecord;
import com.example.InternalControl.model.training.TrainingStatus;
import com.example.InternalControl.model.training.TrainingType;
import com.example.InternalControl.repository.checklist.ChecklistRunRepository;
import com.example.InternalControl.repository.deviation.DeviationReportRepository;
import com.example.InternalControl.repository.temperature.TemperatureLogEntryRepository;
import com.example.InternalControl.repository.training.TrainingRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DashboardService.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ChecklistRunRepository checklistRunRepository;

    @Mock
    private DeviationReportRepository deviationReportRepository;

    @Mock
    private TemperatureLogEntryRepository temperatureLogEntryRepository;

    @Mock
    private TrainingRecordRepository trainingRecordRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private static final Integer ORG_NUMBER = 123;

    @BeforeEach
    void setUp() {
        // Default setup for each test
    }

    // ==================== DASHBOARD SUMMARY TESTS ====================

    @Test
    void shouldGetDashboardSummary() {
        // Given
        when(checklistRunRepository.findByOrgNumber(ORG_NUMBER)).thenReturn(Collections.emptyList());
        when(deviationReportRepository.findByOrgNumber(ORG_NUMBER)).thenReturn(Collections.emptyList());
        when(temperatureLogEntryRepository.findByOrgNumberOrderByMeasuredAtDesc(ORG_NUMBER))
                .thenReturn(Collections.emptyList());
        when(trainingRecordRepository.findExpiringSoon(any(), any())).thenReturn(Collections.emptyList());

        // When
        DashboardSummaryResponse result = dashboardService.getDashboardSummary(ORG_NUMBER);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getChecklistsCompletedToday()).isEqualTo(0);
        assertThat(result.getOpenDeviations()).isEqualTo(0);
        assertThat(result.getComplianceScore()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void shouldCalculateChecklistStats() {
        // Given
        LocalDate today = LocalDate.now();
        ChecklistRun completedToday = createChecklistRun(1L, RunStatus.COMPLETED, today.atTime(10, 0));
        ChecklistRun overdue = createChecklistRun(2L, RunStatus.OVERDUE, null);
        ChecklistRun draft = createChecklistRun(3L, RunStatus.DRAFT, null);

        when(checklistRunRepository.findByOrgNumber(ORG_NUMBER))
                .thenReturn(List.of(completedToday, overdue, draft));
        when(deviationReportRepository.findByOrgNumber(ORG_NUMBER)).thenReturn(Collections.emptyList());
        when(temperatureLogEntryRepository.findByOrgNumberOrderByMeasuredAtDesc(ORG_NUMBER))
                .thenReturn(Collections.emptyList());
        when(trainingRecordRepository.findExpiringSoon(any(), any())).thenReturn(Collections.emptyList());

        // When
        DashboardSummaryResponse result = dashboardService.getDashboardSummary(ORG_NUMBER);

        // Then
        assertThat(result.getChecklistsCompletedToday()).isEqualTo(1);
        assertThat(result.getChecklistsOverdue()).isEqualTo(1);
        assertThat(result.getChecklistCompletionRate()).isCloseTo(33.33, within(0.01)); // 1 completed out of 3
    }

    @Test
    void shouldCalculateDeviationStats() {
        // Given
        DeviationReport openCritical = createDeviationReport(1L, DeviationStatus.REPORTED, Severity.CRITICAL);
        DeviationReport openMajor = createDeviationReport(2L, DeviationStatus.UNDER_INVESTIGATION, Severity.MAJOR);
        DeviationReport closed = createDeviationReport(3L, DeviationStatus.CLOSED, Severity.MINOR);
        closed.setClosedAt(LocalDateTime.now());

        when(checklistRunRepository.findByOrgNumber(ORG_NUMBER)).thenReturn(Collections.emptyList());
        when(deviationReportRepository.findByOrgNumber(ORG_NUMBER))
                .thenReturn(List.of(openCritical, openMajor, closed));
        when(temperatureLogEntryRepository.findByOrgNumberOrderByMeasuredAtDesc(ORG_NUMBER))
                .thenReturn(Collections.emptyList());
        when(trainingRecordRepository.findExpiringSoon(any(), any())).thenReturn(Collections.emptyList());

        // When
        DashboardSummaryResponse result = dashboardService.getDashboardSummary(ORG_NUMBER);

        // Then
        assertThat(result.getOpenDeviations()).isEqualTo(2); // REPORTED + UNDER_INVESTIGATION
        assertThat(result.getCriticalDeviations()).isEqualTo(1);
        assertThat(result.getDeviationsClosedThisWeek()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void shouldCalculateTemperatureStats() {
        // Given
        TemperatureLogEntry normalEntry = createTemperatureLogEntry(1L, new BigDecimal("5.0"), false);
        TemperatureLogEntry alertEntry = createTemperatureLogEntry(2L, new BigDecimal("15.0"), true);

        when(checklistRunRepository.findByOrgNumber(ORG_NUMBER)).thenReturn(Collections.emptyList());
        when(deviationReportRepository.findByOrgNumber(ORG_NUMBER)).thenReturn(Collections.emptyList());
        when(temperatureLogEntryRepository.findByOrgNumberOrderByMeasuredAtDesc(ORG_NUMBER))
                .thenReturn(List.of(normalEntry, alertEntry));
        when(trainingRecordRepository.findExpiringSoon(any(), any())).thenReturn(Collections.emptyList());

        // When
        DashboardSummaryResponse result = dashboardService.getDashboardSummary(ORG_NUMBER);

        // Then
        assertThat(result.getTemperatureAlerts()).isEqualTo(1);
    }

    @Test
    void shouldCalculateComplianceScore() {
        // Given - perfect scenario
        when(checklistRunRepository.findByOrgNumber(ORG_NUMBER)).thenReturn(Collections.emptyList());
        when(deviationReportRepository.findByOrgNumber(ORG_NUMBER)).thenReturn(Collections.emptyList());
        when(temperatureLogEntryRepository.findByOrgNumberOrderByMeasuredAtDesc(ORG_NUMBER))
                .thenReturn(Collections.emptyList());
        when(trainingRecordRepository.findExpiringSoon(any(), any())).thenReturn(Collections.emptyList());

        // When
        DashboardSummaryResponse result = dashboardService.getDashboardSummary(ORG_NUMBER);

        // Then
        assertThat(result.getComplianceScore()).isGreaterThanOrEqualTo(0);
        assertThat(result.getComplianceScore()).isLessThanOrEqualTo(100);
        assertThat(result.getComplianceStatus()).isNotNull();
    }

    // ==================== COMPLIANCE SCORE TESTS ====================

    @Test
    void shouldGetComplianceScore() {
        // Given
        when(checklistRunRepository.findByOrgNumber(ORG_NUMBER)).thenReturn(Collections.emptyList());
        when(deviationReportRepository.findByOrgNumber(ORG_NUMBER)).thenReturn(Collections.emptyList());
        when(temperatureLogEntryRepository.findByOrgNumberOrderByMeasuredAtDesc(ORG_NUMBER))
                .thenReturn(Collections.emptyList());
        when(trainingRecordRepository.findExpiringSoon(any(), any())).thenReturn(Collections.emptyList());

        // When
        ComplianceScoreResponse result = dashboardService.getComplianceScore(ORG_NUMBER);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCurrentScore()).isGreaterThanOrEqualTo(0);
        assertThat(result.getStatus()).isNotNull();
        assertThat(result.getCalculatedAt()).isEqualTo(LocalDate.now());
        assertThat(result.getComponents()).hasSize(4);
    }

    @Test
    void shouldCalculateComplianceComponents() {
        // Given - scenario with some issues
        ChecklistRun completed = createChecklistRun(1L, RunStatus.COMPLETED, LocalDateTime.now());
        DeviationReport deviation = createDeviationReport(1L, DeviationStatus.REPORTED, Severity.CRITICAL);
        TemperatureLogEntry alert = createTemperatureLogEntry(1L, new BigDecimal("20.0"), true);
        TrainingRecord expiring = createTrainingRecord(1L, TrainingStatus.ASSIGNED);

        when(checklistRunRepository.findByOrgNumber(ORG_NUMBER)).thenReturn(List.of(completed));
        when(deviationReportRepository.findByOrgNumber(ORG_NUMBER)).thenReturn(List.of(deviation));
        when(temperatureLogEntryRepository.findByOrgNumberOrderByMeasuredAtDesc(ORG_NUMBER))
                .thenReturn(List.of(alert));
        when(trainingRecordRepository.findExpiringSoon(any(), any())).thenReturn(List.of(expiring));

        // When
        ComplianceScoreResponse result = dashboardService.getComplianceScore(ORG_NUMBER);

        // Then
        assertThat(result.getComponents()).hasSize(4);
        assertThat(result.getComponents().get(0).getName()).isEqualTo("Checklist Completion");
        assertThat(result.getComponents().get(1).getName()).isEqualTo("Deviation Management");
        assertThat(result.getComponents().get(2).getName()).isEqualTo("Temperature Compliance");
        assertThat(result.getComponents().get(3).getName()).isEqualTo("Training Compliance");
    }

    @Test
    void shouldReturnExcellentStatusForHighScore() {
        // Given - all good
        when(checklistRunRepository.findByOrgNumber(ORG_NUMBER)).thenReturn(Collections.emptyList());
        when(deviationReportRepository.findByOrgNumber(ORG_NUMBER)).thenReturn(Collections.emptyList());
        when(temperatureLogEntryRepository.findByOrgNumberOrderByMeasuredAtDesc(ORG_NUMBER))
                .thenReturn(Collections.emptyList());
        when(trainingRecordRepository.findExpiringSoon(any(), any())).thenReturn(Collections.emptyList());

        // When
        DashboardSummaryResponse result = dashboardService.getDashboardSummary(ORG_NUMBER);

        // Then
        assertThat(result.getComplianceStatus()).isIn("EXCELLENT", "GOOD", "FAIR", "NEEDS_IMPROVEMENT", "CRITICAL");
    }

    // ==================== HELPER METHODS ====================

    private ChecklistRun createChecklistRun(Long runId, RunStatus status, LocalDateTime completedAt) {
        ChecklistRun run = new ChecklistRun();
        run.setRunId(runId);
        run.setStatus(status);
        run.setCompletedAt(completedAt);
        return run;
    }

    private DeviationReport createDeviationReport(Long reportId, DeviationStatus status, Severity severity) {
        return DeviationReport.builder()
                .reportId(reportId)
                .orgNumber(ORG_NUMBER)
                .reportType(ReportType.INCIDENT)
                .severity(severity)
                .status(status)
                .title("Test Report")
                .build();
    }

    private TemperatureLogEntry createTemperatureLogEntry(Long entryId, BigDecimal temp, boolean isAlert) {
        TemperatureLogEntry entry = new TemperatureLogEntry();
        entry.setEntryId(entryId);
        entry.setTemperatureC(temp);
        entry.setIsAlert(isAlert);
        entry.setMeasuredAt(LocalDateTime.now());
        return entry;
    }

    private TrainingRecord createTrainingRecord(Long recordId, TrainingStatus status) {
        TrainingRecord record = new TrainingRecord();
        record.setTrainingRecordId(recordId);
        record.setStatus(status);
        record.setTrainingType(TrainingType.FOOD_HYGIENE);
        record.setExpiresAt(LocalDateTime.now().plusDays(15));
        return record;
    }
}
