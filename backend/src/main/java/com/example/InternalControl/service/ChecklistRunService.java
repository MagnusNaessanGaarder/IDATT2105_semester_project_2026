package com.example.InternalControl.service;

import com.example.InternalControl.model.ChecklistRun;
import com.example.InternalControl.model.ChecklistRunItem;
import com.example.InternalControl.model.enums.RunStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for checklist run operations.
 *
 * @author TriTacLe
 * @since 1.0
 */
public interface ChecklistRunService {

    /**
     * Creates a new checklist run from template.
     *
     * @param templateId the template ID
     * @param orgNumber the organization number
     * @param userId the user ID
     * @param runDate the run date
     * @return the created run
     */
    ChecklistRun createRun(Long templateId, Integer orgNumber, Long userId, LocalDate runDate);

    /**
     * Gets a run by ID.
     *
     * @param runId the run ID
     * @param orgNumber the organization number
     * @return the run
     */
    ChecklistRun getRun(Long runId, Integer orgNumber);

    /**
     * Gets all runs for an organization.
     *
     * @param orgNumber the organization number
     * @return list of runs
     */
    List<ChecklistRun> getRunsByOrg(Integer orgNumber);

    /**
     * Gets runs by status.
     *
     * @param orgNumber the organization number
     * @param status the status
     * @return list of runs
     */
    List<ChecklistRun> getRunsByStatus(Integer orgNumber, RunStatus status);

    /**
     * Completes a run.
     *
     * @param runId the run ID
     * @param orgNumber the organization number
     * @return the completed run
     */
    ChecklistRun completeRun(Long runId, Integer orgNumber);

    /**
     * Updates a run item (answer).
     *
     * @param runId the run ID
     * @param itemId the item ID
     * @param item the updated item
     * @param orgNumber the organization number
     * @return the updated item
     */
    ChecklistRunItem updateRunItem(Long runId, Long itemId, ChecklistRunItem item, Integer orgNumber);

    /**
     * Checks for overdue runs and updates status.
     *
     * @param orgNumber the organization number
     */
    void checkOverdueRuns(Integer orgNumber);
}
