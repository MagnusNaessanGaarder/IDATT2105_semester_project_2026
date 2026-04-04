package com.example.InternalControl.controller.export;

import com.example.InternalControl.dto.export.request.ExportRequest;
import com.example.InternalControl.dto.export.response.ExportResponse;
import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.UserOrganizationService;
import com.example.InternalControl.service.export.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Controller for export operations.
 *
 * @author TriTacLe
 * @since 1.0
 */
@RestController
@RequestMapping("/api/exports")
@RequiredArgsConstructor
@Tag(name = "Export", description = "Export operations for compliance reports")
@SecurityRequirement(name = "bearerAuth")
public class ExportController {

  private static final int BEARER_PREFIX_LENGTH = 7;

  private final ExportService exportService;
  private final JwtService jwtService;
  private final UserOrganizationService userOrgService;

  @PostMapping
  @Operation(summary = "Create export job", description = "Creates a new PDF or JSON export job")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Export job created"),
      @ApiResponse(responseCode = "400", description = "Invalid request"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
  public ResponseEntity<ExportResponse> createExport(
      @Valid @RequestBody ExportRequest request,
      @RequestParam Integer orgNumber,
      HttpServletRequest httpRequest) {

    Long userId = extractUserId(httpRequest);
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
  @Operation(summary = "Get export status", description = "Gets the status of an export job")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Export status retrieved"),
      @ApiResponse(responseCode = "404", description = "Export job not found"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
  public ResponseEntity<ExportResponse> getExportStatus(
      @Parameter(description = "Export job ID") @PathVariable Long exportJobId,
      @RequestParam Integer orgNumber,
      HttpServletRequest httpRequest) {

    Long userId = extractUserId(httpRequest);
    validateUserOrganizationAccess(userId, orgNumber);

    ExportResponse result = exportService.getExportStatus(exportJobId, orgNumber);
    return ResponseEntity.ok(result);
  }

  @GetMapping("/{exportJobId}/download")
  @Operation(summary = "Download export", description = "Gets a presigned URL to download the export file")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Download URL generated"),
      @ApiResponse(responseCode = "404", description = "Export job not found"),
      @ApiResponse(responseCode = "409", description = "Export not ready"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
  public ResponseEntity<String> downloadExport(
      @Parameter(description = "Export job ID") @PathVariable Long exportJobId,
      @RequestParam Integer orgNumber,
      HttpServletRequest httpRequest) {

    Long userId = extractUserId(httpRequest);
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
      @RequestParam Integer orgNumber,
      Pageable pageable,
      HttpServletRequest httpRequest) {

    Long userId = extractUserId(httpRequest);
    validateUserOrganizationAccess(userId, orgNumber);

    Page<ExportResponse> results = exportService.listExports(orgNumber, pageable);
    return ResponseEntity.ok(results);
  }

  private Long extractUserId(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new IllegalArgumentException("Missing or invalid Authorization header");
    }
    String token = authHeader.substring(BEARER_PREFIX_LENGTH);
    Long userId = jwtService.extractUserId(token);
    if (userId == null) {
      throw new IllegalArgumentException("User ID not found in token");
    }
    return userId;
  }

  private void validateUserOrganizationAccess(Long userId, Integer orgNumber) {
    if (!userOrgService.isUserInOrganization(userId, orgNumber)) {
      throw new EntityNotFoundException("Organization not found or user does not have access");
    }
  }
}
