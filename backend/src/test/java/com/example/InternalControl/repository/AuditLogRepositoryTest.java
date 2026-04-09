package com.example.InternalControl.repository;

import com.example.InternalControl.AbstractIntegrationTest;
import com.example.InternalControl.model.audit.AuditLog;
import com.example.InternalControl.model.user.AppUser;
import com.example.InternalControl.repository.audit.AuditLogRepository;
import com.example.InternalControl.repository.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for AuditLogRepository.
 * Tests audit log queries for compliance tracking.
 *
 * @author TriTacLe
 * @since 1.0
 */
@SpringBootTest
@DisplayName("AuditLogRepository Integration Tests")
class AuditLogRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    private static final Integer ORG_NUMBER = 937219997;
    private AppUser testUser;

    @BeforeEach
    void setUp() {
        String suffix = String.valueOf(System.nanoTime());
        testUser = appUserRepository.save(AppUser.builder()
                .email("test+" + suffix + "@example.com")
                .displayName("Test User")
                .isActive(true)
                .build());
    }

    @Test
    @DisplayName("Should find audit logs by org number ordered by created date")
    void shouldFindAuditLogsByOrgNumberOrderedByCreatedDate() {
        // Given
        AuditLog log1 = createAuditLog("CREATE", "DeviationReport", 1L, "Old log");
        AuditLog log2 = createAuditLog("UPDATE", "DeviationReport", 1L, "New log");
        auditLogRepository.save(log1);
        auditLogRepository.save(log2);

        // When
        List<AuditLog> logs = auditLogRepository.findByOrgNumberOrderByCreatedAtDesc(ORG_NUMBER);

        // Then
        assertThat(logs).extracting(AuditLog::getActionType).contains("UPDATE", "CREATE");
    }

    @Test
    @DisplayName("Should find audit logs by org number since specific time")
    void shouldFindAuditLogsByOrgNumberSinceSpecificTime() throws InterruptedException {
        // Given - Save logs and capture the time
        AuditLog log1 = auditLogRepository.save(createAuditLog("CREATE", "DeviationReport", 1L, "First log"));
        Thread.sleep(50);
        AuditLog log2 = auditLogRepository.save(createAuditLog("UPDATE", "DeviationReport", 2L, "Second log"));
        LocalDateTime afterFirstLog = log1.getCreatedAt();

        // When
        List<AuditLog> recentLogs = auditLogRepository.findByOrgNumberAndCreatedAtAfter(ORG_NUMBER, afterFirstLog);

        // Then - should include entries after the first one
        assertThat(recentLogs).extracting(AuditLog::getAuditLogId).contains(log2.getAuditLogId());
    }

    @Test
    @DisplayName("Should find audit logs by entity type and ID")
    void shouldFindAuditLogsByEntityTypeAndId() {
        // Given
        AuditLog deviationLog1 = createAuditLog("CREATE", "DeviationReport", 100L, "Create deviation");
        AuditLog deviationLog2 = createAuditLog("UPDATE", "DeviationReport", 100L, "Update deviation");
        AuditLog checklistLog = createAuditLog("CREATE", "Checklist", 200L, "Create checklist");

        auditLogRepository.save(deviationLog1);
        auditLogRepository.save(deviationLog2);
        auditLogRepository.save(checklistLog);

        // When
        List<AuditLog> deviationLogs = auditLogRepository.findByEntityTypeAndEntityId("DeviationReport", 100L);

        // Then
        assertThat(deviationLogs).hasSize(2);
        assertThat(deviationLogs).extracting(AuditLog::getActionType).contains("CREATE", "UPDATE");
    }

    @Test
    @DisplayName("Should find audit logs by acting user")
    void shouldFindAuditLogsByActingUser() {
        // Given
        AppUser otherUser = appUserRepository.save(AppUser.builder()
                .email("other+" + System.nanoTime() + "@example.com")
                .displayName("Other User")
                .isActive(true)
                .build());

        AuditLog testUserLog = createAuditLog("CREATE", "DeviationReport", 1L, "Test user action");
        AuditLog otherUserLog = createAuditLog("UPDATE", "DeviationReport", 2L, "Other user action");
        otherUserLog.setActedByUser(otherUser);

        auditLogRepository.save(testUserLog);
        auditLogRepository.save(otherUserLog);

        // When
        List<AuditLog> testUserLogs = auditLogRepository.findByActedByUserOrderByCreatedAtDesc(testUser);

        // Then
        assertThat(testUserLogs).hasSize(1);
        assertThat(testUserLogs.get(0).getActionType()).isEqualTo("CREATE");
    }

    @Test
    @DisplayName("Should return empty list for non-existent org number")
    void shouldReturnEmptyListForNonExistentOrgNumber() {
        // When
        List<AuditLog> logs = auditLogRepository.findByOrgNumberOrderByCreatedAtDesc(999999999);

        // Then
        assertThat(logs).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for non-existent entity")
    void shouldReturnEmptyListForNonExistentEntity() {
        // Given
        auditLogRepository.save(createAuditLog("CREATE", "DeviationReport", 1L, "Test"));

        // When
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityId("NonExistent", 99999L);

        // Then
        assertThat(logs).isEmpty();
    }

    @Test
    @DisplayName("Should store JSON values correctly")
    void shouldStoreJsonValuesCorrectly() {
        // Given
        String oldValues = "{\"status\":\"OPEN\"}";
        String newValues = "{\"status\":\"CLOSED\"}";

        AuditLog log = AuditLog.builder()
                .orgNumber(ORG_NUMBER)
                .actionType("UPDATE")
                .entityType("DeviationReport")
                .entityId(1L)
                .oldValuesJson(oldValues)
                .newValuesJson(newValues)
                .actedByUser(testUser)
                .build();

        AuditLog saved = auditLogRepository.save(log);

        // When
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityId("DeviationReport", 1L);

        // Then
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getOldValuesJson()).isEqualTo(oldValues);
        assertThat(logs.get(0).getNewValuesJson()).isEqualTo(newValues);
    }

    @Test
    @DisplayName("Should store user agent correctly")
    void shouldStoreUserAgentCorrectly() {
        // Given
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

        AuditLog log = AuditLog.builder()
                .orgNumber(ORG_NUMBER)
                .actionType("VIEW")
                .entityType("DeviationReport")
                .entityId(1L)
                .actedByUser(testUser)
                .userAgent(userAgent)
                .build();

        auditLogRepository.save(log);

        // When
        List<AuditLog> logs = auditLogRepository.findByOrgNumberOrderByCreatedAtDesc(ORG_NUMBER);

        // Then
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getUserAgent()).isEqualTo(userAgent);
    }

    private AuditLog createAuditLog(String actionType, String entityType, Long entityId, String description) {
        return AuditLog.builder()
                .orgNumber(ORG_NUMBER)
                .actionType(actionType)
                .entityType(entityType)
                .entityId(entityId)
                .oldValuesJson(null)
                .newValuesJson("{\"description\":\"" + description + "\"}")
                .actedByUser(testUser)
                .build();
    }
}
