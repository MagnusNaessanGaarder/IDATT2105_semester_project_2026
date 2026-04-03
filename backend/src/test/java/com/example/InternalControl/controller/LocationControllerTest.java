package com.example.InternalControl.controller;

import com.example.InternalControl.dto.location.LocationCreateRequest;
import com.example.InternalControl.dto.location.LocationResponse;
import com.example.InternalControl.model.location.Location;
import com.example.InternalControl.shared.enums.LocationType;
import com.example.InternalControl.security.AuthenticationFacade;
import com.example.InternalControl.service.location.LocationService;
import com.example.InternalControl.service.location.mapper.LocationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.InternalControl.controller.location.LocationController;

/**
 * Unit tests for LocationController.
 *
 * @author TriTacLe
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class LocationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LocationService locationService;

    @Mock
    private AuthenticationFacade authenticationFacade;

    @Mock
    private LocationMapper locationMapper;

    @InjectMocks
    private LocationController controller;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getLocations_WithValidRequest_ReturnsList() throws Exception {
        // Given
        Long userId = 1L;
        Integer orgNumber = 123456789;
        Location location = createTestLocation();
        LocationResponse response = createTestResponse();

        when(authenticationFacade.extractAndValidateUser(any(), eq(orgNumber))).thenReturn(userId);
        when(locationService.getLocationsByOrg(orgNumber)).thenReturn(List.of(location));
        when(locationMapper.toResponseList(any())).thenReturn(List.of(response));

        // When & Then
        mockMvc.perform(get("/api/locations")
                        .param("orgNumber", orgNumber.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].locationId").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Location"));
    }

    @Test
    void getLocationById_WithValidId_ReturnsLocation() throws Exception {
        // Given
        Long locationId = 1L;
        Location location = createTestLocation();
        LocationResponse response = createTestResponse();

        when(locationService.getLocationById(locationId)).thenReturn(location);
        when(locationMapper.toResponse(location)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/locations/{id}", locationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationId").value(1))
                .andExpect(jsonPath("$.name").value("Test Location"));
    }

    @Test
    void createLocation_WithValidRequest_ReturnsCreated() throws Exception {
        // Given
        Long userId = 1L;
        Integer orgNumber = 123456789;
        LocationCreateRequest requestDto = createTestRequest();
        Location location = createTestLocation();
        Location created = createTestLocation();
        LocationResponse response = createTestResponse();

        when(authenticationFacade.extractAndValidateUser(any(), eq(orgNumber))).thenReturn(userId);
        when(locationMapper.toEntity(any(LocationCreateRequest.class))).thenReturn(location);
        when(locationService.createLocation(any(Location.class), eq(orgNumber))).thenReturn(created);
        when(locationMapper.toResponse(created)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/locations")
                        .param("orgNumber", orgNumber.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.locationId").value(1))
                .andExpect(jsonPath("$.name").value("Test Location"));
    }

    @Test
    void updateLocation_WithValidRequest_ReturnsUpdated() throws Exception {
        // Given
        Long locationId = 1L;
        LocationCreateRequest requestDto = createTestRequest();
        Location location = createTestLocation();
        Location updated = createTestLocation();
        LocationResponse response = createTestResponse();

        when(locationMapper.toEntity(any(LocationCreateRequest.class))).thenReturn(location);
        when(locationService.updateLocation(eq(locationId), any(Location.class))).thenReturn(updated);
        when(locationMapper.toResponse(updated)).thenReturn(response);

        // When & Then
        mockMvc.perform(put("/api/locations/{id}", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationId").value(1))
                .andExpect(jsonPath("$.name").value("Test Location"));
    }

    @Test
    void deleteLocation_WithValidId_ReturnsNoContent() throws Exception {
        // Given
        Long locationId = 1L;

        // When & Then
        mockMvc.perform(delete("/api/locations/{id}", locationId))
                .andExpect(status().isNoContent());
    }

    private Location createTestLocation() {
        Location location = new Location();
        location.setLocationId(1L);
        location.setOrgNumber(123456789);
        location.setName("Test Location");
        location.setDescription("Test Description");
        location.setLocationType(LocationType.STORAGE);
        location.setTempMinC(new BigDecimal("-5.00"));
        location.setTempMaxC(new BigDecimal("25.00"));
        location.setIsActive(true);
        return location;
    }

    private LocationResponse createTestResponse() {
        return LocationResponse.builder()
                .locationId(1L)
                .orgNumber(123456789)
                .name("Test Location")
                .description("Test Description")
                .locationType(LocationType.STORAGE)
                .isActive(true)
                .build();
    }

    private LocationCreateRequest createTestRequest() {
        return LocationCreateRequest.builder()
                .name("Test Location")
                .description("Test Description")
                .locationType(LocationType.STORAGE)
                .tempMinC(new BigDecimal("-5.00"))
                .tempMaxC(new BigDecimal("25.00"))
                .build();
    }
}
