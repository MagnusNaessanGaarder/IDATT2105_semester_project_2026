package com.example.InternalControl.repository.audit;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.InternalControl.model.audit.ActionType;
import com.example.InternalControl.model.audit.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByOrgNumberOrderByCreatedAtDesc(Integer orgNumber);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);

    @Query("SELECT a FROM AuditLog a WHERE a.orgNumber = :orgNumber AND a.createdAt BETWEEN :fromDate AND :toDate ORDER BY a.createdAt DESC")
    List<AuditLog> findByOrgNumberAndDateRange(@Param("orgNumber") Integer orgNumber,
                                              @Param("fromDate") LocalDateTime fromDate,
                                              @Param("toDate") LocalDateTime toDate);

    @Query("SELECT a FROM AuditLog a WHERE a.orgNumber = :orgNumber AND a.actionType = :actionType ORDER BY a.createdAt DESC")
    List<AuditLog> findByOrgNumberAndActionType(@Param("orgNumber") Integer orgNumber,
                                               @Param("actionType") ActionType actionType);

    @Query("SELECT a FROM AuditLog a WHERE a.actedByUserId = :userId ORDER BY a.createdAt DESC")
    List<AuditLog> findByUserId(@Param("userId") Long userId);
}
