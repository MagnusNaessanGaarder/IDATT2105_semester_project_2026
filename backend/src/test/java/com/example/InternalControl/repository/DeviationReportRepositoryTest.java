package com.example.InternalControl.repository;

import com.example.InternalControl.AbstractIntegrationTest;
import com.example.InternalControl.model.deviation.DeviationReport;
import com.example.InternalControl.model.enums.DeviationStatus;
import com.example.InternalControl.model.enums.ReportType;
import com.example.InternalControl.model.enums.Severity;
import com.example.InternalControl.model.user.AppUser;
import com.example.InternalControl.repository.deviation.DeviationReportRepository;
import com.example.InternalControl.repository.user.AppUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DeviationReportRepository.
 * Tests custom query methods for deviation report management.
 */
@SpringBootTest
@DisplayName("DeviationReportRepository Integration Tests")
class DeviationReportRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private DeviationReportRepository deviationReportRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    @DisplayName("Should find deviation reports by organization number")
    void shouldFindByOrgNumber() {
        // Given
        AppUser user = createAndSaveTestUser("deviation@test.com", "Test User");
        DeviationReport report = createTestReport(999001, user, Severity.MINOR, DeviationStatus.REPORTED);
        deviationReportRepository.save(report);

        // When
        List<DeviationReport> reports = deviationReportRepository.findByOrgNumber(999001);

        // Then
        assertThat(reports).hasSize(1);
        assertThat(reports.get(0).getOrgNumber()).isEqualTo(999001);
        assertThat(reports.get(0).getTitle()).isEqualTo("Test Deviation Report");
    }

    @Test
    @DisplayName("Should find deviation reports by organization number and status")
    void shouldFindByOrgNumberAndStatus() {
        // Given
        AppUser user = createAndSaveTestUser("status@test.com", "Test User");
        DeviationReport report1 = createTestReport(999002, user, Severity.MAJOR, DeviationStatus.REPORTED);
        DeviationReport report2 = createTestReport(999002, user, Severity.CRITICAL, DeviationStatus.CLOSED);
        deviationReportRepository.save(report1);
        deviationReportRepository.save(report2);

        // When
        List<DeviationReport> reportedReports = deviationReportRepository.findByOrgNumberAndStatus(999002, DeviationStatus.REPORTED);

        // Then
        assertThat(reportedReports).hasSize(1);
        assertThat(reportedReports.get(0).getStatus()).isEqualTo(DeviationStatus.REPORTED);
    }

    @Test
    @DisplayName("Should find deviation reports by organization number and severity")
    void shouldFindByOrgNumberAndSeverity() {
        // Given
        AppUser user = createAndSaveTestUser("severity@test.com", "Test User");
        DeviationReport report1 = createTestReport(999003, user, Severity.MINOR, DeviationStatus.REPORTED);
        DeviationReport report2 = createTestReport(999003, user, Severity.CRITICAL, DeviationStatus.REPORTED);
        deviationReportRepository.save(report1);
        deviationReportRepository.save(report2);

        // When
        List<DeviationReport> criticalReports = deviationReportRepository.findByOrgNumberAndSeverity(999003, Severity.CRITICAL);

        // Then
        assertThat(criticalReports).hasSize(1);
        assertThat(criticalReports.get(0).getSeverity()).isEqualTo(Severity.CRITICAL);
    }

    @Test
    @DisplayName("Should find deviation reports by assigned user and organization")
    void shouldFindByAssignedToUserIdAndOrgNumber() {
        // Given
        AppUser reporter = createAndSaveTestUser("reporter@test.com", "Reporter");
        AppUser assignee = createAndSaveTestUser("assignee@test.com", "Assignee");
        DeviationReport report = createTestReport(999004, reporter, Severity.MAJOR, DeviationStatus.REPORTED);
        report.setAssignedTo(assignee);
        deviationReportRepository.save(report);

        // When
        List<DeviationReport> assignedReports = deviationReportRepository.findByAssignedToUserIdAndOrgNumber(assignee.getUserId(), 999004);

        // Then
        assertThat(assignedReports).hasSize(1);
        assertThat(assignedReports.get(0).getAssignedTo().getUserId()).isEqualTo(assignee.getUserId());
    }

    @Test
    @DisplayName("Should find deviation report by ID and organization number")
    void shouldFindByReportIdAndOrgNumber() {
        // Given
        AppUser user = createAndSaveTestUser("id@test.com", "Test User");
        DeviationReport report = createTestReport(999005, user, Severity.MINOR, DeviationStatus.REPORTED);
        DeviationReport saved = deviationReportRepository.save(report);

        // When
        Optional<DeviationReport> found = deviationReportRepository.findByReportIdAndOrgNumber(saved.getReportId(), 999005);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getReportId()).isEqualTo(saved.getReportId());
    }

    @Test
    @DisplayName("Should check if deviation report exists by ID and organization number")
    void shouldCheckExistsByReportIdAndOrgNumber() {
        // Given
        AppUser user = createAndSaveTestUser("exists@test.com", "Test User");
        DeviationReport report = createTestReport(999006, user, Severity.MAJOR, DeviationStatus.REPORTED);
        DeviationReport saved = deviationReportRepository.save(report);

        // When & Then
        assertThat(deviationReportRepository.existsByReportIdAndOrgNumber(saved.getReportId(), 999006)).isTrue();
        assertThat(deviationReportRepository.existsByReportIdAndOrgNumber(999999L, 999006)).isFalse();
    }

    @Test
    @DisplayName("Should count open deviation reports by organization")
    void shouldCountOpenByOrgNumber() {
        // Given
        AppUser user = createAndSaveTestUser("count@test.com", "Test User");
        DeviationReport openReport1 = createTestReport(999007, user, Severity.MINOR, DeviationStatus.REPORTED);
        DeviationReport openReport2 = createTestReport(999007, user, Severity.MAJOR, DeviationStatus.UNDER_INVESTIGATION);
        DeviationReport closedReport = createTestReport(999007, user, Severity.CRITICAL, DeviationStatus.CLOSED);
        deviationReportRepository.save(openReport1);
        deviationReportRepository.save(openReport2);
        deviationReportRepository.save(closedReport);

        // When
        Long openCount = deviationReportRepository.countOpenByOrgNumber(999007);

        // Then
        assertThat(openCount).isEqualTo(2);
    }

    @Test
    @DisplayName("Should search deviation reports with multiple filters")
    void shouldSearchReports() {
        // Given
        AppUser user = createAndSaveTestUser("search@test.com", "Test User");
        DeviationReport report1 = createTestReport(999008, user, Severity.MINOR, DeviationStatus.REPORTED);
        report1.setReportDate(LocalDate.now().minusDays(1));
        DeviationReport report2 = createTestReport(999008, user, Severity.MAJOR, DeviationStatus.UNDER_INVESTIGATION);
        report2.setReportDate(LocalDate.now());
        deviationReportRepository.save(report1);
        deviationReportRepository.save(report2);

        // When - search by severity only
        List<DeviationReport> results = deviationReportRepository.searchReports(
                999008, null, Severity.MINOR, null, null, null);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSeverity()).isEqualTo(Severity.MINOR);
    }

    @Test
    @DisplayName("Should search deviation reports by date range")
    void shouldSearchReportsByDateRange() {
        // Given
        AppUser user = createAndSaveTestUser("datefilter@test.com", "Test User");
        DeviationReport oldReport = createTestReport(999009, user, Severity.MINOR, DeviationStatus.REPORTED);
        oldReport.setReportDate(LocalDate.now().minusDays(10));
        DeviationReport recentReport = createTestReport(999009, user, Severity.MAJOR, DeviationStatus.REPORTED);
        recentReport.setReportDate(LocalDate.now().minusDays(2));
        deviationReportRepository.save(oldReport);
        deviationReportRepository.save(recentReport);

        // When
        List<DeviationReport> results = deviationReportRepository.searchReports(
                999009, null, null, null, LocalDate.now().minusDays(5), null);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getReportDate()).isEqualTo(LocalDate.now().minusDays(2));
    }

    private AppUser createAndSaveTestUser(String email, String displayName) {
        AppUser user = AppUser.builder()
                .email(email)
                .displayName(displayName)
                .isActive(true)
                .build();
        return appUserRepository.save(user);
    }

    private DeviationReport createTestReport(Integer orgNumber, AppUser reportedBy, Severity severity, DeviationStatus status) {
        return DeviationReport.builder()
                .orgNumber(orgNumber)
                .reportType(ReportType.INCIDENT)
                .severity(severity)
                .status(status)
                .title("Test Deviation Report")
                .description("Test description for deviation report")
                .reportedBy(reportedBy)
                .reportDate(LocalDate.now())
                .build();
    }
}
