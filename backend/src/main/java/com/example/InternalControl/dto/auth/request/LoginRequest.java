package com.example.InternalControl.dto.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for login requests.
 */
@Schema(name = "LoginRequest", description = "Credentials for user login")
public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Schema(description = "User email address", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,
        @NotBlank(message = "Password is required")
        @Schema(description = "User password", example = "SecurePassword123!", requiredMode = Schema.RequiredMode.REQUIRED)
        String password
        ) {

}
