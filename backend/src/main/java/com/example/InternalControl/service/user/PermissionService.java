package com.example.InternalControl.service.user;

import com.example.InternalControl.dto.user.PermissionResponse;

import java.util.List;

/**
 * Service interface for permission management.
 */
public interface PermissionService {

    /**
     * Get all permissions.
     *
     * @return list of all permissions
     */
    List<PermissionResponse> getAllPermissions();

    /**
     * Get permission by ID.
     *
     * @param permissionId the permission ID
     * @return the permission
     */
    PermissionResponse getPermissionById(Long permissionId);

    /**
     * Get permissions for a role.
     *
     * @param roleId the role ID
     * @return list of permissions
     */
    List<PermissionResponse> getPermissionsByRoleId(Long roleId);

    /**
     * Assign permission to role.
     *
     * @param roleId       the role ID
     * @param permissionId the permission ID
     */
    void assignPermissionToRole(Long roleId, Long permissionId);

    /**
     * Remove permission from role.
     *
     * @param roleId       the role ID
     * @param permissionId the permission ID
     */
    void removePermissionFromRole(Long roleId, Long permissionId);
}
