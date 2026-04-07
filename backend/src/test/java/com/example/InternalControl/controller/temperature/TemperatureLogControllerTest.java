package com.example.InternalControl.controller.temperature;

import com.example.InternalControl.dto.temperature.request.TemperatureLogEntryRequest;
import com.example.InternalControl.dto.temperature.response.TemperatureLogEntryResponse;
import com.example.InternalControl.dto.temperature.response.TemperatureLogPointResponse;
import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.temperature.TemperatureLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author TriTacLe
 * @since 1.0
 */
@WebMvcTest(TemperatureLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
class TemperatureLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TemperatureLogService temperatureLogService;

    @MockBean
    private JwtService jwtService;

    private TemperatureLogEntryResponse mockEntry;
    private TemperatureLogPointResponse mockPoint;

    @BeforeEach
    void setUp() {
        mockEntry = new TemperatureLogEntryResponse();
        mockPoint = new TemperatureLogPointResponse();
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getAllEntries_AsAdmin_ReturnsOk() throws Exception {
        List<TemperatureLogEntryResponse> entries = Arrays.asList(mockEntry);
        when(temperatureLogService.listEntries(anyInt())).thenReturn(entries);

        mockMvc.perform(get("/api/v1/temperature/entries")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
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
    @WithMockUser(roles = {"ADMIN"})
    void getEntryById_ExistingEntry_ReturnsOk() throws Exception {
        when(temperatureLogService.getEntry(anyLong(), anyInt())).thenReturn(mockEntry);

        mockMvc.perform(get("/api/v1/temperature/entries/1")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
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
    @WithMockUser(roles = {"ADMIN"})
    void createEntry_InvalidTemperature_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/temperature/entries")
                        .param("orgNumber", "937219997")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logPointId\": 1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void deleteEntry_ExistingEntry_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/temperature/entries/1")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void deleteEntry_AsEmployee_ReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/temperature/entries/1")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getAlertEntries_ReturnsAlerts() throws Exception {
        List<TemperatureLogEntryResponse> alerts = Arrays.asList(mockEntry);
        when(temperatureLogService.listAlerts(anyInt())).thenReturn(alerts);

        mockMvc.perform(get("/api/v1/temperature/entries/alerts")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getEntriesByPoint_ReturnsEntries() throws Exception {
        List<TemperatureLogEntryResponse> entries = Arrays.asList(mockEntry);
        when(temperatureLogService.listEntriesByPoint(anyLong(), anyInt())).thenReturn(entries);

        mockMvc.perform(get("/api/v1/temperature/entries/by-point/1")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }
}
