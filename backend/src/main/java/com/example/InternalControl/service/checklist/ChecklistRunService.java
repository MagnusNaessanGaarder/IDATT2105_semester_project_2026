package com.example.InternalControl.service.checklist;

import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.checklist.ChecklistRunItem;
import com.example.InternalControl.model.enums.RunStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for managing checklist runs (instances).
 * <p>
 * Checklist runs are daily/weekly/monthly executions of checklist templates.
 * Each run tracks the completion status of individual items and overall progress.
 * The service handles automatic overdue detection and status transitions.
 *
 * @author TriTacLe
 * @version 1.0
 * @since 1.0
 */
public interface ChecklistRunService {

    /**
     * Creates a new checklist run from a template.
     * <p>
     * The run inherits all items from the template and is created in DRAFT status.
     * It can be scheduled for a specific date or immediate execution.
     *
     * @param templateId the ID of the template to create the run from
     * @param orgNumber  the organization number for access validation
     * @param userId     the ID of the user creating the run
     * @param runDate    the scheduled date for the checklist run
     * @return the created ChecklistRun with generated ID and inherited items
     * @throws jakarta.persistence.EntityNotFoundException if template or organization not found
     * @throws IllegalStateException                       if template is inactive
     */
    ChecklistRun createRun(Long templateId, Integer orgNumber, Long userId, LocalDate runDate);

    /**
     * Retrieves a specific checklist run by ID.
     *
     * @param runId     the ID of the run to retrieve
     * @param orgNumber the organization number for access validation
     * @return the ChecklistRun with all items and status
     * @throws jakarta.persistence.EntityNotFoundException if run not found or access denied
     */
    ChecklistRun getRun(Long runId, Integer orgNumber);

    /**
     * Retrieves all checklist runs for an organization.
     *
     * @param orgNumber the organization number
     * @return list of all runs sorted by run date (newest first)
     */
    List<ChecklistRun> getRunsByOrg(Integer orgNumber);

    /**
     * Retrieves runs filtered by status.
     * <p>
     * Useful for dashboard views showing overdue, completed, or pending checklists.
     *
     * @param orgNumber the organization number
     * @param status    the run status to filter by (DRAFT, IN_PROGRESS, COMPLETED, OVERDUE)
     * @return list of runs with the specified status
     */
    List<ChecklistRun> getRunsByStatus(Integer orgNumber, RunStatus status);

    /**
     * Marks a checklist run as completed.
     * <p>
     * Validates that all mandatory items are checked before completion.
     * Triggers audit logging and potential notifications upon completion.
     *
     * @param runId     the ID of the run to complete
     * @param orgNumber the organization number for access validation
     * @return the completed ChecklistRun with timestamp
     * @throws jakarta.persistence.EntityNotFoundException if run not found
     * @throws IllegalStateException                       if mandatory items are unchecked
     */
    ChecklistRun completeRun(Long runId, Integer orgNumber);

    /**
     * Updates a single checklist run item (mark as checked/unchecked).
     * <p>
     * Records the user who checked the item and timestamp for audit trail.
     * Automatically updates the run status to IN_PROGRESS if it was in DRAFT.
     *
     * @param runId     the ID of the parent run
     * @param itemId    the ID of the item to update
     * @param item      the updated item data (checked status, notes, attachments)
     * @param orgNumber the organization number for access validation
     * @return the updated ChecklistRunItem
     * @throws jakarta.persistence.EntityNotFoundException if run or item not found
     */
    ChecklistRunItem updateRunItem(Long runId, Long itemId, ChecklistRunItem item, Integer orgNumber);

    /**
     * Checks for and marks overdue runs.
     * <p>
     * This method is typically called by a scheduled job.
     * Runs become overdue when their due date passes without completion.
     * Notifications may be sent when runs become overdue.
     *
     * @param orgNumber the organization number to check (or null for all organizations)
     */
    void checkOverdueRuns(Integer orgNumber);
}
