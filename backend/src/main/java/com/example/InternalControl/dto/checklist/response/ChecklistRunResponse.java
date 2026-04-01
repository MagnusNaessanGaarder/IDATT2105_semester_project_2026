package com.example.InternalControl.dto.checklist.response;

import com.example.InternalControl.model.enums.RunStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for checklist run.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistRunResponse {

    private Long runId;

    private Long templateId;

    private String templateTitle;

    private Integer orgNumber;

    private Long locationId;

    private Long performedByUserId;

    private Long assignedToUserId;

    private LocalDate runDate;

    private LocalDateTime dueAt;

    private LocalDateTime completedAt;

    private RunStatus status;

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<ChecklistRunItemResponse> items;
}
