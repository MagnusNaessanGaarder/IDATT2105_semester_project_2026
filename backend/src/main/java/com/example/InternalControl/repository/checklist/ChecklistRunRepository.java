package com.example.InternalControl.repository.checklist;

import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.enums.RunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link ChecklistRun} entities.
 * Manages checklist execution instances and their status.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Repository
public interface ChecklistRunRepository extends JpaRepository<ChecklistRun, Long> {

    List<ChecklistRun> findByOrgNumber(Integer orgNumber);

    List<ChecklistRun> findByOrgNumberAndStatus(Integer orgNumber, RunStatus status);

    List<ChecklistRun> findByOrgNumberAndRunDateBetween(
            Integer orgNumber, LocalDate startDate, LocalDate endDate);

    List<ChecklistRun> findByTemplateTemplateId(Long templateId);

    Optional<ChecklistRun> findByRunIdAndOrgNumber(Long runId, Integer orgNumber);

    @Query("SELECT DISTINCT r FROM ChecklistRun r LEFT JOIN FETCH r.template LEFT JOIN FETCH r.items WHERE r.orgNumber = :orgNumber")
    List<ChecklistRun> findByOrgNumberWithTemplate(@Param("orgNumber") Integer orgNumber);

    @Query("SELECT DISTINCT r FROM ChecklistRun r LEFT JOIN FETCH r.template LEFT JOIN FETCH r.items WHERE r.orgNumber = :orgNumber AND r.runDate BETWEEN :from AND :to")
    List<ChecklistRun> findByOrgNumberAndRunDateBetweenWithTemplate(
            @Param("orgNumber") Integer orgNumber,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /**
     * Check if run exists for template and date.
     * Used by scheduler to avoid duplicates.
     */
    boolean existsByTemplateTemplateIdAndRunDate(Long templateId, LocalDate runDate);

    List<ChecklistRun> findByOrgNumberAndStatusAndDueAtBefore(
            Integer orgNumber, RunStatus status, java.time.LocalDateTime now);
}
