package com.example.InternalControl.repository;

import com.example.InternalControl.AbstractIntegrationTest;
import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.checklist.ChecklistTemplate;
import com.example.InternalControl.model.enums.Frequency;
import com.example.InternalControl.model.enums.ModuleType;
import com.example.InternalControl.model.enums.RunStatus;
import com.example.InternalControl.repository.checklist.ChecklistRunRepository;
import com.example.InternalControl.repository.checklist.ChecklistTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ChecklistRunRepository.
 * Tests checklist run queries with status and date filtering.
 */
@SpringBootTest
@Transactional
@DisplayName("ChecklistRunRepository Integration Tests")
class ChecklistRunRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private ChecklistRunRepository checklistRunRepository;

    @Autowired
    private ChecklistTemplateRepository checklistTemplateRepository;

    private static final Integer ORG_NUMBER = 937219997;
    private ChecklistTemplate testTemplate;

    @BeforeEach
    void setUp() {
        testTemplate = checklistTemplateRepository.save(ChecklistTemplate.builder()
                .orgNumber(ORG_NUMBER)
                .moduleType(ModuleType.FOOD)
                .title("Test Template")
                .frequency(Frequency.DAILY)
                .isActive(true)
                .createdByUserId(1L)
                .build());
    }

    @Test
    @DisplayName("Should find runs by org number")
    void shouldFindRunsByOrgNumber() {
        // Given
        ChecklistRun run1 = createRun(testTemplate, RunStatus.DRAFT, LocalDate.now());
        ChecklistRun run2 = createRun(testTemplate, RunStatus.COMPLETED, LocalDate.now());
        checklistRunRepository.save(run1);
        checklistRunRepository.save(run2);

        // When
        List<ChecklistRun> runs = checklistRunRepository.findByOrgNumber(ORG_NUMBER);

        // Then
        assertThat(runs).hasSize(2);
    }

    @Test
    @DisplayName("Should find runs by org number and status")
    void shouldFindRunsByOrgNumberAndStatus() {
        // Given
        ChecklistRun draftRun = createRun(testTemplate, RunStatus.DRAFT, LocalDate.now());
        ChecklistRun completedRun = createRun(testTemplate, RunStatus.COMPLETED, LocalDate.now());
        checklistRunRepository.save(draftRun);
        checklistRunRepository.save(completedRun);

        // When
        List<ChecklistRun> draftRuns = checklistRunRepository.findByOrgNumberAndStatus(ORG_NUMBER, RunStatus.DRAFT);

        // Then
        assertThat(draftRuns).hasSize(1);
        assertThat(draftRuns.get(0).getStatus()).isEqualTo(RunStatus.DRAFT);
    }

    @Test
    @DisplayName("Should find runs by org number and date range")
    void shouldFindRunsByOrgNumberAndDateRange() {
        // Given
        LocalDate today = LocalDate.now();
        ChecklistRun oldRun = createRun(testTemplate, RunStatus.COMPLETED, today.minusDays(10));
        ChecklistRun recentRun1 = createRun(testTemplate, RunStatus.COMPLETED, today.minusDays(3));
        ChecklistRun recentRun2 = createRun(testTemplate, RunStatus.COMPLETED, today.minusDays(2));

        checklistRunRepository.save(oldRun);
        checklistRunRepository.save(recentRun1);
        checklistRunRepository.save(recentRun2);

        // When
        List<ChecklistRun> recentRuns = checklistRunRepository.findByOrgNumberAndRunDateBetween(
                ORG_NUMBER, today.minusDays(5), today);

        // Then
        assertThat(recentRuns).hasSize(2);
    }

    @Test
    @DisplayName("Should find runs by template ID")
    void shouldFindRunsByTemplateId() {
        // Given
        ChecklistTemplate anotherTemplate = checklistTemplateRepository.save(ChecklistTemplate.builder()
                .orgNumber(ORG_NUMBER)
                .moduleType(ModuleType.ALCOHOL)
                .title("Another Template")
                .frequency(Frequency.WEEKLY)
                .isActive(true)
                .createdByUserId(1L)
                .build());

        checklistRunRepository.save(createRun(testTemplate, RunStatus.DRAFT, LocalDate.now()));
        checklistRunRepository.save(createRun(testTemplate, RunStatus.DRAFT, LocalDate.now()));
        checklistRunRepository.save(createRun(anotherTemplate, RunStatus.DRAFT, LocalDate.now()));

        // When
        List<ChecklistRun> templateRuns = checklistRunRepository.findByTemplateTemplateId(testTemplate.getTemplateId());

        // Then
        assertThat(templateRuns).hasSize(2);
    }

    @Test
    @DisplayName("Should find run by ID and org number")
    void shouldFindRunByIdAndOrgNumber() {
        // Given
        ChecklistRun run = createRun(testTemplate, RunStatus.DRAFT, LocalDate.now());
        ChecklistRun saved = checklistRunRepository.save(run);

        // When
        Optional<ChecklistRun> found = checklistRunRepository.findByRunIdAndOrgNumber(saved.getRunId(), ORG_NUMBER);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getRunId()).isEqualTo(saved.getRunId());
    }

    @Test
    @DisplayName("Should check if run exists by template and date")
    void shouldCheckIfRunExistsByTemplateAndDate() {
        // Given
        LocalDate runDate = LocalDate.now();
        ChecklistRun run = createRun(testTemplate, RunStatus.DRAFT, runDate);
        checklistRunRepository.save(run);

        // When & Then
        assertThat(checklistRunRepository.existsByTemplateTemplateIdAndRunDate(testTemplate.getTemplateId(), runDate)).isTrue();
        assertThat(checklistRunRepository.existsByTemplateTemplateIdAndRunDate(testTemplate.getTemplateId(), runDate.plusDays(1))).isFalse();
    }

    @Test
    @DisplayName("Should find overdue runs")
    void shouldFindOverdueRuns() {
        // Given
        LocalDateTime pastDue = LocalDateTime.now().minusDays(1);

        ChecklistRun overdueRun = createRun(testTemplate, RunStatus.IN_PROGRESS, LocalDate.now().minusDays(2));
        overdueRun.setDueAt(pastDue);

        ChecklistRun onTimeRun = createRun(testTemplate, RunStatus.IN_PROGRESS, LocalDate.now());
        onTimeRun.setDueAt(LocalDateTime.now().plusDays(1));

        ChecklistRun completedRun = createRun(testTemplate, RunStatus.COMPLETED, LocalDate.now().minusDays(2));
        completedRun.setDueAt(pastDue);
        completedRun.setCompletedAt(LocalDateTime.now().minusDays(3));

        checklistRunRepository.save(overdueRun);
        checklistRunRepository.save(onTimeRun);
        checklistRunRepository.save(completedRun);

        // When
        List<ChecklistRun> overdueRuns = checklistRunRepository.findByOrgNumberAndStatusAndDueAtBefore(
                ORG_NUMBER, RunStatus.IN_PROGRESS, LocalDateTime.now());

        // Then
        assertThat(overdueRuns).hasSize(1);
        assertThat(overdueRuns.get(0).getStatus()).isEqualTo(RunStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Should return empty list for non-existent org")
    void shouldReturnEmptyListForNonExistentOrg() {
        // When
        List<ChecklistRun> runs = checklistRunRepository.findByOrgNumber(999999999);

        // Then
        assertThat(runs).isEmpty();
    }

    @Test
    @DisplayName("Should return empty optional for run in different org")
    void shouldReturnEmptyOptionalForRunInDifferentOrg() {
        // Given
        ChecklistRun run = createRun(testTemplate, RunStatus.DRAFT, LocalDate.now());
        ChecklistRun saved = checklistRunRepository.save(run);

        // When
        Optional<ChecklistRun> found = checklistRunRepository.findByRunIdAndOrgNumber(saved.getRunId(), 123456789);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should filter runs by multiple statuses")
    void shouldFilterRunsByMultipleStatuses() {
        // Given
        checklistRunRepository.save(createRun(testTemplate, RunStatus.DRAFT, LocalDate.now()));
        checklistRunRepository.save(createRun(testTemplate, RunStatus.DRAFT, LocalDate.now()));
        checklistRunRepository.save(createRun(testTemplate, RunStatus.IN_PROGRESS, LocalDate.now()));
        checklistRunRepository.save(createRun(testTemplate, RunStatus.COMPLETED, LocalDate.now()));
        checklistRunRepository.save(createRun(testTemplate, RunStatus.CANCELLED, LocalDate.now()));

        // When
        List<ChecklistRun> draftRuns = checklistRunRepository.findByOrgNumberAndStatus(ORG_NUMBER, RunStatus.DRAFT);
        List<ChecklistRun> completedRuns = checklistRunRepository.findByOrgNumberAndStatus(ORG_NUMBER, RunStatus.COMPLETED);

        // Then
        assertThat(draftRuns).hasSize(2);
        assertThat(completedRuns).hasSize(1);
    }

    private ChecklistRun createRun(ChecklistTemplate template, RunStatus status, LocalDate runDate) {
        return ChecklistRun.builder()
                .template(template)
                .orgNumber(ORG_NUMBER)
                .runDate(runDate)
                .performedByUserId(1L)
                .status(status)
                .dueAt(LocalDateTime.now().plusDays(1))
                .build();
    }
}
