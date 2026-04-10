package com.example.InternalControl.dto.checklist.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for updating a checklist run item (answering a question).

 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ChecklistRunItemUpdateRequest", description = "DTO for ChecklistRunItemUpdateRequest")
public class ChecklistRunItemUpdateRequest {

    private Boolean booleanValue;

    private String textValue;

    private BigDecimal numericValue;

    private String selectedChoice;

    private Boolean isDeviation;

    private String commentText;
}
