package com.example.InternalControl.dto.checklist.request;

import com.example.InternalControl.model.enums.ItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating a checklist template item.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistTemplateItemCreateRequest {

    @NotNull(message = "Sort order is required")
    private Integer sortOrder;

    @NotBlank(message = "Label is required")
    @Size(max = 255, message = "Label must be less than 255 characters")
    private String label;

    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;

    @NotNull(message = "Item type is required")
    private ItemType itemType;

    private Boolean isRequired;

    private String expectedText;

    private BigDecimal expectedNumericMin;

    private BigDecimal expectedNumericMax;

    private String choiceOptionsJson;
}
