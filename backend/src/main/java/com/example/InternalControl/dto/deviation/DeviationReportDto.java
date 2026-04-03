package com.example.InternalControl.dto.deviation;

import com.example.InternalControl.dto.user.UserDto;
import com.example.InternalControl.shared.enums.DeviationStatus;
import com.example.InternalControl.shared.enums.ReportType;
import com.example.InternalControl.shared.enums.Severity;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

/**
 * Data Transfer Object for Deviation Report.
 * Used to avoid exposing internal entity relationships.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Data
@Builder
public class DeviationReportDto {

  private Long reportId;
  private Integer orgNumber;
  private ReportType reportType;
  private Severity severity;
  private String title;
  private String description;
  private LocalDate reportDate;
  private LocalDate occurredDate;
  private LocalTime occurredTime;
  private String locationText;
  private UserDto reportedBy;
  private UserDto discoveredBy;
  private String discoveredByName;
  private UserDto reportedTo;
  private String reportedToName;
  private UserDto assignedTo;
  private String immediateActionText;
  private UserDto immediateActionSignedBy;
  private String causeAnalysisText;
  private UserDto causeAnalysisSignedBy;
  private String correctiveActionText;
  private UserDto correctiveActionSignedBy;
  private String completionText;
  private UserDto completionSignedBy;
  private DeviationStatus status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime closedAt;
  private Set<Long> documentIds;
}
