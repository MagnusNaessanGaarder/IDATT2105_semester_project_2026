package com.example.InternalControl.service.checklist;

import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.checklist.ChecklistRunItem;
import com.example.InternalControl.model.checklist.ChecklistTemplate;
import com.example.InternalControl.model.checklist.ChecklistTemplateItem;
import com.example.InternalControl.model.enums.RunStatus;
import com.example.InternalControl.repository.checklist.ChecklistRunItemRepository;
import com.example.InternalControl.repository.checklist.ChecklistRunRepository;
import com.example.InternalControl.repository.checklist.ChecklistTemplateRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of ChecklistRunService.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ChecklistRunServiceImpl implements ChecklistRunService {

    private final ChecklistRunRepository runRepository;

    private final ChecklistRunItemRepository runItemRepository;

    private final ChecklistTemplateRepository templateRepository;

    @Override
    public ChecklistRun createRun(Long templateId, Integer orgNumber, Long userId, LocalDate runDate) {
        ChecklistTemplate template = templateRepository
                .findByTemplateIdAndOrgNumber(templateId, orgNumber)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Template not found: " + templateId));

        if (Boolean.FALSE.equals(template.getIsActive())) {
            throw new IllegalStateException("Cannot create run from inactive template");
        }

        if (runRepository.existsByTemplateTemplateIdAndRunDate(templateId, runDate)) {
            throw new IllegalStateException(
                    "Run already exists for this template and date");
        }

        ChecklistRun run = ChecklistRun.builder()
                .template(template)
                .orgNumber(orgNumber)
                .runDate(runDate)
                .performedByUserId(userId)
                .status(RunStatus.DRAFT)
                .build();

        ChecklistRun savedRun = runRepository.save(run);

        // Create run items from template items
        List<ChecklistTemplateItem> templateItems = template.getItems();
        for (ChecklistTemplateItem templateItem : templateItems) {
            ChecklistRunItem runItem = ChecklistRunItem.builder()
                    .run(savedRun)
                    .templateItemId(templateItem.getItemId())
                    .isDeviation(false)
                    .build();
            runItemRepository.save(runItem);
        }

        return savedRun;
    }

    @Override
    @Transactional(readOnly = true)
    public ChecklistRun getRun(Long runId, Integer orgNumber) {
        return runRepository.findByRunIdAndOrgNumberWithDetails(runId, orgNumber)
                .or(() -> runRepository.findByRunIdAndOrgNumber(runId, orgNumber))
                .orElseThrow(() -> new EntityNotFoundException("Run not found: " + runId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChecklistRun> getRunsByOrg(Integer orgNumber) {
        List<ChecklistRun> runs = runRepository.findByOrgNumberWithDetails(orgNumber);
        return !runs.isEmpty() ? runs : runRepository.findByOrgNumber(orgNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChecklistRun> getRunsByStatus(Integer orgNumber, RunStatus status) {
        List<ChecklistRun> runs = runRepository.findByOrgNumberAndStatusWithDetails(orgNumber, status);
        return !runs.isEmpty() ? runs : runRepository.findByOrgNumberAndStatus(orgNumber, status);
    }

    @Override
    public ChecklistRun completeRun(Long runId, Integer orgNumber, Long userId) {
        ChecklistRun run = getRun(runId, orgNumber);

        if (!run.isEditable()) {
            throw new IllegalStateException("Run cannot be completed: " + run.getStatus());
        }

        run.setPerformedByUserId(userId);
        run.markAsCompleted();
        return runRepository.save(run);
    }

    @Override
    public ChecklistRun uncompleteRun(Long runId, Integer orgNumber) {
        ChecklistRun run = getRun(runId, orgNumber);

        if (run.getStatus() != RunStatus.COMPLETED) {
            throw new IllegalStateException("Run cannot be uncompleted: " + run.getStatus());
        }

        run.markAsIncomplete();
        return runRepository.save(run);
    }

    @Override
    public ChecklistRunItem updateRunItem(Long runId, Long itemId, ChecklistRunItem item, Integer orgNumber, Long userId) {
        ChecklistRun run = getRun(runId, orgNumber);

        if (!run.isEditable()) {
            throw new IllegalStateException("Cannot update items for completed run");
        }

        run.setPerformedByUserId(userId);

        ChecklistRunItem existing = runItemRepository
                .findByRunRunIdAndTemplateItemId(runId, itemId)
                .orElseThrow(() -> new EntityNotFoundException("Run item not found"));

        if (item.getBooleanValue() != null) {
            existing.setBooleanValue(item.getBooleanValue());
        }
        if (item.getTextValue() != null) {
            existing.setTextValue(item.getTextValue());
        }
        if (item.getNumericValue() != null) {
            existing.setNumericValue(item.getNumericValue());
        }
        if (item.getSelectedChoice() != null) {
            existing.setSelectedChoice(item.getSelectedChoice());
        }
        if (item.getIsDeviation() != null) {
            existing.setIsDeviation(item.getIsDeviation());
        }
        if (item.getCommentText() != null) {
            existing.setCommentText(item.getCommentText());
        }

        return runItemRepository.save(existing);
    }

    @Override
    public void checkOverdueRuns(Integer orgNumber) {
        LocalDateTime now = LocalDateTime.now();
        
        // Find all runs that are not completed/cancelled and are past their due date
        List<ChecklistRun> potentiallyOverdueRuns = runRepository.findByOrgNumber(orgNumber);
        
        for (ChecklistRun run : potentiallyOverdueRuns) {
            if (run.getStatus() != RunStatus.COMPLETED 
                    && run.getStatus() != RunStatus.CANCELLED 
                    && run.getStatus() != RunStatus.OVERDUE
                    && run.getDueAt() != null 
                    && run.getDueAt().isBefore(now)) {
                run.setStatus(RunStatus.OVERDUE);
                runRepository.save(run);
            }
        }
    }
}
