package com.example.InternalControl.service.organization;

import com.example.InternalControl.model.organization.Location;
import java.util.List;

public interface LocationService {
  Location createLocation(Location location, Integer orgNumber);

  Location getLocationById(Long locationId);

  List<Location> getLocationsByOrg(Integer orgNumber);

  Location updateLocation(Long locationId, Location location);

  void deleteLocation(Long locationId);
}
