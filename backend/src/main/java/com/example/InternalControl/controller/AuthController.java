package com.example.InternalControl.controller;

import com.example.InternalControl.dto.AuthResponse;
import com.example.InternalControl.dto.LoginRequest;
import com.example.InternalControl.dto.RefreshTokenRequest;
import com.example.InternalControl.dto.RegisterRequest;
import com.example.InternalControl.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication.
 *
 * @author TriTacLe
 * @since 1.0
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication endpoints for login, register, and token refresh")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register new user", description = "Creates a new user account and returns JWT tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or email already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Login", description = "Authenticates user and returns JWT tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or account locked")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Refresh token", description = "Refreshes access token using refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }
}
