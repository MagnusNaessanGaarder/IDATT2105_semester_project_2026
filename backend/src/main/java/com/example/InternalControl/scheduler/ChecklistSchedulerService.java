package com.example.InternalControl.scheduler;

import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.checklist.ChecklistRunItem;
import com.example.InternalControl.model.checklist.ChecklistTemplate;
import com.example.InternalControl.model.checklist.ChecklistTemplateItem;
import com.example.InternalControl.shared.enums.Frequency;
import com.example.InternalControl.shared.enums.RunStatus;
import com.example.InternalControl.repository.checklist.ChecklistRunItemRepository;
import com.example.InternalControl.repository.checklist.ChecklistRunRepository;
import com.example.InternalControl.repository.checklist.ChecklistTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for automatically generating checklist runs based on template
 * frequency.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChecklistSchedulerService {

  private final ChecklistTemplateRepository templateRepository;

  private final ChecklistRunRepository runRepository;

  private final ChecklistRunItemRepository runItemRepository;

  @Scheduled(cron = "0 0 6 * * ?")
  @Transactional
  public void generateDailyChecklists() {
    log.info("Starting daily checklist generation");

    LocalDate today = LocalDate.now();
    List<ChecklistTemplate> activeTemplates = templateRepository.findByIsActiveTrue();

    int createdCount = 0;
    for (ChecklistTemplate template : activeTemplates) {
      if (shouldGenerateRun(template, today)) {
        try {
          createChecklistRun(template, today);
          createdCount++;
        } catch (Exception e) {
          log.error("Failed to create run for template {}: {}",
              template.getTemplateId(), e.getMessage());
        }
      }
    }

    log.info("Created {} checklist runs for {}", createdCount, today);
  }

  @Scheduled(cron = "0 0 * * * ?")
  @Transactional
  public void checkOverdueRuns() {
    log.debug("Checking for overdue runs");

    LocalDateTime now = LocalDateTime.now();
    List<ChecklistRun> overdueRuns = runRepository.findAll().stream()
        .filter(run -> run.getStatus() != RunStatus.COMPLETED)
        .filter(run -> run.getStatus() != RunStatus.CANCELLED)
        .filter(run -> run.getDueAt() != null && run.getDueAt().isBefore(now))
        .toList();

    for (ChecklistRun run : overdueRuns) {
      run.setStatus(RunStatus.OVERDUE);
      runRepository.save(run);
      log.info("Marked run {} as overdue", run.getRunId());
    }
  }

  private boolean shouldGenerateRun(ChecklistTemplate template, LocalDate date) {
    Frequency frequency = template.getFrequency();

    return switch (frequency) {
      case DAILY -> true;
      case WEEKLY -> date.getDayOfWeek().getValue() == 1; // Monday
      case MONTHLY -> date.getDayOfMonth() == 1; // First day of month
      case CUSTOM -> false; // Manual creation only
    };
  }

  private void createChecklistRun(ChecklistTemplate template, LocalDate date) {
    boolean exists = runRepository.existsByTemplateTemplateIdAndRunDate(
        template.getTemplateId(), date);
    if (exists) {
      log.debug("Run already exists for template {} on {}",
          template.getTemplateId(), date);
      return;
    }

    ChecklistRun run = ChecklistRun.builder()
        .template(template)
        .orgNumber(template.getOrgNumber())
        .runDate(date)
        .dueAt(date.atTime(23, 59)) // End of day
        .status(RunStatus.DRAFT)
        .performedByUserId(template.getCreatedByUserId())
        .build();

    ChecklistRun savedRun = runRepository.save(run);

    List<ChecklistTemplateItem> templateItems = template.getItems();
    for (ChecklistTemplateItem templateItem : templateItems) {
      ChecklistRunItem runItem = ChecklistRunItem.builder()
          .run(savedRun)
          .templateItemId(templateItem.getItemId())
          .isDeviation(false)
          .build();
      runItemRepository.save(runItem);
    }

    log.debug("Created run {} for template {}",
        savedRun.getRunId(), template.getTemplateId());
  }
}
