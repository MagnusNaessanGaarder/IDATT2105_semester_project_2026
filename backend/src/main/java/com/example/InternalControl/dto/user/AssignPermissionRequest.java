package com.example.InternalControl.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for assigning permission to role request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignPermissionRequest {

    @NotNull(message = "Permission ID is required")
    private Long permissionId;
}
