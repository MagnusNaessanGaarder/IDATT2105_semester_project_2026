package com.example.InternalControl.controller.temperature;

import com.example.InternalControl.dto.temperature.request.TemperatureLogEntryRequest;
import com.example.InternalControl.dto.temperature.response.TemperatureLogEntryResponse;
import com.example.InternalControl.dto.temperature.response.TemperatureLogPointResponse;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.temperature.TemperatureLogService;
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

import java.math.BigDecimal;
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
@WebMvcTest(controllers = TemperatureLogController.class, excludeAutoConfiguration = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class TemperatureLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TemperatureLogService temperatureLogService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private com.example.InternalControl.service.user.UserOrganizationService userOrgService;

    private TemperatureLogEntryResponse mockEntry;
    private TemperatureLogPointResponse mockPoint;

    @BeforeEach
    void setUp() {
        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@example.com", "password", 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        // Mock userOrgService to return true for organization access
        when(userOrgService.isUserInOrganization(anyLong(), anyInt())).thenReturn(true);
        
        mockEntry = new TemperatureLogEntryResponse();
        mockPoint = new TemperatureLogPointResponse();
    }

    @Test
    void getAllEntries_AsAdmin_ReturnsOk() throws Exception {
        List<TemperatureLogEntryResponse> entries = Arrays.asList(mockEntry);
        when(temperatureLogService.listEntries(anyInt())).thenReturn(entries);

        mockMvc.perform(get("/api/v1/temperature/entries")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getAllEntries_AsManager_ReturnsOk() throws Exception {
        List<TemperatureLogEntryResponse> entries = Arrays.asList(mockEntry);
        when(temperatureLogService.listEntries(anyInt())).thenReturn(entries);

        mockMvc.perform(get("/api/v1/temperature/entries")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllEntries_Unauthorized_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/temperature/entries")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isUnauthorized());
    }

    @Test

    void getEntryById_ExistingEntry_ReturnsOk() throws Exception {
        when(temperatureLogService.getEntry(anyLong(), anyInt())).thenReturn(mockEntry);

        mockMvc.perform(get("/api/v1/temperature/entries/1")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }

    @Test

    void createEntry_ValidRequest_ReturnsCreated() throws Exception {
        when(temperatureLogService.recordEntry(any(), anyInt(), anyLong())).thenReturn(mockEntry);

        TemperatureLogEntryRequest request = new TemperatureLogEntryRequest();
        request.setLogPointId(1L);
        request.setTemperatureC(new BigDecimal("3.0"));
        request.setNoteText("Normal reading");

        mockMvc.perform(post("/api/v1/temperature/entries")
                        .param("orgNumber", "937219997")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test

    void createEntry_InvalidTemperature_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/temperature/entries")
                        .param("orgNumber", "937219997")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logPointId\": 1}"))
                .andExpect(status().isBadRequest());
    }

    @Test

    void deleteEntry_ExistingEntry_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/temperature/entries/1")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isNoContent());
    }

    @Test

    void deleteEntry_AsEmployee_ReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/temperature/entries/1")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isForbidden());
    }

    @Test

    void getAlertEntries_ReturnsAlerts() throws Exception {
        List<TemperatureLogEntryResponse> alerts = Arrays.asList(mockEntry);
        when(temperatureLogService.listAlerts(anyInt())).thenReturn(alerts);

        mockMvc.perform(get("/api/v1/temperature/entries/alerts")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }

    @Test

    void getEntriesByPoint_ReturnsEntries() throws Exception {
        List<TemperatureLogEntryResponse> entries = Arrays.asList(mockEntry);
        when(temperatureLogService.listEntriesByPoint(anyLong(), anyInt())).thenReturn(entries);

        mockMvc.perform(get("/api/v1/temperature/entries/by-point/1")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }
}
