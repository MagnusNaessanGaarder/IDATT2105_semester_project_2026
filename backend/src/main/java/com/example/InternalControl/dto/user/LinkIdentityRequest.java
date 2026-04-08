package com.example.InternalControl.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for linking external identity request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkIdentityRequest {

    @NotBlank(message = "Provider name is required")
    private String providerName;

    @NotBlank(message = "Provider user ID is required")
    private String providerUserId;

    private String providerEmail;
}
