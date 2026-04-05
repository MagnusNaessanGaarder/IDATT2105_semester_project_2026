package com.example.InternalControl.service;

import com.example.InternalControl.model.organization.Location;
import com.example.InternalControl.model.enums.LocationType;
import com.example.InternalControl.repository.organization.LocationRepository;
import com.example.InternalControl.repository.organization.OrganizationRepository;
import com.example.InternalControl.service.organization.LocationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for LocationService.
 */
@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private OrganizationRepository orgRepository;

    @InjectMocks
    private LocationServiceImpl locationService;

    @Test
    void shouldCreateLocation() {
        // Given
        Location location = Location.builder()
                .name("Kitchen")
                .description("Main kitchen area")
                .locationType(LocationType.KITCHEN)
                .tempMinC(new BigDecimal("2.00"))
                .tempMaxC(new BigDecimal("8.00"))
                .build();

        when(orgRepository.existsById(123)).thenReturn(true);
        when(locationRepository.save(any())).thenAnswer(inv -> {
            Location saved = inv.getArgument(0);
            saved.setLocationId(1L);
            return saved;
        });

        // When
        Location result = locationService.createLocation(location, 123);

        // Then
        assertThat(result.getLocationId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Kitchen");
        assertThat(result.getOrgNumber()).isEqualTo(123);
        assertThat(result.getLocationType()).isEqualTo(LocationType.KITCHEN);
    }

    @Test
    void shouldCreateLocationWithDefaultType() {
        // Given
        Location location = Location.builder()
                .name("Storage")
                .description("Storage room")
                .build();

        when(orgRepository.existsById(123)).thenReturn(true);
        when(locationRepository.save(any())).thenAnswer(inv -> {
            Location saved = inv.getArgument(0);
            saved.setLocationId(1L);
            return saved;
        });

        // When
        Location result = locationService.createLocation(location, 123);

        // Then
        assertThat(result.getLocationType()).isEqualTo(LocationType.OTHER);
    }
}
