package com.example.InternalControl.dto.export.response;

import com.example.InternalControl.model.export.ExportFormat;
import com.example.InternalControl.model.export.ExportStatus;
import com.example.InternalControl.model.export.ExportType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for export job response.
 * Contains export job details, status, and download information.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Data
@Builder
@Schema(name = "ExportResponse", description = "Response containing export job details and status")
public class ExportResponse {

    @Schema(description = "Unique identifier for the export job")
    private Long exportJobId;

    @Schema(description = "Type of data exported")
    private ExportType exportType;

    @Schema(description = "Export file format")
    private ExportFormat format;

    @Schema(description = "Current status of the export job (PENDING, IN_PROGRESS, COMPLETED, FAILED)")
    private ExportStatus status;

    @Schema(description = "URL to download the exported file (null if not yet completed or failed)")
    private String downloadUrl;

    @Schema(description = "Generated file name of the export")
    private String fileName;

    @Schema(description = "Number of records included in the export")
    private Integer recordCount;

    @Schema(description = "Error message if export failed")
    private String failureReason;

    @Schema(description = "Timestamp when export was requested")
    private LocalDateTime requestedAt;

    @Schema(description = "Timestamp when export was completed")
    private LocalDateTime completedAt;

    @Schema(description = "Display name of user that requested job")
    private String requestedByDisplayName;

    @Schema(description = "Supplied parameters (e.g. date range")
    private String parametersJson;
}