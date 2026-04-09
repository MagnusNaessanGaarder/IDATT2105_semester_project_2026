package com.example.InternalControl.service.training;

import com.example.InternalControl.dto.training.TrainingRecordRequest;
import com.example.InternalControl.model.document.OrganizationDocument;
import com.example.InternalControl.model.training.TrainingRecord;
import com.example.InternalControl.model.training.TrainingStatus;
import com.example.InternalControl.model.training.TrainingType;
import com.example.InternalControl.model.user.AppUser;
import com.example.InternalControl.repository.document.OrganizationDocumentRepository;
import com.example.InternalControl.repository.training.TrainingRecordRepository;
import com.example.InternalControl.repository.user.AppUserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TrainingRecordService.
 */
@ExtendWith(MockitoExtension.class)
class TrainingRecordServiceTest {

    @Mock
    private TrainingRecordRepository trainingRecordRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private OrganizationDocumentRepository documentRepository;

    @InjectMocks
    private TrainingRecordServiceImpl trainingRecordService;

    private static final Long RECORD_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final Integer ORG_NUMBER = 123;
    private static final Long DOCUMENT_ID = 100L;

    private AppUser testUser;
    private TrainingRecord testRecord;
    private OrganizationDocument testDocument;

    @BeforeEach
    void setUp() {
        testUser = createTestUser(USER_ID, "test@example.com");
        testDocument = createTestDocument(DOCUMENT_ID);
        testRecord = createTestRecord(RECORD_ID, USER_ID, TrainingStatus.ASSIGNED);
    }

    // ==================== CREATE TRAINING RECORD TESTS ====================

    @Test
    void shouldCreateTrainingRecord() {
        // Given
        TrainingRecordRequest request = createTrainingRequest();
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(testDocument));
        when(trainingRecordRepository.save(any(TrainingRecord.class))).thenAnswer(inv -> {
            TrainingRecord record = inv.getArgument(0);
            record.setTrainingRecordId(RECORD_ID);
            return record;
        });

        // When
        TrainingRecord result = trainingRecordService.createTrainingRecord(request, ORG_NUMBER, USER_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTrainingRecordId()).isEqualTo(RECORD_ID);
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.getOrgNumber()).isEqualTo(ORG_NUMBER);
        assertThat(result.getTrainingType()).isEqualTo(TrainingType.FOOD_HYGIENE);
        assertThat(result.getTitle()).isEqualTo("Food Safety Training");
        assertThat(result.getStatus()).isEqualTo(TrainingStatus.COMPLETED);
    }

    @Test
    void shouldThrowWhenCreatingForNonExistentUser() {
        // Given
        TrainingRecordRequest request = createTrainingRequest();
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> trainingRecordService.createTrainingRecord(request, ORG_NUMBER, USER_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(trainingRecordRepository, never()).save(any());
    }

    @Test
    void shouldCreateWithoutCertificateDocument() {
        // Given
        TrainingRecordRequest request = createTrainingRequest();
        request.setCertificateDocumentId(null);
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(trainingRecordRepository.save(any(TrainingRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        TrainingRecord result = trainingRecordService.createTrainingRecord(request, ORG_NUMBER, USER_ID);

        // Then
        assertThat(result.getCertificateDocument()).isNull();
    }

    @Test
    void shouldDetermineStatusAsCompleted() {
        // Given
        TrainingRecordRequest request = createTrainingRequest();
        request.setCompletedAt(LocalDateTime.now());
        request.setExpiresAt(LocalDateTime.now().plusMonths(12));
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(testDocument));
        when(trainingRecordRepository.save(any(TrainingRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        TrainingRecord result = trainingRecordService.createTrainingRecord(request, ORG_NUMBER, USER_ID);

        // Then
        assertThat(result.getStatus()).isEqualTo(TrainingStatus.COMPLETED);
    }

    @Test
    void shouldDetermineStatusAsExpired() {
        // Given
        TrainingRecordRequest request = createTrainingRequest();
        request.setCompletedAt(LocalDateTime.now().minusMonths(13));
        request.setExpiresAt(LocalDateTime.now().minusMonths(1)); // Already expired
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(testDocument));
        when(trainingRecordRepository.save(any(TrainingRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        TrainingRecord result = trainingRecordService.createTrainingRecord(request, ORG_NUMBER, USER_ID);

        // Then
        assertThat(result.getStatus()).isEqualTo(TrainingStatus.EXPIRED);
    }

    @Test
    void shouldDetermineStatusAsAssigned() {
        // Given
        TrainingRecordRequest request = createTrainingRequest();
        request.setCompletedAt(null);
        request.setCertificateDocumentId(null);
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(trainingRecordRepository.save(any(TrainingRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        TrainingRecord result = trainingRecordService.createTrainingRecord(request, ORG_NUMBER, USER_ID);

        // Then
        assertThat(result.getStatus()).isEqualTo(TrainingStatus.ASSIGNED);
    }

    // ==================== GET TRAINING RECORD TESTS ====================

    @Test
    void shouldGetTrainingRecord() {
        // Given
        when(trainingRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(testRecord));

        // When
        TrainingRecord result = trainingRecordService.getTrainingRecord(RECORD_ID, ORG_NUMBER);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTrainingRecordId()).isEqualTo(RECORD_ID);
    }

    @Test
    void shouldThrowWhenRecordNotFound() {
        // Given
        when(trainingRecordRepository.findById(RECORD_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> trainingRecordService.getTrainingRecord(RECORD_ID, ORG_NUMBER))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Training record not found");
    }

    @Test
    void shouldThrowWhenRecordBelongsToDifferentOrg() {
        // Given
        testRecord.setOrgNumber(999);
        when(trainingRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(testRecord));

        // When/Then
        assertThatThrownBy(() -> trainingRecordService.getTrainingRecord(RECORD_ID, ORG_NUMBER))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Training record not found");
    }

    // ==================== UPDATE TRAINING RECORD TESTS ====================

    @Test
    void shouldUpdateTrainingRecord() {
        // Given
        TrainingRecordRequest request = createTrainingRequest();
        request.setTitle("Updated Title");
        when(trainingRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(testRecord));
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(testDocument));
        when(trainingRecordRepository.save(any(TrainingRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        TrainingRecord result = trainingRecordService.updateTrainingRecord(RECORD_ID, request, ORG_NUMBER);

        // Then
        assertThat(result.getTitle()).isEqualTo("Updated Title");
    }

    // ==================== DELETE TRAINING RECORD TESTS ====================

    @Test
    void shouldDeleteTrainingRecord() {
        // Given
        when(trainingRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(testRecord));

        // When
        trainingRecordService.deleteTrainingRecord(RECORD_ID, ORG_NUMBER);

        // Then
        verify(trainingRecordRepository).delete(testRecord);
    }

    // ==================== LISTING TESTS ====================

    @Test
    void shouldGetTrainingRecordsByOrg() {
        // Given
        when(trainingRecordRepository.findByOrgNumber(ORG_NUMBER))
                .thenReturn(List.of(testRecord));

        // When
        List<TrainingRecord> result = trainingRecordService.getTrainingRecordsByOrg(ORG_NUMBER);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void shouldGetTrainingRecordsByUser() {
        // Given
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(trainingRecordRepository.findByUser(testUser))
                .thenReturn(List.of(testRecord));

        // When
        List<TrainingRecord> result = trainingRecordService.getTrainingRecordsByUser(USER_ID);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void shouldGetTrainingRecordsByUserAndOrg() {
        // Given
        when(trainingRecordRepository.findByUserIdAndOrgNumber(USER_ID, ORG_NUMBER))
                .thenReturn(List.of(testRecord));

        // When
        List<TrainingRecord> result = trainingRecordService.getTrainingRecordsByUserAndOrg(USER_ID, ORG_NUMBER);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void shouldGetTrainingRecordsByStatus() {
        // Given
        TrainingRecord assignedRecord = createTestRecord(1L, USER_ID, TrainingStatus.ASSIGNED);
        TrainingRecord completedRecord = createTestRecord(2L, USER_ID, TrainingStatus.COMPLETED);
        when(trainingRecordRepository.findByOrgNumber(ORG_NUMBER))
                .thenReturn(List.of(assignedRecord, completedRecord));

        // When
        List<TrainingRecord> result = trainingRecordService.getTrainingRecordsByStatus(
                ORG_NUMBER, TrainingStatus.ASSIGNED);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(TrainingStatus.ASSIGNED);
    }

    // ==================== COMPLETE TRAINING RECORD TESTS ====================

    @Test
    void shouldCompleteTrainingRecord() {
        // Given
        testRecord.setStatus(TrainingStatus.ASSIGNED);
        when(trainingRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(testRecord));
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(testDocument));
        when(trainingRecordRepository.save(any(TrainingRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        TrainingRecord result = trainingRecordService.completeTrainingRecord(RECORD_ID, ORG_NUMBER, DOCUMENT_ID);

        // Then
        assertThat(result.getStatus()).isEqualTo(TrainingStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getCertificateDocument()).isEqualTo(testDocument);
    }

    @Test
    void shouldCompleteWithoutCertificate() {
        // Given
        when(trainingRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(testRecord));
        when(trainingRecordRepository.save(any(TrainingRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        TrainingRecord result = trainingRecordService.completeTrainingRecord(RECORD_ID, ORG_NUMBER, null);

        // Then
        assertThat(result.getStatus()).isEqualTo(TrainingStatus.COMPLETED);
        assertThat(result.getCertificateDocument()).isNull();
    }

    // ==================== EXPIRING TRAINING TESTS ====================

    @Test
    void shouldGetExpiringTrainingRecords() {
        // Given
        when(trainingRecordRepository.findExpiringSoon(eq(ORG_NUMBER), any(LocalDateTime.class)))
                .thenReturn(List.of(testRecord));

        // When
        List<TrainingRecord> result = trainingRecordService.getExpiringTrainingRecords(ORG_NUMBER, 30);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void shouldGetExpiringCount() {
        // Given
        when(trainingRecordRepository.findExpiringSoon(eq(ORG_NUMBER), any(LocalDateTime.class)))
                .thenReturn(List.of(testRecord, createTestRecord(2L, USER_ID, TrainingStatus.COMPLETED)));

        // When
        Long result = trainingRecordService.getExpiringCount(ORG_NUMBER);

        // Then
        assertThat(result).isEqualTo(2L);
    }

    // ==================== HELPER METHODS ====================

    private AppUser createTestUser(Long userId, String email) {
        AppUser user = new AppUser();
        user.setUserId(userId);
        user.setEmail(email);
        user.setDisplayName("Test User");
        return user;
    }

    private OrganizationDocument createTestDocument(Long documentId) {
        OrganizationDocument doc = new OrganizationDocument();
        doc.setDocumentId(documentId);
        doc.setTitle("Certificate.pdf");
        return doc;
    }

    private TrainingRecord createTestRecord(Long id, Long userId, TrainingStatus status) {
        TrainingRecord record = new TrainingRecord();
        record.setTrainingRecordId(id);
        record.setUser(testUser);
        record.setOrgNumber(ORG_NUMBER);
        record.setTrainingType(TrainingType.FOOD_HYGIENE);
        record.setTitle("Food Safety Training");
        record.setStatus(status);
        return record;
    }

    private TrainingRecordRequest createTrainingRequest() {
        TrainingRecordRequest request = new TrainingRecordRequest();
        request.setUserId(USER_ID);
        request.setTrainingType(TrainingType.FOOD_HYGIENE);
        request.setTitle("Food Safety Training");
        request.setCompletedAt(LocalDateTime.now());
        request.setExpiresAt(LocalDateTime.now().plusMonths(12));
        request.setCertificateDocumentId(DOCUMENT_ID);
        request.setNotes("Completed with excellence");
        return request;
    }
}
