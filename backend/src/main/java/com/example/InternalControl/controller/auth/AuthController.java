package com.example.InternalControl.controller.auth;

import com.example.InternalControl.dto.auth.AuthResponse;
import com.example.InternalControl.dto.auth.LoginRequest;
import com.example.InternalControl.dto.auth.RefreshTokenRequest;
import com.example.InternalControl.dto.auth.RegisterRequest;
import com.example.InternalControl.security.AuthenticationFacade;
import com.example.InternalControl.security.JwtService;
import com.example.InternalControl.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication.
 *
 * @author TriTacLe
 * @since 1.0
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication endpoints for login, register, and token refresh")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;
  private final JwtService jwtService;
  private final AuthenticationFacade authenticationFacade;

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

  @Operation(summary = "Logout", description = "Revokes the refresh token, effectively logging out the user from the current device")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Logout successful"),
      @ApiResponse(responseCode = "400", description = "Invalid refresh token")
  })
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
    authService.logout(request.refreshToken());
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Logout all devices", description = "Revokes all refresh tokens for the user, logging them out from all devices")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "All sessions terminated successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  @SecurityRequirement(name = "bearerAuth")
  @PostMapping("/logout-all")
  public ResponseEntity<Void> logoutAllDevices(@RequestHeader("Authorization") String authHeader) {
    String token = authHeader.replace("Bearer ", "");
    String email = jwtService.extractUsername(token);
    Long userId = jwtService.extractUserId(token);
    authService.logoutAllDevices(userId);
    return ResponseEntity.ok().build();
  }
}
