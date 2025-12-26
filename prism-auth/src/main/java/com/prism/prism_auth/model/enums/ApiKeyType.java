package com.prism.prism_auth.model.enums;

/**
 * Enum representing the different types of API keys.
 */
public enum ApiKeyType {
    LIVE, // Production keys
    TEST, // Sandbox/Testing keys
    RESTRICTED // Limited scope keys
}
