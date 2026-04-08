package com.example.InternalControl.controller.organization;

import com.example.InternalControl.dto.settings.OrganizationSettingsRequest;
import com.example.InternalControl.dto.settings.OrganizationSettingsResponse;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.settings.OrganizationSettingsService;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author TriTacLe
 * @since 1.0
 */
@WebMvcTest(controllers = OrganizationSettingsAdminController.class, excludeAutoConfiguration = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class OrganizationSettingsAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrganizationSettingsService settingsService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserOrganizationService userOrgService;

    private OrganizationSettingsResponse mockSettings;

    @BeforeEach
    void setUp() {
        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@example.com", "password", 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        // Mock userOrgService to return true for organization access
        when(userOrgService.isUserInOrganization(anyLong(), anyInt())).thenReturn(true);
        
        mockSettings = new OrganizationSettingsResponse();
    }

    @Test

    void getSettings_AsAdmin_ReturnsOk() throws Exception {
        when(settingsService.getSettings(anyInt())).thenReturn(mockSettings);

        mockMvc.perform(get("/api/admin/organizations/settings")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orgNumber").value(937219997))
                .andExpect(jsonPath("$.timezoneName").value("Europe/Oslo"));
    }

    @Test

    void getSettings_AsManager_ReturnsOk() throws Exception {
        when(settingsService.getSettings(anyInt())).thenReturn(mockSettings);

        mockMvc.perform(get("/api/admin/organizations/settings")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }

    @Test
    void getSettings_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/organizations/settings")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isUnauthorized());
    }

    @Test

    void updateSettings_ValidRequest_ReturnsOk() throws Exception {
        when(settingsService.updateSettings(anyInt(), any(), anyLong())).thenReturn(mockSettings);

        OrganizationSettingsRequest request = OrganizationSettingsRequest.builder()
                .timezoneName("Europe/London")
                .localeCode("en-GB")
                .build();

        mockMvc.perform(put("/api/admin/organizations/settings")
                        .param("orgNumber", "937219997")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test

    void updateSettings_AsEmployee_ReturnsForbidden() throws Exception {
        OrganizationSettingsRequest request = OrganizationSettingsRequest.builder()
                .timezoneName("Europe/London")
                .build();

        mockMvc.perform(put("/api/admin/organizations/settings")
                        .param("orgNumber", "937219997")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
