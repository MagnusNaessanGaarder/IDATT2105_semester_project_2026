package com.example.InternalControl.controller.organization;

import com.example.InternalControl.dto.settings.OrganizationSettingsRequest;
import com.example.InternalControl.dto.settings.OrganizationSettingsResponse;
import com.example.InternalControl.service.settings.OrganizationSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for organization settings management.
 *
 * @author TriTacLe
 * @since 1.0
 */
@RestController
@RequestMapping("/api/admin/organizations/settings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Organization Settings Admin", description = "Administrative endpoints for managing organization settings")
@SecurityRequirement(name = "bearerAuth")
public class OrganizationSettingsAdminController {

    private final OrganizationSettingsService settingsService;

    /**
     * Get settings for an organization.
     *
     * @param orgNumber the organization number
     * @return the organization settings
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get organization settings", description = "Retrieve settings for a specific organization")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved settings"),
            @ApiResponse(responseCode = "404", description = "Organization not found")
    })
    public ResponseEntity<OrganizationSettingsResponse> getSettings(
            @Parameter(description = "The orgNumber parameter")
            @RequestParam Integer orgNumber) {
        log.info("Getting settings for organization: {}", orgNumber);
        return ResponseEntity.ok(settingsService.getSettings(orgNumber));
    }

    /**
     * Update settings for an organization.
     *
     * @param orgNumber the organization number
     * @param request the settings update request
     * @param userDetails the authenticated user details
     * @return the updated settings
     */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update organization settings", description = "Update settings for a specific organization (ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully updated settings"),
            @ApiResponse(responseCode = "404", description = "Organization not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<OrganizationSettingsResponse> updateSettings(
            @Parameter(description = "The orgNumber parameter")
            @RequestParam Integer orgNumber,
            @Valid @RequestBody OrganizationSettingsRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Updating settings for organization: {}", orgNumber);
        Long userId = extractUserId(userDetails);
        return ResponseEntity.ok(settingsService.updateSettings(orgNumber, request, userId));
    }

    private Long extractUserId(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        try {
            return Long.parseLong(userDetails.getUsername());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
