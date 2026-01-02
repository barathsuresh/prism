package com.prism.prism_auth.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prism.prism_auth.dto.AppRequest;
import com.prism.prism_auth.dto.AppResponse;
import com.prism.prism_auth.dto.AppUpdateRequest;
import com.prism.prism_auth.model.App;
import com.prism.prism_auth.security.services.UserPrincipal;
import com.prism.prism_auth.service.AppService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * App Management Controller
 * Handles CRUD operations for developer applications
 * All endpoints require authentication (JWT)
 */
@RestController
@RequestMapping("/api/auth/apps")
@RequiredArgsConstructor
@Slf4j
public class AppController {

    private final AppService appService;

    /**
     * Create a new app
     * POST /api/apps
     * Requires: JWT authentication
     *
     * @param request     App creation request
     * @param userDetails Authenticated user
     * @return Created app details
     */
    @PostMapping
    public ResponseEntity<AppResponse> createApp(
            @Valid @RequestBody AppRequest request,
            @AuthenticationPrincipal UserPrincipal userDetails) {

        log.info("[APP] Create app request initiated - userId: {}, appName: {}",
                userDetails.getId(), request.getName());

        log.debug("[APP] Creating app - name: {}, description: {}, userId: {}",
                request.getName(), request.getDescription(), userDetails.getId());

        App app = appService.createApp(request, userDetails.getId());

        log.info("[APP] App created successfully - appId: {}, appName: {}, slug: {}, userId: {}",
                app.getId(), app.getName(), app.getSlug(), userDetails.getId());

        return ResponseEntity.ok(mapToResponse(app));
    }

    /**
     * Update an existing app
     * PUT /api/apps/{appId}
     * Requires: JWT authentication + ownership
     * 
     * @param appId       App ID to update
     * @param request     Update request
     * @param userDetails Authenticated user
     * @return Updated app details
     */
    @PutMapping("/{appId}")
    public ResponseEntity<AppResponse> updateApp(
            @PathVariable String appId,
            @Valid @RequestBody AppUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userDetails) {

        log.info("[APP] Update app request initiated - appId: {}, userId: {}",
                appId, userDetails.getId());

        log.debug("[APP] Updating app - appId: {}, newName: {}, userId: {}",
                appId, request.getName(), userDetails.getId());

        App app = appService.updateApp(appId, request, userDetails.getId());

        log.info("[APP] App updated successfully - appId: {}, appName: {}, userId: {}",
                app.getId(), app.getName(), userDetails.getId());

        return ResponseEntity.ok(mapToResponse(app));
    }

    /**
     * Delete an app (soft delete)
     * DELETE /api/apps/{appId}
     * Requires: JWT authentication + ownership
     * 
     * @param appId       App ID to delete
     * @param userDetails Authenticated user
     * @return 204 No Content
     */
    @DeleteMapping("/{appId}")
    public ResponseEntity<Void> deleteApp(
            @PathVariable String appId,
            @AuthenticationPrincipal UserPrincipal userDetails) {

        log.info("[APP] Delete app request initiated - appId: {}, userId: {}",
                appId, userDetails.getId());

        appService.deleteApp(appId, userDetails.getId());

        log.info("[APP] App deleted successfully - appId: {}, userId: {}",
                appId, userDetails.getId());

        return ResponseEntity.noContent().build();
    }

    /**
     * Get a single app by ID
     * GET /api/apps/{appId}
     * Requires: JWT authentication + ownership
     * 
     * @param appId       App ID
     * @param userDetails Authenticated user
     * @return App details
     */
    @GetMapping("/{appId}")
    public ResponseEntity<AppResponse> getApp(
            @PathVariable String appId,
            @AuthenticationPrincipal UserPrincipal userDetails) {

        log.info("[APP] Get app request - appId: {}, userId: {}",
                appId, userDetails.getId());

        App app = appService.getApp(appId, userDetails.getId());

        log.debug("[APP] App retrieved - appId: {}, appName: {}, userId: {}",
                app.getId(), app.getName(), userDetails.getId());

        return ResponseEntity.ok(mapToResponse(app));
    }

    /**
     * List all apps for the authenticated user
     * GET /api/apps
     * Requires: JWT authentication
     * 
     * @param userDetails Authenticated user
     * @return List of user's apps
     */
    @GetMapping
    public ResponseEntity<List<AppResponse>> listApps(
            @AuthenticationPrincipal UserPrincipal userDetails) {

        log.info("[APP] List apps request - userId: {}", userDetails.getId());

        List<App> apps = appService.listUserApps(userDetails.getId());
        List<AppResponse> response = apps.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        log.info("[APP] Apps listed successfully - userId: {}, appCount: {}",
                userDetails.getId(), apps.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get a single app by slug
     * GET /api/apps/slug/{slug}
     * Requires: JWT authentication + ownership
     * 
     * @param slug        App slug
     * @param userDetails Authenticated user
     * @return App details
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<AppResponse> getAppBySlug(
            @PathVariable String slug,
            @AuthenticationPrincipal UserPrincipal userDetails) {

        log.info("[APP] Get app by slug request - slug: {}, userId: {}",
                slug, userDetails.getId());

        App app = appService.getAppBySlug(slug, userDetails.getId());

        log.debug("[APP] App retrieved by slug - slug: {}, appId: {}, appName: {}, userId: {}",
                slug, app.getId(), app.getName(), userDetails.getId());

        return ResponseEntity.ok(mapToResponse(app));
    }

    /**
     * Update an existing app by slug
     * PUT /api/apps/slug/{slug}
     * Requires: JWT authentication + ownership
     * 
     * @param slug        App slug to update
     * @param request     Update request
     * @param userDetails Authenticated user
     * @return Updated app details
     */
    @PutMapping("/slug/{slug}")
    public ResponseEntity<AppResponse> updateAppBySlug(
            @PathVariable String slug,
            @Valid @RequestBody AppUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userDetails) {

        log.info("[APP] Update app by slug request initiated - slug: {}, userId: {}",
                slug, userDetails.getId());

        log.debug("[APP] Updating app by slug - slug: {}, newName: {}, userId: {}",
                slug, request.getName(), userDetails.getId());

        App app = appService.updateAppBySlug(slug, request, userDetails.getId());

        log.info("[APP] App updated successfully by slug - slug: {}, appId: {}, appName: {}, userId: {}",
                slug, app.getId(), app.getName(), userDetails.getId());

        return ResponseEntity.ok(mapToResponse(app));
    }

    /**
     * Delete an app by slug (soft delete)
     * DELETE /api/apps/slug/{slug}
     * Requires: JWT authentication + ownership
     * 
     * @param slug        App slug to delete
     * @param userDetails Authenticated user
     * @return 204 No Content
     */
    @DeleteMapping("/slug/{slug}")
    public ResponseEntity<Void> deleteAppBySlug(
            @PathVariable String slug,
            @AuthenticationPrincipal UserPrincipal userDetails) {

        log.info("[APP] Delete app by slug request initiated - slug: {}, userId: {}",
                slug, userDetails.getId());

        appService.deleteAppBySlug(slug, userDetails.getId());

        log.info("[APP] App deleted successfully by slug - slug: {}, userId: {}",
                slug, userDetails.getId());

        return ResponseEntity.noContent().build();
    }

    /**
     * Map App entity to AppResponse DTO
     */
    private AppResponse mapToResponse(App app) {
        return AppResponse.builder()
                .id(app.getId())
                .name(app.getName())
                .slug(app.getSlug())
                .description(app.getDescription())
                .ownerId(app.getOwnerId())
                .allowedOrigins(app.getAllowedOrigins())
                .webhookUrl(app.getWebhookUrl())
                .customMetadata(app.getCustomMetadata())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
