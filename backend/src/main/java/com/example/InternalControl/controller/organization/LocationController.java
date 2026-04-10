package com.example.InternalControl.controller.organization;

import com.example.InternalControl.model.organization.Location;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.service.organization.LocationService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@Tag(name = "Locations", description = "Manage locations/areas within an organization")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class LocationController {

  private final LocationService locationService;
  private final UserOrganizationService userOrgService;
  private final OrganizationModuleAccessService moduleAccessService;

  @GetMapping
  @Operation(summary = "Get all locations for organization")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved locations"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  public ResponseEntity<List<Location>> getLocations(
      @Parameter(description = "The orgNumber parameter")
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(locationService.getLocationsByOrg(orgNumber));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get location by ID")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Successfully retrieved location"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "Location not found")
  })
  public ResponseEntity<Location> getLocation(
      @Parameter(description = "Identifier of the id")
      @PathVariable Long id,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    Location location = locationService.getLocationById(id);
    if (!location.getOrgNumber().equals(orgNumber)) {
      throw new EntityNotFoundException("Location not found");
    }
    return ResponseEntity.ok(location);
  }

  @PostMapping
  @Operation(summary = "Create new location")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Location created successfully"),
      @ApiResponse(responseCode = "400", description = "Bad request"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden")
  })
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<Location> createLocation(
      @Valid @RequestBody Location location,
      @Parameter(description = "The orgNumber parameter")
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    Location created = locationService.createLocation(location, orgNumber);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update location")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Location updated successfully"),
      @ApiResponse(responseCode = "400", description = "Bad request"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "Location not found")
  })
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<Location> updateLocation(
      @Parameter(description = "Identifier of the id")
      @PathVariable Long id,
      @Valid @RequestBody Location location,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    Location existing = locationService.getLocationById(id);
    if (!existing.getOrgNumber().equals(orgNumber)) {
      throw new EntityNotFoundException("Location not found");
    }
    return ResponseEntity.ok(locationService.updateLocation(id, location));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete location (soft delete)")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Location deleted successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "Location not found")
  })
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<Void> deleteLocation(
      @Parameter(description = "Identifier of the id")
      @PathVariable Long id,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    Location existing = locationService.getLocationById(id);
    if (!existing.getOrgNumber().equals(orgNumber)) {
      throw new EntityNotFoundException("Location not found");
    }
    locationService.deleteLocation(id);
    return ResponseEntity.noContent().build();
  }

  private void validateUserOrganizationAccess(Long userId, Integer orgNumber) {
    if (!userOrgService.isUserInOrganization(userId, orgNumber)) {
      throw new EntityNotFoundException("Organization not found or user does not have access");
    }
    moduleAccessService.ensureFoodModuleEnabled(orgNumber);
  }
}
