package com.example.InternalControl.dto.checklist;

import com.example.InternalControl.dto.user.UserDto;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for checklist run responses.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Data
@Builder
public class ChecklistRunResponse {
  private Long runId;
  private Long templateId;
  private String templateTitle;
  private Integer orgNumber;
  private LocalDate runDate;
  private String status;
  private UserDto performedBy;
  private UserDto assignedTo;
  private LocalDateTime completedAt;
  private LocalDateTime dueAt;
  private String notes;
  private LocalDateTime createdAt;
  private List<ChecklistRunItemResponse> items;
}
