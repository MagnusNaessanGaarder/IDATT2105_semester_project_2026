package com.example.InternalControl.controller.analytics;

import com.example.InternalControl.dto.analytics.DashboardSummaryResponse;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.analytics.DashboardService;
import com.example.InternalControl.service.user.UserOrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author TriTacLe
 * @since 1.0
 */
@WebMvcTest(controllers = AnalyticsController.class, excludeAutoConfiguration = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserOrganizationService userOrgService;

    private DashboardSummaryResponse mockStats;

    @BeforeEach
    void setUp() {
        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@example.com", "password", 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        // Mock userOrgService to return true for organization access
        when(userOrgService.isUserInOrganization(anyLong(), anyInt())).thenReturn(true);
        
        mockStats = DashboardSummaryResponse.builder().build();
    }

    @Test

    void getDashboardStats_AsAdmin_ReturnsOk() throws Exception {
        when(dashboardService.getDashboardSummary(anyInt())).thenReturn(mockStats);

        mockMvc.perform(get("/api/v1/analytics/dashboard")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openDeviations").value(5));
    }

    @Test

    void getDashboardStats_AsManager_ReturnsOk() throws Exception {
        when(dashboardService.getDashboardSummary(anyInt())).thenReturn(mockStats);

        mockMvc.perform(get("/api/v1/analytics/dashboard")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }

    @Test
    void getDashboardStats_ReturnsOk() throws Exception {
        when(dashboardService.getDashboardSummary(anyInt())).thenReturn(mockStats);

        mockMvc.perform(get("/api/v1/analytics/dashboard")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }

    @Test

    void getComplianceReport_AsAdmin_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/compliance")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }
}
