package com.example.InternalControl.service.training;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.InternalControl.dto.training.TrainingRecordRequest;
import com.example.InternalControl.model.audit.ActionType;
import com.example.InternalControl.model.audit.Audited;
import com.example.InternalControl.model.training.TrainingRecord;
import com.example.InternalControl.model.training.TrainingStatus;
import com.example.InternalControl.repository.training.TrainingRecordRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainingRecordServiceImpl implements TrainingRecordService {

    private final TrainingRecordRepository trainingRecordRepository;

    @Override
    @Transactional
    @Audited(action = ActionType.CREATE, entityType = "TrainingRecord")
    public TrainingRecord createTrainingRecord(TrainingRecordRequest request, Integer orgNumber, Long currentUserId) {
        TrainingRecord trainingRecord = TrainingRecord.builder()
                .userId(request.getUserId())
                .orgNumber(orgNumber)
                .trainingType(request.getTrainingType())
                .title(request.getTitle())
                .completedAt(request.getCompletedAt())
                .expiresAt(request.getExpiresAt())
                .certificateDocumentId(request.getCertificateDocumentId())
                .notes(request.getNotes())
                .status(determineStatus(request))
                .build();

        TrainingRecord saved = trainingRecordRepository.save(trainingRecord);
        log.info("Created training record {} for user {} in org {}", saved.getTrainingRecordId(), request.getUserId(), orgNumber);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public TrainingRecord getTrainingRecord(Long id, Integer orgNumber) {
        return trainingRecordRepository.findByTrainingRecordIdAndOrgNumber(id, orgNumber)
                .orElseThrow(() -> new EntityNotFoundException("Training record not found: " + id));
    }

    @Override
    @Transactional
    @Audited(action = ActionType.UPDATE, entityType = "TrainingRecord")
    public TrainingRecord updateTrainingRecord(Long id, TrainingRecordRequest request, Integer orgNumber) {
        TrainingRecord trainingRecord = getTrainingRecord(id, orgNumber);

        trainingRecord.setUserId(request.getUserId());
        trainingRecord.setTrainingType(request.getTrainingType());
        trainingRecord.setTitle(request.getTitle());
        trainingRecord.setCompletedAt(request.getCompletedAt());
        trainingRecord.setExpiresAt(request.getExpiresAt());
        trainingRecord.setCertificateDocumentId(request.getCertificateDocumentId());
        trainingRecord.setNotes(request.getNotes());
        trainingRecord.setStatus(determineStatus(request));

        TrainingRecord updated = trainingRecordRepository.save(trainingRecord);
        log.info("Updated training record {} in org {}", id, orgNumber);
        return updated;
    }

    @Override
    @Transactional
    @Audited(action = ActionType.DELETE, entityType = "TrainingRecord")
    public void deleteTrainingRecord(Long id, Integer orgNumber) {
        TrainingRecord trainingRecord = getTrainingRecord(id, orgNumber);
        trainingRecordRepository.delete(trainingRecord);
        log.info("Deleted training record {} from org {}", id, orgNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainingRecord> getTrainingRecordsByOrg(Integer orgNumber) {
        return trainingRecordRepository.findByOrgNumberOrderByCreatedAtDesc(orgNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainingRecord> getTrainingRecordsByUser(Long userId) {
        return trainingRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainingRecord> getTrainingRecordsByUserAndOrg(Long userId, Integer orgNumber) {
        return trainingRecordRepository.findByOrgNumberAndUserIdOrderByCreatedAtDesc(orgNumber, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainingRecord> getTrainingRecordsByStatus(Integer orgNumber, TrainingStatus status) {
        return trainingRecordRepository.findByOrgNumberAndStatusOrderByCreatedAtDesc(orgNumber, status);
    }

    @Override
    @Transactional
    @Audited(action = ActionType.UPDATE, entityType = "TrainingRecord")
    public TrainingRecord completeTrainingRecord(Long id, Integer orgNumber, Long certificateDocumentId) {
        TrainingRecord trainingRecord = getTrainingRecord(id, orgNumber);
        trainingRecord.setCompletedAt(LocalDateTime.now());
        trainingRecord.setCertificateDocumentId(certificateDocumentId);
        trainingRecord.setStatus(TrainingStatus.COMPLETED);

        TrainingRecord updated = trainingRecordRepository.save(trainingRecord);
        log.info("Completed training record {} in org {}", id, orgNumber);
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainingRecord> getExpiringTrainingRecords(Integer orgNumber, int daysThreshold) {
        LocalDateTime thresholdDate = LocalDateTime.now().plusDays(daysThreshold);
        return trainingRecordRepository.findExpiringByOrgNumber(orgNumber, thresholdDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getExpiringCount(Integer orgNumber) {
        LocalDateTime thresholdDate = LocalDateTime.now().plusDays(30);
        return trainingRecordRepository.countExpiringByOrgNumber(orgNumber, thresholdDate);
    }

    private TrainingStatus determineStatus(TrainingRecordRequest request) {
        if (request.getCompletedAt() != null) {
            if (request.getExpiresAt() != null && request.getExpiresAt().isBefore(LocalDateTime.now())) {
                return TrainingStatus.EXPIRED;
            }
            return TrainingStatus.COMPLETED;
        }
        return TrainingStatus.ASSIGNED;
    }
}
