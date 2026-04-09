package com.example.InternalControl.controller.analytics;

import com.example.InternalControl.AbstractIntegrationTest;
import com.example.InternalControl.dto.analytics.ComplianceScoreResponse;
import com.example.InternalControl.dto.analytics.DashboardSummaryResponse;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.service.analytics.DashboardService;
import com.example.InternalControl.service.user.UserOrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AnalyticsController using TestContainers.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AnalyticsControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private UserOrganizationService userOrganizationService;

    private static final Integer ORG_NUMBER = 123456789;
    private static final String BASE_URL = "/api/v1/analytics";

    @BeforeEach
    void setUp() {
        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
        if (existingAuth != null) {
            CustomUserDetails userDetails = new CustomUserDetails(
                    1L, existingAuth.getName(), "password", existingAuth.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
        }
        when(userOrganizationService.isUserInOrganization(anyLong(), anyInt())).thenReturn(true);
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getDashboardSummary_AsEmployee_ReturnsOk() throws Exception {
        // Given
        DashboardSummaryResponse response = DashboardSummaryResponse.builder()
                .checklistsCompletedToday(5)
                .openDeviations(2)
                .complianceScore(85.5)
                .build();

        when(dashboardService.getDashboardSummary(ORG_NUMBER)).thenReturn(response);

        // When & Then
        mockMvc.perform(get(BASE_URL + "/dashboard/summary")
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checklistsCompletedToday").value(5))
                .andExpect(jsonPath("$.complianceScore").value(85.5));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getComplianceScore_AsEmployee_ReturnsOk() throws Exception {
        // Given
        ComplianceScoreResponse response = ComplianceScoreResponse.builder()
                .currentScore(85)
                .status("GOOD")
                .build();

        when(dashboardService.getComplianceScore(ORG_NUMBER)).thenReturn(response);

        // When & Then
        mockMvc.perform(get(BASE_URL + "/compliance-score")
                        .param("orgNumber", ORG_NUMBER.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentScore").value(85));
    }
}
