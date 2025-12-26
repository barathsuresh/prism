package com.prism.prism_auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.prism.prism_auth.model.enums.ApiKeyScope;
import com.prism.prism_auth.model.enums.ApiKeyStatus;
import com.prism.prism_auth.model.enums.ApiKeyType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ApiKey entity representing credentials for a developer app.
 *
 * API keys are the primary mechanism for app-to-Prism authentication.
 * They are stored as hashes (like passwords) for security.
 *
 * Workflow:
 * 1. Developer creates an API key for their app and selects scopes
 * (permissions)
 * 2. Prism generates a random key string and returns it ONCE to the developer
 * 3. Prism stores only the hash of the key
 * 4. For subsequent requests, developer includes the raw key in header
 * (X-API-KEY)
 * 5. Gateway validates by hashing the incoming key and comparing to stored hash
 */
@Document(collection = "api_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndexes({
        @CompoundIndex(name = "app_status_idx", def = "{'appId': 1, 'status': 1}"),
        @CompoundIndex(name = "owner_app_idx", def = "{'ownerId': 1, 'appId': 1}")
})
public class ApiKey {

    /**
     * MongoDB ObjectId (auto-generated)
     */
    @Id
    private String id;

    /**
     * Hash of the actual API key (using BCrypt or similar)
     * NEVER store the plaintext key
     * This hash is compared against hashed incoming keys for validation
     */
    private String keyHash;

    /**
     * Prefix of the key for display purposes (e.g., "pk_test_abc123xyz...")
     * Helps developers identify which key is which without exposing the full secret
     */
    private String keyPrefix;

    /**
     * Encrypted last 12 characters of the key
     * Used for additional security verification
     */
    private String keySecret;

    /**
     * Which app this key belongs to
     * Indexed for quick lookups
     */
    @Indexed
    private String appId;

    /**
     * The user ID who created/owns this key
     * Useful for audit trail
     */
    private String ownerId;

    /**
     * Human-readable label (e.g., "Upload Key", "Admin Key", "Mobile App Key")
     * Helps developer identify the purpose of this key
     */
    private String label;

    /**
     * Key type: LIVE (production), TEST (sandbox), RESTRICTED (limited)
     */
    private ApiKeyType type;

    /**
     * List of scopes (permissions) this key has
     * Example: [VIDEOS_UPLOAD, VIDEOS_READ, ANALYTICS_READ]
     * Empty list means no permissions (essentially useless)
     */
    private List<ApiKeyScope> scopes;

    /**
     * Current status of the key: ACTIVE, REVOKED, or EXPIRED
     */
    private ApiKeyStatus status;

    /**
     * When the key was revoked (if status is REVOKED)
     */
    private LocalDateTime revokedAt;

    /**
     * Reason for revocation (for audit purposes)
     * Example: "Compromised", "No longer needed", "Rotated"
     */
    private String revokedReason;

    /**
     * Optional: When this key expires (if null, never expires)
     */
    private LocalDateTime expiresAt;

    /**
     * When was this key last used
     * Useful for identifying stale keys
     */
    private LocalDateTime lastUsedAt;

    /**
     * IP address of the last request using this key
     * Can be used for anomaly detection
     */
    private String lastUsedIp;

    /**
     * Total number of requests made with this key
     * Useful for analytics
     */
    private Long totalRequests;

    /**
     * Audit fields - automatically managed
     */
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Helper method to check if key is currently valid and active
     */
    public boolean isValidAndActive() {
        if (status != ApiKeyStatus.ACTIVE) {
            return false;
        }
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return false;
        }
        return true;
    }

    /**
     * Helper method to check if key has a specific scope (permission)
     */
    public boolean hasScope(ApiKeyScope scope) {
        return scopes != null && scopes.contains(scope);
    }

    /**
     * Helper method to check if key has all required scopes
     */
    public boolean hasAllScopes(List<ApiKeyScope> requiredScopes) {
        if (scopes == null || requiredScopes == null) {
            return false;
        }
        return scopes.containsAll(requiredScopes);
    }

    /**
     * Increment request counter
     */
    public void incrementRequestCount() {
        if (this.totalRequests == null) {
            this.totalRequests = 0L;
        }
        this.totalRequests++;
    }

    /**
     * Update last used timestamp and IP
     */
    public void updateLastUsage(String ip) {
        this.lastUsedAt = LocalDateTime.now();
        this.lastUsedIp = ip;
        incrementRequestCount();
    }
}
