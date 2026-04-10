package com.example.InternalControl.controller.auth;

import com.example.InternalControl.dto.auth.request.LoginRequest;
import com.example.InternalControl.dto.auth.request.RefreshTokenRequest;
import com.example.InternalControl.dto.auth.request.RegisterRequest;
import com.example.InternalControl.dto.auth.response.AuthResponse;
import com.example.InternalControl.service.auth.AuthService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST controller for authentication operations.
 * <p>
 * Handles user registration, login, and token refresh.
 * Implements rate limiting to prevent brute force attacks.
 * All endpoints are public (no authentication required).
 * <p>
 * Rate limits:
 * <ul>
 * <li>Login: 5 attempts per minute per email</li>
 * <li>Refresh: 10 attempts per minute per token</li>
 * </ul>
 *
 * @author TriTacLe
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication endpoints for login, register, and token refresh")
public class AuthController {
  private final AuthService authService;
  private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
  private final Map<String, Bucket> refreshBuckets = new ConcurrentHashMap<>();

  /**
   * Constructs AuthController with required dependencies.
   *
   * @param authService the authentication service for business logic
   */
  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /**
   * Registers a new user account.
   * <p>
   * Creates a new user with default EMPLOYEE role in the specified organization.
   * Returns JWT access and refresh tokens upon successful registration.
   *
   * @param request the registration request containing email, password, full
   *                name, and phone
   * @return ResponseEntity with AuthResponse containing JWT tokens (HTTP 201)
   * @throws IllegalArgumentException if email already exists or input validation
   *                                  fails
   */
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

  /**
   * Authenticates a user and returns JWT tokens.
   * <p>
   * Validates credentials, checks account status (not locked/deactivated),
   * and returns access and refresh tokens.
   * <p>
   * Rate limited: 5 attempts per minute per email address.
   *
   * @param request the login request containing email and password
   * @return ResponseEntity with AuthResponse containing JWT tokens (HTTP 200)
   * @throws IllegalStateException    if too many login attempts (rate limited)
   * @throws IllegalArgumentException if credentials are invalid or account is
   *                                  locked
   */
  @Operation(summary = "Login", description = "Authenticates user and returns JWT tokens")
  @ApiResponses({

      @ApiResponse(responseCode = "200", description = "Login successful"),
      @ApiResponse(responseCode = "401", description = "Invalid credentials or account locked"),
      @ApiResponse(responseCode = "429", description = "Too many login attempts")
  })
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    Bucket bucket = loginBuckets.computeIfAbsent(request.getEmail(), k -> createLoginBucket());
    if (!bucket.tryConsume(1)) {
      throw new IllegalStateException("Too many login attempts. Please try again later.");
    }
    AuthResponse response = authService.login(request);
    return ResponseEntity.ok(response);
  }

  /**
   * Refreshes the access token using a valid refresh token.
   * <p>
   * Returns new access and refresh tokens. The old refresh token
   * is invalidated after use.
   * <p>
   * Rate limited: 10 attempts per minute per refresh token.
   *
   * @param request the refresh token request containing the refresh token
   * @return ResponseEntity with AuthResponse containing new JWT tokens (HTTP 200)
   * @throws IllegalStateException    if too many refresh attempts (rate limited)
   * @throws IllegalArgumentException if refresh token is invalid or expired
   */
  @Operation(summary = "Refresh token", description = "Refreshes access token using refresh token")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
      @ApiResponse(responseCode = "401", description = "Invalid refresh token"),
      @ApiResponse(responseCode = "429", description = "Too many refresh attempts")
  })
  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
    Bucket bucket = refreshBuckets.computeIfAbsent(request.refreshToken(), k -> createRefreshBucket());
    if (!bucket.tryConsume(1)) {
      throw new IllegalStateException("Too many refresh attempts. Please try again later.");
    }
    AuthResponse response = authService.refreshToken(request.refreshToken());
    return ResponseEntity.ok(response);
  }

  /**
   * Creates a rate limit bucket for login attempts.
   * <p>
   * Configuration: 5 attempts per minute per email address.
   *
   * @return configured Bucket for rate limiting
   */
  private Bucket createLoginBucket() {
    Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
    return Bucket.builder()
        .addLimit(limit)
        .build();
  }

  /**
   * Creates a rate limit bucket for token refresh attempts.
   * <p>
   * Configuration: 10 attempts per minute per refresh token.
   *
   * @return configured Bucket for rate limiting
   */
  private Bucket createRefreshBucket() {
    Bandwidth limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)));
    return Bucket.builder()
        .addLimit(limit)
        .build();
  }
}
