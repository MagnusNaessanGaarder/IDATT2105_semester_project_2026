package com.example.InternalControl.service.organization;

import com.example.InternalControl.model.organization.Location;
import java.util.List;

/**
 * Defines operations for managing physical locations within an organization.
 *
 * @author TriTacLe
 * @since 1.0
 */
public interface LocationService {
  Location createLocation(Location location, Integer orgNumber);

  Location getLocationById(Long locationId);

  List<Location> getLocationsByOrg(Integer orgNumber);

  Location updateLocation(Long locationId, Location location);

  void deleteLocation(Long locationId);
}
