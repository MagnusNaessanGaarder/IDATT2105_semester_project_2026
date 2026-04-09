package com.example.InternalControl.service.analytics;

import com.example.InternalControl.dto.analytics.ComplianceScoreResponse;
import com.example.InternalControl.dto.analytics.DashboardSummaryResponse;
import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.deviation.DeviationReport;
import com.example.InternalControl.model.enums.DeviationStatus;
import com.example.InternalControl.model.enums.RunStatus;
import com.example.InternalControl.model.enums.Severity;
import com.example.InternalControl.model.temperature.TemperatureLogEntry;
import com.example.InternalControl.model.training.TrainingRecord;
import com.example.InternalControl.model.training.TrainingStatus;
import com.example.InternalControl.repository.checklist.ChecklistRunRepository;
import com.example.InternalControl.repository.deviation.DeviationReportRepository;
import com.example.InternalControl.repository.temperature.TemperatureLogEntryRepository;
import com.example.InternalControl.repository.training.TrainingRecordRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final ChecklistRunRepository checklistRunRepository;
    private final DeviationReportRepository deviationReportRepository;
    private final TemperatureLogEntryRepository temperatureLogEntryRepository;
    private final TrainingRecordRepository trainingRecordRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(Integer orgNumber) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(7);

        // Checklist stats
        List<ChecklistRun> completedRuns = checklistRunRepository.findByOrgNumber(orgNumber);
        long checklistsCompletedToday = completedRuns.stream()
                .filter(r -> r.getCompletedAt() != null && r.getCompletedAt().toLocalDate().equals(today))
                .count();
        long checklistsCompletedThisWeek = completedRuns.stream()
                .filter(r -> r.getCompletedAt() != null && r.getCompletedAt().toLocalDate().isAfter(weekStart))
                .count();
        long checklistsOverdue = completedRuns.stream()
                .filter(r -> r.getStatus() == RunStatus.OVERDUE)
                .count();

        double checklistCompletionRate = completedRuns.isEmpty() ? 0 :
                (double) completedRuns.stream().filter(r -> r.getStatus() == RunStatus.COMPLETED).count() / completedRuns.size() * 100;

        // Deviation stats
        List<DeviationReport> deviations = deviationReportRepository.findByOrgNumber(orgNumber);
        long openDeviations = deviations.stream()
                .filter(d -> d.getStatus() != DeviationStatus.CLOSED)
                .count();
        long criticalDeviations = deviations.stream()
                .filter(d -> d.getSeverity() == Severity.CRITICAL && d.getStatus() != DeviationStatus.CLOSED)
                .count();
        long deviationsClosedThisWeek = deviations.stream()
                .filter(d -> d.getClosedAt() != null && d.getClosedAt().toLocalDate().isAfter(weekStart))
                .count();

        // Temperature stats
        List<TemperatureLogEntry> tempEntries = temperatureLogEntryRepository.findByOrgNumberOrderByMeasuredAtDesc(orgNumber);
        long temperatureAlerts = tempEntries.stream()
                .filter(e -> Boolean.TRUE.equals(e.getIsAlert()))
                .count();

        double avgTemperatureToday = tempEntries.stream()
                .filter(e -> e.getMeasuredAt().toLocalDate().equals(today))
                .mapToDouble(e -> e.getTemperatureC().doubleValue())
                .average()
                .orElse(0);

        // Training stats
        LocalDateTime thresholdDate = LocalDateTime.now().plusDays(30);
        long expiringTrainingCount = trainingRecordRepository.findExpiringSoon(orgNumber, thresholdDate).size();

        // Calculate compliance score
        double complianceScore = calculateComplianceScore(
                checklistCompletionRate, openDeviations, criticalDeviations, temperatureAlerts, expiringTrainingCount);

        String complianceStatus = getComplianceStatus(complianceScore);

        DashboardSummaryResponse response = new DashboardSummaryResponse();
        response.setChecklistsCompletedToday(checklistsCompletedToday);
        response.setChecklistsCompletedThisWeek(checklistsCompletedThisWeek);
        response.setChecklistsOverdue(checklistsOverdue);
        response.setChecklistCompletionRate(checklistCompletionRate);
        response.setOpenDeviations(openDeviations);
        response.setCriticalDeviations(criticalDeviations);
        response.setDeviationsClosedThisWeek(deviationsClosedThisWeek);
        response.setTemperatureAlerts(temperatureAlerts);
        response.setAvgTemperatureToday(avgTemperatureToday);
        response.setExpiringTrainingCount(expiringTrainingCount);
        response.setComplianceScore(complianceScore);
        response.setComplianceStatus(complianceStatus);
        response.setPeriod("today");
        return response;
    }

    @Transactional(readOnly = true)
    public ComplianceScoreResponse getComplianceScore(Integer orgNumber) {
        DashboardSummaryResponse summary = getDashboardSummary(orgNumber);

        ComplianceScoreResponse.ScoreComponent checklistComponent = new ComplianceScoreResponse.ScoreComponent();
        checklistComponent.setName("Checklist Completion");
        checklistComponent.setWeight(30);
        checklistComponent.setScore(summary.getChecklistCompletionRate());
        checklistComponent.setDescription("Percentage of checklists completed on time");

        ComplianceScoreResponse.ScoreComponent deviationComponent = new ComplianceScoreResponse.ScoreComponent();
        deviationComponent.setName("Deviation Management");
        deviationComponent.setWeight(25);
        deviationComponent.setScore(calculateDeviationScore(summary.getOpenDeviations(), summary.getCriticalDeviations()));
        deviationComponent.setDescription("Based on open and critical deviations");

        ComplianceScoreResponse.ScoreComponent temperatureComponent = new ComplianceScoreResponse.ScoreComponent();
        temperatureComponent.setName("Temperature Compliance");
        temperatureComponent.setWeight(20);
        temperatureComponent.setScore(summary.getTemperatureAlerts() == 0 ? 100 : Math.max(0, 100 - summary.getTemperatureAlerts() * 10));
        temperatureComponent.setDescription("Based on temperature alert count");

        ComplianceScoreResponse.ScoreComponent trainingComponent = new ComplianceScoreResponse.ScoreComponent();
        trainingComponent.setName("Training Compliance");
        trainingComponent.setWeight(25);
        trainingComponent.setScore(summary.getExpiringTrainingCount() == 0 ? 100 : Math.max(0, 100 - summary.getExpiringTrainingCount() * 5));
        trainingComponent.setDescription("Based on expiring training records");

        ComplianceScoreResponse response = new ComplianceScoreResponse();
        response.setCurrentScore(summary.getComplianceScore());
        response.setStatus(summary.getComplianceStatus());
        response.setCalculatedAt(LocalDate.now());
        response.setComponents(List.of(checklistComponent, deviationComponent, temperatureComponent, trainingComponent));
        return response;
    }

    private double calculateComplianceScore(double checklistRate, long openDeviations,
                                           long criticalDeviations, long tempAlerts, long expiringTrainingCount) {
        double score = 0;

        // Checklist completion (30%)
        score += checklistRate * 0.3;

        // Deviation management (25%)
        double deviationScore = 100;
        if (criticalDeviations > 0) deviationScore -= criticalDeviations * 20;
        if (openDeviations > 5) deviationScore -= (openDeviations - 5) * 5;
        score += Math.max(0, deviationScore) * 0.25;

        // Temperature compliance (20%)
        double tempScore = tempAlerts == 0 ? 100 : Math.max(0, 100 - tempAlerts * 10);
        score += tempScore * 0.2;

        // Training compliance (25%)
        double trainingScore = expiringTrainingCount == 0 ? 100 : Math.max(0, 100 - expiringTrainingCount * 5);
        score += trainingScore * 0.25;

        return Math.round(score * 10) / 10.0;
    }

    private double calculateDeviationScore(long openDeviations, long criticalDeviations) {
        double score = 100;
        if (criticalDeviations > 0) score -= criticalDeviations * 20;
        if (openDeviations > 5) score -= (openDeviations - 5) * 5;
        return Math.max(0, score);
    }

    private String getComplianceStatus(double score) {
        if (score >= 90) return "EXCELLENT";
        if (score >= 75) return "GOOD";
        if (score >= 60) return "FAIR";
        if (score >= 40) return "NEEDS_IMPROVEMENT";
        return "CRITICAL";
    }
}
