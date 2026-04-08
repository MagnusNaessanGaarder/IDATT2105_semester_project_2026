package com.example.InternalControl.model.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite key for RolePermission entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermissionId implements Serializable {

    private Long roleId;
    private Long permissionId;
}
