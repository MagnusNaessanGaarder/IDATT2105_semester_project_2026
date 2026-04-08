package com.example.InternalControl.dto.export.response;

import com.example.InternalControl.model.export.ExportFormat;
import com.example.InternalControl.model.export.ExportStatus;
import com.example.InternalControl.shared.enums.ExportType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for export job status and result.

 */
@Data
@Builder
public class ExportResponse {
  private Long exportJobId;
  private ExportType exportType;
  private ExportFormat format;
  private ExportStatus status;
  private String downloadUrl;
  private String fileName;
  private Integer recordCount;
  private String failureReason;
  private LocalDateTime requestedAt;
  private LocalDateTime completedAt;
}
