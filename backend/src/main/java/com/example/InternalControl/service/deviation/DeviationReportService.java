package com.example.InternalControl.service.deviation;

import com.example.InternalControl.dto.deviation.request.DeviationActionRequest;
import com.example.InternalControl.dto.deviation.request.DeviationReportCreateRequest;
import com.example.InternalControl.dto.deviation.request.DeviationReportUpdateRequest;
import com.example.InternalControl.model.deviation.DeviationReport;
import com.example.InternalControl.shared.enums.DeviationStatus;
import com.example.InternalControl.shared.enums.Severity;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for deviation/incident report management.
 *
 * @author TriTacLe
 * @since 1.0
 */
public interface DeviationReportService {

    DeviationReport createReport(DeviationReportCreateRequest request, Integer orgNumber, Long userId);

    DeviationReport getReport(Long reportId, Integer orgNumber);

    DeviationReport updateReport(Long reportId, DeviationReportUpdateRequest request, Integer orgNumber);

    void deleteReport(Long reportId, Integer orgNumber);

    List<DeviationReport> getReportsByOrg(Integer orgNumber);

    List<DeviationReport> getReportsByStatus(Integer orgNumber, DeviationStatus status);

    List<DeviationReport> getReportsBySeverity(Integer orgNumber, Severity severity);

    List<DeviationReport> getReportsAssignedTo(Long userId, Integer orgNumber);

    DeviationReport updateStatus(Long reportId, DeviationStatus newStatus, Integer orgNumber, Long userId);

    DeviationReport assignReport(Long reportId, Long assignedToUserId, Integer orgNumber, Long currentUserId);

    DeviationReport addImmediateAction(Long reportId, DeviationActionRequest request, Integer orgNumber, Long userId);

    DeviationReport addCauseAnalysis(Long reportId, DeviationActionRequest request, Integer orgNumber, Long userId);

    DeviationReport addCorrectiveAction(Long reportId, DeviationActionRequest request, Integer orgNumber, Long userId);

    DeviationReport completeReport(Long reportId, DeviationActionRequest request, Integer orgNumber, Long userId);

    DeviationReport closeReport(Long reportId, Integer orgNumber, Long userId);

    List<DeviationReport> searchReports(Integer orgNumber, DeviationStatus status, Severity severity,
                                        Long assignedToId, LocalDate fromDate, LocalDate toDate);

    Long getOpenReportCount(Integer orgNumber);
}
