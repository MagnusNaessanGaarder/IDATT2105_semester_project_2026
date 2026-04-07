package com.example.InternalControl.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for permission response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponse {

    private Long permissionId;
    private String permissionKey;
    private String description;
}
