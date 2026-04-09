package com.example.InternalControl.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * User registration request with validated password requirements.
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
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$",
                 message = "Password must contain at least one digit, one lowercase, one uppercase, and one special character")
        String password
) {}
