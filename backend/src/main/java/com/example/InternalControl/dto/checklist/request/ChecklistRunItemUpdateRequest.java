package com.example.InternalControl.dto.checklist.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for updating a checklist run item (answering a question).
 *
 * @author TriTacLe
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistRunItemUpdateRequest {

    private Boolean booleanValue;

    private String textValue;

    private BigDecimal numericValue;

    private String selectedChoice;

    private Boolean isDeviation;

    private String commentText;
}
