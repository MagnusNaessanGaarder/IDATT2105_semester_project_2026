package com.example.InternalControl.controller.analytics;

import com.example.InternalControl.dto.analytics.ComplianceScoreResponse;
import com.example.InternalControl.dto.analytics.DashboardSummaryResponse;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.service.analytics.DashboardService;
import com.example.InternalControl.service.user.UserOrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Dashboard and Analytics", description = "Dashboard statistics and compliance analytics")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AnalyticsController {

    private final DashboardService dashboardService;
    private final UserOrganizationService userOrgService;

    @Operation(summary = "Get dashboard summary")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved dashboard summary"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/dashboard/summary")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary(
            @Parameter(description = "The orgNumber parameter")
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(dashboardService.getDashboardSummary(orgNumber));
    }

    @Operation(summary = "Get compliance score")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved compliance score"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/compliance-score")
    public ResponseEntity<ComplianceScoreResponse> getComplianceScore(
            @Parameter(description = "The orgNumber parameter")
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);
        return ResponseEntity.ok(dashboardService.getComplianceScore(orgNumber));
    }

    private void validateUserOrganizationAccess(Long userId, Integer orgNumber) {
        if (!userOrgService.isUserInOrganization(userId, orgNumber)) {
            throw new EntityNotFoundException("Organization not found or user does not have access");
        }
    }
}
