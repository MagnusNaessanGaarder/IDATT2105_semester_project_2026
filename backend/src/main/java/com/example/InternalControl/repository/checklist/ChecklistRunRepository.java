package com.example.InternalControl.repository.checklist;

import com.example.InternalControl.model.checklist.ChecklistRun;
import com.example.InternalControl.model.enums.RunStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ChecklistRun entity.
 * Provides CRUD operations and queries for checklist runs.
 */
@Repository
public interface ChecklistRunRepository extends JpaRepository<ChecklistRun, Long> {

    @EntityGraph(attributePaths = {"template", "items"})
    List<ChecklistRun> findByOrgNumber(Integer orgNumber);

    @EntityGraph(attributePaths = {"template", "items"})
    List<ChecklistRun> findByOrgNumberAndStatus(Integer orgNumber, RunStatus status);

    List<ChecklistRun> findByOrgNumberAndRunDateBetween(
            Integer orgNumber, LocalDate startDate, LocalDate endDate);

    List<ChecklistRun> findByTemplateTemplateId(Long templateId);

    @EntityGraph(attributePaths = {"template", "items"})
    Optional<ChecklistRun> findByRunIdAndOrgNumber(Long runId, Integer orgNumber);

    @Query("""
            SELECT DISTINCT r
            FROM ChecklistRun r
            LEFT JOIN FETCH r.template
            LEFT JOIN FETCH r.items
            WHERE r.orgNumber = :orgNumber
            """)
    List<ChecklistRun> findByOrgNumberWithDetails(@Param("orgNumber") Integer orgNumber);

    @Query("""
            SELECT DISTINCT r
            FROM ChecklistRun r
            LEFT JOIN FETCH r.template
            LEFT JOIN FETCH r.items
            WHERE r.orgNumber = :orgNumber
              AND r.status = :status
            """)
    List<ChecklistRun> findByOrgNumberAndStatusWithDetails(
            @Param("orgNumber") Integer orgNumber,
            @Param("status") RunStatus status);

    @Query("""
            SELECT DISTINCT r
            FROM ChecklistRun r
            LEFT JOIN FETCH r.template
            LEFT JOIN FETCH r.items
            WHERE r.runId = :runId
              AND r.orgNumber = :orgNumber
            """)
    Optional<ChecklistRun> findByRunIdAndOrgNumberWithDetails(
            @Param("runId") Long runId,
            @Param("orgNumber") Integer orgNumber);

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

    @Query("""
            SELECT r FROM ChecklistRun r
            WHERE r.orgNumber = :orgNumber
              AND r.status IN :statuses
              AND r.dueAt IS NOT NULL
              AND r.dueAt < :cutoff
            """)
    List<ChecklistRun> findReminderCandidates(@Param("orgNumber") Integer orgNumber,
                                              @Param("statuses") Collection<RunStatus> statuses,
                                              @Param("cutoff") LocalDateTime cutoff);
}
