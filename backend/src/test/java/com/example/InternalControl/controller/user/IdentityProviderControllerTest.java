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

    private static final String BASE_URL = "/api/v1/identity";
    private static final Long USER_ID = 1L;

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getUserIdentities_AsAdmin_ReturnsOk() throws Exception {
        // Given
        IdentityResponse identity = IdentityResponse.builder()
                .identityId(1L)
                .providerName("google")
                .build();

        when(identityProviderService.getUserIdentities(USER_ID)).thenReturn(List.of(identity));

        // When & Then
        mockMvc.perform(get(BASE_URL + "/user/{userId}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].providerName").value("google"));
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void getUserIdentities_AsManager_ReturnsOk() throws Exception {
        // Given
        when(identityProviderService.getUserIdentities(USER_ID)).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get(BASE_URL + "/user/{userId}", USER_ID))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getUserIdentities_AsEmployee_ReturnsForbidden() throws Exception {
        // When & Then - employee can't access other user's identities
        mockMvc.perform(get(BASE_URL + "/user/{userId}", 2L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void linkIdentity_AsAdmin_ReturnsCreated() throws Exception {
        // Given
        LinkIdentityRequest request = new LinkIdentityRequest();
        request.setProviderName("google");
        request.setProviderUserId("google-123");

        IdentityResponse response = IdentityResponse.builder()
                .identityId(1L)
                .providerName("google")
                .build();

        when(identityProviderService.linkIdentity(eq(USER_ID), any())).thenReturn(response);

        // When & Then
        mockMvc.perform(post(BASE_URL + "/user/{userId}/link", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identityId").value(1));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void linkIdentity_AsEmployee_ReturnsForbidden() throws Exception {
        // Given
        LinkIdentityRequest request = new LinkIdentityRequest();
        request.setProviderName("google");
        request.setProviderUserId("google-123");

        // When & Then
        mockMvc.perform(post(BASE_URL + "/user/{userId}/link", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void unlinkIdentity_AsAdmin_ReturnsNoContent() throws Exception {
        // Given
        doNothing().when(identityProviderService).unlinkIdentity(USER_ID, 1L);

        // When & Then
        mockMvc.perform(delete(BASE_URL + "/user/{userId}/{identityId}", USER_ID, 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void unlinkIdentity_AsEmployee_ReturnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(delete(BASE_URL + "/user/{userId}/{identityId}", 2L, 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getSupportedProviders_AsAuthenticatedUser_ReturnsOk() throws Exception {
        // Given
        when(identityProviderService.getSupportedProviders()).thenReturn(List.of("vipps", "google", "microsoft"));

        // When & Then
        mockMvc.perform(get(BASE_URL + "/providers"))
                .andExpect(status().isOk());
    }
}
