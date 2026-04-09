package com.example.InternalControl.dto.auth.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for token refresh requests.
 * Contains the refresh token used to obtain a new access token.
 *
 * @author TriTacLe
 * @since 1.0
 */
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {}
