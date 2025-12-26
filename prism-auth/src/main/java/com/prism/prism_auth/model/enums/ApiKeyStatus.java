package com.prism.prism_auth.model.enums;

/**
 * Enum representing the different statuses an API key can have.
 */
public enum ApiKeyStatus {
    ACTIVE, // Key is currently valid and can be used
    REVOKED, // Key has been explicitly revoked
    EXPIRED // Key has expired due to age or expiration policy
}
