package com.example.InternalControl.dto;

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
        LocalDateTime joinedAt
) {}
