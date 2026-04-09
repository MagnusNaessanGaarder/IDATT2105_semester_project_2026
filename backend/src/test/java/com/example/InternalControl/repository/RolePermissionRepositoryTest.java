package com.example.InternalControl.repository;

import com.example.InternalControl.AbstractIntegrationTest;
import com.example.InternalControl.model.user.Permission;
import com.example.InternalControl.model.user.Role;
import com.example.InternalControl.model.user.RolePermission;
import com.example.InternalControl.repository.user.PermissionRepository;
import com.example.InternalControl.repository.user.RolePermissionRepository;
import com.example.InternalControl.repository.user.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for RolePermissionRepository.
 * Tests role-permission relationship queries.
 */
@SpringBootTest
@DisplayName("RolePermissionRepository Integration Tests")
class RolePermissionRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    private Role adminRole;
    private Permission readPermission;
    private Permission writePermission;

    @BeforeEach
    void setUp() {
        adminRole = roleRepository.save(Role.builder()
                .roleName("ADMIN_TEST_" + System.nanoTime())
                .description("Administrator role")
                .isSystemRole(false)
                .build());

        readPermission = permissionRepository.findByPermissionKey("users:read")
                .orElseGet(() -> permissionRepository.save(Permission.builder()
                        .permissionKey("users:read")
                        .description("Read users permission")
                        .build()));

        writePermission = permissionRepository.findByPermissionKey("users:write")
                .orElseGet(() -> permissionRepository.save(Permission.builder()
                        .permissionKey("users:write")
                        .description("Write users permission")
                        .build()));
    }

    @Test
    @DisplayName("Should find permissions by role ID")
    void shouldFindPermissionsByRoleId() {
        // Given
        createRolePermission(adminRole.getRoleId(), readPermission.getPermissionId());
        createRolePermission(adminRole.getRoleId(), writePermission.getPermissionId());

        // When
        List<Permission> permissions = rolePermissionRepository.findPermissionsByRoleId(adminRole.getRoleId());

        // Then
        assertThat(permissions).hasSize(2);
        assertThat(permissions).extracting(Permission::getPermissionKey).contains("users:read", "users:write");
    }

    @Test
    @DisplayName("Should check if role has permission")
    void shouldCheckIfRoleHasPermission() {
        // Given
        createRolePermission(adminRole.getRoleId(), readPermission.getPermissionId());

        // When & Then
        assertThat(rolePermissionRepository.existsByRoleIdAndPermissionId(adminRole.getRoleId(), readPermission.getPermissionId())).isTrue();
        assertThat(rolePermissionRepository.existsByRoleIdAndPermissionId(adminRole.getRoleId(), writePermission.getPermissionId())).isFalse();
    }

    @Test
    @DisplayName("Should delete role permission")
    void shouldDeleteRolePermission() {
        // Given
        createRolePermission(adminRole.getRoleId(), readPermission.getPermissionId());
        createRolePermission(adminRole.getRoleId(), writePermission.getPermissionId());

        // Verify both exist
        assertThat(rolePermissionRepository.existsByRoleIdAndPermissionId(adminRole.getRoleId(), readPermission.getPermissionId())).isTrue();
        assertThat(rolePermissionRepository.existsByRoleIdAndPermissionId(adminRole.getRoleId(), writePermission.getPermissionId())).isTrue();

        // When
        rolePermissionRepository.deleteByRoleIdAndPermissionId(adminRole.getRoleId(), readPermission.getPermissionId());

        // Then
        assertThat(rolePermissionRepository.existsByRoleIdAndPermissionId(adminRole.getRoleId(), readPermission.getPermissionId())).isFalse();
        assertThat(rolePermissionRepository.existsByRoleIdAndPermissionId(adminRole.getRoleId(), writePermission.getPermissionId())).isTrue();
    }

    @Test
    @DisplayName("Should return empty list for role with no permissions")
    void shouldReturnEmptyListForRoleWithNoPermissions() {
        // Given - create a new role without permissions
        Role emptyRole = roleRepository.save(Role.builder()
                .roleName("EMPTY_ROLE")
                .description("Role without permissions")
                .isSystemRole(false)
                .build());

        // When
        List<Permission> permissions = rolePermissionRepository.findPermissionsByRoleId(emptyRole.getRoleId());

        // Then
        assertThat(permissions).isEmpty();
    }

    @Test
    @DisplayName("Should save role permission with composite key")
    void shouldSaveRolePermissionWithCompositeKey() {
        // Given
        RolePermission rolePermission = RolePermission.builder()
                .roleId(adminRole.getRoleId())
                .permissionId(readPermission.getPermissionId())
                .build();

        // When
        RolePermission saved = rolePermissionRepository.save(rolePermission);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getRoleId()).isEqualTo(adminRole.getRoleId());
        assertThat(saved.getPermissionId()).isEqualTo(readPermission.getPermissionId());

        // Verify it can be found
        List<Permission> permissions = rolePermissionRepository.findPermissionsByRoleId(adminRole.getRoleId());
        assertThat(permissions).hasSize(1);
        assertThat(permissions.get(0).getPermissionKey()).isEqualTo("users:read");
    }

    @Test
    @DisplayName("Should handle multiple roles with different permissions")
    void shouldHandleMultipleRolesWithDifferentPermissions() {
        // Given
        Role managerRole = roleRepository.save(Role.builder()
                .roleName("MANAGER_TEST_" + System.nanoTime())
                .description("Manager role")
                .isSystemRole(false)
                .build());

        Permission deletePermission = permissionRepository.findByPermissionKey("users:delete")
                .orElseGet(() -> permissionRepository.save(Permission.builder()
                        .permissionKey("users:delete")
                        .description("Delete users permission")
                        .build()));

        // Admin has read and write
        createRolePermission(adminRole.getRoleId(), readPermission.getPermissionId());
        createRolePermission(adminRole.getRoleId(), writePermission.getPermissionId());

        // Manager has read and delete
        createRolePermission(managerRole.getRoleId(), readPermission.getPermissionId());
        createRolePermission(managerRole.getRoleId(), deletePermission.getPermissionId());

        // When
        List<Permission> adminPermissions = rolePermissionRepository.findPermissionsByRoleId(adminRole.getRoleId());
        List<Permission> managerPermissions = rolePermissionRepository.findPermissionsByRoleId(managerRole.getRoleId());

        // Then
        assertThat(adminPermissions).hasSize(2);
        assertThat(adminPermissions).extracting(Permission::getPermissionKey).contains("users:read", "users:write");

        assertThat(managerPermissions).hasSize(2);
        assertThat(managerPermissions).extracting(Permission::getPermissionKey).contains("users:read", "users:delete");

        // Both have read permission
        assertThat(rolePermissionRepository.existsByRoleIdAndPermissionId(adminRole.getRoleId(), readPermission.getPermissionId())).isTrue();
        assertThat(rolePermissionRepository.existsByRoleIdAndPermissionId(managerRole.getRoleId(), readPermission.getPermissionId())).isTrue();

        // But only admin has write
        assertThat(rolePermissionRepository.existsByRoleIdAndPermissionId(adminRole.getRoleId(), writePermission.getPermissionId())).isTrue();
        assertThat(rolePermissionRepository.existsByRoleIdAndPermissionId(managerRole.getRoleId(), writePermission.getPermissionId())).isFalse();
    }

    private void createRolePermission(Long roleId, Long permissionId) {
        RolePermission rolePermission = RolePermission.builder()
                .roleId(roleId)
                .permissionId(permissionId)
                .build();
        rolePermissionRepository.save(rolePermission);
    }
}
