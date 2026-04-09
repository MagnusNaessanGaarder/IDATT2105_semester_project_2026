package com.example.InternalControl.service.user;

import com.example.InternalControl.dto.user.PermissionResponse;
import com.example.InternalControl.model.user.Permission;
import com.example.InternalControl.model.user.RolePermission;
import com.example.InternalControl.repository.user.PermissionRepository;
import com.example.InternalControl.repository.user.RolePermissionRepository;
import com.example.InternalControl.repository.user.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages role-based permissions and assignments within the system.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleRepository roleRepository;

    @Override
    public List<PermissionResponse> getAllPermissions() {
        log.debug("Getting all permissions");
        return permissionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PermissionResponse getPermissionById(Long permissionId) {
        log.debug("Getting permission by ID: {}", permissionId);
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new EntityNotFoundException("Permission not found: " + permissionId));
        return mapToResponse(permission);
    }

    @Override
    public List<PermissionResponse> getPermissionsByRoleId(Long roleId) {
        log.debug("Getting permissions for role ID: {}", roleId);
        return rolePermissionRepository.findPermissionsByRoleId(roleId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignPermissionToRole(Long roleId, Long permissionId) {
        log.info("Assigning permission {} to role {}", permissionId, roleId);

        // Verify role exists
        if (!roleRepository.existsById(roleId)) {
            throw new EntityNotFoundException("Role not found: " + roleId);
        }

        // Verify permission exists
        permissionRepository.findById(permissionId)
                .orElseThrow(() -> new EntityNotFoundException("Permission not found: " + permissionId));

        // Check if already assigned
        if (rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
            log.debug("Permission {} already assigned to role {}", permissionId, roleId);
            return;
        }

        RolePermission rolePermission = RolePermission.builder()
                .roleId(roleId)
                .permissionId(permissionId)
                .build();

        rolePermissionRepository.save(rolePermission);
        log.info("Permission {} assigned to role {}", permissionId, roleId);
    }

    @Override
    @Transactional
    public void removePermissionFromRole(Long roleId, Long permissionId) {
        log.info("Removing permission {} from role {}", permissionId, roleId);

        if (!rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
            throw new EntityNotFoundException("Permission assignment not found for role " + roleId + " and permission " + permissionId);
        }

        rolePermissionRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);
        log.info("Permission {} removed from role {}", permissionId, roleId);
    }

    private PermissionResponse mapToResponse(Permission permission) {
        return PermissionResponse.builder()
                .permissionId(permission.getPermissionId())
                .permissionKey(permission.getPermissionKey())
                .description(permission.getDescription())
                .build();
    }
}
