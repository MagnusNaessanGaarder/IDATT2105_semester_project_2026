package com.example.InternalControl.controller.auth;

import com.example.InternalControl.dto.auth.request.LoginRequest;
import com.example.InternalControl.dto.auth.request.RefreshTokenRequest;
import com.example.InternalControl.dto.auth.request.RegisterRequest;
import com.example.InternalControl.dto.auth.response.AuthResponse;
import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AuthController.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String REFRESH_URL = "/api/v1/auth/refresh";

    @Test
    void register_WithValidRequest_ReturnsCreated() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest(
                "Test User",
                "test@example.com",
                "+4712345678",
                "Password123!"
        );

        AuthResponse response = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .email("test@example.com")
                .role("EMPLOYEE")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void register_WithInvalidEmail_ReturnsBadRequest() throws Exception {
        // Given - invalid email format
        RegisterRequest request = new RegisterRequest(
                "Test User",
                "invalid-email",
                null,
                "Password123!"
        );

        // When & Then
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_WithShortPassword_ReturnsBadRequest() throws Exception {
        // Given - password too short
        RegisterRequest request = new RegisterRequest(
                "Test User",
                "test@example.com",
                null,
                "123"
        );

        // When & Then
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_WithBlankName_ReturnsBadRequest() throws Exception {
        // Given - blank display name
        RegisterRequest request = new RegisterRequest(
                "",
                "test@example.com",
                null,
                "Password123!"
        );

        // When & Then
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithValidCredentials_ReturnsOk() throws Exception {
        // Given
        LoginRequest request = new LoginRequest(
                "test@example.com",
                "Password123!"
        );

        AuthResponse response = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .email("test@example.com")
                .role("EMPLOYEE")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void login_WithInvalidEmailFormat_ReturnsBadRequest() throws Exception {
        // Given
        LoginRequest request = new LoginRequest(
                "invalid-email",
                "Password123!"
        );

        // When & Then
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithBlankPassword_ReturnsBadRequest() throws Exception {
        // Given
        LoginRequest request = new LoginRequest(
                "test@example.com",
                ""
        );

        // When & Then
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_WithValidToken_ReturnsOk() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        AuthResponse response = AuthResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .email("test@example.com")
                .role("EMPLOYEE")
                .build();

        when(authService.refreshToken("valid-refresh-token")).thenReturn(response);

        // When & Then
        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    void refresh_WithBlankToken_ReturnsBadRequest() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest("");

        // When & Then
        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_WithNullToken_ReturnsBadRequest() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest(null);

        // When & Then
        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
