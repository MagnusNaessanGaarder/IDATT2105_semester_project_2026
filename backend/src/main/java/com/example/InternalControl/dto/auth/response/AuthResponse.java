package com.example.InternalControl.dto.auth.response;

import lombok.Builder;

import java.util.List;

import com.example.InternalControl.dto.auth.response.OrganizationRoleResponse;

/**
 * DTO for authentication responses.
 * Contains JWT tokens and user info with organizations and roles.
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
