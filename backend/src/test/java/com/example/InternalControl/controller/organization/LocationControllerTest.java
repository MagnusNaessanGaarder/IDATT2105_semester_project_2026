package com.example.InternalControl.controller.organization;

import com.example.InternalControl.AbstractIntegrationTest;
import com.example.InternalControl.model.organization.Location;
import com.example.InternalControl.service.organization.LocationService;
import com.example.InternalControl.service.user.UserOrganizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for LocationController using TestContainers.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class LocationControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LocationService locationService;

    @MockBean
    private UserOrganizationService userOrgService;

    private static final Integer ORG_NUMBER = 123456789;
    private static final String BASE_URL = "/api/v1/locations";

    @BeforeEach
    void setUp() {
        when(userOrgService.isUserInOrganization(anyLong(), anyInt())).thenReturn(true);
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getLocations_AsEmployee_ReturnsOk() throws Exception {
        // Given
        Location location = new Location();
        location.setLocationId(1L);
        location.setName("Kitchen");

        when(locationService.getLocationsByOrg(ORG_NUMBER)).thenReturn(List.of(location));

        // When & Then
        mockMvc.perform(get(BASE_URL)
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].locationId").value(1));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getLocation_AsEmployee_ReturnsOk() throws Exception {
        // Given
        Location location = new Location();
        location.setLocationId(1L);
        location.setName("Kitchen");
        location.setOrgNumber(ORG_NUMBER);

        when(locationService.getLocationById(1L)).thenReturn(location);

        // When & Then
        mockMvc.perform(get(BASE_URL + "/1")
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationId").value(1));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void createLocation_AsAdmin_ReturnsCreated() throws Exception {
        // Given
        Location location = new Location();
        location.setName("New Location");

        Location created = new Location();
        created.setLocationId(1L);
        created.setName("New Location");

        when(locationService.createLocation(any(Location.class), eq(ORG_NUMBER))).thenReturn(created);

        // When & Then
        mockMvc.perform(post(BASE_URL)
                        .param("orgNumber", ORG_NUMBER.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(location)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.locationId").value(1));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void createLocation_AsEmployee_ReturnsForbidden() throws Exception {
        // Given
        Location location = new Location();
        location.setName("New Location");

        // When & Then
        mockMvc.perform(post(BASE_URL)
                        .param("orgNumber", ORG_NUMBER.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(location)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void updateLocation_AsManager_ReturnsOk() throws Exception {
        // Given
        Location existing = new Location();
        existing.setLocationId(1L);
        existing.setName("Old Name");
        existing.setOrgNumber(ORG_NUMBER);

        Location updated = new Location();
        updated.setName("Updated Name");

        when(locationService.getLocationById(1L)).thenReturn(existing);
        when(locationService.updateLocation(eq(1L), any(Location.class))).thenReturn(updated);

        // When & Then
        mockMvc.perform(put(BASE_URL + "/1")
                        .param("orgNumber", ORG_NUMBER.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void deleteLocation_AsAdmin_ReturnsNoContent() throws Exception {
        // Given
        Location existing = new Location();
        existing.setLocationId(1L);
        existing.setOrgNumber(ORG_NUMBER);

        when(locationService.getLocationById(1L)).thenReturn(existing);

        // When & Then
        mockMvc.perform(delete(BASE_URL + "/1")
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isNoContent());
    }
}
