package com.example.InternalControl.controller.admin;

import com.example.InternalControl.model.audit.ActionType;
import com.example.InternalControl.model.audit.AuditLog;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.service.audit.AuditLogService;
import com.example.InternalControl.service.user.UserOrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for audit log management.
 * Provides administrative access to system audit trails for compliance and security monitoring.
 *
 * @author TriTacLe
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/admin/audit-log")
@Tag(name = "Admin - Audit Log", description = "Audit log management (Admin only)")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final UserOrganizationService userOrgService;

    @Operation(summary = "Get audit logs for organization")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved audit logs"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin only")
    })
    @GetMapping
    public ResponseEntity<List<AuditLog>> getAuditLogs(
            @Parameter(description = "The orgNumber parameter")
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(auditLogService.getAuditLogsByOrganization(orgNumber));
    }

    @Operation(summary = "Get audit logs for specific entity")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved entity audit logs"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin only")
    })
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<AuditLog>> getEntityAuditLogs(
            @Parameter(description = "Identifier of the entityType")
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(auditLogService.getAuditLogsByEntity(entityType, entityId));
    }

    @Operation(summary = "Get audit logs by date range")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved audit logs"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin only")
    })
    @GetMapping("/date-range")
    public ResponseEntity<List<AuditLog>> getAuditLogsByDateRange(
            @Parameter(description = "The orgNumber parameter")
            @RequestParam Integer orgNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(auditLogService.getAuditLogsByDateRange(orgNumber, fromDate, toDate));
    }

    @Operation(summary = "Get audit logs by action type")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved audit logs"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin only")
    })
    @GetMapping("/action/{actionType}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByActionType(
            @Parameter(description = "Identifier of the actionType")
            @PathVariable ActionType actionType,
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(auditLogService.getAuditLogsByActionType(orgNumber, actionType));
    }

    private void validateUserOrganizationAccess(Long userId, Integer orgNumber) {
        if (!userOrgService.isUserInOrganization(userId, orgNumber)) {
            throw new EntityNotFoundException("Organization not found or user does not have access");
        }
    }
}
