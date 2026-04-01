package com.example.InternalControl.dto.deviation.request;

import com.example.InternalControl.model.enums.DeviationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating deviation report status.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviationStatusUpdateRequest {

    @NotNull
    private DeviationStatus status;
}
