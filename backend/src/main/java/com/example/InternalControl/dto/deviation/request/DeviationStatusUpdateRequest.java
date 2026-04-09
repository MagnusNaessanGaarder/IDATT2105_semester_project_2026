package com.example.InternalControl.dto.deviation.request;

import com.example.InternalControl.model.enums.DeviationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for updating deviation report status.

 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "DeviationStatusUpdateRequest", description = "DTO for DeviationStatusUpdateRequest")
public class DeviationStatusUpdateRequest {

    @NotNull
    private DeviationStatus status;
}
