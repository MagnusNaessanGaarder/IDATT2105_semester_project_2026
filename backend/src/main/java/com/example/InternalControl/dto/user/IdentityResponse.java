package com.example.InternalControl.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for external identity response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentityResponse {

    private Long identityId;
    private Long userId;
    private String providerName;
    private String providerUserId;
    private String providerEmail;
    private LocalDateTime createdAt;
}
