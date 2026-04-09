package com.example.InternalControl.dto.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

import com.example.InternalControl.dto.auth.response.OrganizationRoleResponse;

/**
 * DTO for authentication responses.
 * Contains JWT tokens and user information including organizations and roles.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Schema(name = "AuthResponse", description = "Authentication response with JWT tokens and user details")
@Builder
public record AuthResponse(
        @Schema(description = "JWT access token for API requests", required = true)
        String accessToken,
        @Schema(description = "JWT refresh token for obtaining new access tokens", required = true)
        String refreshToken,
        @Schema(description = "Authenticated user's email address", required = true)
        String email,
        @Schema(description = "Primary role of the authenticated user", required = true)
        String role,
        @Schema(description = "List of organizations the user belongs to with their roles")
        List<OrganizationRoleResponse> organizations
        ) {

    // Constructor without organizations (for backwards compatibility)
    public AuthResponse(String accessToken, String refreshToken, String email, String role) {
        this(accessToken, refreshToken, email, role, List.of());
    }
}
