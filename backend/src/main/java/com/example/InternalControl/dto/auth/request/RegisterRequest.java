package com.example.InternalControl.dto.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for user registration requests.
 * <p>
 * Contains all required and optional fields for creating a new user account.
 * Validation annotations ensure data integrity before processing.
 * <p>
 * Password requirements:
 * <ul>
 *   <li>Minimum 8 characters</li>
 *   <li>At least one digit (0-9)</li>
 *   <li>At least one lowercase letter (a-z)</li>
 *   <li>At least one uppercase letter (A-Z)</li>
 *   <li>At least one special character (@#$%^&+=!)</li>
 * </ul>
 *
 * @author TriTacLe
 * @version 1.0
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    /**
     * User's full name (first and last name).
     * <p>
     * Must be between 2 and 67 characters to accommodate
     * international naming conventions while preventing abuse.
     */
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 67, message = "Name must be between 2 and 67 characters")
    private String fullName;

    /**
     * User's email address.
     * <p>
     * Must be unique across the system. Used for login and notifications.
     * Validated against RFC 5322 email format.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    /**
     * User's phone number (optional).
     * <p>
     * Used for SMS notifications and two-factor authentication.
     * Should include country code (e.g., +47 for Norway).
     */
    private String phone;

    /**
     * User's password.
     * <p>
     * Must meet complexity requirements for security:
     * minimum length, mixed case, digits, and special characters.
     * Stored as bcrypt hash, never in plain text.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$",
             message = "Password must contain at least one digit, one lowercase, one uppercase, and one special character")
    private String password;
}
