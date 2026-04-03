package com.example.InternalControl.dto.checklist.request;

import com.example.InternalControl.shared.enums.Frequency;
import com.example.InternalControl.shared.enums.ModuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a checklist template.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistTemplateCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    @NotNull(message = "Module type is required")
    private ModuleType moduleType;

    @NotNull(message = "Frequency is required")
    private Frequency frequency;

    private List<ChecklistTemplateItemCreateRequest> items;
}
