package com.example.InternalControl.dto.export.request;

import com.example.InternalControl.model.export.ExportFormat;
import com.example.InternalControl.model.export.ExportType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for creating an export job.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ExportRequest", description = "Request to create a new export job")
public class ExportRequest {

    @NotNull(message = "Export type is required")
    @Schema(description = "Type of export (CHECKLIST, DEVIATION, TRAINING, TEMPERATURE, etc.)", requiredMode = Schema.RequiredMode.REQUIRED)
    private ExportType exportType;

    @NotNull(message = "Format is required")
    @Schema(description = "Export file format (PDF or JSON)", requiredMode = Schema.RequiredMode.REQUIRED)
    private ExportFormat format;

    @Schema(description = "Start date for filtering records (optional)")
    private LocalDate dateFrom;

    @Schema(description = "End date for filtering records (optional)")
    private LocalDate dateTo;

    @Schema(description = "Filter by specific location ID (optional)")
    private Long locationId;

    @Schema(description = "Filter by checklist type (optional)")
    private String checklistType;

    public void setExportType(ExportType exportType) {
        this.exportType = exportType;
    }
}
