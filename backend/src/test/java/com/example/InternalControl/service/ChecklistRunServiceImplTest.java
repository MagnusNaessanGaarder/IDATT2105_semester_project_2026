package com.example.InternalControl.service;

import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.checklist.ChecklistRunItem;
import com.example.InternalControl.model.checklist.ChecklistTemplate;
import com.example.InternalControl.model.checklist.ChecklistTemplateItem;
import com.example.InternalControl.shared.enums.Frequency;
import com.example.InternalControl.shared.enums.ItemType;
import com.example.InternalControl.shared.enums.ModuleType;
import com.example.InternalControl.shared.enums.RunStatus;
import com.example.InternalControl.repository.checklist.ChecklistRunItemRepository;
import com.example.InternalControl.repository.checklist.ChecklistRunRepository;
import com.example.InternalControl.repository.checklist.ChecklistTemplateRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChecklistRunServiceImpl.
 *
 * @author TriTacLe
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class ChecklistRunServiceImplTest {

    @Mock
    private ChecklistRunRepository runRepository;

    @Mock
    private ChecklistRunItemRepository runItemRepository;

    @Mock
    private ChecklistTemplateRepository templateRepository;

    @InjectMocks
    private com.example.InternalControl.service.checklist.ChecklistRunServiceImpl runService;

    @Test
    void shouldCreateRun() {
        // Given
        Long templateId = 1L;
        Integer orgNumber = 123;
        Long userId = 1L;
        LocalDate runDate = LocalDate.now();

        ChecklistTemplate template = createTemplateWithItems(templateId, orgNumber);

        when(templateRepository.findByTemplateIdAndOrgNumber(templateId, orgNumber))
                .thenReturn(Optional.of(template));
        when(runRepository.existsByTemplateTemplateIdAndRunDate(templateId, runDate))
                .thenReturn(false);
        when(runRepository.save(any(ChecklistRun.class))).thenAnswer(inv -> {
            ChecklistRun run = inv.getArgument(0);
            run.setRunId(1L);
            return run;
        });

        // When
        ChecklistRun result = runService.createRun(templateId, orgNumber, userId, runDate);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRunId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(RunStatus.DRAFT);
        assertThat(result.getOrgNumber()).isEqualTo(orgNumber);

        verify(runRepository).save(any(ChecklistRun.class));
    }

    @Test
    void shouldCreateRunItemsFromTemplateItems() {
        // Given
        Long templateId = 1L;
        Integer orgNumber = 123;
        Long userId = 1L;
        LocalDate runDate = LocalDate.now();

        ChecklistTemplateItem item1 = ChecklistTemplateItem.builder()
                .itemId(1L)
                .label("Question 1")
                .itemType(ItemType.BOOLEAN)
                .build();

        ChecklistTemplateItem item2 = ChecklistTemplateItem.builder()
                .itemId(2L)
                .label("Question 2")
                .itemType(ItemType.TEXT)
                .build();

        ChecklistTemplate template = ChecklistTemplate.builder()
                .templateId(templateId)
                .orgNumber(orgNumber)
                .title("Test Template")
                .moduleType(ModuleType.FOOD)
                .frequency(Frequency.DAILY)
                .isActive(true)
                .items(Arrays.asList(item1, item2))
                .build();

        when(templateRepository.findByTemplateIdAndOrgNumber(templateId, orgNumber))
                .thenReturn(Optional.of(template));
        when(runRepository.existsByTemplateTemplateIdAndRunDate(templateId, runDate))
                .thenReturn(false);
        when(runRepository.save(any(ChecklistRun.class))).thenAnswer(inv -> {
            ChecklistRun run = inv.getArgument(0);
            run.setRunId(1L);
            return run;
        });

        // When
        ChecklistRun result = runService.createRun(templateId, orgNumber, userId, runDate);

        // Then
        verify(runItemRepository, times(2)).save(any(ChecklistRunItem.class));
    }

    @Test
    void shouldThrowWhenTemplateNotFound() {
        // Given
        Long templateId = 999L;

        when(templateRepository.findByTemplateIdAndOrgNumber(any(), any()))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> runService.createRun(templateId, 123, 1L, LocalDate.now()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Template not found");
    }

    @Test
    void shouldThrowWhenTemplateInactive() {
        // Given
        Long templateId = 1L;
        Integer orgNumber = 123;

        ChecklistTemplate template = ChecklistTemplate.builder()
                .templateId(templateId)
                .orgNumber(orgNumber)
                .isActive(false)
                .build();

        when(templateRepository.findByTemplateIdAndOrgNumber(templateId, orgNumber))
                .thenReturn(Optional.of(template));

        // When/Then
        assertThatThrownBy(() -> runService.createRun(templateId, orgNumber, 1L, LocalDate.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot create run from inactive template");
    }

    @Test
    void shouldThrowWhenDuplicateRunExists() {
        // Given
        Long templateId = 1L;
        Integer orgNumber = 123;
        LocalDate runDate = LocalDate.now();

        ChecklistTemplate template = createTemplateWithItems(templateId, orgNumber);

        when(templateRepository.findByTemplateIdAndOrgNumber(templateId, orgNumber))
                .thenReturn(Optional.of(template));
        when(runRepository.existsByTemplateTemplateIdAndRunDate(templateId, runDate))
                .thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> runService.createRun(templateId, orgNumber, 1L, runDate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Run already exists");
    }

    @Test
    void shouldCompleteRun() {
        // Given
        Long runId = 1L;
        Integer orgNumber = 123;

        ChecklistRun run = ChecklistRun.builder()
                .runId(runId)
                .orgNumber(orgNumber)
                .status(RunStatus.IN_PROGRESS)
                .build();

        when(runRepository.findByRunIdAndOrgNumber(runId, orgNumber))
                .thenReturn(Optional.of(run));
        when(runRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        ChecklistRun result = runService.completeRun(runId, orgNumber);

        // Then
        assertThat(result.getStatus()).isEqualTo(RunStatus.COMPLETED);
    }

    @Test
    void shouldCompleteDraftRun() {
        // Given
        Long runId = 1L;
        Integer orgNumber = 123;

        ChecklistRun run = ChecklistRun.builder()
                .runId(runId)
                .orgNumber(orgNumber)
                .status(RunStatus.DRAFT)
                .build();

        when(runRepository.findByRunIdAndOrgNumber(runId, orgNumber))
                .thenReturn(Optional.of(run));
        when(runRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        ChecklistRun result = runService.completeRun(runId, orgNumber);

        // Then
        assertThat(result.getStatus()).isEqualTo(RunStatus.COMPLETED);
    }

    @Test
    void shouldThrowWhenCompletingAlreadyCompletedRun() {
        // Given
        Long runId = 1L;
        Integer orgNumber = 123;

        ChecklistRun run = ChecklistRun.builder()
                .runId(runId)
                .orgNumber(orgNumber)
                .status(RunStatus.COMPLETED)
                .build();

        when(runRepository.findByRunIdAndOrgNumber(runId, orgNumber))
                .thenReturn(Optional.of(run));

        // When/Then
        assertThatThrownBy(() -> runService.completeRun(runId, orgNumber))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Run cannot be completed");
    }

    @Test
    void shouldThrowWhenCompletingCancelledRun() {
        // Given
        Long runId = 1L;
        Integer orgNumber = 123;

        ChecklistRun run = ChecklistRun.builder()
                .runId(runId)
                .orgNumber(orgNumber)
                .status(RunStatus.CANCELLED)
                .build();

        when(runRepository.findByRunIdAndOrgNumber(runId, orgNumber))
                .thenReturn(Optional.of(run));

        // When/Then
        assertThatThrownBy(() -> runService.completeRun(runId, orgNumber))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Run cannot be completed");
    }

    @Test
    void shouldGetRun() {
        // Given
        Long runId = 1L;
        Integer orgNumber = 123;

        ChecklistRun run = ChecklistRun.builder()
                .runId(runId)
                .orgNumber(orgNumber)
                .status(RunStatus.DRAFT)
                .build();

        when(runRepository.findByRunIdAndOrgNumber(runId, orgNumber))
                .thenReturn(Optional.of(run));

        // When
        ChecklistRun result = runService.getRun(runId, orgNumber);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRunId()).isEqualTo(runId);
    }

    @Test
    void shouldThrowWhenRunNotFound() {
        // Given
        Long runId = 999L;
        Integer orgNumber = 123;

        when(runRepository.findByRunIdAndOrgNumber(runId, orgNumber))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> runService.getRun(runId, orgNumber))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Run not found");
    }

    @Test
    void shouldGetRunsByOrg() {
        // Given
        Integer orgNumber = 123;

        ChecklistRun run1 = ChecklistRun.builder()
                .runId(1L)
                .orgNumber(orgNumber)
                .build();

        ChecklistRun run2 = ChecklistRun.builder()
                .runId(2L)
                .orgNumber(orgNumber)
                .build();

        when(runRepository.findByOrgNumber(orgNumber))
                .thenReturn(Arrays.asList(run1, run2));

        // When
        List<ChecklistRun> result = runService.getRunsByOrg(orgNumber);

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    void shouldGetRunsByStatus() {
        // Given
        Integer orgNumber = 123;

        ChecklistRun run = ChecklistRun.builder()
                .runId(1L)
                .orgNumber(orgNumber)
                .status(RunStatus.COMPLETED)
                .build();

        when(runRepository.findByOrgNumberAndStatus(orgNumber, RunStatus.COMPLETED))
                .thenReturn(Arrays.asList(run));

        // When
        List<ChecklistRun> result = runService.getRunsByStatus(orgNumber, RunStatus.COMPLETED);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(RunStatus.COMPLETED);
    }

    @Test
    void shouldUpdateRunItem() {
        // Given
        Long runId = 1L;
        Long itemId = 1L;
        Integer orgNumber = 123;

        ChecklistRun run = ChecklistRun.builder()
                .runId(runId)
                .orgNumber(orgNumber)
                .status(RunStatus.DRAFT)
                .build();

        ChecklistRunItem item = ChecklistRunItem.builder()
                .runItemId(itemId)
                .booleanValue(true)
                .build();

        ChecklistRunItem update = ChecklistRunItem.builder()
                .booleanValue(false)
                .textValue("Test comment")
                .numericValue(new BigDecimal("10.5"))
                .selectedChoice("Option A")
                .isDeviation(true)
                .commentText("Deviation noted")
                .build();

        when(runRepository.findByRunIdAndOrgNumber(runId, orgNumber))
                .thenReturn(Optional.of(run));
        when(runItemRepository.findByRunRunIdAndTemplateItemId(runId, itemId))
                .thenReturn(Optional.of(item));
        when(runItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        ChecklistRunItem result = runService.updateRunItem(runId, itemId, update, orgNumber);

        // Then
        assertThat(result.getBooleanValue()).isFalse();
        assertThat(result.getTextValue()).isEqualTo("Test comment");
        assertThat(result.getNumericValue()).isEqualTo(new BigDecimal("10.5"));
        assertThat(result.getSelectedChoice()).isEqualTo("Option A");
        assertThat(result.getIsDeviation()).isTrue();
        assertThat(result.getCommentText()).isEqualTo("Deviation noted");
    }

    @Test
    void shouldThrowWhenUpdatingRunItemForCompletedRun() {
        // Given
        Long runId = 1L;
        Long itemId = 1L;
        Integer orgNumber = 123;

        ChecklistRun run = ChecklistRun.builder()
                .runId(runId)
                .orgNumber(orgNumber)
                .status(RunStatus.COMPLETED)
                .build();

        when(runRepository.findByRunIdAndOrgNumber(runId, orgNumber))
                .thenReturn(Optional.of(run));

        // When/Then
        assertThatThrownBy(() -> runService.updateRunItem(runId, itemId, new ChecklistRunItem(), orgNumber))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot update items for completed run");
    }

    @Test
    void shouldThrowWhenRunItemNotFound() {
        // Given
        Long runId = 1L;
        Long itemId = 999L;
        Integer orgNumber = 123;

        ChecklistRun run = ChecklistRun.builder()
                .runId(runId)
                .orgNumber(orgNumber)
                .status(RunStatus.DRAFT)
                .build();

        when(runRepository.findByRunIdAndOrgNumber(runId, orgNumber))
                .thenReturn(Optional.of(run));
        when(runItemRepository.findByRunRunIdAndTemplateItemId(runId, itemId))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> runService.updateRunItem(runId, itemId, new ChecklistRunItem(), orgNumber))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Run item not found");
    }

    @Test
    void shouldCheckOverdueRuns() {
        // Given
        Integer orgNumber = 123;
        LocalDateTime now = LocalDateTime.now();

        ChecklistRun overdueRun = ChecklistRun.builder()
                .runId(1L)
                .orgNumber(orgNumber)
                .status(RunStatus.IN_PROGRESS)
                .dueAt(now.minusHours(1))
                .build();

        when(runRepository.findByOrgNumber(orgNumber))
                .thenReturn(Arrays.asList(overdueRun));

        // When
        runService.checkOverdueRuns(orgNumber);

        // Then
        verify(runRepository).save(overdueRun);
        assertThat(overdueRun.getStatus()).isEqualTo(RunStatus.OVERDUE);
    }

    @Test
    void shouldHandleNoOverdueRuns() {
        // Given
        Integer orgNumber = 123;

        when(runRepository.findByOrgNumber(orgNumber))
                .thenReturn(Collections.emptyList());

        // When
        runService.checkOverdueRuns(orgNumber);

        // Then
        verify(runRepository, never()).save(any());
    }

    private ChecklistTemplate createTemplateWithItems(Long templateId, Integer orgNumber) {
        ChecklistTemplateItem item = ChecklistTemplateItem.builder()
                .itemId(1L)
                .label("Test Question")
                .itemType(ItemType.BOOLEAN)
                .build();

        return ChecklistTemplate.builder()
                .templateId(templateId)
                .orgNumber(orgNumber)
                .title("Test Template")
                .moduleType(ModuleType.FOOD)
                .frequency(Frequency.DAILY)
                .isActive(true)
                .items(Arrays.asList(item))
                .build();
    }
}
