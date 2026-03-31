package com.example.InternalControl.controller;

import com.example.InternalControl.model.Location;
import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.LocationService;
import com.example.InternalControl.service.UserOrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Location operations.
 *
 * @author TriTacLe
 * @since 1.0
 */
@RestController
@RequestMapping("/api/locations")
@Tag(name = "Locations", description = "Manage locations/areas within an organization")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class LocationController {

  private final LocationService locationService;
  private final JwtService jwtService;
  private final UserOrganizationService userOrgService;

  @Operation(summary = "Get all locations for organization")
  @GetMapping
  public ResponseEntity<List<Location>> getLocations(
      @RequestParam Integer orgNumber,
      HttpServletRequest request) {
    Long userId = extractUserId(request);
    validateUserOrganizationAccess(userId, orgNumber);
    return ResponseEntity.ok(locationService.getLocationsByOrg(orgNumber));
  }

  @Operation(summary = "Get location by ID")
  @GetMapping("/{id}")
  public ResponseEntity<Location> getLocation(@PathVariable Long id) {
    return ResponseEntity.ok(locationService.getLocationById(id));
  }

  @Operation(summary = "Create new location")
  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<Location> createLocation(
      @RequestBody Location location,
      @RequestParam Integer orgNumber,
      HttpServletRequest request) {
    Long userId = extractUserId(request);
    validateUserOrganizationAccess(userId, orgNumber);
    Location created = locationService.createLocation(location, orgNumber);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @Operation(summary = "Update location")
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<Location> updateLocation(
      @PathVariable Long id,
      @RequestBody Location location) {
    return ResponseEntity.ok(locationService.updateLocation(id, location));
  }

  @Operation(summary = "Delete location (soft delete)")
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
    locationService.deleteLocation(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Extract user ID from JWT token in the Authorization header.
   */
  private Long extractUserId(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new IllegalArgumentException("Missing or invalid Authorization header");
    }
    String token = authHeader.substring(7);
    Long userId = jwtService.extractUserId(token);
    if (userId == null) {
      throw new IllegalArgumentException("User ID not found in token");
    }
    return userId;
  }

  /**
   * Validate that user has access to the organization.
   */
  private void validateUserOrganizationAccess(Long userId, Integer orgNumber) {
    if (!userOrgService.isUserInOrganization(userId, orgNumber)) {
      throw new EntityNotFoundException("Organization not found or user does not have access");
    }
  }
}
