package com.example.InternalControl.service.audit;

import com.example.InternalControl.model.audit.ActionType;
import com.example.InternalControl.model.audit.AuditLog;
import com.example.InternalControl.model.user.AppUser;
import com.example.InternalControl.repository.audit.AuditLogRepository;
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
 * Unit tests for AuditLogService.
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    private static final Integer ORG_NUMBER = 123;
    private static final Long USER_ID = 1L;
    private static final Long AUDIT_LOG_ID = 100L;

    private AppUser testUser;
    private AuditLog testAuditLog;

    @BeforeEach
    void setUp() {
        testUser = createTestUser(USER_ID, "test@example.com");
        testAuditLog = createTestAuditLog(AUDIT_LOG_ID, ActionType.CREATE);
    }

    // ==================== LOG ACTION TESTS ====================

    @Test
    void shouldLogAction() {
        // Given
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> {
            AuditLog log = inv.getArgument(0);
            log.setAuditLogId(AUDIT_LOG_ID);
            return log;
        });

        // When
        AuditLog result = auditLogService.logAction(
                ORG_NUMBER, USER_ID, ActionType.CREATE, "ChecklistTemplate",
                1L, "{}", "{\"name\":\"test\"}", "192.168.1.1", "Mozilla/5.0");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAuditLogId()).isEqualTo(AUDIT_LOG_ID);
        assertThat(result.getOrgNumber()).isEqualTo(ORG_NUMBER);
        assertThat(result.getActionType()).isEqualTo(ActionType.CREATE.name());
        assertThat(result.getEntityType()).isEqualTo("ChecklistTemplate");
        assertThat(result.getEntityId()).isEqualTo(1L);
        assertThat(result.getOldValuesJson()).isEqualTo("{}");
        assertThat(result.getNewValuesJson()).isEqualTo("{\"name\":\"test\"}");
        assertThat(result.getUserAgent()).isEqualTo("Mozilla/5.0");
    }

    @Test
    void shouldLogActionWithNullUserId() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        AuditLog result = auditLogService.logAction(
                ORG_NUMBER, null, ActionType.CREATE, "Entity", 1L,
                null, null, null, null);

        // Then
        assertThat(result.getActedByUser()).isNull();
    }

    @Test
    void shouldHandleNonExistentUser() {
        // Given
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        AuditLog result = auditLogService.logAction(
                ORG_NUMBER, USER_ID, ActionType.UPDATE, "Entity", 1L,
                null, null, null, null);

        // Then
        assertThat(result.getActedByUser()).isNull();
    }

    // ==================== GET AUDIT LOGS TESTS ====================

    @Test
    void shouldGetAuditLogsByOrganization() {
        // Given
        AuditLog log1 = createTestAuditLog(1L, ActionType.CREATE);
        AuditLog log2 = createTestAuditLog(2L, ActionType.UPDATE);
        when(auditLogRepository.findByOrgNumberOrderByCreatedAtDesc(ORG_NUMBER))
                .thenReturn(List.of(log2, log1)); // Descending order

        // When
        List<AuditLog> result = auditLogService.getAuditLogsByOrganization(ORG_NUMBER);

        // Then
        assertThat(result).hasSize(2);
        verify(auditLogRepository).findByOrgNumberOrderByCreatedAtDesc(ORG_NUMBER);
    }

    @Test
    void shouldGetAuditLogsByEntity() {
        // Given
        when(auditLogRepository.findByEntityTypeAndEntityId("ChecklistTemplate", 1L))
                .thenReturn(List.of(testAuditLog));

        // When
        List<AuditLog> result = auditLogService.getAuditLogsByEntity("ChecklistTemplate", 1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEntityType()).isEqualTo("ChecklistTemplate");
    }

    @Test
    void shouldGetAuditLogsByDateRange() {
        // Given
        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
        when(auditLogRepository.findByOrgNumberAndCreatedAtAfter(ORG_NUMBER, fromDate))
                .thenReturn(List.of(testAuditLog));

        // When
        List<AuditLog> result = auditLogService.getAuditLogsByDateRange(
                ORG_NUMBER, fromDate, LocalDateTime.now());

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void shouldGetAuditLogsByActionType() {
        // Given
        AuditLog createLog = createTestAuditLog(1L, ActionType.CREATE);
        AuditLog updateLog = createTestAuditLog(2L, ActionType.UPDATE);
        when(auditLogRepository.findByOrgNumberOrderByCreatedAtDesc(ORG_NUMBER))
                .thenReturn(List.of(updateLog, createLog));

        // When
        List<AuditLog> result = auditLogService.getAuditLogsByActionType(ORG_NUMBER, ActionType.CREATE);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActionType()).isEqualTo(ActionType.CREATE.name());
    }

    @Test
    void shouldGetAuditLogsByUser() {
        // Given
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(auditLogRepository.findByActedByUserOrderByCreatedAtDesc(testUser))
                .thenReturn(List.of(testAuditLog));

        // When
        List<AuditLog> result = auditLogService.getAuditLogsByUser(USER_ID);

        // Then
        assertThat(result).hasSize(1);
        verify(auditLogRepository).findByActedByUserOrderByCreatedAtDesc(testUser);
    }

    @Test
    void shouldThrowWhenGettingLogsForNonExistentUser() {
        // Given
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> auditLogService.getAuditLogsByUser(USER_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void shouldReturnEmptyListWhenNoLogsFound() {
        // Given
        when(auditLogRepository.findByOrgNumberOrderByCreatedAtDesc(ORG_NUMBER))
                .thenReturn(Collections.emptyList());

        // When
        List<AuditLog> result = auditLogService.getAuditLogsByOrganization(ORG_NUMBER);

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== HELPER METHODS ====================

    private AppUser createTestUser(Long userId, String email) {
        AppUser user = new AppUser();
        user.setUserId(userId);
        user.setEmail(email);
        user.setDisplayName("Test User");
        return user;
    }

    private AuditLog createTestAuditLog(Long id, ActionType actionType) {
        AuditLog log = new AuditLog();
        log.setAuditLogId(id);
        log.setOrgNumber(ORG_NUMBER);
        log.setActionType(actionType.name());
        log.setEntityType("ChecklistTemplate");
        log.setEntityId(1L);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }
}
