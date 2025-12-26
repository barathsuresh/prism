package com.prism.prism_auth.dto;

import com.prism.prism_auth.model.enums.ApiKeyScope;
import com.prism.prism_auth.model.enums.ApiKeyType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for API key creation response with raw key.
 * 
 * This is ONLY returned once during key creation.
 * The raw key is never stored or shown again.
 * Developers must save this key securely (e.g., environment variable).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyCreateResponse {

    /**
     * Key ID (MongoDB ObjectId)
     */
    private String id;

    /**
     * THE ACTUAL API KEY (raw value)
     * ⚠️ CRITICAL: This is shown ONLY ONCE
     * Developer must copy and save this immediately
     * Example: "pk_test_1a2b3c4d5e6f7g8h9i0j"
     */
    private String key;

    /**
     * App ID this key belongs to
     */
    private String appId;

    /**
     * Human-readable label
     */
    private String label;

    /**
     * Key type
     */
    private ApiKeyType type;

    /**
     * Key prefix (for identification)
     * Example: "pk_test_1a2b..."
     */
    private String keyPrefix;

    /**
     * Scopes/permissions
     */
    private java.util.List<ApiKeyScope> scopes;

    /**
     * ⚠️ WARNING MESSAGE
     * Reminds developer to save the key
     */
    @Builder.Default
    private String warningMessage = "⚠️  IMPORTANT: Save this key in a secure location. You will not be able to view it again. If you lose this key, you'll need to create a new one.";
}
