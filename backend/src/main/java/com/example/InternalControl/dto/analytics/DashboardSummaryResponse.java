package com.example.InternalControl.dto.analytics;

import lombok.Builder;
import lombok.Data;

@Data
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

    public DashboardSummaryResponse() {
    }

    public long getChecklistsCompletedToday() {
        return checklistsCompletedToday;
    }

    public long getChecklistsCompletedThisWeek() {
        return checklistsCompletedThisWeek;
    }

    public long getChecklistsOverdue() {
        return checklistsOverdue;
    }

    public double getChecklistCompletionRate() {
        return checklistCompletionRate;
    }

    public long getOpenDeviations() {
        return openDeviations;
    }

    public long getCriticalDeviations() {
        return criticalDeviations;
    }

    public long getDeviationsClosedThisWeek() {
        return deviationsClosedThisWeek;
    }

    public long getTemperatureAlerts() {
        return temperatureAlerts;
    }

    public double getAvgTemperatureToday() {
        return avgTemperatureToday;
    }

    public long getExpiringTrainingCount() {
        return expiringTrainingCount;
    }

    public double getComplianceScore() {
        return complianceScore;
    }

    public String getComplianceStatus() {
        return complianceStatus;
    }

    public String getPeriod() {
        return period;
    }

    public void setChecklistsCompletedToday(long checklistsCompletedToday) {
        this.checklistsCompletedToday = checklistsCompletedToday;
    }

    public void setChecklistsCompletedThisWeek(long checklistsCompletedThisWeek) {
        this.checklistsCompletedThisWeek = checklistsCompletedThisWeek;
    }

    public void setChecklistsOverdue(long checklistsOverdue) {
        this.checklistsOverdue = checklistsOverdue;
    }

    public void setChecklistCompletionRate(double checklistCompletionRate) {
        this.checklistCompletionRate = checklistCompletionRate;
    }

    public void setOpenDeviations(long openDeviations) {
        this.openDeviations = openDeviations;
    }

    public void setCriticalDeviations(long criticalDeviations) {
        this.criticalDeviations = criticalDeviations;
    }

    public void setDeviationsClosedThisWeek(long deviationsClosedThisWeek) {
        this.deviationsClosedThisWeek = deviationsClosedThisWeek;
    }

    public void setTemperatureAlerts(long temperatureAlerts) {
        this.temperatureAlerts = temperatureAlerts;
    }

    public void setAvgTemperatureToday(double avgTemperatureToday) {
        this.avgTemperatureToday = avgTemperatureToday;
    }

    public void setExpiringTrainingCount(long expiringTrainingCount) {
        this.expiringTrainingCount = expiringTrainingCount;
    }

    public void setComplianceScore(double complianceScore) {
        this.complianceScore = complianceScore;
    }

    public void setComplianceStatus(String complianceStatus) {
        this.complianceStatus = complianceStatus;
    }

    public void setPeriod(String period) {
        this.period = period;
    }
}
