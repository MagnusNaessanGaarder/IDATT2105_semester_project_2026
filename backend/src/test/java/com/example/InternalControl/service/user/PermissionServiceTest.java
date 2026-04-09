package com.example.InternalControl.service.user;

import com.example.InternalControl.dto.user.PermissionResponse;
import com.example.InternalControl.model.user.Permission;
import com.example.InternalControl.model.user.RolePermission;
import com.example.InternalControl.repository.user.PermissionRepository;
import com.example.InternalControl.repository.user.RolePermissionRepository;
import com.example.InternalControl.repository.user.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PermissionService.
 */
@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    private static final Long PERMISSION_ID = 1L;
    private static final Long ROLE_ID = 1L;

    private Permission testPermission;

    @BeforeEach
    void setUp() {
        testPermission = createTestPermission(PERMISSION_ID, "USER_READ", "Can read users");
    }

    // ==================== GET ALL PERMISSIONS TESTS ====================

    @Test
    void shouldGetAllPermissions() {
        // Given
        Permission perm1 = createTestPermission(1L, "USER_READ", "Can read users");
        Permission perm2 = createTestPermission(2L, "USER_WRITE", "Can write users");
        when(permissionRepository.findAll()).thenReturn(List.of(perm1, perm2));

        // When
        List<PermissionResponse> result = permissionService.getAllPermissions();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPermissionKey()).isEqualTo("USER_READ");
        assertThat(result.get(1).getPermissionKey()).isEqualTo("USER_WRITE");
    }

    @Test
    void shouldReturnEmptyListWhenNoPermissions() {
        // Given
        when(permissionRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<PermissionResponse> result = permissionService.getAllPermissions();

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== GET PERMISSION BY ID TESTS ====================

    @Test
    void shouldGetPermissionById() {
        // Given
        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(testPermission));

        // When
        PermissionResponse result = permissionService.getPermissionById(PERMISSION_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPermissionId()).isEqualTo(PERMISSION_ID);
        assertThat(result.getPermissionKey()).isEqualTo("USER_READ");
        assertThat(result.getDescription()).isEqualTo("Can read users");
    }

    @Test
    void shouldThrowWhenPermissionNotFound() {
        // Given
        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> permissionService.getPermissionById(PERMISSION_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Permission not found");
    }

    // ==================== GET PERMISSIONS BY ROLE TESTS ====================

    @Test
    void shouldGetPermissionsByRoleId() {
        // Given
        when(rolePermissionRepository.findPermissionsByRoleId(ROLE_ID))
                .thenReturn(List.of(testPermission));

        // When
        List<PermissionResponse> result = permissionService.getPermissionsByRoleId(ROLE_ID);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPermissionKey()).isEqualTo("USER_READ");
    }

    @Test
    void shouldReturnEmptyListWhenRoleHasNoPermissions() {
        // Given
        when(rolePermissionRepository.findPermissionsByRoleId(ROLE_ID))
                .thenReturn(Collections.emptyList());

        // When
        List<PermissionResponse> result = permissionService.getPermissionsByRoleId(ROLE_ID);

        // Then
        assertThat(result).isEmpty();
    }

    // ==================== ASSIGN PERMISSION TO ROLE TESTS ====================

    @Test
    void shouldAssignPermissionToRole() {
        // Given
        when(roleRepository.existsById(ROLE_ID)).thenReturn(true);
        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(testPermission));
        when(rolePermissionRepository.existsByRoleIdAndPermissionId(ROLE_ID, PERMISSION_ID))
                .thenReturn(false);
        when(rolePermissionRepository.save(any(RolePermission.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        permissionService.assignPermissionToRole(ROLE_ID, PERMISSION_ID);

        // Then
        verify(rolePermissionRepository).save(any(RolePermission.class));
    }

    @Test
    void shouldNotAssignDuplicatePermission() {
        // Given
        when(roleRepository.existsById(ROLE_ID)).thenReturn(true);
        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.of(testPermission));
        when(rolePermissionRepository.existsByRoleIdAndPermissionId(ROLE_ID, PERMISSION_ID))
                .thenReturn(true);

        // When
        permissionService.assignPermissionToRole(ROLE_ID, PERMISSION_ID);

        // Then
        verify(rolePermissionRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenAssigningToNonExistentRole() {
        // Given
        when(roleRepository.existsById(ROLE_ID)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> permissionService.assignPermissionToRole(ROLE_ID, PERMISSION_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Role not found");

        verify(rolePermissionRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenAssigningNonExistentPermission() {
        // Given
        when(roleRepository.existsById(ROLE_ID)).thenReturn(true);
        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> permissionService.assignPermissionToRole(ROLE_ID, PERMISSION_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Permission not found");

        verify(rolePermissionRepository, never()).save(any());
    }

    // ==================== REMOVE PERMISSION FROM ROLE TESTS ====================

    @Test
    void shouldRemovePermissionFromRole() {
        // Given
        when(rolePermissionRepository.existsByRoleIdAndPermissionId(ROLE_ID, PERMISSION_ID))
                .thenReturn(true);

        // When
        permissionService.removePermissionFromRole(ROLE_ID, PERMISSION_ID);

        // Then
        verify(rolePermissionRepository).deleteByRoleIdAndPermissionId(ROLE_ID, PERMISSION_ID);
    }

    @Test
    void shouldThrowWhenRemovingNonExistentAssignment() {
        // Given
        when(rolePermissionRepository.existsByRoleIdAndPermissionId(ROLE_ID, PERMISSION_ID))
                .thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> permissionService.removePermissionFromRole(ROLE_ID, PERMISSION_ID))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Permission assignment not found");

        verify(rolePermissionRepository, never()).deleteByRoleIdAndPermissionId(any(), any());
    }

    // ==================== HELPER METHODS ====================

    private Permission createTestPermission(Long id, String key, String description) {
        Permission permission = new Permission();
        permission.setPermissionId(id);
        permission.setPermissionKey(key);
        permission.setDescription(description);
        return permission;
    }
}
