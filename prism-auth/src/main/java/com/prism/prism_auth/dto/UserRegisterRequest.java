package com.prism.prism_auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user registration request.
 * 
 * Validation rules:
 * - Email: valid email format
 * - Username: 3-20 alphanumeric + underscore/hyphen
 * - Password: min 8 chars, must contain uppercase, lowercase, digit, special
 * char
 * - Names: optional, max 50 chars if provided
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3-20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain letters, numbers, underscore, and hyphen")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$", message = "Password must contain uppercase, lowercase, digit, and special character (@$!%*?&)")
    private String password;

    @Size(max = 50, message = "First name must be max 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name must be max 50 characters")
    private String lastName;
}