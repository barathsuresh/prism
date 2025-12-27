package com.prism.prism_auth.model.enums;

/**
 * Enum representing granular permissions (scopes) for API keys.
 *
 * Scopes define what operations a developer app is allowed to perform.
 *
 * Examples:
 * - Admin Key: ALL scopes
 * - Upload Key: VIDEOS_UPLOAD, VIDEOS_READ
 * - View-Only Key: VIDEOS_READ
 */
public enum ApiKeyScope {
    // Video Read Operations
    VIDEOS_READ("videos:read"),

    // Video Write Operations
    VIDEOS_UPLOAD("videos:upload"),
    VIDEOS_UPDATE("videos:update"),
    VIDEOS_DELETE("videos:delete"),

    // Catalog Operations
    CATALOG_READ("catalog:read"),
    CATALOG_WRITE("catalog:write"),

    // Analytics
    ANALYTICS_READ("analytics:read"),

    // Key Management (for admins)
    KEYS_CREATE("keys:create"),
    KEYS_REVOKE("keys:revoke"),
    KEYS_LIST("keys:list"),

    // Internal Service-to-Service
    INTERNAL_SERVICE("internal:service");

    /**
     * The scope value used in documentation and API
     * Example: "videos:read", "videos:upload"
     */
    private final String value;

    /**
     * Constructor for ApiKeyScope enum
     * 
     * @param value the scope string value
     */
    ApiKeyScope(String value) {
        this.value = value;
    }

    /**
     * Get the scope value as a string
     * 
     * @return the scope value (e.g., "videos:read")
     */
    public String getValue() {
        return value;
    }
}
