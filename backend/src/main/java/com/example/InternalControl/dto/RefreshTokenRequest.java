package com.example.InternalControl.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for refresh token requests.
 *
 * @author TriTacLe
 * @since 1.0
 */
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {}
