package com.example.InternalControl.repository.training;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.InternalControl.model.training.TrainingRecord;
import com.example.InternalControl.model.training.TrainingStatus;

@Repository
public interface TrainingRecordRepository extends JpaRepository<TrainingRecord, Long> {

    List<TrainingRecord> findByOrgNumberOrderByCreatedAtDesc(Integer orgNumber);

    List<TrainingRecord> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<TrainingRecord> findByOrgNumberAndUserIdOrderByCreatedAtDesc(Integer orgNumber, Long userId);

    List<TrainingRecord> findByOrgNumberAndStatusOrderByCreatedAtDesc(Integer orgNumber, TrainingStatus status);

    Optional<TrainingRecord> findByTrainingRecordIdAndOrgNumber(Long id, Integer orgNumber);

    @Query("SELECT t FROM TrainingRecord t WHERE t.orgNumber = :orgNumber AND t.expiresAt <= :date AND t.status = 'COMPLETED'")
    List<TrainingRecord> findExpiringByOrgNumber(@Param("orgNumber") Integer orgNumber, @Param("date") LocalDateTime date);

    @Query("SELECT t FROM TrainingRecord t WHERE t.orgNumber = :orgNumber AND t.expiresAt <= :date AND t.status = 'COMPLETED'")
    List<TrainingRecord> findExpiringSoon(@Param("orgNumber") Integer orgNumber, @Param("date") LocalDateTime date);

    Long countByOrgNumberAndStatus(Integer orgNumber, TrainingStatus status);
}
