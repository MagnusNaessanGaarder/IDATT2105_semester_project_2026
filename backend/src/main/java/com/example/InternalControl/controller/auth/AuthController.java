package com.example.InternalControl.controller.auth;

import com.example.InternalControl.dto.AuthResponse;
import com.example.InternalControl.dto.LoginRequest;
import com.example.InternalControl.dto.RefreshTokenRequest;
import com.example.InternalControl.dto.RegisterRequest;
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

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication endpoints for login, register, and token refresh")
public class AuthController {
    private final AuthService authService;
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> refreshBuckets = new ConcurrentHashMap<>();

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
            @ApiResponse(responseCode = "401", description = "Invalid credentials or account locked"),
            @ApiResponse(responseCode = "429", description = "Too many login attempts")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Bucket bucket = loginBuckets.computeIfAbsent(request.email(), k -> createLoginBucket());
        if (!bucket.tryConsume(1)) {
            throw new IllegalStateException("Too many login attempts. Please try again later.");
        }
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

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

    private Bucket createLoginBucket() {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket createRefreshBucket() {
        Bandwidth limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
