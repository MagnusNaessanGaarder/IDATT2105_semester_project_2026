package com.example.InternalControl.controller.user;

import com.example.InternalControl.dto.user.IdentityResponse;

import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.user.IdentityProviderService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

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
@WebMvcTest(controllers = IdentityProviderController.class,
        excludeAutoConfiguration = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class IdentityProviderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IdentityProviderService identityProviderService;

    @MockBean
    private JwtService jwtService;

    private IdentityResponse mockIdentity;

    @BeforeEach
    void setUp() {
        mockIdentity = new IdentityResponse();
    }

    @Test
    @WithMockUser
    void getUserIdentities_Authenticated_ReturnsOk() throws Exception {
        List<IdentityResponse> identities = Arrays.asList(mockIdentity);
        when(identityProviderService.getUserIdentities(anyLong())).thenReturn(identities);

        mockMvc.perform(get("/api/v1/identity/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].identityId").value(1));
    }

    @Test
    void getUserIdentities_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/identity/user/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void linkIdentity_ValidRequest_ReturnsCreated() throws Exception {
        when(identityProviderService.linkIdentity(anyLong(), any())).thenReturn(mockIdentity);

        String request = "{\"provider\": \"VIPPS\", \"providerUserId\": \"vipp-user-123\", \"accessToken\": \"token\"}";

        mockMvc.perform(post("/api/v1/identity/user/1/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    void unlinkIdentity_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/identity/user/1/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void getSupportedProviders_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/identity/providers"))
                .andExpect(status().isOk());
    }
}
