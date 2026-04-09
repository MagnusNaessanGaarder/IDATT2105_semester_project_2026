package com.example.InternalControl.service.training;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.InternalControl.dto.training.TrainingRecordRequest;
import com.example.InternalControl.model.audit.ActionType;
import com.example.InternalControl.model.audit.Audited;
import com.example.InternalControl.model.document.OrganizationDocument;
import com.example.InternalControl.model.training.TrainingRecord;
import com.example.InternalControl.model.training.TrainingStatus;
import com.example.InternalControl.model.user.AppUser;
import com.example.InternalControl.repository.document.OrganizationDocumentRepository;
import com.example.InternalControl.repository.training.TrainingRecordRepository;
import com.example.InternalControl.repository.user.AppUserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of TrainingRecordService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrainingRecordServiceImpl implements TrainingRecordService {

    private final TrainingRecordRepository trainingRecordRepository;
    private final AppUserRepository appUserRepository;
    private final OrganizationDocumentRepository documentRepository;

    @Override
    @Transactional
    @Audited(action = ActionType.CREATE, entityType = "TrainingRecord")
    public TrainingRecord createTrainingRecord(TrainingRecordRequest request, Integer orgNumber, Long currentUserId) {
        AppUser user = appUserRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.getUserId()));

        OrganizationDocument certificateDocument = null;
        if (request.getCertificateDocumentId() != null) {
            certificateDocument = documentRepository.findById(request.getCertificateDocumentId())
                    .orElseThrow(() -> new EntityNotFoundException("Document not found: " + request.getCertificateDocumentId()));
        }

        TrainingRecord trainingRecord = new TrainingRecord();
        trainingRecord.setUser(user);
        trainingRecord.setOrgNumber(orgNumber);
        trainingRecord.setTrainingType(request.getTrainingType());
        trainingRecord.setTitle(request.getTitle());
        trainingRecord.setCompletedAt(request.getCompletedAt());
        trainingRecord.setExpiresAt(request.getExpiresAt());
        trainingRecord.setCertificateDocument(certificateDocument);
        trainingRecord.setNotes(request.getNotes());
        trainingRecord.setStatus(determineStatus(request));

        TrainingRecord saved = trainingRecordRepository.save(trainingRecord);
        log.info("Created training record {} for user {} in org {}", saved.getTrainingRecordId(), request.getUserId(), orgNumber);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public TrainingRecord getTrainingRecord(Long id, Integer orgNumber) {
        return trainingRecordRepository.findById(id)
                .filter(tr -> tr.getOrgNumber().equals(orgNumber))
                .orElseThrow(() -> new EntityNotFoundException("Training record not found: " + id));
    }

    @Override
    @Transactional
    @Audited(action = ActionType.UPDATE, entityType = "TrainingRecord")
    public TrainingRecord updateTrainingRecord(Long id, TrainingRecordRequest request, Integer orgNumber) {
        TrainingRecord trainingRecord = getTrainingRecord(id, orgNumber);

        AppUser user = appUserRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.getUserId()));

        OrganizationDocument certificateDocument = null;
        if (request.getCertificateDocumentId() != null) {
            certificateDocument = documentRepository.findById(request.getCertificateDocumentId())
                    .orElseThrow(() -> new EntityNotFoundException("Document not found: " + request.getCertificateDocumentId()));
        }

        trainingRecord.setUser(user);
        trainingRecord.setTrainingType(request.getTrainingType());
        trainingRecord.setTitle(request.getTitle());
        trainingRecord.setCompletedAt(request.getCompletedAt());
        trainingRecord.setExpiresAt(request.getExpiresAt());
        trainingRecord.setCertificateDocument(certificateDocument);
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
        return trainingRecordRepository.findByOrgNumber(orgNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainingRecord> getTrainingRecordsByUser(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        return trainingRecordRepository.findByUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainingRecord> getTrainingRecordsByUserAndOrg(Long userId, Integer orgNumber) {
        return trainingRecordRepository.findByUserIdAndOrgNumber(userId, orgNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainingRecord> getTrainingRecordsByStatus(Integer orgNumber, TrainingStatus status) {
        // Use the repository method or filter in memory
        return trainingRecordRepository.findByOrgNumber(orgNumber)
                .stream()
                .filter(tr -> tr.getStatus() == status)
                .toList();
    }

    @Override
    @Transactional
    @Audited(action = ActionType.UPDATE, entityType = "TrainingRecord")
    public TrainingRecord completeTrainingRecord(Long id, Integer orgNumber, Long certificateDocumentId) {
        TrainingRecord trainingRecord = getTrainingRecord(id, orgNumber);
        trainingRecord.setCompletedAt(LocalDateTime.now());

        if (certificateDocumentId != null) {
            OrganizationDocument document = documentRepository.findById(certificateDocumentId)
                    .orElseThrow(() -> new EntityNotFoundException("Document not found: " + certificateDocumentId));
            trainingRecord.setCertificateDocument(document);
        }

        trainingRecord.setStatus(TrainingStatus.COMPLETED);

        TrainingRecord updated = trainingRecordRepository.save(trainingRecord);
        log.info("Completed training record {} in org {}", id, orgNumber);
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainingRecord> getExpiringTrainingRecords(Integer orgNumber, int daysThreshold) {
        LocalDateTime thresholdDate = LocalDateTime.now().plusDays(daysThreshold);
        return trainingRecordRepository.findExpiringSoon(orgNumber, thresholdDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getExpiringCount(Integer orgNumber) {
        LocalDateTime thresholdDate = LocalDateTime.now().plusDays(30);
        return (long) trainingRecordRepository.findExpiringSoon(orgNumber, thresholdDate).size();
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
