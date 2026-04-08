package com.example.InternalControl.dto.checklist.response;

import com.example.InternalControl.model.enums.ItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for checklist template item.

 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistTemplateItemResponse {

    private Long itemId;

    private Long templateId;

    private Integer sortOrder;

    private String label;

    private String description;

    private ItemType itemType;

    private Boolean isRequired;

    private String expectedText;

    private BigDecimal expectedNumericMin;

    private BigDecimal expectedNumericMax;

    private String choiceOptionsJson;
}
