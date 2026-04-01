package com.example.InternalControl.repository;

import com.example.InternalControl.model.ChecklistRun;
import com.example.InternalControl.model.enums.RunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ChecklistRun entity.
 * Provides CRUD operations and queries for checklist runs.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Repository
public interface ChecklistRunRepository extends JpaRepository<ChecklistRun, Long> {

    /**
     * Find all runs by organization.
     *
     * @param orgNumber the organization number
     * @return list of runs
     */
    List<ChecklistRun> findByOrgNumber(Integer orgNumber);

    /**
     * Find runs by organization and status.
     *
     * @param orgNumber the organization number
     * @param status the run status
     * @return list of runs
     */
    List<ChecklistRun> findByOrgNumberAndStatus(Integer orgNumber, RunStatus status);

    /**
     * Find runs by organization and date range.
     *
     * @param orgNumber the organization number
     * @param startDate the start date
     * @param endDate the end date
     * @return list of runs
     */
    List<ChecklistRun> findByOrgNumberAndRunDateBetween(
            Integer orgNumber, LocalDate startDate, LocalDate endDate);

    /**
     * Find runs by template.
     *
     * @param templateId the template ID
     * @return list of runs
     */
    List<ChecklistRun> findByTemplateTemplateId(Long templateId);

    /**
     * Find run by ID and organization.
     *
     * @param runId the run ID
     * @param orgNumber the organization number
     * @return optional run
     */
    Optional<ChecklistRun> findByRunIdAndOrgNumber(Long runId, Integer orgNumber);

    /**
     * Check if run exists for template and date.
     * Used by scheduler to avoid duplicates.
     *
     * @param templateId the template ID
     * @param runDate the run date
     * @return true if exists
     */
    boolean existsByTemplateTemplateIdAndRunDate(Long templateId, LocalDate runDate);

    /**
     * Find overdue runs.
     *
     * @param orgNumber the organization number
     * @param status the status (should be OVERDUE)
     * @return list of overdue runs
     */
    List<ChecklistRun> findByOrgNumberAndStatusAndDueAtBefore(
            Integer orgNumber, RunStatus status, java.time.LocalDateTime now);
}
