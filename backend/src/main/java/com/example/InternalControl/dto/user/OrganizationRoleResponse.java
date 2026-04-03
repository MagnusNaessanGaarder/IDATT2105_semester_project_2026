package com.example.InternalControl.dto.user;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * DTO for organization and role information in auth response.
 *
 * @author TriTacLe
 * @since 1.0
 */
@Builder
public record OrganizationRoleResponse(
    Integer orgNumber,
    String orgName,
    String role,
    LocalDateTime joinedAt) {
}
