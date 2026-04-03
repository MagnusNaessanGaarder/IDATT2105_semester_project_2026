package com.example.InternalControl.controller.location;

import com.example.InternalControl.dto.location.LocationCreateRequest;
import com.example.InternalControl.dto.location.LocationResponse;
import com.example.InternalControl.service.location.mapper.LocationMapper;
import com.example.InternalControl.model.location.Location;
import com.example.InternalControl.security.AuthenticationFacade;
import com.example.InternalControl.service.location.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
  private final AuthenticationFacade authenticationFacade;
  private final LocationMapper locationMapper;

  @Operation(summary = "Get all locations for organization")
  @GetMapping
  public ResponseEntity<List<LocationResponse>> getLocations(
      @RequestParam Integer orgNumber,
      HttpServletRequest request) {
    authenticationFacade.extractAndValidateUser(request, orgNumber);
    List<Location> locations = locationService.getLocationsByOrg(orgNumber);
    return ResponseEntity.ok(locationMapper.toResponseList(locations));
  }

  @Operation(summary = "Get location by ID")
  @GetMapping("/{id}")
  public ResponseEntity<LocationResponse> getLocation(@PathVariable Long id) {
    Location location = locationService.getLocationById(id);
    return ResponseEntity.ok(locationMapper.toResponse(location));
  }

  @Operation(summary = "Create new location")
  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<LocationResponse> createLocation(
      @Valid @RequestBody LocationCreateRequest requestDto,
      @RequestParam Integer orgNumber,
      HttpServletRequest request) {
    authenticationFacade.extractAndValidateUser(request, orgNumber);
    Location location = locationMapper.toEntity(requestDto);
    Location created = locationService.createLocation(location, orgNumber);
    return ResponseEntity.status(HttpStatus.CREATED).body(locationMapper.toResponse(created));
  }

  @Operation(summary = "Update location")
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<LocationResponse> updateLocation(
      @PathVariable Long id,
      @Valid @RequestBody LocationCreateRequest requestDto) {
    Location location = locationMapper.toEntity(requestDto);
    Location updated = locationService.updateLocation(id, location);
    return ResponseEntity.ok(locationMapper.toResponse(updated));
  }

  @Operation(summary = "Delete location (soft delete)")
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
    locationService.deleteLocation(id);
    return ResponseEntity.noContent().build();
  }
}
