package com.example.InternalControl.dto.checklist.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for checklist run item.

 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ChecklistRunItemResponse", description = "DTO for ChecklistRunItemResponse")
public class ChecklistRunItemResponse {

    private Long runItemId;

    private Long runId;

    private Long templateItemId;

    private String templateItemLabel;

    private Boolean booleanValue;

    private String textValue;

    private BigDecimal numericValue;

    private String selectedChoice;

    private Boolean isDeviation;

    private String commentText;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Boolean hasAnswer;
}
