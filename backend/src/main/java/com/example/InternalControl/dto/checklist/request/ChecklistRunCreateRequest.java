package com.example.InternalControl.dto.checklist.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for creating a checklist run from template.

 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ChecklistRunCreateRequest", description = "DTO for ChecklistRunCreateRequest")
public class ChecklistRunCreateRequest {

    @NotNull(message = "Template ID is required")
    private Long templateId;

    @NotNull(message = "Run date is required")
    private LocalDate runDate;

    private Long assignedToUserId;

    private String notes;
}
