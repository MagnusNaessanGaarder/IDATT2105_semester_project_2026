package com.example.InternalControl.controller.analytics;

import com.example.InternalControl.dto.analytics.DashboardSummaryResponse;
import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.analytics.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author TriTacLe
 * @since 1.0
 */
@WebMvcTest(AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private JwtService jwtService;

    private DashboardSummaryResponse mockStats;

    @BeforeEach
    void setUp() {
        mockStats = DashboardSummaryResponse.builder().build();
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getDashboardStats_AsAdmin_ReturnsOk() throws Exception {
        when(dashboardService.getDashboardSummary(anyInt())).thenReturn(mockStats);

        mockMvc.perform(get("/api/v1/analytics/dashboard")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openDeviations").value(5));
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void getDashboardStats_AsManager_ReturnsOk() throws Exception {
        when(dashboardService.getDashboardSummary(anyInt())).thenReturn(mockStats);

        mockMvc.perform(get("/api/v1/analytics/dashboard")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }

    @Test
    void getDashboardStats_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/dashboard")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getComplianceReport_AsAdmin_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/compliance")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }
}
