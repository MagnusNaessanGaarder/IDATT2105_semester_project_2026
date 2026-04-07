package com.example.InternalControl.controller.user;

import com.example.InternalControl.model.user.Role;
import com.example.InternalControl.model.user.UserOrganizationRole;
import com.example.InternalControl.model.user.UserOrganizationRoleId;
import com.example.InternalControl.repository.user.AppUserRepository;
import com.example.InternalControl.repository.user.RoleRepository;
import com.example.InternalControl.repository.user.UserOrganizationRoleRepository;
import com.example.InternalControl.security.UserSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for RoleController.
 */
@WebMvcTest(RoleController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoleRepository roleRepository;

    @MockBean
    private UserOrganizationRoleRepository userOrgRoleRepository;

    private Role adminRole;
    private Role managerRole;
    private Role employeeRole;

    @BeforeEach
    void setUp() {
        adminRole = Role.builder()
                .roleId(1L)
                .roleName("ADMIN")
                .description("Administrator with full access")
                .isSystemRole(true)
                .build();

        managerRole = Role.builder()
                .roleId(2L)
                .roleName("MANAGER")
                .description("Manager with limited admin access")
                .isSystemRole(true)
                .build();

        employeeRole = Role.builder()
                .roleId(3L)
                .roleName("EMPLOYEE")
                .description("Regular employee")
                .isSystemRole(true)
                .build();
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getAllRoles_AsAdmin_ReturnsAllRoles() throws Exception {
        // Given
        when(roleRepository.findAll())
                .thenReturn(List.of(adminRole, managerRole, employeeRole));

        // When & Then
        mockMvc.perform(get("/api/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].roleName").value("ADMIN"))
                .andExpect(jsonPath("$[1].roleName").value("MANAGER"))
                .andExpect(jsonPath("$[2].roleName").value("EMPLOYEE"));
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void getAllRoles_AsManager_ReturnsAllRoles() throws Exception {
        // Given
        when(roleRepository.findAll())
                .thenReturn(List.of(managerRole, employeeRole));

        // When & Then
        mockMvc.perform(get("/api/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getAllRoles_AsEmployee_ReturnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/admin/roles"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getRole_AsAdmin_ReturnsRole() throws Exception {
        // Given
        when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));

        // When & Then
        mockMvc.perform(get("/api/admin/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleId").value(1))
                .andExpect(jsonPath("$.roleName").value("ADMIN"))
                .andExpect(jsonPath("$.description").value("Administrator with full access"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getRole_NotFound_ReturnsNotFound() throws Exception {
        // Given
        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/admin/roles/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getUserRoles_AsAdmin_ReturnsUserRoles() throws Exception {
        // Given
        UserOrganizationRole userRole = UserOrganizationRole.builder()
                .id(new UserOrganizationRoleId(1L, 937219997, 2L))
                .role(managerRole)
                .assignedAt(LocalDateTime.now())
                .build();

        when(userOrgRoleRepository.findByUserIdAndOrgNumber(1L, 937219997))
                .thenReturn(List.of(userRole));

        // When & Then
        mockMvc.perform(get("/api/admin/roles/user/1")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].roleName").value("MANAGER"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getUserRoles_NoRoles_ReturnsEmptyList() throws Exception {
        // Given
        when(userOrgRoleRepository.findByUserIdAndOrgNumber(1L, 937219997))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/admin/roles/user/1")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void getUserRoles_AsManager_ReturnsUserRoles() throws Exception {
        // Given
        when(userOrgRoleRepository.findByUserIdAndOrgNumber(1L, 937219997))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/admin/roles/user/1")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void getUserRoles_AsEmployee_ReturnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/admin/roles/user/1")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void assignRoleToUser_AsAdmin_ReturnsCreated() throws Exception {
        // Given
        when(userOrgRoleRepository.existsByUserIdAndOrgNumberAndRoleId(1L, 937219997, 2L))
                .thenReturn(false);
        when(userOrgRoleRepository.save(any(UserOrganizationRole.class)))
                .thenReturn(UserOrganizationRole.builder().build());

        // When & Then
        mockMvc.perform(post("/api/admin/roles/user/1")
                        .param("orgNumber", "937219997")
                        .param("roleId", "2"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void assignRoleToUser_AlreadyAssigned_ReturnsOk() throws Exception {
        // Given
        when(userOrgRoleRepository.existsByUserIdAndOrgNumberAndRoleId(1L, 937219997, 2L))
                .thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/admin/roles/user/1")
                        .param("orgNumber", "937219997")
                        .param("roleId", "2"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void assignRoleToUser_AsManager_ReturnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/admin/roles/user/1")
                        .param("orgNumber", "937219997")
                        .param("roleId", "2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void removeRoleFromUser_AsAdmin_ReturnsNoContent() throws Exception {
        // Given
        UserOrganizationRole userRole = UserOrganizationRole.builder()
                .id(new UserOrganizationRoleId(1L, 937219997, 2L))
                .role(managerRole)
                .assignedAt(LocalDateTime.now())
                .build();

        when(userOrgRoleRepository.findByUserIdAndOrgNumberAndRoleId(1L, 937219997, 2L))
                .thenReturn(Optional.of(userRole));

        // When & Then
        mockMvc.perform(delete("/api/admin/roles/user/1")
                        .param("orgNumber", "937219997")
                        .param("roleId", "2"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void removeRoleFromUser_NotFound_ReturnsNotFound() throws Exception {
        // Given
        when(userOrgRoleRepository.findByUserIdAndOrgNumberAndRoleId(1L, 937219997, 999L))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(delete("/api/admin/roles/user/1")
                        .param("orgNumber", "937219997")
                        .param("roleId", "999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void removeRoleFromUser_AsManager_ReturnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/admin/roles/user/1")
                        .param("orgNumber", "937219997")
                        .param("roleId", "2"))
                .andExpect(status().isForbidden());
    }
}
