package com.example.InternalControl.service.organization;

import com.example.InternalControl.model.organization.Location;
import com.example.InternalControl.repository.organization.LocationRepository;
import com.example.InternalControl.repository.organization.OrganizationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implements location management with CRUD operations and soft deletion.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Service
@Transactional
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

  private final LocationRepository locationRepository;
  private final OrganizationRepository orgRepository;

  @Override
  public Location createLocation(Location location, Integer orgNumber) {
    if (!orgRepository.existsById(orgNumber)) {
      throw new EntityNotFoundException("Organization not found: " + orgNumber);
    }
    location.setOrgNumber(orgNumber);
    return locationRepository.save(location);
  }

  @Override
  @Transactional(readOnly = true)
  public Location getLocationById(Long locationId) {
    return locationRepository.findById(locationId)
        .orElseThrow(() -> new EntityNotFoundException("Location not found: " + locationId));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Location> getLocationsByOrg(Integer orgNumber) {
    return locationRepository.findByOrgNumberAndIsActiveTrue(orgNumber);
  }

  @Override
  public Location updateLocation(Long locationId, Location location) {
    Location existing = getLocationById(locationId);
    existing.setName(location.getName());
    existing.setDescription(location.getDescription());
    existing.setLocationType(location.getLocationType());
    existing.setTempMinC(location.getTempMinC());
    existing.setTempMaxC(location.getTempMaxC());
    return locationRepository.save(existing);
  }

  @Override
  public void deleteLocation(Long locationId) {
    Location location = getLocationById(locationId);
    location.setIsActive(false);
    locationRepository.save(location);
  }
}
