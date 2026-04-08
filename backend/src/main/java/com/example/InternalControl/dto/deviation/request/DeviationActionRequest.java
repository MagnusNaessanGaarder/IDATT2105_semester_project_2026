package com.example.InternalControl.dto.deviation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for adding actions to a deviation report.

 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "DeviationActionRequest", description = "DTO for DeviationActionRequest")
public class DeviationActionRequest {

    @NotBlank
    @Size(max = 10000)
    private String actionText;
}
