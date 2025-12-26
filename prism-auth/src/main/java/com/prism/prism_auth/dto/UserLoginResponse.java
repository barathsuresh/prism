package com.prism.prism_auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user login response.
 * 
 * Returned after successful authentication.
 * Contains JWT token and user information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoginResponse {

    /**
     * User ID (MongoDB ObjectId)
     */
    private String userId;

    /**
     * User's email
     */
    private String email;

    /**
     * User's username
     */
    private String username;

    /**
     * JWT token for subsequent API requests
     * Include this in header: Authorization: Bearer <jwtToken>
     */
    private String jwtToken;

    /**
     * Token expiration time in seconds
     * Example: 86400 (24 hours)
     */
    private long expiresIn;

    /**
     * Token type (usually "Bearer")
     */
    private String tokenType;
}
