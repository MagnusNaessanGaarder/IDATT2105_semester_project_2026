package com.example.InternalControl.dto;

import lombok.Builder;

import java.util.List;

/**
 * DTO for authentication responses.
 * Contains JWT tokens and user info with organizations and roles.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Builder
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String email,
        String role,
        List<OrganizationRoleResponse> organizations
) {
    // Constructor without organizations (for backwards compatibility)
    public AuthResponse(String accessToken, String refreshToken, String email, String role) {
        this(accessToken, refreshToken, email, role, List.of());
    }
}
