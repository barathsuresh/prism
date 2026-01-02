package com.prism.prism_auth.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prism.prism_auth.dto.ApiKeyCreateResponse;
import com.prism.prism_auth.dto.ApiKeyRequest;
import com.prism.prism_auth.dto.ApiKeyResponse;
import com.prism.prism_auth.security.services.UserPrincipal;
import com.prism.prism_auth.service.ApiKeyService;
import com.prism.prism_auth.service.AppService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * API Key Management Controller
 * 
 * Handles CRUD operations for API keys
 * All endpoints require JWT authentication
 */
@RestController
@RequestMapping("/api/auth/api-keys")
@RequiredArgsConstructor
@Slf4j
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final AppService appService;

    /**
     * Create a new API key for an app
     * POST /api/auth/api-keys
     * Requires: JWT authentication
     *
     * @param request     API key creation request
     * @param userDetails Authenticated user
     * @return Created API key with plaintext secret (returned once only)
     */
    @PostMapping
    public ResponseEntity<ApiKeyCreateResponse> createApiKey(
            @Valid @RequestBody ApiKeyRequest request,
            @AuthenticationPrincipal UserPrincipal userDetails) {

        log.info("[APIKEY] Create API key request initiated - userId: {}, username: {}, appId: {}, appSlug: {}",
                userDetails.getId(), userDetails.getUsername(), request.getAppId(), request.getAppSlug());

        // Resolve appId from either appId or appSlug (owner-verified)
        String ownerId = userDetails.getId();
        String appId = request.getAppId();
        if ((appId == null || appId.isBlank()) && request.getAppSlug() != null && !request.getAppSlug().isBlank()) {
            log.debug("[APIKEY] Resolving appId from slug: {}", request.getAppSlug());
            var app = appService.getAppBySlug(request.getAppSlug(), ownerId);
            appId = app.getId();
            log.debug("[APIKEY] App resolved - appId: {}, appSlug: {}", appId, request.getAppSlug());
        }

        log.debug("[APIKEY] Creating API key - appId: {}, userId: {}, keyType: {}",
                appId, ownerId, request.getType());

        ApiKeyCreateResponse response = apiKeyService.createApiKey(
                appId,
                ownerId,
                userDetails.getUsername(),
                request);

        log.info("[APIKEY] API key created successfully - keyId: {}, appId: {}, userId: {}, keyType: {}",
                response.getId(), appId, ownerId, request.getType());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List all API keys for an app
     * GET /api/auth/api-keys
     * Requires: JWT authentication
     * Query param: appId
     * 
     * @param appId       The app ID
     * @param userDetails Authenticated user
     * @return List of API keys (secrets never included)
     */
    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> listApiKeys(
            String appId,
            @AuthenticationPrincipal UserPrincipal userDetails) {

        log.info("[APIKEY] List API keys request - appId: {}, userId: {}",
                appId, userDetails.getId());

        List<ApiKeyResponse> keys = apiKeyService.listApiKeys(appId, userDetails.getId());

        log.info("[APIKEY] API keys listed successfully - appId: {}, userId: {}, keyCount: {}",
                appId, userDetails.getId(), keys.size());

        return ResponseEntity.ok(keys);
    }

    /**
     * List all API keys for the current authenticated user across all apps
     * GET /api/auth/api-keys/me
     * Requires: JWT authentication
     * 
     * @param userDetails Authenticated user
     * @return List of all API keys owned by this user (secrets never included)
     */
    @GetMapping("/me")
    public ResponseEntity<List<ApiKeyResponse>> listMyApiKeys(
            @AuthenticationPrincipal UserPrincipal userDetails) {

        log.info("[APIKEY] List my API keys request - userId: {}, username: {}",
                userDetails.getId(), userDetails.getUsername());

        List<ApiKeyResponse> keys = apiKeyService.listUserApiKeys(userDetails.getUsername());

        log.info("[APIKEY] User API keys listed successfully - userId: {}, username: {}, keyCount: {}",
                userDetails.getId(), userDetails.getUsername(), keys.size());

        return ResponseEntity.ok(keys);
    }

    /**
     * Get a single API key details
     * GET /api/auth/api-keys/{keyId}
     * Requires: JWT authentication + ownership
     * Query param: appId
     * 
     * @param keyId       The key ID
     * @param appId       The app ID
     * @param userDetails Authenticated user
     * @return API key details (no secret)
     */
    @GetMapping("/{keyId}")
    public ResponseEntity<ApiKeyResponse> getApiKey(
            @PathVariable String keyId,
            String appId,
            @AuthenticationPrincipal UserPrincipal userDetails) {

        log.info("[APIKEY] Get API key request - keyId: {}, appId: {}, userId: {}",
                keyId, appId, userDetails.getId());

        ApiKeyResponse response = apiKeyService.getApiKey(keyId, appId, userDetails.getId());

        log.debug("[APIKEY] API key retrieved - keyId: {}, appId: {}, keyType: {}, userId: {}",
                keyId, appId, response.getType(), userDetails.getId());

        return ResponseEntity.ok(response);
    }

    /**
     * Revoke an API key
     * DELETE /api/auth/api-keys/{keyId}
     * Requires: JWT authentication + ownership
     * Query param: appId
     * 
     * @param keyId       The key ID
     * @param appId       The app ID
     * @param userDetails Authenticated user
     * @return 204 No Content
     */
    @DeleteMapping("/{keyId}")
    public ResponseEntity<Void> revokeApiKey(
            @PathVariable String keyId,
            String appId,
            @AuthenticationPrincipal UserPrincipal userDetails) {

        log.info("[APIKEY] Revoke API key request initiated - keyId: {}, appId: {}, userId: {}",
                keyId, appId, userDetails.getId());

        apiKeyService.revokeApiKey(keyId, appId, userDetails.getId());

        log.info("[APIKEY] API key revoked successfully - keyId: {}, appId: {}, userId: {}",
                keyId, appId, userDetails.getId());

        return ResponseEntity.noContent().build();
    }
}
