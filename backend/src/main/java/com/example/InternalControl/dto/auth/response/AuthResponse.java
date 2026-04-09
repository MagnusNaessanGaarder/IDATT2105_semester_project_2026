package com.example.InternalControl.dto.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

import com.example.InternalControl.dto.auth.response.OrganizationRoleResponse;

/**
 * DTO for authentication responses. Contains JWT tokens and user info with
 * organizations and roles.
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String accessToken;
        private String refreshToken;
        private String email;
        private String role;
        private List<OrganizationRoleResponse> organizations = List.of();

        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public Builder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder organizations(List<OrganizationRoleResponse> organizations) {
            this.organizations = organizations;
            return this;
        }

        public AuthResponse build() {
            return new AuthResponse(accessToken, refreshToken, email, role, organizations);
        }
    }
}
