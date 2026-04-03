package com.example.InternalControl.dto.checklist;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for checklist template item responses.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Data
@Builder
public class ChecklistTemplateItemResponse {
  private Long itemId;
  private Integer sortOrder;
  private String label;
  private String description;
  private String itemType;
  private Boolean isRequired;
  private String expectedText;
  private BigDecimal expectedNumericMin;
  private BigDecimal expectedNumericMax;
  private List<String> choiceOptions;
}
