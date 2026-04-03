package com.example.InternalControl.service.deviation;

import com.example.InternalControl.dto.deviation.request.DeviationActionRequest;
import com.example.InternalControl.dto.deviation.request.DeviationReportCreateRequest;
import com.example.InternalControl.dto.deviation.request.DeviationReportUpdateRequest;
import com.example.InternalControl.dto.deviation.request.DeviationStatusUpdateRequest;
import com.example.InternalControl.model.auth.AppUser;
import com.example.InternalControl.model.deviation.DeviationReport;
import com.example.InternalControl.model.location.Location;
import com.example.InternalControl.shared.enums.DeviationStatus;
import com.example.InternalControl.shared.enums.Severity;
import com.example.InternalControl.repository.user.AppUserRepository;
import com.example.InternalControl.repository.deviation.DeviationReportRepository;
import com.example.InternalControl.repository.location.LocationRepository;
import com.example.InternalControl.repository.organization.OrganizationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Implementation of DeviationReportService.
 * Handles deviation/incident report management with workflow state machine.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DeviationReportServiceImpl implements DeviationReportService {

    private final DeviationReportRepository deviationReportRepository;
    private final OrganizationRepository organizationRepository;
    private final LocationRepository locationRepository;
    private final AppUserRepository appUserRepository;

    @Override
    public DeviationReport createReport(DeviationReportCreateRequest request, Integer orgNumber, Long userId) {
        validateOrganizationExists(orgNumber);

        AppUser reportedBy = findUserById(userId);
        Location location = request.getLocationId() != null 
            ? findLocationById(request.getLocationId(), orgNumber) 
            : null;

        DeviationReport report = DeviationReport.builder()
                .orgNumber(orgNumber)
                .reportType(request.getReportType())
                .severity(request.getSeverity())
                .title(request.getTitle())
                .description(request.getDescription())
                .location(location)
                .locationText(request.getLocationText())
                .occurredDate(request.getOccurredDate())
                .occurredTime(request.getOccurredTime())
                .reportDate(LocalDate.now())
                .reportedBy(reportedBy)
                .discoveredBy(request.getDiscoveredByUserId() != null 
                    ? findUserById(request.getDiscoveredByUserId()) 
                    : null)
                .discoveredByName(request.getDiscoveredByName())
                .reportedTo(request.getReportedToUserId() != null 
                    ? findUserById(request.getReportedToUserId()) 
                    : null)
                .reportedToName(request.getReportedToName())
                .assignedTo(request.getAssignedToUserId() != null 
                    ? findUserById(request.getAssignedToUserId()) 
                    : null)
                .status(DeviationStatus.REPORTED)
                .build();

        return deviationReportRepository.save(report);
    }

    @Override
    @Transactional(readOnly = true)
    public DeviationReport getReport(Long reportId, Integer orgNumber) {
        return findReportByIdAndOrg(reportId, orgNumber);
    }

    @Override
    public DeviationReport updateReport(Long reportId, DeviationReportUpdateRequest request, Integer orgNumber) {
        DeviationReport report = findReportByIdAndOrg(reportId, orgNumber);

        if (!report.isEditable()) {
            throw new IllegalStateException("Cannot edit report in status: " + report.getStatus());
        }

        if (request.getReportType() != null) {
            report.setReportType(request.getReportType());
        }
        if (request.getSeverity() != null) {
            report.setSeverity(request.getSeverity());
        }
        if (request.getTitle() != null) {
            report.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            report.setDescription(request.getDescription());
        }
        if (request.getLocationId() != null) {
            report.setLocation(findLocationById(request.getLocationId(), orgNumber));
        }
        if (request.getLocationText() != null) {
            report.setLocationText(request.getLocationText());
        }
        if (request.getOccurredDate() != null) {
            report.setOccurredDate(request.getOccurredDate());
        }
        if (request.getOccurredTime() != null) {
            report.setOccurredTime(request.getOccurredTime());
        }
        if (request.getDiscoveredByUserId() != null) {
            report.setDiscoveredBy(findUserById(request.getDiscoveredByUserId()));
        }
        if (request.getDiscoveredByName() != null) {
            report.setDiscoveredByName(request.getDiscoveredByName());
        }
        if (request.getReportedToUserId() != null) {
            report.setReportedTo(findUserById(request.getReportedToUserId()));
        }
        if (request.getReportedToName() != null) {
            report.setReportedToName(request.getReportedToName());
        }
        if (request.getAssignedToUserId() != null) {
            report.setAssignedTo(findUserById(request.getAssignedToUserId()));
        }

        return deviationReportRepository.save(report);
    }

    @Override
    public void deleteReport(Long reportId, Integer orgNumber) {
        DeviationReport report = findReportByIdAndOrg(reportId, orgNumber);
        deviationReportRepository.delete(report);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviationReport> getReportsByOrg(Integer orgNumber) {
        return deviationReportRepository.findByOrgNumber(orgNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviationReport> getReportsByStatus(Integer orgNumber, DeviationStatus status) {
        return deviationReportRepository.findByOrgNumberAndStatus(orgNumber, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviationReport> getReportsBySeverity(Integer orgNumber, Severity severity) {
        return deviationReportRepository.findByOrgNumberAndSeverity(orgNumber, severity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviationReport> getReportsAssignedTo(Long userId, Integer orgNumber) {
        return deviationReportRepository.findByAssignedToUserIdAndOrgNumber(userId, orgNumber);
    }

    @Override
    public DeviationReport updateStatus(Long reportId, DeviationStatus newStatus, Integer orgNumber, Long userId) {
        DeviationReport report = findReportByIdAndOrg(reportId, orgNumber);

        validateStatusTransition(report.getStatus(), newStatus);

        report.setStatus(newStatus);

        if (newStatus == DeviationStatus.CLOSED) {
            report.close();
        }

        return deviationReportRepository.save(report);
    }

    @Override
    public DeviationReport assignReport(Long reportId, Long assignedToUserId, Integer orgNumber, Long currentUserId) {
        DeviationReport report = findReportByIdAndOrg(reportId, orgNumber);

        if (report.isClosed()) {
            throw new IllegalStateException("Cannot assign closed report");
        }

        AppUser assignedTo = findUserById(assignedToUserId);
        report.setAssignedTo(assignedTo);

        return deviationReportRepository.save(report);
    }

    @Override
    public DeviationReport addImmediateAction(Long reportId, DeviationActionRequest request, 
                                              Integer orgNumber, Long userId) {
        DeviationReport report = findReportByIdAndOrg(reportId, orgNumber);

        if (report.getStatus() != DeviationStatus.UNDER_INVESTIGATION) {
            throw new IllegalStateException("Immediate action can only be added during investigation");
        }

        AppUser signedBy = findUserById(userId);
        report.setImmediateActionText(request.getActionText());
        report.setImmediateActionSignedBy(signedBy);

        checkAndAdvanceStatus(report);

        return deviationReportRepository.save(report);
    }

    @Override
    public DeviationReport addCauseAnalysis(Long reportId, DeviationActionRequest request, 
                                            Integer orgNumber, Long userId) {
        DeviationReport report = findReportByIdAndOrg(reportId, orgNumber);

        if (report.getStatus() != DeviationStatus.UNDER_INVESTIGATION) {
            throw new IllegalStateException("Cause analysis can only be added during investigation");
        }

        AppUser signedBy = findUserById(userId);
        report.setCauseAnalysisText(request.getActionText());
        report.setCauseAnalysisSignedBy(signedBy);

        checkAndAdvanceStatus(report);

        return deviationReportRepository.save(report);
    }

    @Override
    public DeviationReport addCorrectiveAction(Long reportId, DeviationActionRequest request, 
                                               Integer orgNumber, Long userId) {
        DeviationReport report = findReportByIdAndOrg(reportId, orgNumber);

        if (report.getStatus() != DeviationStatus.UNDER_INVESTIGATION && 
            report.getStatus() != DeviationStatus.CORRECTIVE_ACTION_PLANNED) {
            throw new IllegalStateException("Corrective action can only be added during investigation or planning");
        }

        AppUser signedBy = findUserById(userId);
        report.setCorrectiveActionText(request.getActionText());
        report.setCorrectiveActionSignedBy(signedBy);
        report.setStatus(DeviationStatus.CORRECTIVE_ACTION_PLANNED);

        return deviationReportRepository.save(report);
    }

    @Override
    public DeviationReport completeReport(Long reportId, DeviationActionRequest request, 
                                          Integer orgNumber, Long userId) {
        DeviationReport report = findReportByIdAndOrg(reportId, orgNumber);

        if (report.getStatus() != DeviationStatus.CORRECTIVE_ACTION_PLANNED) {
            throw new IllegalStateException("Completion can only be added after corrective action is planned");
        }

        AppUser signedBy = findUserById(userId);
        report.setCompletionText(request.getActionText());
        report.setCompletionSignedBy(signedBy);
        report.setStatus(DeviationStatus.CORRECTIVE_ACTION_COMPLETED);

        return deviationReportRepository.save(report);
    }

    @Override
    public DeviationReport closeReport(Long reportId, Integer orgNumber, Long userId) {
        DeviationReport report = findReportByIdAndOrg(reportId, orgNumber);

        if (report.getStatus() != DeviationStatus.CORRECTIVE_ACTION_COMPLETED) {
            throw new IllegalStateException("Report can only be closed after corrective action is completed");
        }

        report.close();
        return deviationReportRepository.save(report);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviationReport> searchReports(Integer orgNumber, DeviationStatus status,
                                               Severity severity, Long assignedToId,
                                               LocalDate fromDate, LocalDate toDate) {
        return deviationReportRepository.searchReports(orgNumber, status, severity, assignedToId, fromDate, toDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getOpenReportCount(Integer orgNumber) {
        return deviationReportRepository.countOpenByOrgNumber(orgNumber);
    }

    // Helper methods

    private DeviationReport findReportByIdAndOrg(Long reportId, Integer orgNumber) {
        return deviationReportRepository.findByReportIdAndOrgNumber(reportId, orgNumber)
                .orElseThrow(() -> new EntityNotFoundException("Deviation report not found: " + reportId));
    }

    private void validateOrganizationExists(Integer orgNumber) {
        if (!organizationRepository.existsById(orgNumber)) {
            throw new EntityNotFoundException("Organization not found: " + orgNumber);
        }
    }

    private AppUser findUserById(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }

    private Location findLocationById(Long locationId, Integer orgNumber) {
        return locationRepository.findByLocationIdAndOrgNumber(locationId, orgNumber)
                .orElseThrow(() -> new EntityNotFoundException("Location not found: " + locationId));
    }

    private void validateStatusTransition(DeviationStatus currentStatus, DeviationStatus newStatus) {
        boolean valid = switch (currentStatus) {
            case DRAFT -> newStatus == DeviationStatus.REPORTED;
            case REPORTED -> newStatus == DeviationStatus.UNDER_INVESTIGATION;
            case UNDER_INVESTIGATION -> newStatus == DeviationStatus.CORRECTIVE_ACTION_PLANNED 
                || newStatus == DeviationStatus.REPORTED;
            case CORRECTIVE_ACTION_PLANNED -> newStatus == DeviationStatus.CORRECTIVE_ACTION_COMPLETED 
                || newStatus == DeviationStatus.UNDER_INVESTIGATION;
            case CORRECTIVE_ACTION_COMPLETED -> newStatus == DeviationStatus.CLOSED 
                || newStatus == DeviationStatus.CORRECTIVE_ACTION_PLANNED;
            case CLOSED -> false; // Cannot transition from closed
        };

        if (!valid) {
            throw new IllegalStateException(
                String.format("Invalid status transition from %s to %s", currentStatus, newStatus));
        }
    }

    private void checkAndAdvanceStatus(DeviationReport report) {
        if (report.getImmediateActionText() != null && report.getCauseAnalysisText() != null) {
            report.setStatus(DeviationStatus.CORRECTIVE_ACTION_PLANNED);
        }
    }
}
