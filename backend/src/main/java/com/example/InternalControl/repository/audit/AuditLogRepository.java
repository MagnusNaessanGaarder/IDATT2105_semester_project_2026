package com.example.InternalControl.repository.audit;

import com.example.InternalControl.model.audit.AuditLog;
import com.example.InternalControl.model.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AuditLog entities.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT al FROM AuditLog al WHERE al.orgNumber = :orgNumber ORDER BY al.createdAt DESC")
    List<AuditLog> findByOrgNumberOrderByCreatedAtDesc(@Param("orgNumber") Integer orgNumber);

    @Query("SELECT al FROM AuditLog al WHERE al.orgNumber = :orgNumber AND al.createdAt >= :since ORDER BY al.createdAt DESC")
    List<AuditLog> findByOrgNumberAndCreatedAtAfter(@Param("orgNumber") Integer orgNumber, @Param("since") LocalDateTime since);

    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

    List<AuditLog> findByActedByUserOrderByCreatedAtDesc(AppUser actedByUser);

    @Modifying
    @Query("DELETE FROM AuditLog al WHERE al.orgNumber = :orgNumber AND al.createdAt < :cutoff")
    int deleteByOrgNumberAndCreatedAtBefore(@Param("orgNumber") Integer orgNumber,
                                            @Param("cutoff") LocalDateTime cutoff);
}
