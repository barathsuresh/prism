package com.prism.prism_auth.dto;

import java.time.LocalDateTime;

import com.prism.prism_auth.model.enums.UserRole;
import com.prism.prism_auth.model.enums.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user response.
 * 
 * Used when returning user information (no sensitive data like password).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    /**
     * User ID
     */
    private String id;

    /**
     * User's email
     */
    private String email;

    /**
     * User's username
     */
    private String username;

    /**
     * User's first name
     */
    private String firstName;

    /**
     * User's last name
     */
    private String lastName;

    /**
     * Account status (ACTIVE, SUSPENDED, DELETED, etc.)
     */
    private UserStatus status;

    /**
     * User role (DEVELOPER or ADMIN)
     */
    private UserRole role;

    /**
     * Whether email is verified
     */
    private Boolean emailVerified;

    /**
     * When email was verified
     */
    private LocalDateTime emailVerifiedAt;

    /**
     * When account was created
     */
    private LocalDateTime createdAt;

    /**
     * When account was last updated
     */
    private LocalDateTime updatedAt;
}
