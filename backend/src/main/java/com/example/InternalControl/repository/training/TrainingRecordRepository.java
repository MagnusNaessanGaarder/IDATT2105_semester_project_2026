package com.example.InternalControl.repository.training;

import com.example.InternalControl.model.training.TrainingRecord;
import com.example.InternalControl.model.user.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for TrainingRecord entities.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Repository
public interface TrainingRecordRepository extends JpaRepository<TrainingRecord, Long> {

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT tr FROM TrainingRecord tr WHERE tr.orgNumber = :orgNumber ORDER BY tr.createdAt DESC")
    List<TrainingRecord> findByOrgNumber(@Param("orgNumber") Integer orgNumber);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT tr FROM TrainingRecord tr WHERE tr.user.userId = :userId AND tr.orgNumber = :orgNumber")
    List<TrainingRecord> findByUserIdAndOrgNumber(@Param("userId") Long userId, @Param("orgNumber") Integer orgNumber);

    List<TrainingRecord> findByUser(AppUser user);

    @Query("SELECT tr FROM TrainingRecord tr WHERE tr.orgNumber = :orgNumber AND tr.status = 'ASSIGNED'")
    List<TrainingRecord> findAssignedByOrgNumber(@Param("orgNumber") Integer orgNumber);

    @Query("SELECT tr FROM TrainingRecord tr WHERE tr.orgNumber = :orgNumber AND tr.expiresAt <= :threshold AND tr.status = 'COMPLETED'")
    List<TrainingRecord> findExpiringSoon(@Param("orgNumber") Integer orgNumber, @Param("threshold") LocalDateTime threshold);
}
