package com.example.InternalControl.service.location;

import com.example.InternalControl.model.location.Location;
import java.util.List;

/**
 * Service interface for location operations.
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
