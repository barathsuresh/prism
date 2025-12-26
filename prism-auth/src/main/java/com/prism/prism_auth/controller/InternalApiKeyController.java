package com.prism.prism_auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prism.prism_auth.dto.ApiKeyResponse;
import com.prism.prism_auth.dto.ApiKeyValidationResponse;
import com.prism.prism_auth.service.ApiKeyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Internal API Key Validation Controller
 * 
 * Handles API key validation for service-to-service requests.
 * These endpoints are NOT protected by JWT - they allow other services
 * to validate API keys without authentication.
 * 
 * Security Note: This endpoint should only be accessible from internal
 * services within the network (API Gateway, load balancer rules, etc).
 */
@RestController
@RequestMapping("/api/auth/internal")
@RequiredArgsConstructor
@Slf4j
public class InternalApiKeyController {

    private final ApiKeyService apiKeyService;

    /**
     * Validate an API key for service-to-service requests
     * 
     * POST /api/auth/internal/validate
     * NO JWT required - for service-to-service communication
     * 
     * Used by: Upload Service, Catalog Service, Stream Service, Transcoder Service
     * 
     * Request headers:
     * X-API-Key: pk_abc123...:sk_secret...
     * 
     * Optional query param:
     * ?scope=videos:upload
     * 
     * Response on success:
     * {
     * "valid": true,
     * "apiKey": { ... full key details ... }
     * }
     * 
     * Response on failure:
     * {
     * "valid": false,
     * "message": "API key not found"
     * }
     * 
     * @param apiKey Full API key in format "prefix:secret"
     * @param scope  Optional scope to validate
     * @return Validation response with key details if valid
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiKeyValidationResponse> validateApiKey(
            @RequestHeader(value = "X-API-Key", required = true) String apiKey,
            @RequestParam(value = "scope", required = false) java.util.List<String> scope,
            @RequestParam(value = "scopes", required = false) String scopesParam) {

        try {
            // Parse the API key (format: "pk_xxx:sk_xxx")
            String[] keyParts = apiKey.split(":");
            if (keyParts.length != 2) {
                log.warn("Invalid API key format");
                return ResponseEntity.status(401).body(ApiKeyValidationResponse.builder()
                        .valid(false)
                        .message("Invalid API key format. Expected format: prefix:secret")
                        .build());
            }

            String keyPrefix = keyParts[0];
            String keySecret = keyParts[1];

            log.debug("Validating API key: {}", keyPrefix);

            // Build required scopes list (supports repeated scope params and
            // comma-separated scopes)
            java.util.List<String> requiredScopes = new java.util.ArrayList<>();
            if (scope != null) {
                requiredScopes.addAll(scope);
            }
            if (scopesParam != null && !scopesParam.isBlank()) {
                for (String s : scopesParam.split(",")) {
                    String trimmed = s.trim();
                    if (!trimmed.isEmpty()) {
                        requiredScopes.add(trimmed);
                    }
                }
            }

            // Validate the key with required scopes (if any)
            ApiKeyResponse apiKeyResponse = apiKeyService.validateApiKey(
                    keyPrefix,
                    keySecret,
                    requiredScopes);

            log.info("API key validation successful: {}", keyPrefix);

            // Return success response
            return ResponseEntity.ok(ApiKeyValidationResponse.builder()
                    .valid(true)
                    .apiKey(apiKeyResponse)
                    .build());

        } catch (IllegalArgumentException e) {
            log.warn("API key validation failed: {}", e.getMessage());

            // Return failure response
            return ResponseEntity.status(401).body(ApiKeyValidationResponse.builder()
                    .valid(false)
                    .message(e.getMessage())
                    .build());

        } catch (Exception e) {
            log.error("Unexpected error during API key validation", e);

            return ResponseEntity.status(500).body(ApiKeyValidationResponse.builder()
                    .valid(false)
                    .message("Internal server error")
                    .build());
        }
    }
}
