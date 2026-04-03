package com.example.InternalControl.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for registration requests.
 *
 * @author TriTacLe
 * @since 1.0
 */
public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 67, message = "Name must be between 2 and 67 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        String phone,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {}
