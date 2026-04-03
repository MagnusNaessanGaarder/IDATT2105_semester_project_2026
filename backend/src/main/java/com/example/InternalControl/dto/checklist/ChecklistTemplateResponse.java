package com.example.InternalControl.dto.checklist;

import com.example.InternalControl.dto.user.UserDto;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for checklist template responses.
 * Avoids exposing entity relationships.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Data
@Builder
public class ChecklistTemplateResponse {
  private Long templateId;
  private Integer orgNumber;
  private String moduleType;
  private String title;
  private String description;
  private String frequency;
  private Boolean isActive;
  private UserDto createdBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private List<ChecklistTemplateItemResponse> items;
}
