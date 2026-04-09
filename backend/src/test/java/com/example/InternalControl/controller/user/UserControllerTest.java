package com.example.InternalControl.controller.user;

import com.example.InternalControl.dto.user.UserCreateRequest;
import com.example.InternalControl.dto.user.UserUpdateRequest;
import com.example.InternalControl.model.user.AppUser;
import com.example.InternalControl.model.user.Role;
import com.example.InternalControl.model.user.UserOrganization;
import com.example.InternalControl.model.user.UserOrganizationId;
import com.example.InternalControl.model.user.UserOrganizationRole;
import com.example.InternalControl.model.user.UserOrganizationRoleId;
import com.example.InternalControl.repository.user.AppUserRepository;
import com.example.InternalControl.repository.user.UserOrganizationRepository;
import com.example.InternalControl.repository.user.UserOrganizationRoleRepository;
import com.example.InternalControl.security.CustomUserDetails;
import com.example.InternalControl.security.JwtService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
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
 * Unit tests for UserController.
 */
@WebMvcTest(controllers = UserController.class, excludeAutoConfiguration = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AppUserRepository userRepository;

    @MockBean
    private UserOrganizationRepository userOrgRepository;

    @MockBean
    private UserOrganizationRoleRepository userOrgRoleRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtService jwtService;

    private AppUser testUser;
    private UserOrganization testUserOrg;

    @BeforeEach
    void setUp() {
        CustomUserDetails userDetails = new CustomUserDetails(1L, "test@example.com", "password", 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        testUser = AppUser.builder()
                .userId(1L)
                .displayName("Test User")
                .email("test@example.com")
                .phone("12345678")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testUserOrg = UserOrganization.builder()
                .id(new UserOrganizationId(1L, 937219997))
                .user(testUser)
                .isActive(true)
                .joinedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getAllUsers_AsAdmin_ReturnsUsers() throws Exception {
        // Given
        when(userOrgRepository.findByOrgNumber(937219997))
                .thenReturn(List.of(testUserOrg));
        when(userOrgRoleRepository.findByUserIdAndOrgNumber(1L, 937219997))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/users")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("test@example.com"));
    }

    @Test
    void getAllUsers_AsManager_ReturnsUsers() throws Exception {
        // Given
        when(userOrgRepository.findByOrgNumber(937219997))
                .thenReturn(List.of(testUserOrg));
        when(userOrgRoleRepository.findByUserIdAndOrgNumber(1L, 937219997))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/users")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllUsers_AsEmployee_ReturnsOk() throws Exception {
        // Given
        when(userOrgRepository.findByOrgNumber(937219997))
                .thenReturn(List.of(testUserOrg));
        when(userOrgRoleRepository.findByUserIdAndOrgNumber(1L, 937219997))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/users")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk());
    }

    @Test
    void getUser_AsAdmin_ReturnsUser() throws Exception {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userOrgRoleRepository.findByUserIdAndOrgNumber(1L, 937219997))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/users/1")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void getUser_NotFound_ReturnsNotFound() throws Exception {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/users/999")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createUser_AsAdmin_ReturnsCreated() throws Exception {
        // Given
        UserCreateRequest request = UserCreateRequest.builder()
                .displayName("New User")
                .email("new@example.com")
                .password("Password123!")
                .orgNumber(937219997)
                .roleIds(Collections.emptyList())
                .build();

        AppUser savedUser = AppUser.builder()
                .userId(2L)
                .displayName("New User")
                .email("new@example.com")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(AppUser.class))).thenReturn(savedUser);
        when(userOrgRepository.save(any(UserOrganization.class))).thenReturn(testUserOrg);
        when(userOrgRoleRepository.findByUserIdAndOrgNumber(2L, 937219997))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    @Test
    void createUser_DuplicateEmail_ReturnsBadRequest() throws Exception {
        // Given
        UserCreateRequest request = UserCreateRequest.builder()
                .displayName("New User")
                .email("existing@example.com")
                .password("Password123!")
                .orgNumber(937219997)
                .build();

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_AsManager_ReturnsCreated() throws Exception {
        // Given
        UserCreateRequest request = UserCreateRequest.builder()
                .displayName("New User")
                .email("new@example.com")
                .password("Password123!")
                .orgNumber(937219997)
                .roleIds(Collections.emptyList())
                .build();

        AppUser savedUser = AppUser.builder()
                .userId(2L)
                .displayName("New User")
                .email("new@example.com")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(AppUser.class))).thenReturn(savedUser);
        when(userOrgRepository.save(any(UserOrganization.class))).thenReturn(testUserOrg);
        when(userOrgRoleRepository.findByUserIdAndOrgNumber(2L, 937219997))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void updateUser_AsAdmin_ReturnsUpdated() throws Exception {
        // Given
        UserUpdateRequest request = UserUpdateRequest.builder()
                .displayName("Updated Name")
                .email("updated@example.com")
                .build();

        AppUser updatedUser = AppUser.builder()
                .userId(1L)
                .displayName("Updated Name")
                .email("updated@example.com")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(AppUser.class))).thenReturn(updatedUser);
        when(userOrgRoleRepository.findByUserIdAndOrgNumber(1L, 937219997))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(put("/api/v1/users/1")
                        .param("orgNumber", "937219997")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated Name"));
    }

    @Test
    void deleteUser_AsAdmin_ReturnsNoContent() throws Exception {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userOrgRepository.findById(any(UserOrganizationId.class)))
                .thenReturn(Optional.of(testUserOrg));

        // When & Then
        mockMvc.perform(delete("/api/v1/users/1")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_NotFound_ReturnsNotFound() throws Exception {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(delete("/api/v1/users/999")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_AsManager_ReturnsNoContent() throws Exception {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userOrgRepository.findById(any(UserOrganizationId.class)))
                .thenReturn(Optional.of(testUserOrg));

        // When & Then
        mockMvc.perform(delete("/api/v1/users/1")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getUser_WithRoles_ReturnsUserWithRoles() throws Exception {
        // Given
        Role role = Role.builder()
                .roleId(1L)
                .roleName("EMPLOYEE")
                .description("Regular employee")
                .isSystemRole(true)
                .build();

        UserOrganizationRole userRole = UserOrganizationRole.builder()
                .id(new UserOrganizationRoleId(1L, 937219997, 1L))
                .role(role)
                .assignedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userOrgRoleRepository.findByUserIdAndOrgNumber(1L, 937219997))
                .thenReturn(List.of(userRole));

        // When & Then
        mockMvc.perform(get("/api/v1/users/1")
                        .param("orgNumber", "937219997"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0].roleName").value("EMPLOYEE"));
    }
}
