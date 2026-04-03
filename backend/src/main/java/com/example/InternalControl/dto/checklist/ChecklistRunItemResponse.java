package com.example.InternalControl.dto.checklist;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for checklist run item responses.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Data
@Builder
public class ChecklistRunItemResponse {
  private Long runItemId;
  private Long templateItemId;
  private String label;
  private String itemType;
  private Boolean booleanValue;
  private String textValue;
  private BigDecimal numericValue;
  private String selectedChoice;
  private Boolean isDeviation;
  private String commentText;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
