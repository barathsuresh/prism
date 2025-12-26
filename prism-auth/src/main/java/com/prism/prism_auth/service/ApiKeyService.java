package com.prism.prism_auth.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prism.prism_auth.dto.ApiKeyCreateResponse;
import com.prism.prism_auth.dto.ApiKeyRequest;
import com.prism.prism_auth.dto.ApiKeyResponse;
import com.prism.prism_auth.model.ApiKey;
import com.prism.prism_auth.model.enums.ApiKeyStatus;
import com.prism.prism_auth.repository.ApiKeyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing API Keys.
 * 
 * Responsible for:
 * - Creating new API keys with proper hashing
 * - Validating API keys for service-to-service requests
 * - Revoking keys
 * - Listing keys for developers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String KEY_PREFIX = "pk_";
    private static final String KEY_SECRET_PREFIX = "sk_";
    private static final int PREFIX_LENGTH = 16;
    private static final int SECRET_LENGTH = 32;

    /**
     * Create a new API key for an app
     * 
     * @param appId   The app ID
     * @param ownerId The developer/owner ID
     * @param request API key creation request
     * @return ApiKeyCreateResponse with the plaintext key (returned once only)
     */
    @Transactional
    public ApiKeyCreateResponse createApiKey(String appId, String ownerId, ApiKeyRequest request) {
        // Generate unique key prefix and secret
        String keyPrefix = generateKeyPrefix();
        String keySecret = generateKeySecret();
        String fullKey = keyPrefix + ":" + keySecret;

        // Hash the secret for storage
        String secretHash = passwordEncoder.encode(keySecret);

        // Create API key entity
        ApiKey apiKey = ApiKey.builder()
                .appId(appId)
                .ownerId(ownerId)
                .label(request.getLabel())
                .keyPrefix(keyPrefix)
                .keyHash(secretHash)
                .type(request.getType())
                .scopes(request.getScopes())
                .status(ApiKeyStatus.ACTIVE)
                .expiresAt(request.getExpiresAt())
                .totalRequests(0L)
                .build();

        ApiKey saved = apiKeyRepository.save(apiKey);
        log.info("Created API key {} for app {}", keyPrefix, appId);

        // Return the key ONCE - never return plaintext again
        return ApiKeyCreateResponse.builder()
                .id(saved.getId())
                .key(fullKey)
                .keyPrefix(keyPrefix)
                .appId(saved.getAppId())
                .label(saved.getLabel())
                .type(saved.getType())
                .scopes(saved.getScopes())
                .build();
    }

    /**
     * Validate an API key for service-to-service requests
     * Used by other services (Upload, Catalog, Stream) to verify API keys
     * 
     * @param keyPrefix The public prefix
     * @param keySecret The secret part
     * @param scope     The requested scope (string value like "videos:upload")
     * @return ApiKeyResponse if valid, throws exception if invalid
     */
    public ApiKeyResponse validateApiKey(String keyPrefix, String keySecret, String scope) {
        // Delegate to multi-scope validator with single scope
        java.util.List<String> scopes = (scope == null || scope.isBlank()) ? java.util.List.of()
                : java.util.List.of(scope);
        return validateApiKey(keyPrefix, keySecret, scopes);
    }

    /**
     * Validate an API key with optional required scopes (must have all)
     */
    public ApiKeyResponse validateApiKey(String keyPrefix, String keySecret, java.util.List<String> requiredScopes) {
        // Find key by prefix, then verify the secret using BCrypt
        ApiKey apiKey = apiKeyRepository.findByKeyPrefix(keyPrefix)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));

        // Check if key is active
        if (!apiKey.isValidAndActive()) {
            throw new IllegalArgumentException("API key is revoked or expired");
        }

        // Verify the secret against the stored hash
        if (!passwordEncoder.matches(keySecret, apiKey.getKeyHash())) {
            log.warn("Invalid secret for key prefix: {}", keyPrefix);
            throw new IllegalArgumentException("Invalid API key credentials");
        }

        // Verify required scopes: key must contain ALL requested scope values
        if (requiredScopes != null && !requiredScopes.isEmpty()) {
            java.util.List<String> keyScopeValues = (apiKey.getScopes() == null) ? java.util.List.of()
                    : apiKey.getScopes().stream().map(s -> s.getValue()).toList();
            java.util.List<String> missing = requiredScopes.stream()
                    .filter(s -> !keyScopeValues.contains(s))
                    .toList();
            if (!missing.isEmpty()) {
                log.warn("Insufficient scopes for key: {} (missing: {}, required: {}, granted: {})",
                        keyPrefix, missing, requiredScopes, keyScopeValues);
                String msg = String.format(
                        "Insufficient scopes. Missing: %s. Required: %s. Granted: %s",
                        missing, requiredScopes, keyScopeValues);
                throw new IllegalArgumentException(msg);
            }
        }

        // Update last usage
        apiKey.updateLastUsage(null); // IP can be added if needed
        apiKeyRepository.save(apiKey);

        log.debug("API key validated: {}", keyPrefix);

        // Return key info for the requesting service
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .appId(apiKey.getAppId())
                .label(apiKey.getLabel())
                .keyPrefix(apiKey.getKeyPrefix())
                .type(apiKey.getType())
                .scopes(apiKey.getScopes())
                .status(apiKey.getStatus())
                .createdAt(apiKey.getCreatedAt())
                .lastUsedAt(apiKey.getLastUsedAt())
                .totalRequests(apiKey.getTotalRequests())
                .build();
    }

    /**
     * List all API keys for an app
     * 
     * @param appId   The app ID
     * @param ownerId The developer ID (for ownership verification)
     * @return List of API keys (secrets are never included)
     */
    public List<ApiKeyResponse> listApiKeys(String appId, String ownerId) {
        List<ApiKey> keys = apiKeyRepository.findByAppId(appId);

        // Verify ownership - all keys for an app belong to the same owner
        if (!keys.isEmpty() && !keys.get(0).getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("You don't have permission to view these keys");
        }

        return keys.stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Revoke an API key
     * 
     * @param keyId   The key ID
     * @param appId   The app ID (for ownership verification)
     * @param ownerId The developer ID (for ownership verification)
     */
    @Transactional
    public void revokeApiKey(String keyId, String appId, String ownerId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));

        // Verify ownership
        if (!apiKey.getAppId().equals(appId) || !apiKey.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("You don't have permission to revoke this key");
        }

        // Soft delete
        apiKey.setStatus(ApiKeyStatus.REVOKED);
        apiKey.setRevokedAt(LocalDateTime.now());
        apiKey.setRevokedReason("Manually revoked by developer");

        apiKeyRepository.save(apiKey);
        log.info("Revoked API key: {}", apiKey.getKeyPrefix());
    }

    /**
     * Get a single API key by ID
     * 
     * @param keyId   The key ID
     * @param appId   The app ID (for ownership verification)
     * @param ownerId The developer ID (for ownership verification)
     * @return API key response
     */
    public ApiKeyResponse getApiKey(String keyId, String appId, String ownerId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));

        // Verify ownership
        if (!apiKey.getAppId().equals(appId) || !apiKey.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("You don't have permission to view this key");
        }

        return mapToResponse(apiKey);
    }

    /**
     * Map ApiKey entity to response DTO (secrets never included)
     */
    private ApiKeyResponse mapToResponse(ApiKey apiKey) {
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .appId(apiKey.getAppId())
                .label(apiKey.getLabel())
                .keyPrefix(apiKey.getKeyPrefix())
                .type(apiKey.getType())
                .scopes(apiKey.getScopes())
                .status(apiKey.getStatus())
                .createdAt(apiKey.getCreatedAt())
                .lastUsedAt(apiKey.getLastUsedAt())
                .totalRequests(apiKey.getTotalRequests())
                .revokedAt(apiKey.getRevokedAt())
                .build();
    }

    /**
     * Generate a unique key prefix
     */
    private String generateKeyPrefix() {
        return KEY_PREFIX + UUID.randomUUID().toString().substring(0, PREFIX_LENGTH);
    }

    /**
     * Generate a secure key secret
     */
    private String generateKeySecret() {
        return KEY_SECRET_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, SECRET_LENGTH);
    }
}
