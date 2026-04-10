package com.example.InternalControl.controller.user;

import com.example.InternalControl.dto.user.RoleResponse;
import com.example.InternalControl.model.user.Role;
import com.example.InternalControl.model.user.UserOrganizationRole;
import com.example.InternalControl.model.user.UserOrganizationRoleId;
import com.example.InternalControl.repository.user.RoleRepository;
import com.example.InternalControl.repository.user.UserOrganizationRoleRepository;
import com.example.InternalControl.security.UserSecurity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for role management operations. Provides endpoints for
 * managing roles and user role assignments.
 *
 * @author TriTacLe
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Role Management", description = "Manage roles and user role assignments")
@SecurityRequirement(name = "bearerAuth")
public class RoleController {

    private final RoleRepository roleRepository;
    private final UserOrganizationRoleRepository userOrgRoleRepository;
    private final ObjectProvider<UserSecurity> userSecurityProvider;

    /**
     * Get all roles in the system.
     *
     * @return list of roles
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all roles", description = "Retrieve all available roles in the system")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved roles"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        requireAnyRole("ROLE_ADMIN", "ROLE_MANAGER");
        log.info("Getting all roles");

        List<Role> roles = roleRepository.findAll();

        List<RoleResponse> response = roles.stream()
            .map(this::mapToRoleResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific role by ID.
     *
     * @param roleId the role ID
     * @return the role
     */
    @GetMapping("/{roleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get role by ID", description = "Retrieve a specific role by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved role"),
        @ApiResponse(responseCode = "404", description = "Role not found")
    })
    @Parameter(description = "Identifier of the roleId")
    public ResponseEntity<RoleResponse> getRole(@PathVariable Long roleId) {
        requireAnyRole("ROLE_ADMIN", "ROLE_MANAGER");
        log.info("Getting role: {}", roleId);

        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));

        return ResponseEntity.ok(mapToRoleResponse(role));
    }

    /**
     * Get roles assigned to a user in an organization.
     *
     * @param userId the user ID
     * @param orgNumber the organization number
     * @return list of roles
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or @userSecurity.isCurrentUser(#userId)")
    @Operation(summary = "Get user roles", description = "Retrieve roles assigned to a specific user in an organization")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user roles"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<RoleResponse>> getUserRoles(
            @Parameter(description = "Identifier of the userId")
            @PathVariable Long userId,
            @RequestParam Integer orgNumber) {
        requireAdminManagerOrCurrentUser(userId);
        log.info("Getting roles for user: {} in organization: {}", userId, orgNumber);

        List<UserOrganizationRole> userRoles = userOrgRoleRepository
            .findByUserIdAndOrgNumber(userId, orgNumber);

        List<RoleResponse> response = userRoles.stream()
            .map(uor -> mapToRoleResponse(uor.getRole()))
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Assign a role to a user in an organization.
     *
     * @param userId the user ID
     * @param orgNumber the organization number
     * @param roleId the role ID to assign
     * @return no content
     */
    @PostMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign role to user", description = "Assign a role to a user in an organization (ADMIN only)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Role successfully assigned"),
        @ApiResponse(responseCode = "200", description = "Role already assigned"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Void> assignRoleToUser(
            @Parameter(description = "Identifier of the userId")
            @PathVariable Long userId,
            @RequestParam Integer orgNumber,
            @RequestParam Long roleId) {
        requireAnyRole("ROLE_ADMIN");
        log.info("Assigning role: {} to user: {} in organization: {}", roleId, userId, orgNumber);

        if (userOrgRoleRepository.existsByUserIdAndOrgNumberAndRoleId(userId, orgNumber, roleId)) {
            log.info("Role already assigned to user");
            return ResponseEntity.ok().build();
        }

        UserOrganizationRoleId id = UserOrganizationRoleId.builder()
            .userId(userId)
            .orgNumber(orgNumber)
            .roleId(roleId)
            .build();

        UserOrganizationRole userRole = UserOrganizationRole.builder()
            .id(id)
            .assignedAt(LocalDateTime.now())
            .build();

        userOrgRoleRepository.save(userRole);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Remove a role from a user in an organization.
     *
     * @param userId the user ID
     * @param orgNumber the organization number
     * @param roleId the role ID to remove
     * @return no content
     */
    @DeleteMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove role from user", description = "Remove a role from a user in an organization (ADMIN only)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Role successfully removed"),
        @ApiResponse(responseCode = "404", description = "User role assignment not found"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Void> removeRoleFromUser(
            @Parameter(description = "Identifier of the userId")
            @PathVariable Long userId,
            @RequestParam Integer orgNumber,
            @RequestParam Long roleId) {
        requireAnyRole("ROLE_ADMIN");
        log.info("Removing role: {} from user: {} in organization: {}", roleId, userId, orgNumber);

        UserOrganizationRole userRole = userOrgRoleRepository
            .findByUserIdAndOrgNumberAndRoleId(userId, orgNumber, roleId)
            .orElseThrow(() -> new EntityNotFoundException("User role assignment not found"));

        userOrgRoleRepository.delete(userRole);

        return ResponseEntity.noContent().build();
    }

    private void requireAdminManagerOrCurrentUser(Long userId) {
        if (hasAnyRole("ROLE_ADMIN", "ROLE_MANAGER")) {
            return;
        }

        UserSecurity userSecurity = userSecurityProvider.getIfAvailable();
        if (userSecurity != null && userSecurity.isCurrentUser(userId)) {
            return;
        }

        throw new AccessDeniedException("Insufficient permissions");
    }

    private void requireAnyRole(String... roles) {
        if (hasAnyRole(roles)) {
            return;
        }
        throw new AccessDeniedException("Insufficient permissions");
    }

    private boolean hasAnyRole(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        for (String role : roles) {
            boolean hasRole = authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
            if (hasRole) {
                return true;
            }
        }

        return false;
    }

    private RoleResponse mapToRoleResponse(Role role) {
        if (role == null) {
            return null;
        }
        return RoleResponse.builder()
            .roleId(role.getRoleId())
            .roleName(role.getRoleName())
            .description(role.getDescription())
            .isSystemRole(role.getIsSystemRole())
            .build();
    }
}
