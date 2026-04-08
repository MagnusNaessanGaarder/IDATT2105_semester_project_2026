package com.example.InternalControl.controller.user;

import com.example.InternalControl.dto.user.AssignPermissionRequest;
import com.example.InternalControl.dto.user.PermissionResponse;
import com.example.InternalControl.service.user.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for permission management.
 * Provides endpoints for managing permissions and role-permission assignments.
 */
@RestController
@RequestMapping("/api/admin/permissions")
@Tag(name = "Permissions", description = "Permission management API")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "Get all permissions")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved permissions"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<PermissionResponse>> getAllPermissions() {
        log.info("Getting all permissions");
        return ResponseEntity.ok(permissionService.getAllPermissions());
    }

    @Operation(summary = "Get permission by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved permission"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Permission not found")
    })
    @GetMapping("/{permissionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Parameter(description = "Identifier of the permissionId")
    public ResponseEntity<PermissionResponse> getPermission(@PathVariable Long permissionId) {
        log.info("Getting permission: {}", permissionId);
        return ResponseEntity.ok(permissionService.getPermissionById(permissionId));
    }

    @Operation(summary = "Get permissions for a role")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved role permissions"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/role/{roleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Parameter(description = "Identifier of the roleId")
    public ResponseEntity<List<PermissionResponse>> getPermissionsByRole(@PathVariable Long roleId) {
        log.info("Getting permissions for role: {}", roleId);
        return ResponseEntity.ok(permissionService.getPermissionsByRoleId(roleId));
    }

    @Operation(summary = "Assign permission to role")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Permission assigned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Permission or role not found")
    })
    @PostMapping("/role/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> assignPermissionToRole(
            @Parameter(description = "Identifier of the roleId")
            @PathVariable Long roleId,
            @Valid @RequestBody AssignPermissionRequest request) {
        log.info("Assigning permission {} to role {}", request.getPermissionId(), roleId);
        permissionService.assignPermissionToRole(roleId, request.getPermissionId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Remove permission from role")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Permission removed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Permission assignment not found")
    })
    @DeleteMapping("/role/{roleId}/{permissionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removePermissionFromRole(
            @Parameter(description = "Identifier of the roleId")
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {
        log.info("Removing permission {} from role {}", permissionId, roleId);
        permissionService.removePermissionFromRole(roleId, permissionId);
        return ResponseEntity.noContent().build();
    }
}
