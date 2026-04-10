package com.example.InternalControl.service.checklist;

import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.checklist.ChecklistRunItem;
import com.example.InternalControl.model.enums.RunStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for checklist run operations.
 */
public interface ChecklistRunService {

    ChecklistRun createRun(Long templateId, Integer orgNumber, Long userId, LocalDate runDate);

    ChecklistRun getRun(Long runId, Integer orgNumber);

    List<ChecklistRun> getRunsByOrg(Integer orgNumber);

    List<ChecklistRun> getRunsByStatus(Integer orgNumber, RunStatus status);

    ChecklistRun completeRun(Long runId, Integer orgNumber, Long userId);

    default ChecklistRun completeRun(Long runId, Integer orgNumber) {
        return completeRun(runId, orgNumber, null);
    }

    ChecklistRun uncompleteRun(Long runId, Integer orgNumber);

    ChecklistRunItem updateRunItem(Long runId, Long itemId, ChecklistRunItem item, Integer orgNumber, Long userId);

    default ChecklistRunItem updateRunItem(Long runId, Long itemId, ChecklistRunItem item, Integer orgNumber) {
        return updateRunItem(runId, itemId, item, orgNumber, null);
    }

    void checkOverdueRuns(Integer orgNumber);
}
