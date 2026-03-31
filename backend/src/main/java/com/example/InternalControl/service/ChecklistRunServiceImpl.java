package com.example.InternalControl.service;

import com.example.InternalControl.model.ChecklistRun;
import com.example.InternalControl.model.ChecklistRunItem;
import com.example.InternalControl.model.ChecklistTemplate;
import com.example.InternalControl.model.ChecklistTemplateItem;
import com.example.InternalControl.model.enums.RunStatus;
import com.example.InternalControl.repository.ChecklistRunItemRepository;
import com.example.InternalControl.repository.ChecklistRunRepository;
import com.example.InternalControl.repository.ChecklistTemplateRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of ChecklistRunService.
 *
 * @author TriTacLe
 * @since 1.0
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
        return runRepository.findByRunIdAndOrgNumber(runId, orgNumber)
                .orElseThrow(() -> new EntityNotFoundException("Run not found: " + runId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChecklistRun> getRunsByOrg(Integer orgNumber) {
        return runRepository.findByOrgNumber(orgNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChecklistRun> getRunsByStatus(Integer orgNumber, RunStatus status) {
        return runRepository.findByOrgNumberAndStatus(orgNumber, status);
    }

    @Override
    public ChecklistRun completeRun(Long runId, Integer orgNumber) {
        ChecklistRun run = getRun(runId, orgNumber);

        if (!run.isEditable()) {
            throw new IllegalStateException("Run cannot be completed: " + run.getStatus());
        }

        run.markAsCompleted();
        return runRepository.save(run);
    }

    @Override
    public ChecklistRunItem updateRunItem(Long runId, Long itemId, ChecklistRunItem item, Integer orgNumber) {
        ChecklistRun run = getRun(runId, orgNumber);

        if (!run.isEditable()) {
            throw new IllegalStateException("Cannot update items for completed run");
        }

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
        List<ChecklistRun> overdueRuns = runRepository
                .findByOrgNumberAndStatusAndDueAtBefore(
                        orgNumber, RunStatus.OVERDUE, now);

        for (ChecklistRun run : overdueRuns) {
            if (run.isOverdue()) {
                run.setStatus(RunStatus.OVERDUE);
                runRepository.save(run);
            }
        }
    }
}
