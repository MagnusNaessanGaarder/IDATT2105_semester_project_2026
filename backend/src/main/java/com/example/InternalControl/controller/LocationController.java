package com.example.InternalControl.controller;

import com.example.InternalControl.model.Location;
import com.example.InternalControl.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@Tag(name = "Locations", description = "Manage locations/areas within an organization")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class LocationController {

  private final LocationService locationService;

  @Operation(summary = "Get all locations for organization")
  @GetMapping
  public ResponseEntity<List<Location>> getLocations(
      @RequestParam Integer orgNumber) {
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
      @RequestParam Integer orgNumber) {
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
}
