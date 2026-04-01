package com.example.InternalControl.dto.checklist.response;

import com.example.InternalControl.model.enums.Frequency;
import com.example.InternalControl.model.enums.ModuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for checklist template.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistTemplateResponse {

    private Long templateId;

    private Integer orgNumber;

    private ModuleType moduleType;

    private String title;

    private String description;

    private Frequency frequency;

    private Boolean isActive;

    private Long createdByUserId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<ChecklistTemplateItemResponse> items;
}
