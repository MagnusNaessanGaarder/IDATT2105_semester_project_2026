package com.example.InternalControl.dto.analytics;

import lombok.Builder;
import lombok.Data;

/**
 * Aggregated dashboard metrics for compliance overview.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Data
@Builder
public class DashboardSummaryResponse {

    // Checklist stats
    private long checklistsCompletedToday;
    private long checklistsCompletedThisWeek;
    private long checklistsOverdue;
    private double checklistCompletionRate;

    // Deviation stats
    private long openDeviations;
    private long criticalDeviations;
    private long deviationsClosedThisWeek;

    // Temperature stats
    private long temperatureAlerts;
    private double avgTemperatureToday;

    // Training stats
    private long expiringTrainingCount;

    // Compliance score
    private double complianceScore;
    private String complianceStatus;

    // Time period
    private String period;
}
