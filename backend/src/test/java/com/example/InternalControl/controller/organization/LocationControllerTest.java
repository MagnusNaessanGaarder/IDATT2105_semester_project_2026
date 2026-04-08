package com.example.InternalControl.controller.organization;

import com.example.InternalControl.model.organization.Location;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.organization.LocationService;
import com.example.InternalControl.service.user.UserOrganizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author TriTacLe
 * @since 1.0
 */
@WebMvcTest(controllers = LocationController.class, excludeAutoConfiguration = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LocationService locationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserOrganizationService userOrgService;

    private Location mockLocation;

    @BeforeEach
    void setUp() {
        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@example.com", "password", 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        // Mock userOrgService to return true for organization access
        when(userOrgService.isUserInOrganization(anyLong(), anyInt())).thenReturn(true);
        
        mockLocation = new Location();
    }

    @Test

    void getAllLocations_AsAdmin_ReturnsOk() throws Exception {
        List<Location> locations = Arrays.asList(mockLocation);
        when(locationService.getLocationsByOrg(anyInt())).thenReturn(locations);

        mockMvc.perform(get("/api/v1/locations")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test

    void getAllLocations_AsManager_ReturnsOk() throws Exception {
        List<Location> locations = Arrays.asList(mockLocation);
        when(locationService.getLocationsByOrg(anyInt())).thenReturn(locations);

        mockMvc.perform(get("/api/v1/locations")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllLocations_Unauthorized_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/locations")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isUnauthorized());
    }

    @Test

    void getLocationById_ExistingLocation_ReturnsOk() throws Exception {
        when(locationService.getLocationById(anyLong())).thenReturn(mockLocation);

        mockMvc.perform(get("/api/v1/locations/1"))
                .andExpect(status().isOk());
    }

    @Test

    void createLocation_ValidRequest_ReturnsCreated() throws Exception {
        when(locationService.createLocation(any(), anyInt())).thenReturn(mockLocation);

        mockMvc.perform(post("/api/v1/locations")
                        .param("orgNumber", "937219997")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Kjøleskap 1\"}"))
                .andExpect(status().isCreated());
    }

    @Test

    void createLocation_AsEmployee_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/locations")
                        .param("orgNumber", "937219997")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test

    void updateLocation_ValidRequest_ReturnsOk() throws Exception {
        when(locationService.updateLocation(anyLong(), any())).thenReturn(mockLocation);

        mockMvc.perform(put("/api/v1/locations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Name\"}"))
                .andExpect(status().isOk());
    }

    @Test

    void deleteLocation_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/locations/1"))
                .andExpect(status().isNoContent());
    }
}
