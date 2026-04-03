package com.example.InternalControl.service.deviation.mapper;

import com.example.InternalControl.dto.user.UserDto;
import com.example.InternalControl.dto.deviation.DeviationReportDto;
import com.example.InternalControl.model.auth.AppUser;
import com.example.InternalControl.model.deviation.DeviationReport;
import com.example.InternalControl.model.document.OrganizationDocument;
import com.example.InternalControl.service.user.mapper.UserMapper;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper for converting DeviationReport entities to DTOs.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Component
public class DeviationReportMapper {

  private final UserMapper userMapper;

  public DeviationReportMapper(UserMapper userMapper) {
    this.userMapper = userMapper;
  }

  public DeviationReportDto toDto(DeviationReport report) {
    if (report == null) {
      return null;
    }

    return DeviationReportDto.builder()
        .reportId(report.getReportId())
        .orgNumber(report.getOrgNumber())
        .reportType(report.getReportType())
        .severity(report.getSeverity())
        .title(report.getTitle())
        .description(report.getDescription())
        .reportDate(report.getReportDate())
        .occurredDate(report.getOccurredDate())
        .occurredTime(report.getOccurredTime())
        .locationText(report.getLocationText())
        .reportedBy(toUserDtoSafe(report.getReportedBy()))
        .discoveredBy(toUserDtoSafe(report.getDiscoveredBy()))
        .discoveredByName(report.getDiscoveredByName())
        .reportedTo(toUserDtoSafe(report.getReportedTo()))
        .reportedToName(report.getReportedToName())
        .assignedTo(toUserDtoSafe(report.getAssignedTo()))
        .immediateActionText(report.getImmediateActionText())
        .immediateActionSignedBy(toUserDtoSafe(report.getImmediateActionSignedBy()))
        .causeAnalysisText(report.getCauseAnalysisText())
        .causeAnalysisSignedBy(toUserDtoSafe(report.getCauseAnalysisSignedBy()))
        .correctiveActionText(report.getCorrectiveActionText())
        .correctiveActionSignedBy(toUserDtoSafe(report.getCorrectiveActionSignedBy()))
        .completionText(report.getCompletionText())
        .completionSignedBy(toUserDtoSafe(report.getCompletionSignedBy()))
        .status(report.getStatus())
        .createdAt(report.getCreatedAt())
        .updatedAt(report.getUpdatedAt())
        .closedAt(report.getClosedAt())
        .documentIds(toDocumentIds(report.getDocuments()))
        .build();
  }

  private UserDto toUserDtoSafe(AppUser user) {
    if (user == null) {
      return null;
    }
    return userMapper.toDto(user);
  }

  private Set<Long> toDocumentIds(Set<OrganizationDocument> documents) {
    if (documents == null) {
      return null;
    }
    return documents.stream()
        .map(OrganizationDocument::getDocumentId)
        .collect(Collectors.toSet());
  }
}
