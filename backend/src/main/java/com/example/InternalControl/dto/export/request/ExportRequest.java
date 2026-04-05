package com.example.InternalControl.dto.export.request;

import com.example.InternalControl.model.export.ExportFormat;
import com.example.InternalControl.shared.enums.ExportType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * Request DTO for creating an export job.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Data
@Builder
public class ExportRequest {

  @NotNull(message = "Export type is required")
  private ExportType exportType;

  @NotNull(message = "Format is required")
  private ExportFormat format;

  private LocalDate dateFrom;

  private LocalDate dateTo;

  private Long locationId;

  private String checklistType;
}
