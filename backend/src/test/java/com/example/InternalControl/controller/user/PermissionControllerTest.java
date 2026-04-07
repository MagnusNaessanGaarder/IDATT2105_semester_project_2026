package com.example.InternalControl.controller.user;

import com.example.InternalControl.dto.user.PermissionResponse;
import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.user.PermissionService;
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
@WebMvcTest(PermissionController.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
class PermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PermissionService permissionService;

    @MockBean
    private JwtService jwtService;

    private PermissionResponse mockPermission;

    @BeforeEach
    void setUp() {
        mockPermission = new PermissionResponse();
        mockPermission.setPermissionId(1L);
        mockPermission.setPermissionKey("READ_DEVIATIONS");
        mockPermission.setDescription("Can read deviation reports");
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getAllPermissions_AsAdmin_ReturnsOk() throws Exception {
        List<PermissionResponse> permissions = Arrays.asList(mockPermission);
        when(permissionService.getAllPermissions()).thenReturn(permissions);

        mockMvc.perform(get("/api/admin/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].permissionId").value(1));
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void getAllPermissions_AsManager_ReturnsOk() throws Exception {
        List<PermissionResponse> permissions = Arrays.asList(mockPermission);
        when(permissionService.getAllPermissions()).thenReturn(permissions);

        mockMvc.perform(get("/api/admin/permissions"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllPermissions_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/permissions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getAllPermissions_AsEmployee_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/permissions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getPermissionById_Existing_ReturnsOk() throws Exception {
        when(permissionService.getPermissionById(anyLong())).thenReturn(mockPermission);

        mockMvc.perform(get("/api/admin/permissions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissionId").value(1));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getPermissionsByRole_ReturnsRolePermissions() throws Exception {
        List<PermissionResponse> permissions = Arrays.asList(mockPermission);
        when(permissionService.getPermissionsByRoleId(anyLong())).thenReturn(permissions);

        mockMvc.perform(get("/api/admin/permissions/role/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void assignPermissionToRole_ValidRequest_ReturnsCreated() throws Exception {
        String request = "{\"permissionId\": 1}";

        mockMvc.perform(post("/api/admin/permissions/role/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void assignPermissionToRole_AsManager_ReturnsForbidden() throws Exception {
        String request = "{\"permissionId\": 1}";

        mockMvc.perform(post("/api/admin/permissions/role/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void removePermissionFromRole_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/admin/permissions/role/1/1"))
                .andExpect(status().isNoContent());
    }
}
