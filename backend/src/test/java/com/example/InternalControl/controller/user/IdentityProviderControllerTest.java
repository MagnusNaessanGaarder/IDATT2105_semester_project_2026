package com.example.InternalControl.controller.user;

import com.example.InternalControl.AbstractIntegrationTest;
import com.example.InternalControl.dto.user.IdentityResponse;
import com.example.InternalControl.dto.user.LinkIdentityRequest;
import com.example.InternalControl.service.user.IdentityProviderService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for IdentityProviderController using TestContainers.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class IdentityProviderControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IdentityProviderService identityProviderService;

    private static final String BASE_URL = "/api/v1/users/identities";

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getUserIdentities_AsAuthenticatedUser_ReturnsOk() throws Exception {
        // Given
        IdentityResponse identity = IdentityResponse.builder()
                .identityId(1L)
                .providerName("google")
                .build();

        when(identityProviderService.getUserIdentities(anyLong())).thenReturn(List.of(identity));

        // When & Then
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].providerName").value("google"));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void linkIdentity_AsAuthenticatedUser_ReturnsCreated() throws Exception {
        // Given
        LinkIdentityRequest request = new LinkIdentityRequest();
        request.setProviderName("google");
        request.setProviderUserId("google-123");

        IdentityResponse response = IdentityResponse.builder()
                .identityId(1L)
                .providerName("google")
                .build();

        when(identityProviderService.linkIdentity(anyLong(), any())).thenReturn(response);

        // When & Then
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identityId").value(1));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void unlinkIdentity_AsAuthenticatedUser_ReturnsNoContent() throws Exception {
        // Given
        doNothing().when(identityProviderService).unlinkIdentity(anyLong(), eq(1L));

        // When & Then
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getSupportedProviders_AsAdmin_ReturnsOk() throws Exception {
        // When & Then
        mockMvc.perform(get(BASE_URL + "/providers"))
                .andExpect(status().isOk());
    }
}
