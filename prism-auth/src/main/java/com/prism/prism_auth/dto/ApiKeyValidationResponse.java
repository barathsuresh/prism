package com.prism.prism_auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for API key validation response.
 * 
 * Returned by POST /api/auth/internal/validate endpoint.
 * 
 * On success:
 * {
 * "valid": true,
 * "apiKey": { ... full ApiKeyResponse object ... }
 * }
 * 
 * On failure:
 * {
 * "valid": false,
 * "message": "API key not found"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyValidationResponse {

    private boolean valid; // true if key is valid and active

    private ApiKeyResponse apiKey; // Full key details if valid

    private String message; // Error message if invalid
}
