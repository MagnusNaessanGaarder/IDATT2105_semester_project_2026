package com.example.InternalControl.controller.user;

import com.example.InternalControl.AbstractIntegrationTest;
import com.example.InternalControl.dto.user.PermissionResponse;
import com.example.InternalControl.service.user.PermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PermissionController using TestContainers.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class PermissionControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PermissionService permissionService;

    private static final String BASE_URL = "/api/v1/permissions";

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getAllPermissions_AsAdmin_ReturnsOk() throws Exception {
        // Given
        PermissionResponse permission = PermissionResponse.builder()
                .permissionId(1L)
                .permissionKey("USER_READ")
                .build();

        when(permissionService.getAllPermissions()).thenReturn(List.of(permission));

        // When & Then
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].permissionKey").value("USER_READ"));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getAllPermissions_AsEmployee_ReturnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getPermissionsByRole_AsAdmin_ReturnsOk() throws Exception {
        // Given
        when(permissionService.getPermissionsByRoleId(1L)).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get(BASE_URL + "/by-role/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void assignPermissionToRole_AsAdmin_ReturnsNoContent() throws Exception {
        // Given
        doNothing().when(permissionService).assignPermissionToRole(1L, 1L);

        // When & Then
        mockMvc.perform(post(BASE_URL + "/role/1/permission/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void removePermissionFromRole_AsAdmin_ReturnsNoContent() throws Exception {
        // Given
        doNothing().when(permissionService).removePermissionFromRole(1L, 1L);

        // When & Then
        mockMvc.perform(delete(BASE_URL + "/role/1/permission/1"))
                .andExpect(status().isNoContent());
    }
}
