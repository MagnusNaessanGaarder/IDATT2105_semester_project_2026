package com.example.InternalControl.service.deviation;

import com.example.InternalControl.dto.deviation.request.DeviationActionRequest;
import com.example.InternalControl.dto.deviation.request.DeviationReportCreateRequest;
import com.example.InternalControl.dto.deviation.request.DeviationReportUpdateRequest;
import com.example.InternalControl.model.deviation.DeviationReport;
import com.example.InternalControl.model.enums.DeviationStatus;
import com.example.InternalControl.model.enums.Severity;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for managing deviation reports and incident investigations.
 * <p>
 * Deviation reports track non-conformances in both IK-MAT (food safety) and
 * IK-ALKOHOL (alcohol service) compliance. The service supports full lifecycle
 * management from initial report through investigation, corrective actions, to closure.
 * <p>
 * Key features:
 * <ul>
 *   <li>Incident reporting with severity classification</li>
 *   <li>Assignment and tracking of investigation responsibility</li>
 *   <li>Structured cause analysis (5 Whys, Fishbone, etc.)</li>
 *   <li>Corrective and preventive action (CAPA) tracking</li>
 *   <li>Audit trail for all status changes and actions</li>
 * </ul>
 *
 * @author TriTacLe
 * @version 1.0
 * @since 1.0
 */
public interface DeviationReportService {

    /**
     * Creates a new deviation report.
     * <p>
     * Automatically assigns a report ID, sets initial status to REPORTED,
     * and creates notifications for assigned users and managers.
     * Triggers audit logging of the creation event.
     *
     * @param request   the report data including title, description, severity, and location
     * @param orgNumber the organization number for access validation
     * @param userId    the ID of the user creating the report
     * @return the created DeviationReport with generated ID
     * @throws jakarta.persistence.EntityNotFoundException if organization or reporter not found
     * @throws IllegalArgumentException                    if required fields are missing
     */
    DeviationReport createReport(DeviationReportCreateRequest request, Integer orgNumber, Long userId);

    /**
     * Retrieves a specific deviation report by ID.
     *
     * @param reportId  the ID of the report to retrieve
     * @param orgNumber the organization number for access validation
     * @return the DeviationReport with full investigation history
     * @throws jakarta.persistence.EntityNotFoundException if report not found or access denied
     */
    DeviationReport getReport(Long reportId, Integer orgNumber);

    /**
     * Updates an existing deviation report's basic information.
     * <p>
     * Only allowed while report is in REPORTED or UNDER_INVESTIGATION status.
     * Changes are logged to the audit trail.
     *
     * @param reportId  the ID of the report to update
     * @param request   the updated report data
     * @param orgNumber the organization number for access validation
     * @return the updated DeviationReport
     * @throws jakarta.persistence.EntityNotFoundException if report not found
     * @throws IllegalStateException                       if report is already closed or completed
     */
    DeviationReport updateReport(Long reportId, DeviationReportUpdateRequest request, Integer orgNumber);

    /**
     * Soft-deletes a deviation report.
     * <p>
     * Reports with audit trail cannot be hard-deleted. They are marked as
     * deleted but retained for compliance purposes. Only ADMIN or the original
     * reporter can delete reports.
     *
     * @param reportId  the ID of the report to delete
     * @param orgNumber the organization number for access validation
     * @throws jakarta.persistence.EntityNotFoundException if report not found
     * @throws org.springframework.security.access.AccessDeniedException if user lacks permission
     */
    void deleteReport(Long reportId, Integer orgNumber);

    /**
     * Retrieves all deviation reports for an organization.
     *
     * @param orgNumber the organization number
     * @return list of all reports sorted by creation date (newest first)
     */
    List<DeviationReport> getReportsByOrg(Integer orgNumber);

    /**
     * Retrieves reports filtered by status.
     *
     * @param orgNumber the organization number
     * @param status    the status to filter by (REPORTED, UNDER_INVESTIGATION, COMPLETED, CLOSED)
     * @return list of reports with the specified status
     */
    List<DeviationReport> getReportsByStatus(Integer orgNumber, DeviationStatus status);

    /**
     * Retrieves reports filtered by severity level.
     * <p>
     * Useful for prioritizing critical and major deviations.
     *
     * @param orgNumber the organization number
     * @param severity  the severity level (CRITICAL, MAJOR, MINOR)
     * @return list of reports with the specified severity
     */
    List<DeviationReport> getReportsBySeverity(Integer orgNumber, Severity severity);

    /**
     * Retrieves all reports assigned to a specific user for investigation.
     *
     * @param userId    the ID of the assigned investigator
     * @param orgNumber the organization number for access validation
     * @return list of reports assigned to the user
     */
    List<DeviationReport> getReportsAssignedTo(Long userId, Integer orgNumber);

    /**
     * Updates the status of a deviation report.
     * <p>
     * Status transitions are validated (e.g., cannot jump from REPORTED to CLOSED
     * without going through UNDER_INVESTIGATION). Notifications are sent on
     * significant status changes.
     *
     * @param reportId   the ID of the report
     * @param newStatus  the new status to set
     * @param orgNumber  the organization number for access validation
     * @param userId     the ID of the user making the status change
     * @return the updated DeviationReport
     * @throws IllegalStateException if the status transition is invalid
     */
    DeviationReport updateStatus(Long reportId, DeviationStatus newStatus, Integer orgNumber, Long userId);

    /**
     * Assigns a deviation report to a user for investigation.
     * <p>
     * Sends notification to the assigned user. Previous assignee (if any)
     * is also notified of the reassignment.
     *
     * @param reportId        the ID of the report
     * @param assignedToUserId the ID of the user to assign
     * @param orgNumber       the organization number for access validation
     * @param currentUserId   the ID of the user making the assignment
     * @return the updated DeviationReport with new assignee
     * @throws jakarta.persistence.EntityNotFoundException if report or user not found
     */
    DeviationReport assignReport(Long reportId, Long assignedToUserId, Integer orgNumber, Long currentUserId);

    /**
     * Adds an immediate action to contain a deviation.
     * <p>
     * Immediate actions are short-term fixes to prevent further impact.
     * Examples: removing contaminated food, stopping a process, etc.
     *
     * @param reportId  the ID of the report
     * @param request   the action description and responsible person
     * @param orgNumber the organization number for access validation
     * @param userId    the ID of the user adding the action
     * @return the updated DeviationReport with added action
     */
    DeviationReport addImmediateAction(Long reportId, DeviationActionRequest request, Integer orgNumber, Long userId);

    /**
     * Adds a root cause analysis to the report.
     * <p>
     * Documents the investigation findings including methods used
     * (5 Whys, Fishbone diagram, etc.) and identified root causes.
     *
     * @param reportId  the ID of the report
     * @param request   the cause analysis description and methods
     * @param orgNumber the organization number for access validation
     * @param userId    the ID of the user adding the analysis
     * @return the updated DeviationReport with cause analysis
     */
    DeviationReport addCauseAnalysis(Long reportId, DeviationActionRequest request, Integer orgNumber, Long userId);

    /**
     * Adds a corrective action to prevent recurrence.
     * <p>
     * Corrective actions address the root cause. Each action has an
     * assigned person and due date for tracking completion.
     *
     * @param reportId  the ID of the report
     * @param request   the corrective action details and assignment
     * @param orgNumber the organization number for access validation
     * @param userId    the ID of the user adding the action
     * @return the updated DeviationReport with corrective action
     */
    DeviationReport addCorrectiveAction(Long reportId, DeviationActionRequest request, Integer orgNumber, Long userId);

    /**
     * Marks a report as completed with final review.
     * <p>
     * All immediate actions, cause analysis, and corrective actions
     * must be documented before completion. Triggers notifications
     * and audit logging.
     *
     * @param reportId  the ID of the report
     * @param request   final completion notes and verification
     * @param orgNumber the organization number for access validation
     * @param userId    the ID of the user completing the report
     * @return the completed DeviationReport
     * @throws IllegalStateException if required actions are missing
     */
    DeviationReport completeReport(Long reportId, DeviationActionRequest request, Integer orgNumber, Long userId);

    /**
     * Closes a deviation report after verification.
     * <p>
     * Closure is the final step after completion. It indicates that
     * all corrective actions have been implemented and verified.
     * Closed reports cannot be reopened.
     *
     * @param reportId  the ID of the report
     * @param orgNumber the organization number for access validation
     * @param userId    the ID of the user closing the report
     * @return the closed DeviationReport
     * @throws IllegalStateException if report is not in COMPLETED status
     */
    DeviationReport closeReport(Long reportId, Integer orgNumber, Long userId);

    /**
     * Searches deviation reports with multiple filter criteria.
     * <p>
     * All parameters are optional. Returns reports matching all provided criteria.
     *
     * @param orgNumber   the organization number (required)
     * @param status      filter by status (optional)
     * @param severity    filter by severity (optional)
     * @param assignedToId filter by assignee (optional)
     * @param fromDate    filter by creation date - start (optional)
     * @param toDate      filter by creation date - end (optional)
     * @return list of matching reports
     */
    List<DeviationReport> searchReports(Integer orgNumber, DeviationStatus status, Severity severity,
                                        Long assignedToId, LocalDate fromDate, LocalDate toDate);

    /**
     * Gets the count of open (non-closed) deviation reports.
     * <p>
     * Used for dashboard widgets and compliance monitoring.
     *
     * @param orgNumber the organization number
     * @return count of reports with status != CLOSED
     */
    Long getOpenReportCount(Integer orgNumber);
}
