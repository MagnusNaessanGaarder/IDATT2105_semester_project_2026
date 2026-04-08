package com.example.InternalControl.controller.temperature;

import com.example.InternalControl.dto.temperature.request.TemperatureLogEntryRequest;
import com.example.InternalControl.dto.temperature.request.TemperatureLogPointRequest;
import com.example.InternalControl.dto.temperature.response.TemperatureLogEntryResponse;
import com.example.InternalControl.dto.temperature.response.TemperatureLogPointResponse;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.service.user.UserOrganizationService;
import com.example.InternalControl.service.temperature.TemperatureLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/temperature")
@RequiredArgsConstructor
@Tag(name = "Temperature Logging", description = "Temperature monitoring for food safety compliance")
@SecurityRequirement(name = "bearerAuth")
public class TemperatureLogController {

  private final TemperatureLogService temperatureLogService;
  private final UserOrganizationService userOrgService;

  // Log Points
  @PostMapping("/points")
  @Operation(summary = "Create temperature log point")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Log point created"),
      @ApiResponse(responseCode = "400", description = "Invalid request"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<TemperatureLogPointResponse> createLogPoint(
      @Valid @RequestBody TemperatureLogPointRequest request,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.status(HttpStatus.CREATED).body(temperatureLogService.createLogPoint(request, orgNumber));
  }

  @GetMapping("/points")
  @Operation(summary = "List all temperature log points")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved log points"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
  public ResponseEntity<List<TemperatureLogPointResponse>> listLogPoints(
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(temperatureLogService.listLogPoints(orgNumber));
  }

  @GetMapping("/points/active")
  @Operation(summary = "List active temperature log points")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved active log points"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
  public ResponseEntity<List<TemperatureLogPointResponse>> listActiveLogPoints(
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(temperatureLogService.listActiveLogPoints(orgNumber));
  }

  @GetMapping("/points/{pointId}")
  @Operation(summary = "Get specific log point")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved log point"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "Log point not found")
  })
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
  public ResponseEntity<TemperatureLogPointResponse> getLogPoint(
      @Parameter(description = "Log point ID") @PathVariable Long pointId,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(temperatureLogService.getLogPoint(pointId, orgNumber));
  }

  @PutMapping("/points/{pointId}")
  @Operation(summary = "Update log point")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Log point updated successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "Log point not found")
  })
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<TemperatureLogPointResponse> updateLogPoint(
      @Parameter(description = "Log point ID") @PathVariable Long pointId,
      @Valid @RequestBody TemperatureLogPointRequest request,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(temperatureLogService.updateLogPoint(pointId, request, orgNumber));
  }

  @DeleteMapping("/points/{pointId}")
  @Operation(summary = "Delete log point")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Log point deleted successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "Log point not found")
  })
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<Void> deleteLogPoint(
      @Parameter(description = "Log point ID") @PathVariable Long pointId,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    temperatureLogService.deleteLogPoint(pointId, orgNumber);
    return ResponseEntity.noContent().build();
  }

  // Temperature Entries
  @PostMapping("/entries")
  @Operation(summary = "Record temperature reading")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Temperature recorded"),
      @ApiResponse(responseCode = "400", description = "Invalid request")
  })
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
  public ResponseEntity<TemperatureLogEntryResponse> recordEntry(
      @Valid @RequestBody TemperatureLogEntryRequest request,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.status(HttpStatus.CREATED).body(temperatureLogService.recordEntry(request, orgNumber, userId));
  }

  @GetMapping("/entries")
  @Operation(summary = "List temperature entries")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved entries"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
  public ResponseEntity<List<TemperatureLogEntryResponse>> listEntries(
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(temperatureLogService.listEntries(orgNumber));
  }

  @GetMapping("/entries/paginated")
  @Operation(summary = "List temperature entries (paginated)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved entries"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
  public ResponseEntity<Page<TemperatureLogEntryResponse>> listEntriesPaginated(
      @RequestParam Integer orgNumber,
      Pageable pageable,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(temperatureLogService.listEntriesPaginated(orgNumber, pageable));
  }

  @GetMapping("/entries/by-point/{pointId}")
  @Operation(summary = "List entries for specific log point")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved entries"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "Log point not found")
  })
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
  public ResponseEntity<List<TemperatureLogEntryResponse>> listEntriesByPoint(
      @Parameter(description = "Log point ID") @PathVariable Long pointId,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(temperatureLogService.listEntriesByPoint(pointId, orgNumber));
  }

  @GetMapping("/entries/by-date")
  @Operation(summary = "List entries by date range")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved entries"),
      @ApiResponse(responseCode = "400", description = "Invalid date range"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
  public ResponseEntity<List<TemperatureLogEntryResponse>> listEntriesByDateRange(
      @RequestParam Integer orgNumber,
      @RequestParam LocalDateTime from,
      @RequestParam LocalDateTime to,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(temperatureLogService.listEntriesByDateRange(orgNumber, from, to));
  }

  @GetMapping("/entries/{entryId}")
  @Operation(summary = "Get specific temperature entry")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved entry"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "Entry not found")
  })
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN')")
  public ResponseEntity<TemperatureLogEntryResponse> getEntry(
      @Parameter(description = "Entry ID") @PathVariable Long entryId,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(temperatureLogService.getEntry(entryId, orgNumber));
  }

  @GetMapping("/alerts")
  @Operation(summary = "Get all temperature alerts (out-of-range)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved alerts"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<List<TemperatureLogEntryResponse>> listAlerts(
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(temperatureLogService.listAlerts(orgNumber));
  }

  private void validateUserOrganizationAccess(Long userId, Integer orgNumber) {
    if (!userOrgService.isUserInOrganization(userId, orgNumber)) {
      throw new jakarta.persistence.EntityNotFoundException("Organization not found or access denied");
    }
  }
}
