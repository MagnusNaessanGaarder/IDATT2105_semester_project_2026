package com.example.InternalControl.controller.organization;

import com.example.InternalControl.model.organization.Location;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.service.organization.LocationService;
import com.example.InternalControl.service.user.UserOrganizationService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/locations")
@Tag(name = "Locations", description = "Manage locations/areas within an organization")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class LocationController {

  private final LocationService locationService;
  private final UserOrganizationService userOrgService;

  @GetMapping
  @Operation(summary = "Get all locations for organization")
  public ResponseEntity<List<Location>> getLocations(
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(locationService.getLocationsByOrg(orgNumber));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get location by ID")
  public ResponseEntity<Location> getLocation(
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
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<Location> createLocation(
      @Valid @RequestBody Location location,
      @RequestParam Integer orgNumber,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long userId = userDetails.getUserId();
    validateUserOrganizationAccess(userId, orgNumber);
    Location created = locationService.createLocation(location, orgNumber);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update location")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<Location> updateLocation(
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
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<Void> deleteLocation(
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
  }
}
