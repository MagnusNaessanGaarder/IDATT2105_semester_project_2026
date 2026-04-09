package com.example.InternalControl.controller.export;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.InternalControl.dto.export.request.ExportRequest;
import com.example.InternalControl.dto.export.response.ExportResponse;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.service.export.ExportService;
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

/**
 * REST controller for export operations.
 * Manages creation and retrieval of compliance report exports in various formats.
 *
 * @author TriTacLe
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/exports")
@RequiredArgsConstructor
@Tag(name = "Export", description = "Export operations for compliance reports")
@SecurityRequirement(name = "bearerAuth")
public class ExportController {

    private final ExportService exportService;
    private final UserOrganizationService userOrgService;

    @PostMapping
    @Operation(summary = "Create export job", description = "Creates a new PDF or JSON export job for compliance reports in the specified format")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Export job created"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ExportResponse> createExport(
            @Valid @RequestBody ExportRequest request,
            @Parameter(description = "Organization number identifying the tenant", required = true)
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);

        ExportResponse result = exportService.createExportJob(request, orgNumber, userId);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(result.getExportJobId())
                .toUri();

        return ResponseEntity.created(location).body(result);
    }

    @GetMapping("/{exportJobId}")
    @Operation(summary = "Get export status", description = "Retrieves the current status and details of an export job")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Export status retrieved"),
        @ApiResponse(responseCode = "404", description = "Export job not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ExportResponse> getExportStatus(
            @Parameter(description = "Unique identifier of the export job", required = true)
            @PathVariable Long exportJobId,
            @Parameter(description = "Organization number identifying the tenant", required = true)
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);

        ExportResponse result = exportService.getExportStatus(exportJobId, orgNumber);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{exportJobId}/download")
    @Operation(summary = "Download export", description = "Generates a presigned URL to download the completed export file")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Download URL generated"),
        @ApiResponse(responseCode = "404", description = "Export job not found"),
        @ApiResponse(responseCode = "409", description = "Export not ready"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<String> downloadExport(
            @Parameter(description = "Unique identifier of the export job to download", required = true)
            @PathVariable Long exportJobId,
            @Parameter(description = "Organization number identifying the tenant", required = true)
            @RequestParam Integer orgNumber,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);

        String downloadUrl = exportService.getDownloadUrl(exportJobId, orgNumber);
        return ResponseEntity.ok(downloadUrl);
    }

    @GetMapping
    @Operation(summary = "List exports", description = "Lists all export jobs for the organization")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List retrieved")
    })
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Page<ExportResponse>> listExports(
            @Parameter(description = "The orgNumber parameter")
            @RequestParam Integer orgNumber,
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();
        validateUserOrganizationAccess(userId, orgNumber);

        Page<ExportResponse> results = exportService.listExports(orgNumber, pageable);
        return ResponseEntity.ok(results);
    }

    private void validateUserOrganizationAccess(Long userId, Integer orgNumber) {
        if (!userOrgService.isUserInOrganization(userId, orgNumber)) {
            throw new EntityNotFoundException("Organization not found or user does not have access");
        }
    }
}
