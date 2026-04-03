package com.example.InternalControl.dto.auth;

import com.example.InternalControl.dto.user.OrganizationRoleResponse;
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
    public AuthResponse(String accessToken, String refreshToken, String email, String role) {
        this(accessToken, refreshToken, email, role, List.of());
    }
}
