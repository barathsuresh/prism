package com.prism.prism_auth.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.prism.prism_auth.model.enums.ApiKeyScope;
import com.prism.prism_auth.model.enums.ApiKeyStatus;
import com.prism.prism_auth.model.enums.ApiKeyType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for API key response.
 * 
 * Returned when creating or querying API keys.
 * NOTE: The raw key value is only returned during creation, never again.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyResponse {

    /**
     * Key ID (MongoDB ObjectId)
     */
    private String id;

    /**
     * App ID this key belongs to
     */
    private String appId;

    /**
     * Owner's username (for display convenience)
     */
    private String ownerUserName;

    /**
     * Human-readable label
     * Example: "Upload Key", "Admin Key"
     */
    private String label;

    /**
     * Key type: LIVE, TEST, or RESTRICTED
     */
    private ApiKeyType type;

    /**
     * Key prefix for display (first 12 chars)
     * Example: "pk_test_abc123xyz..."
     * The full key is only shown once during creation
     */
    private String keyPrefix;

    /**
     * List of scopes (permissions) this key has
     * Example: [VIDEOS_UPLOAD, VIDEOS_READ, ANALYTICS_READ]
     */
    private List<ApiKeyScope> scopes;

    /**
     * Current status: ACTIVE, REVOKED, or EXPIRED
     */
    private ApiKeyStatus status;

    /**
     * When the key was created
     */
    private LocalDateTime createdAt;

    /**
     * When the key was last used
     */
    private LocalDateTime lastUsedAt;

    /**
     * Total number of API requests made with this key
     */
    private Long totalRequests;

    /**
     * When the key was revoked (if applicable)
     */
    private LocalDateTime revokedAt;

    /**
     * Reason for revocation (if applicable)
     */
    private String revokedReason;
}
