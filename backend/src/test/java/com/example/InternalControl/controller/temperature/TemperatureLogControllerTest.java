package com.example.InternalControl.controller.temperature;

import com.example.InternalControl.AbstractIntegrationTest;
import com.example.InternalControl.dto.temperature.request.TemperatureLogEntryRequest;
import com.example.InternalControl.dto.temperature.request.TemperatureLogPointRequest;
import com.example.InternalControl.dto.temperature.response.TemperatureLogEntryResponse;
import com.example.InternalControl.dto.temperature.response.TemperatureLogPointResponse;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.service.temperature.TemperatureLogService;
import com.example.InternalControl.service.user.UserOrganizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TemperatureLogController using TestContainers.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class TemperatureLogControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TemperatureLogService temperatureLogService;

    @MockBean
    private UserOrganizationService userOrgService;

    private static final Integer ORG_NUMBER = 123456789;
    private static final String BASE_URL = "/api/v1/temperature";

    @BeforeEach
    void setUp() {
        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
        if (existingAuth != null) {
            CustomUserDetails userDetails = new CustomUserDetails(
                    1L, existingAuth.getName(), "password", existingAuth.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
        }
        when(userOrgService.isUserInOrganization(anyLong(), anyInt())).thenReturn(true);
    }

    // ==================== LOG POINT TESTS ====================

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void createLogPoint_AsManager_ReturnsCreated() throws Exception {
        // Given
        TemperatureLogPointRequest request = new TemperatureLogPointRequest();
        request.setName("Refrigerator 1");
        request.setLocationId(1L);

        TemperatureLogPointResponse response = TemperatureLogPointResponse.builder()
                .logPointId(1L)
                .name("Refrigerator 1")
                .build();

        when(temperatureLogService.createLogPoint(any(), eq(ORG_NUMBER))).thenReturn(response);

        // When & Then
        mockMvc.perform(post(BASE_URL + "/points")
                        .param("orgNumber", ORG_NUMBER.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.logPointId").value(1));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void createLogPoint_AsEmployee_ReturnsForbidden() throws Exception {
        // Given
        TemperatureLogPointRequest request = new TemperatureLogPointRequest();
        request.setName("Unauthorized Point");
        request.setLocationId(1L);

        // When & Then
        mockMvc.perform(post(BASE_URL + "/points")
                        .param("orgNumber", ORG_NUMBER.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void listLogPoints_AsEmployee_ReturnsOk() throws Exception {
        // Given
        TemperatureLogPointResponse point = TemperatureLogPointResponse.builder()
                .logPointId(1L)
                .name("Fridge")
                .build();

        when(temperatureLogService.listLogPoints(ORG_NUMBER)).thenReturn(List.of(point));

        // When & Then
        mockMvc.perform(get(BASE_URL + "/points")
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].logPointId").value(1));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void listActiveLogPoints_AsEmployee_ReturnsOk() throws Exception {
        // Given
        when(temperatureLogService.listActiveLogPoints(ORG_NUMBER)).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get(BASE_URL + "/points/active")
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getLogPoint_AsEmployee_ReturnsOk() throws Exception {
        // Given
        TemperatureLogPointResponse point = TemperatureLogPointResponse.builder()
                .logPointId(1L)
                .name("Fridge")
                .build();

        when(temperatureLogService.getLogPoint(1L, ORG_NUMBER)).thenReturn(point);

        // When & Then
        mockMvc.perform(get(BASE_URL + "/points/1")
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logPointId").value(1));
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void updateLogPoint_AsManager_ReturnsOk() throws Exception {
        // Given
        TemperatureLogPointRequest request = new TemperatureLogPointRequest();
        request.setName("Updated Name");
        request.setLocationId(1L);

        TemperatureLogPointResponse response = TemperatureLogPointResponse.builder()
                .logPointId(1L)
                .name("Updated Name")
                .build();

        when(temperatureLogService.updateLogPoint(eq(1L), any(), eq(ORG_NUMBER))).thenReturn(response);

        // When & Then
        mockMvc.perform(put(BASE_URL + "/points/1")
                        .param("orgNumber", ORG_NUMBER.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void deleteLogPoint_AsManager_ReturnsNoContent() throws Exception {
        // When & Then
        mockMvc.perform(delete(BASE_URL + "/points/1")
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isNoContent());
    }

    // ==================== TEMPERATURE ENTRY TESTS ====================

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void recordEntry_AsEmployee_ReturnsCreated() throws Exception {
        // Given
        TemperatureLogEntryRequest request = new TemperatureLogEntryRequest();
        request.setLogPointId(1L);
        request.setTemperatureC(new BigDecimal("5.5"));
        request.setMeasuredAt(LocalDateTime.now());

        TemperatureLogEntryResponse response = TemperatureLogEntryResponse.builder()
                .entryId(1L)
                .temperatureC(new BigDecimal("5.5"))
                .build();

        when(temperatureLogService.recordEntry(any(), eq(ORG_NUMBER), anyLong())).thenReturn(response);

        // When & Then
        mockMvc.perform(post(BASE_URL + "/entries")
                        .param("orgNumber", ORG_NUMBER.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.entryId").value(1));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void listEntries_AsEmployee_ReturnsOk() throws Exception {
        // Given
        when(temperatureLogService.listEntries(ORG_NUMBER)).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get(BASE_URL + "/entries")
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void listEntriesByPoint_AsEmployee_ReturnsOk() throws Exception {
        // Given
        when(temperatureLogService.listEntriesByPoint(1L, ORG_NUMBER)).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get(BASE_URL + "/entries/by-point/1")
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getEntry_AsEmployee_ReturnsOk() throws Exception {
        // Given
        TemperatureLogEntryResponse entry = TemperatureLogEntryResponse.builder()
                .entryId(1L)
                .temperatureC(new BigDecimal("5.5"))
                .build();

        when(temperatureLogService.getEntry(1L, ORG_NUMBER)).thenReturn(entry);

        // When & Then
        mockMvc.perform(get(BASE_URL + "/entries/1")
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryId").value(1));
    }

    // ==================== ALERTS TESTS ====================

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void listAlerts_AsManager_ReturnsOk() throws Exception {
        // Given
        when(temperatureLogService.listAlerts(ORG_NUMBER)).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get(BASE_URL + "/alerts")
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void listAlerts_AsEmployee_ReturnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get(BASE_URL + "/alerts")
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isForbidden());
    }
}
