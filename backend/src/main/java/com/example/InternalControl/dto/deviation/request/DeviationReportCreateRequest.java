package com.example.InternalControl.dto.deviation.request;

import com.example.InternalControl.model.enums.ReportType;
import com.example.InternalControl.model.enums.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request DTO for creating a deviation report.

 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviationReportCreateRequest {

    @NotNull
    private ReportType reportType;

    @NotNull
    private Severity severity;

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    @Size(max = 10000)
    private String description;

    private Long locationId;

    @Size(max = 100)
    private String locationText;

    private LocalDate occurredDate;

    private LocalTime occurredTime;

    private Long discoveredByUserId;

    @Size(max = 255)
    private String discoveredByName;

    private Long reportedToUserId;

    @Size(max = 255)
    private String reportedToName;

    private Long assignedToUserId;

    private Long sourceTemperatureEntryId;
}
