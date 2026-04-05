package com.example.InternalControl.repository.checklist;

import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.enums.RunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ChecklistRun entity.
 * Provides CRUD operations and queries for checklist runs.
 */
@Repository
public interface ChecklistRunRepository extends JpaRepository<ChecklistRun, Long> {

    List<ChecklistRun> findByOrgNumber(Integer orgNumber);

    List<ChecklistRun> findByOrgNumberAndStatus(Integer orgNumber, RunStatus status);

    List<ChecklistRun> findByOrgNumberAndRunDateBetween(
            Integer orgNumber, LocalDate startDate, LocalDate endDate);

    List<ChecklistRun> findByTemplateTemplateId(Long templateId);

    Optional<ChecklistRun> findByRunIdAndOrgNumber(Long runId, Integer orgNumber);

    /**
     * Check if run exists for template and date.
     * Used by scheduler to avoid duplicates.
     */
    boolean existsByTemplateTemplateIdAndRunDate(Long templateId, LocalDate runDate);

    List<ChecklistRun> findByOrgNumberAndStatusAndDueAtBefore(
            Integer orgNumber, RunStatus status, java.time.LocalDateTime now);
}
