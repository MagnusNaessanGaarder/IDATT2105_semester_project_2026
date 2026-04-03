package com.example.InternalControl.repository.deviation;

import com.example.InternalControl.model.deviation.DeviationReport;
import com.example.InternalControl.shared.enums.DeviationStatus;
import com.example.InternalControl.shared.enums.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for DeviationReport entity.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Repository
public interface DeviationReportRepository extends JpaRepository<DeviationReport, Long> {

    List<DeviationReport> findByOrgNumber(Integer orgNumber);

    List<DeviationReport> findByOrgNumberAndStatus(Integer orgNumber, DeviationStatus status);

    List<DeviationReport> findByOrgNumberAndSeverity(Integer orgNumber, Severity severity);

    List<DeviationReport> findByAssignedToUserIdAndOrgNumber(Long userId, Integer orgNumber);

    Optional<DeviationReport> findByReportIdAndOrgNumber(Long reportId, Integer orgNumber);

    boolean existsByReportIdAndOrgNumber(Long reportId, Integer orgNumber);

    @Query("SELECT COUNT(d) FROM DeviationReport d WHERE d.orgNumber = :orgNumber AND d.status != 'closed'")
    Long countOpenByOrgNumber(@Param("orgNumber") Integer orgNumber);

    @Query("SELECT d FROM DeviationReport d WHERE d.orgNumber = :orgNumber " +
           "AND (:status IS NULL OR d.status = :status) " +
           "AND (:severity IS NULL OR d.severity = :severity) " +
           "AND (:assignedToId IS NULL OR d.assignedTo.userId = :assignedToId) " +
           "AND (:fromDate IS NULL OR d.reportDate >= :fromDate) " +
           "AND (:toDate IS NULL OR d.reportDate <= :toDate)")
    List<DeviationReport> searchReports(
            @Param("orgNumber") Integer orgNumber,
            @Param("status") DeviationStatus status,
            @Param("severity") Severity severity,
            @Param("assignedToId") Long assignedToId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
