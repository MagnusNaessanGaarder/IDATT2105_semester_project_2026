package com.example.InternalControl.dto.deviation.request;

import com.example.InternalControl.shared.enums.ReportType;
import com.example.InternalControl.shared.enums.Severity;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request DTO for updating a deviation report.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviationReportUpdateRequest {

    private ReportType reportType;

    private Severity severity;

    @Size(max = 255)
    private String title;

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
}
