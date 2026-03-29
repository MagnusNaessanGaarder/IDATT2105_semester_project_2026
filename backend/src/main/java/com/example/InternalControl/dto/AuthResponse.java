package com.example.InternalControl.dto;

/**
 * DTO for authentication responses.
 * Contains JWT tokens and user info.
 *
 * @author TriTacLe
 * @since 1.0
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String email,
        String role
) {}
