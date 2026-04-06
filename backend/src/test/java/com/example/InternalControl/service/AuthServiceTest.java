package com.example.InternalControl.service;

import com.example.InternalControl.dto.auth.request.LoginRequest;
import com.example.InternalControl.dto.auth.request.RegisterRequest;
import com.example.InternalControl.dto.auth.response.AuthResponse;
import com.example.InternalControl.dto.auth.response.OrganizationRoleResponse;
import com.example.InternalControl.model.user.AppUser;
import com.example.InternalControl.repository.user.AppUserRepository;
import com.example.InternalControl.repository.user.UserOrganizationRepository;
import com.example.InternalControl.repository.user.UserOrganizationRoleRepository;
import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.auth.AuthService;
import com.example.InternalControl.service.auth.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService
 * Tests business logic with mocked dependencies
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private UserOrganizationRepository userOrgRepository;

    @Mock
    private UserOrganizationRoleRepository userOrgRoleRepository;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private AppUser testUser;

    @BeforeEach
    void setUp() {
        validRegisterRequest = new RegisterRequest(
            "Test User",
            "test@example.com",
            "12345678",
            "password123"
        );

        validLoginRequest = new LoginRequest(
            "test@example.com",
            "password123"
        );

        testUser = AppUser.builder()
            .userId(1L)
            .displayName("Test User")
            .email("test@example.com")
            .phone("12345678")
            .isActive(true)
            .build();
        
        // Setup default mocks for organizations
        lenient().when(userOrgRepository.findActiveOrganizationsByUserId(anyLong()))
            .thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("Should successfully register new user")
    void shouldSuccessfullyRegisterNewUser() {
        // Given
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.save(any(AppUser.class))).thenReturn(testUser);
        when(passwordEncoder.encode(any())).thenReturn("hashedPassword");
        when(userDetailsService.loadUserByUsername(any())).thenReturn(
            new User("test@example.com", "password", 
                Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_EMPLOYEE")))
        );
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access.token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh.token");

        // When
        AuthResponse response = authService.register(validRegisterRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.role()).isEqualTo("EMPLOYEE");
        assertThat(response.accessToken()).isEqualTo("access.token");
        assertThat(response.refreshToken()).isEqualTo("refresh.token");
        assertThat(response.organizations()).isNotNull();
        
        verify(userRepository, times(2)).save(any(AppUser.class)); // Called twice: once for user, once for credential
        verify(passwordEncoder).encode("password123");
    }

    @Test
    @DisplayName("Should throw exception when registering duplicate email")
    void shouldThrowExceptionWhenRegisteringDuplicateEmail() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(validRegisterRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Email is already in use");
        
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void shouldSuccessfullyLoginWithValidCredentials() {
        // Given
        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = new User("test@example.com", "password", 
            Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access.token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh.token");

        // When
        AuthResponse response = authService.login(validLoginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(response.accessToken()).isEqualTo("access.token");
        assertThat(response.organizations()).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception when login with invalid credentials")
    void shouldThrowExceptionWhenLoginWithInvalidCredentials() {
        // Given
        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("Invalid credentials"));
        when(userRepository.findByEmailWithCredentials(any())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(validLoginRequest))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("Should get role by email from repository")
    void shouldGetRoleByEmailFromRepository() {
        // Given
        when(userRepository.findRoleByEmail("test@example.com"))
            .thenReturn(Optional.of("MANAGER"));

        // When
        String role = authService.getRoleByEmail("test@example.com");

        // Then
        assertThat(role).isEqualTo("MANAGER");
    }

    @Test
    @DisplayName("Should return default role when user has no role")
    void shouldReturnDefaultRoleWhenUserHasNoRole() {
        // Given
        when(userRepository.findRoleByEmail("test@example.com"))
            .thenReturn(Optional.empty());

        // When
        String role = authService.getRoleByEmail("test@example.com");

        // Then
        assertThat(role).isEqualTo("EMPLOYEE");
    }
}
