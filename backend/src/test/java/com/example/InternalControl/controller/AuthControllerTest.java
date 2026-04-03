package com.example.InternalControl.controller;

import com.example.InternalControl.dto.auth.AuthResponse;
import com.example.InternalControl.dto.auth.LoginRequest;
import com.example.InternalControl.dto.auth.RefreshTokenRequest;
import com.example.InternalControl.dto.auth.RegisterRequest;
import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.InternalControl.controller.auth.AuthController;

/**
 * Unit tests for AuthController.
 *
 * @author TriTacLe
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController controller;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void register_WithValidRequest_ReturnsCreated() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("Test User", "test@example.com", "1234567890", "Password123!");
        AuthResponse response = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .email("test@example.com")
                .role("USER")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void login_WithValidCredentials_ReturnsTokens() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "Password123!");
        AuthResponse response = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .email("test@example.com")
                .role("USER")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void refreshToken_WithValidToken_ReturnsNewTokens() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        AuthResponse response = AuthResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .email("test@example.com")
                .role("USER")
                .build();

        when(authService.refreshToken("valid-refresh-token")).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    void logout_WithValidToken_ReturnsOk() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        // When & Then
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void logoutAllDevices_WithValidAuthHeader_ReturnsOk() throws Exception {
        // Given
        String authHeader = "Bearer valid-token";
        String token = "valid-token";

        when(jwtService.extractUsername(token)).thenReturn("test@example.com");
        when(jwtService.extractUserId(token)).thenReturn(1L);

        // When & Then
        mockMvc.perform(post("/api/auth/logout-all")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk());
    }

    @Test
    void register_WithInvalidEmail_ReturnsBadRequest() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("Test User", "invalid-email", "1234567890", "Password123!");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithBlankPassword_ReturnsBadRequest() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
