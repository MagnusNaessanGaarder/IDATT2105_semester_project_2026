package com.example.InternalControl.service.training;

import java.util.List;

import com.example.InternalControl.dto.training.TrainingRecordRequest;
import com.example.InternalControl.model.training.TrainingRecord;
import com.example.InternalControl.model.training.TrainingStatus;

/**
 * Manages employee training records, assignments, and expiration tracking.
 *
 * @author TriTacLe
 * @since 1.0
 */
public interface TrainingRecordService {

    TrainingRecord createTrainingRecord(TrainingRecordRequest request, Integer orgNumber, Long currentUserId);

    TrainingRecord getTrainingRecord(Long id, Integer orgNumber);

    TrainingRecord updateTrainingRecord(Long id, TrainingRecordRequest request, Integer orgNumber);

    void deleteTrainingRecord(Long id, Integer orgNumber);

    List<TrainingRecord> getTrainingRecordsByOrg(Integer orgNumber);

    List<TrainingRecord> getTrainingRecordsByUser(Long userId);

    List<TrainingRecord> getTrainingRecordsByUserAndOrg(Long userId, Integer orgNumber);

    List<TrainingRecord> getTrainingRecordsByStatus(Integer orgNumber, TrainingStatus status);

    TrainingRecord completeTrainingRecord(Long id, Integer orgNumber, Long certificateDocumentId);

    List<TrainingRecord> getExpiringTrainingRecords(Integer orgNumber, int daysThreshold);

    Long getExpiringCount(Integer orgNumber);
}
