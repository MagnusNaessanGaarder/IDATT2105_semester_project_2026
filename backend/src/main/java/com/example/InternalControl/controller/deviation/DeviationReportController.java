package com.example.InternalControl.controller.deviation;

import com.example.InternalControl.dto.deviation.request.DeviationActionRequest;
import com.example.InternalControl.dto.deviation.request.DeviationReportCreateRequest;
import com.example.InternalControl.dto.deviation.request.DeviationReportUpdateRequest;
import com.example.InternalControl.dto.deviation.request.DeviationStatusUpdateRequest;
import com.example.InternalControl.model.deviation.DeviationReport;
import com.example.InternalControl.model.enums.DeviationStatus;
import com.example.InternalControl.model.enums.Severity;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.service.deviation.DeviationReportService;
import com.example.InternalControl.service.settings.OrganizationModuleAccessService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller for Deviation/Incident Report operations.
 */
@RestController
@RequestMapping("/api/v1/deviations")
@Tag(name = "Deviation Reports", description = "Manage deviation and incident reports")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class DeviationReportController {

  private final DeviationReportService deviationReportService;
  private final UserOrganizationService userOrgService;
  private final OrganizationModuleAccessService moduleAccessService;

  @Operation(summary = "Get all deviation reports for organization")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved deviation reports"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
  public ResponseEntity<List<DeviationReport>> getReports(
      @Parameter(description = "The orgNumber parameter") @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(deviationReportService.getReportsByOrg(orgNumber));
  }

  @Operation(summary = "Get deviation report by ID")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved deviation report"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "Deviation report not found")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
  public ResponseEntity<DeviationReport> getReport(
      @Parameter(description = "Identifier of the id") @PathVariable Long id,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(deviationReportService.getReport(id, orgNumber));
  }

  @Operation(summary = "Search deviation reports with filters")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved filtered deviation reports"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  @GetMapping("/search")
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
  public ResponseEntity<List<DeviationReport>> searchReports(
      @Parameter(description = "The orgNumber parameter") @RequestParam Integer orgNumber,
      @RequestParam(required = false) DeviationStatus status,
      @RequestParam(required = false) Severity severity,
      @RequestParam(required = false) Long assignedToId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(deviationReportService.searchReports(
        orgNumber, status, severity, assignedToId, fromDate, toDate));
  }

  @Operation(summary = "Get deviation reports by status")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved deviation reports by status"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  @GetMapping("/status/{status}")
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
  public ResponseEntity<List<DeviationReport>> getReportsByStatus(
      @Parameter(description = "Identifier of the status") @PathVariable DeviationStatus status,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(deviationReportService.getReportsByStatus(orgNumber, status));
  }

  @Operation(summary = "Get deviation reports by severity")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved deviation reports by severity"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  @GetMapping("/severity/{severity}")
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
  public ResponseEntity<List<DeviationReport>> getReportsBySeverity(
      @Parameter(description = "Identifier of the severity") @PathVariable Severity severity,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(deviationReportService.getReportsBySeverity(orgNumber, severity));
  }

  @Operation(summary = "Get deviation reports assigned to a user")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved assigned deviation reports"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  @GetMapping("/assigned/{assignedToId}")
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
  public ResponseEntity<List<DeviationReport>> getReportsAssignedTo(
      @Parameter(description = "Identifier of the assignedToId") @PathVariable Long assignedToId,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(deviationReportService.getReportsAssignedTo(assignedToId, orgNumber));
  }

  @Operation(summary = "Create new deviation report")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Deviation report created successfully"),
      @ApiResponse(responseCode = "400", description = "Bad request"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "Organization or referenced user not found")
  })
  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
  public ResponseEntity<DeviationReport> createReport(
      @Valid @RequestBody DeviationReportCreateRequest requestDto,
      @Parameter(description = "The orgNumber parameter") @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);

    DeviationReport created = deviationReportService.createReport(requestDto, orgNumber, userId);

    URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(created.getReportId())
        .toUri();

    return ResponseEntity.created(location).body(created);
  }

  @Operation(summary = "Update deviation report")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Deviation report updated successfully"),
      @ApiResponse(responseCode = "400", description = "Bad request"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "Deviation report not found")
  })
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
  public ResponseEntity<DeviationReport> updateReport(
      @Parameter(description = "Identifier of the id") @PathVariable Long id,
      @Valid @RequestBody DeviationReportUpdateRequest requestDto,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(deviationReportService.updateReport(id, requestDto, orgNumber));
  }

  @Operation(summary = "Delete deviation report (admin only)")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Deviation report deleted successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "Deviation report not found")
  })
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteReport(
      @Parameter(description = "Identifier of the id") @PathVariable Long id,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    deviationReportService.deleteReport(id, orgNumber);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Update deviation report status")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Status updated successfully"),
      @ApiResponse(responseCode = "400", description = "Bad request"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "Deviation report not found")
  })
  @PutMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<DeviationReport> updateStatus(
      @Parameter(description = "Identifier of the id") @PathVariable Long id,
      @Valid @RequestBody DeviationStatusUpdateRequest requestDto,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(deviationReportService.updateStatus(
        id, requestDto.getStatus(), orgNumber, userId));
  }

  @Operation(summary = "Assign deviation report to a user")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Deviation report assigned successfully"),
      @ApiResponse(responseCode = "400", description = "Bad request"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "Deviation report or user not found")
  })
  @PostMapping("/{id}/assign")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<DeviationReport> assignReport(
      @Parameter(description = "Identifier of the id") @PathVariable Long id,
      @RequestParam Long assignedToUserId,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(deviationReportService.assignReport(
        id, assignedToUserId, orgNumber, userId));
  }

  @Operation(summary = "Add immediate action to deviation report")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Immediate action added successfully"),
      @ApiResponse(responseCode = "400", description = "Bad request"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "Deviation report not found")
  })
  @PostMapping("/{id}/immediate-action")
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
  public ResponseEntity<DeviationReport> addImmediateAction(
      @Parameter(description = "Identifier of the id") @PathVariable Long id,
      @Valid @RequestBody DeviationActionRequest requestDto,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(deviationReportService.addImmediateAction(
        id, requestDto, orgNumber, userId));
  }

  @Operation(summary = "Add cause analysis to deviation report")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Cause analysis added successfully"),
      @ApiResponse(responseCode = "400", description = "Bad request"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "Deviation report not found")
  })
  @PostMapping("/{id}/cause-analysis")
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
  public ResponseEntity<DeviationReport> addCauseAnalysis(
      @Parameter(description = "Identifier of the id") @PathVariable Long id,
      @Valid @RequestBody DeviationActionRequest requestDto,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(deviationReportService.addCauseAnalysis(
        id, requestDto, orgNumber, userId));
  }

  @Operation(summary = "Add corrective action to deviation report")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Corrective action added successfully"),
      @ApiResponse(responseCode = "400", description = "Bad request"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "Deviation report not found")
  })
  @PostMapping("/{id}/corrective-action")
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
  public ResponseEntity<DeviationReport> addCorrectiveAction(
      @Parameter(description = "Identifier of the id") @PathVariable Long id,
      @Valid @RequestBody DeviationActionRequest requestDto,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(deviationReportService.addCorrectiveAction(
        id, requestDto, orgNumber, userId));
  }

  @Operation(summary = "Complete deviation report")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Completion added successfully"),
      @ApiResponse(responseCode = "400", description = "Bad request"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "Deviation report not found")
  })
  @PostMapping("/{id}/complete")
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
  public ResponseEntity<DeviationReport> completeReport(
      @Parameter(description = "Identifier of the id") @PathVariable Long id,
      @Valid @RequestBody DeviationActionRequest requestDto,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(deviationReportService.completeReport(
        id, requestDto, orgNumber, userId));
  }

  @Operation(summary = "Close deviation report")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Deviation report closed successfully"),
      @ApiResponse(responseCode = "400", description = "Bad request"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "Deviation report not found")
  })
  @PostMapping("/{id}/close")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<DeviationReport> closeReport(
      @Parameter(description = "Identifier of the id") @PathVariable Long id,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(deviationReportService.closeReport(id, orgNumber, userId));
  }

  @Operation(summary = "Get count of open deviation reports")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved count"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  @GetMapping("/count/open")
  @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
  public ResponseEntity<Long> getOpenReportCount(
      @Parameter(description = "The orgNumber parameter") @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(deviationReportService.getOpenReportCount(orgNumber));
  }

  private void validateUserOrganizationAccess(Long userId, Integer orgNumber) {
    if (!userOrgService.isUserInOrganization(userId, orgNumber)) {
      throw new EntityNotFoundException("Organization not found or user does not have access");
    }
    moduleAccessService.ensureFoodModuleEnabled(orgNumber);
  }
}
