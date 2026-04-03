package com.example.InternalControl.service.location.mapper;

import com.example.InternalControl.dto.location.LocationCreateRequest;
import com.example.InternalControl.dto.location.LocationResponse;
import com.example.InternalControl.model.location.Location;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting Location entities to DTOs.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Component
public class LocationMapper {

    public LocationResponse toResponse(Location location) {
        if (location == null) {
            return null;
        }

        return LocationResponse.builder()
                .locationId(location.getLocationId())
                .orgNumber(location.getOrgNumber())
                .name(location.getName())
                .description(location.getDescription())
                .locationType(location.getLocationType())
                .tempMinC(location.getTempMinC())
                .tempMaxC(location.getTempMaxC())
                .isActive(location.getIsActive())
                .createdAt(location.getCreatedAt())
                .updatedAt(location.getUpdatedAt())
                .build();
    }

    public List<LocationResponse> toResponseList(List<Location> locations) {
        if (locations == null) {
            return List.of();
        }
        return locations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public Location toEntity(LocationCreateRequest request) {
        if (request == null) {
            return null;
        }

        Location location = new Location();
        location.setName(request.getName());
        location.setDescription(request.getDescription());
        location.setLocationType(request.getLocationType());
        location.setTempMinC(request.getTempMinC());
        location.setTempMaxC(request.getTempMaxC());
        location.setIsActive(true);

        return location;
    }
}
