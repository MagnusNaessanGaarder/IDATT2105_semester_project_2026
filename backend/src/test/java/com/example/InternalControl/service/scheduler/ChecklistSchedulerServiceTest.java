package com.example.InternalControl.service.scheduler;

import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.checklist.ChecklistTemplate;
import com.example.InternalControl.model.checklist.ChecklistTemplateItem;
import com.example.InternalControl.model.enums.Frequency;
import com.example.InternalControl.model.enums.RunStatus;
import com.example.InternalControl.repository.checklist.ChecklistRunItemRepository;
import com.example.InternalControl.repository.checklist.ChecklistRunRepository;
import com.example.InternalControl.repository.checklist.ChecklistTemplateRepository;
import com.example.InternalControl.scheduler.ChecklistSchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChecklistSchedulerService.
 */
@ExtendWith(MockitoExtension.class)
class ChecklistSchedulerServiceTest {

    @Mock
    private ChecklistTemplateRepository templateRepository;

    @Mock
    private ChecklistRunRepository runRepository;

    @Mock
    private ChecklistRunItemRepository runItemRepository;

    @InjectMocks
    private ChecklistSchedulerService checklistSchedulerService;

    private static final Integer ORG_NUMBER = 123;
    private static final Long TEMPLATE_ID = 1L;
    private static final Long USER_ID = 1L;

    private ChecklistTemplate testTemplate;

    @BeforeEach
    void setUp() {
        testTemplate = createTestTemplate(TEMPLATE_ID, "Daily Checklist", Frequency.DAILY);
    }

    // ==================== GENERATE DAILY CHECKLISTS TESTS ====================

    @Test
    void shouldGenerateDailyChecklists() {
        // Given
        when(templateRepository.findByIsActiveTrue()).thenReturn(List.of(testTemplate));
        when(runRepository.existsByTemplateTemplateIdAndRunDate(any(), any())).thenReturn(false);
        when(runRepository.save(any(ChecklistRun.class))).thenAnswer(inv -> {
            ChecklistRun run = inv.getArgument(0);
            run.setRunId(1L);
            return run;
        });
        when(runItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        checklistSchedulerService.generateDailyChecklists();

        // Then
        verify(runRepository).save(any(ChecklistRun.class));
        verify(runItemRepository, times(2)).save(any()); // Template has 2 items
    }

    @Test
    void shouldNotGenerateDuplicateRuns() {
        // Given
        when(templateRepository.findByIsActiveTrue()).thenReturn(List.of(testTemplate));
        when(runRepository.existsByTemplateTemplateIdAndRunDate(eq(TEMPLATE_ID), any(LocalDate.class)))
                .thenReturn(true);

        // When
        checklistSchedulerService.generateDailyChecklists();

        // Then
        verify(runRepository, never()).save(any());
        verify(runItemRepository, never()).save(any());
    }

    @Test
    void shouldGenerateOnlyForDailyTemplatesOnDailySchedule() {
        // Given - Monday (day 1)
        ChecklistTemplate weeklyTemplate = createTestTemplate(2L, "Weekly Checklist", Frequency.WEEKLY);
        ChecklistTemplate monthlyTemplate = createTestTemplate(3L, "Monthly Checklist", Frequency.MONTHLY);

        when(templateRepository.findByIsActiveTrue())
                .thenReturn(List.of(testTemplate, weeklyTemplate, monthlyTemplate));
        when(runRepository.existsByTemplateTemplateIdAndRunDate(any(), any())).thenReturn(false);
        when(runRepository.save(any(ChecklistRun.class))).thenAnswer(inv -> {
            ChecklistRun run = inv.getArgument(0);
            run.setRunId(1L);
            return run;
        });
        when(runItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        checklistSchedulerService.generateDailyChecklists();

        // Then
        verify(runRepository, times(1)).save(any()); // Only daily template
    }

    @Test
    void shouldNotGenerateForCustomTemplates() {
        // Given
        ChecklistTemplate customTemplate = createTestTemplate(2L, "Custom Checklist", Frequency.CUSTOM);
        when(templateRepository.findByIsActiveTrue()).thenReturn(List.of(customTemplate));

        // When
        checklistSchedulerService.generateDailyChecklists();

        // Then
        verify(runRepository, never()).save(any());
    }

    @Test
    void shouldHandleExceptionWhenCreatingRun() {
        // Given
        when(templateRepository.findByIsActiveTrue()).thenReturn(List.of(testTemplate));
        when(runRepository.existsByTemplateTemplateIdAndRunDate(any(), any())).thenReturn(false);
        when(runRepository.save(any(ChecklistRun.class))).thenThrow(new RuntimeException("Database error"));

        // When - should not throw
        checklistSchedulerService.generateDailyChecklists();

        // Then - error should be logged but not propagated
        verify(runRepository).save(any(ChecklistRun.class));
    }

    // ==================== CHECK OVERDUE RUNS TESTS ====================

    @Test
    void shouldMarkOverdueRuns() {
        // Given
        ChecklistRun overdueRun = createTestRun(1L, RunStatus.DRAFT, LocalDateTime.now().minusHours(1));
        when(runRepository.findAll()).thenReturn(List.of(overdueRun));
        when(runRepository.save(any(ChecklistRun.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        checklistSchedulerService.checkOverdueRuns();

        // Then
        assertThat(overdueRun.getStatus()).isEqualTo(RunStatus.OVERDUE);
        verify(runRepository).save(overdueRun);
    }

    @Test
    void shouldNotMarkCompletedRunsAsOverdue() {
        // Given
        ChecklistRun completedRun = createTestRun(1L, RunStatus.COMPLETED, LocalDateTime.now().minusHours(1));
        when(runRepository.findAll()).thenReturn(List.of(completedRun));

        // When
        checklistSchedulerService.checkOverdueRuns();

        // Then
        verify(runRepository, never()).save(any());
    }

    @Test
    void shouldNotMarkCancelledRunsAsOverdue() {
        // Given
        ChecklistRun cancelledRun = createTestRun(1L, RunStatus.CANCELLED, LocalDateTime.now().minusHours(1));
        when(runRepository.findAll()).thenReturn(List.of(cancelledRun));

        // When
        checklistSchedulerService.checkOverdueRuns();

        // Then
        verify(runRepository, never()).save(any());
    }

    @Test
    void shouldNotMarkOnTimeRunsAsOverdue() {
        // Given
        ChecklistRun onTimeRun = createTestRun(1L, RunStatus.DRAFT, LocalDateTime.now().plusHours(1));
        when(runRepository.findAll()).thenReturn(List.of(onTimeRun));

        // When
        checklistSchedulerService.checkOverdueRuns();

        // Then
        verify(runRepository, never()).save(any());
    }

    @Test
    void shouldHandleEmptyRunList() {
        // Given
        when(runRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        checklistSchedulerService.checkOverdueRuns();

        // Then
        verify(runRepository, never()).save(any());
    }

    // ==================== HELPER METHODS ====================

    private ChecklistTemplate createTestTemplate(Long templateId, String name, Frequency frequency) {
        ChecklistTemplate template = new ChecklistTemplate();
        template.setTemplateId(templateId);
        template.setTitle(name);
        template.setFrequency(frequency);
        template.setOrgNumber(ORG_NUMBER);
        template.setIsActive(true);
        template.setCreatedByUserId(USER_ID);

        // Add some template items
        ChecklistTemplateItem item1 = new ChecklistTemplateItem();
        item1.setItemId(1L);
        item1.setDescription("Check item 1");

        ChecklistTemplateItem item2 = new ChecklistTemplateItem();
        item2.setItemId(2L);
        item2.setDescription("Check item 2");

        template.setItems(List.of(item1, item2));

        return template;
    }

    private ChecklistRun createTestRun(Long runId, RunStatus status, LocalDateTime dueAt) {
        ChecklistRun run = new ChecklistRun();
        run.setRunId(runId);
        run.setStatus(status);
        run.setDueAt(dueAt);
        return run;
    }
}
