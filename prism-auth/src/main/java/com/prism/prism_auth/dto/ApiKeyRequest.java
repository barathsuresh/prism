package com.prism.prism_auth.dto;

import java.util.List;

import com.prism.prism_auth.model.enums.ApiKeyScope;
import com.prism.prism_auth.model.enums.ApiKeyType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating an API key.
 * 
 * Validation rules:
 * - appId: required, valid app ID
 * - label: required, human-readable name
 * - type: required, one of LIVE/TEST/RESTRICTED
 * - scopes: required, at least one scope
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyRequest {

    @NotBlank(message = "App ID is required")
    private String appId;

    @NotBlank(message = "Key label is required")
    @Size(min = 1, max = 100, message = "Key label must be between 1-100 characters")
    private String label;

    @NotBlank(message = "Key type is required")
    private ApiKeyType type;

    /**
     * List of scopes (permissions) for this key
     * Example: [VIDEOS_UPLOAD, VIDEOS_READ, ANALYTICS_READ]
     */
    private List<ApiKeyScope> scopes;
}
