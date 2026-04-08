package com.example.InternalControl.controller.settings;

import com.example.InternalControl.dto.settings.OrganizationSettingsRequest;
import com.example.InternalControl.dto.settings.OrganizationSettingsResponse;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.service.settings.OrganizationSettingsService;
import com.example.InternalControl.service.user.UserOrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{orgNumber}/settings")
@Tag(name = "Organization Settings", description = "Organization configuration management")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class OrganizationSettingsController {

    private final OrganizationSettingsService settingsService;
    private final UserOrganizationService userOrgService;

    @Operation(summary = "Get organization settings")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved settings"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Settings not found")
    })
    @GetMapping
    public ResponseEntity<OrganizationSettingsResponse> getSettings(
            @Parameter(description = "Identifier of the orgNumber")
            @PathVariable Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(settingsService.getSettings(orgNumber));
    }

    @Operation(summary = "Update organization settings (Admin/Manager only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Settings updated successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin/Manager only"),
        @ApiResponse(responseCode = "404", description = "Settings not found")
    })
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<OrganizationSettingsResponse> updateSettings(
            @Parameter(description = "Identifier of the orgNumber")
            @PathVariable Integer orgNumber,
            @Valid @RequestBody OrganizationSettingsRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(settingsService.updateSettings(orgNumber, request, userId));
    }

    private void validateUserOrganizationAccess(Long userId, Integer orgNumber) {
        if (!userOrgService.isUserInOrganization(userId, orgNumber)) {
            throw new EntityNotFoundException("Organization not found or user does not have access");
        }
    }
}
