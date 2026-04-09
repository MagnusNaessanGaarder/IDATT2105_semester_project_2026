package com.example.InternalControl.dto.auth.response;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * DTO for organization and role information in auth response.
 */
@Builder
public record OrganizationRoleResponse(
    Integer orgNumber,
    String orgName,
    String role,
    LocalDateTime joinedAt) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Integer orgNumber;
        private String orgName;
        private String role;
        private LocalDateTime joinedAt;

        public Builder orgNumber(Integer orgNumber) {
            this.orgNumber = orgNumber;
            return this;
        }

        public Builder orgName(String orgName) {
            this.orgName = orgName;
            return this;
        }

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder joinedAt(LocalDateTime joinedAt) {
            this.joinedAt = joinedAt;
            return this;
        }

        public OrganizationRoleResponse build() {
            return new OrganizationRoleResponse(orgNumber, orgName, role, joinedAt);
        }
    }
}
