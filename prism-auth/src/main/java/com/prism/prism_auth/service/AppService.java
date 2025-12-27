package com.prism.prism_auth.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prism.prism_auth.dto.AppRequest;
import com.prism.prism_auth.dto.AppUpdateRequest;
import com.prism.prism_auth.model.App;
import com.prism.prism_auth.repository.AppRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppService {

    private final AppRepository appRepository;
    private final ApiKeyService apiKeyService;

    /**
     * Create a new app for a user
     * 
     * @param request App creation request
     * @param ownerId User ID who owns this app
     * @return Created app entity
     * @throws IllegalArgumentException if slug already exists for this owner
     */
    @Transactional
    public App createApp(AppRequest request, String ownerId) {
        // Generate slug from name if not provided
        String slug = request.getSlug() != null ? request.getSlug() : generateSlug(request.getName());

        // Validate slug uniqueness for this owner
        appRepository.findBySlugAndOwnerIdAndDeletedAtIsNull(slug, ownerId)
                .ifPresent(app -> {
                    throw new IllegalArgumentException("App with slug '" + slug + "' already exists");
                });

        // Create new app
        App app = App.builder()
                .name(request.getName())
                .slug(slug)
                .ownerId(ownerId)
                .description(request.getDescription())
                .allowedOrigins(request.getAllowedOrigins())
                .webhookUrl(request.getWebhookUrl())
                .customMetadata(request.getCustomMetadata())
                .build();

        return appRepository.save(app);
    }

    /**
     * Update an existing app
     * 
     * @param appId   App ID to update
     * @param request Update request
     * @param ownerId User ID (for ownership verification)
     * @return Updated app entity
     * @throws IllegalArgumentException if app not found or not owned by user
     *
     *                                  Note: If the app name changes and no slug is
     *                                  provided in the request,
     *                                  the slug will be auto-regenerated from the
     *                                  new name, but only if the
     *                                  current slug was originally auto-generated
     *                                  from the previous name.
     */
    @Transactional
    public App updateApp(String appId, AppRequest request, String ownerId) {
        // Find app and verify ownership
        App app = appRepository.findByIdAndDeletedAtIsNull(appId)
                .orElseThrow(() -> new IllegalArgumentException("App not found"));

        if (!app.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("You don't have permission to update this app");
        }

        // Update fields (only if provided)
        boolean nameChanged = false;
        if (request.getName() != null && !request.getName().equals(app.getName())) {
            app.setName(request.getName());
            nameChanged = true;
        }
        if (request.getDescription() != null) {
            app.setDescription(request.getDescription());
        }
        if (request.getAllowedOrigins() != null) {
            app.setAllowedOrigins(request.getAllowedOrigins());
        }
        if (request.getWebhookUrl() != null) {
            app.setWebhookUrl(request.getWebhookUrl());
        }
        if (request.getCustomMetadata() != null) {
            app.setCustomMetadata(request.getCustomMetadata());
        }

        // Auto-update slug when name changes.
        // If forceRegenerateSlug is true, always regenerate.
        // Otherwise, regenerate only when caller did not provide a slug.
        if (nameChanged) {
            boolean force = Boolean.TRUE.equals(request.getForceRegenerateSlug());
            if (force || request.getSlug() == null) {
                String newSlug = generateSlug(app.getName());
                if (!newSlug.equals(app.getSlug())) {
                    appRepository.findBySlugAndOwnerIdAndDeletedAtIsNull(newSlug, ownerId)
                            .ifPresent(other -> {
                                if (other.getId() != null && !other.getId().equals(app.getId())) {
                                    throw new IllegalArgumentException(
                                            "App with slug '" + newSlug + "' already exists");
                                }
                            });
                    app.setSlug(newSlug);
                }
            }
        }

        // Note: Explicit slug changes are not allowed via update request
        return appRepository.save(app);
    }

    /**
     * Update an existing app (overload for AppUpdateRequest - partial updates)
     * 
     * @param appId   App ID to update
     * @param request Update request with optional fields
     * @param ownerId User ID (for ownership verification)
     * @return Updated app entity
     */
    @Transactional
    public App updateApp(String appId, AppUpdateRequest request, String ownerId) {
        // Find app and verify ownership
        App app = appRepository.findByIdAndDeletedAtIsNull(appId)
                .orElseThrow(() -> new IllegalArgumentException("App not found"));

        if (!app.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("You don't have permission to update this app");
        }

        // Update fields (only if provided)
        boolean nameChanged = false;
        if (request.getName() != null && !request.getName().equals(app.getName())) {
            app.setName(request.getName());
            nameChanged = true;
        }
        if (request.getDescription() != null) {
            app.setDescription(request.getDescription());
        }
        if (request.getAllowedOrigins() != null) {
            app.setAllowedOrigins(request.getAllowedOrigins());
        }
        if (request.getWebhookUrl() != null) {
            app.setWebhookUrl(request.getWebhookUrl());
        }
        if (request.getCustomMetadata() != null) {
            app.setCustomMetadata(request.getCustomMetadata());
        }

        // Auto-update slug when name changes
        if (nameChanged) {
            boolean force = Boolean.TRUE.equals(request.getForceRegenerateSlug());
            if (force || request.getSlug() == null) {
                String newSlug = generateSlug(app.getName());
                if (!newSlug.equals(app.getSlug())) {
                    appRepository.findBySlugAndOwnerIdAndDeletedAtIsNull(newSlug, ownerId)
                            .ifPresent(other -> {
                                if (other.getId() != null && !other.getId().equals(app.getId())) {
                                    throw new IllegalArgumentException(
                                            "App with slug '" + newSlug + "' already exists");
                                }
                            });
                    app.setSlug(newSlug);
                }
            }
        }

        return appRepository.save(app);
    }

    /**
     * Soft delete an app
     * 
     * @param appId   App ID to delete
     * @param ownerId User ID (for ownership verification)
     * @throws IllegalArgumentException if app not found or not owned by user
     */
    @Transactional
    public void deleteApp(String appId, String ownerId) {
        App app = appRepository.findByIdAndDeletedAtIsNull(appId)
                .orElseThrow(() -> new IllegalArgumentException("App not found"));

        if (!app.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("You don't have permission to delete this app");
        }

        // Soft delete
        app.setDeletedAt(LocalDateTime.now());
        appRepository.save(app);

        // Revoke all API keys associated with this app
        int revoked = apiKeyService.revokeAllKeysForApp(app.getId(), ownerId, "App deleted by owner");
        log.info("Deleted app {} and revoked {} keys", app.getId(), revoked);
    }

    /**
     * Get a single app by ID
     * 
     * @param appId   App ID
     * @param ownerId User ID (for ownership verification)
     * @return App entity
     * @throws IllegalArgumentException if app not found or not owned by user
     */
    public App getApp(String appId, String ownerId) {
        log.debug("App ID: {}, Owner ID: {}", appId, ownerId);
        App app = appRepository.findByIdAndDeletedAtIsNull(appId)
                .orElseThrow(() -> new IllegalArgumentException("App not found"));

        if (!app.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("You don't have permission to view this app");
        }

        return app;
    }

    /**
     * List all apps for a user
     * 
     * @param ownerId User ID
     * @return List of apps owned by the user
     */
    public List<App> listUserApps(String ownerId) {
        return appRepository.findByOwnerIdAndDeletedAtIsNull(ownerId);
    }

    /**
     * Get a single app by slug
     * 
     * @param slug    App slug
     * @param ownerId User ID (for ownership verification)
     * @return App entity
     * @throws IllegalArgumentException if app not found or not owned by user
     */
    public App getAppBySlug(String slug, String ownerId) {
        log.debug("App slug: {}, Owner ID: {}", slug, ownerId);
        App app = appRepository.findBySlugAndOwnerIdAndDeletedAtIsNull(slug, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("App not found"));
        return app;
    }

    /**
     * Update an existing app by slug
     * 
     * @param slug    App slug to update
     * @param request Update request
     * @param ownerId User ID (for ownership verification)
     * @return Updated app entity
     * @throws IllegalArgumentException if app not found or not owned by user
     */
    @Transactional
    public App updateAppBySlug(String slug, AppRequest request, String ownerId) {
        App app = appRepository.findBySlugAndOwnerIdAndDeletedAtIsNull(slug, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("App not found"));

        // Update fields (only if provided)
        boolean nameChanged = false;
        if (request.getName() != null && !request.getName().equals(app.getName())) {
            app.setName(request.getName());
            nameChanged = true;
        }
        if (request.getDescription() != null) {
            app.setDescription(request.getDescription());
        }
        if (request.getAllowedOrigins() != null) {
            app.setAllowedOrigins(request.getAllowedOrigins());
        }
        if (request.getWebhookUrl() != null) {
            app.setWebhookUrl(request.getWebhookUrl());
        }
        if (request.getCustomMetadata() != null) {
            app.setCustomMetadata(request.getCustomMetadata());
        }

        // Auto-update slug when name changes
        if (nameChanged) {
            boolean force = Boolean.TRUE.equals(request.getForceRegenerateSlug());
            if (force || request.getSlug() == null) {
                String newSlug = generateSlug(app.getName());
                if (!newSlug.equals(app.getSlug())) {
                    appRepository.findBySlugAndOwnerIdAndDeletedAtIsNull(newSlug, ownerId)
                            .ifPresent(other -> {
                                if (other.getId() != null && !other.getId().equals(app.getId())) {
                                    throw new IllegalArgumentException(
                                            "App with slug '" + newSlug + "' already exists");
                                }
                            });
                    app.setSlug(newSlug);
                }
            }
        }

        return appRepository.save(app);
    }

    /**
     * Update an existing app by slug (overload for AppUpdateRequest - partial
     * updates)
     * 
     * @param slug    App slug to update
     * @param request Update request with optional fields
     * @param ownerId User ID (for ownership verification)
     * @return Updated app entity
     */
    @Transactional
    public App updateAppBySlug(String slug, AppUpdateRequest request, String ownerId) {
        App app = appRepository.findBySlugAndOwnerIdAndDeletedAtIsNull(slug, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("App not found"));

        // Update fields (only if provided)
        boolean nameChanged = false;
        if (request.getName() != null && !request.getName().equals(app.getName())) {
            app.setName(request.getName());
            nameChanged = true;
        }
        if (request.getDescription() != null) {
            app.setDescription(request.getDescription());
        }
        if (request.getAllowedOrigins() != null) {
            app.setAllowedOrigins(request.getAllowedOrigins());
        }
        if (request.getWebhookUrl() != null) {
            app.setWebhookUrl(request.getWebhookUrl());
        }
        if (request.getCustomMetadata() != null) {
            app.setCustomMetadata(request.getCustomMetadata());
        }

        // Auto-update slug when name changes
        if (nameChanged) {
            boolean force = Boolean.TRUE.equals(request.getForceRegenerateSlug());
            if (force || request.getSlug() == null) {
                String newSlug = generateSlug(app.getName());
                if (!newSlug.equals(app.getSlug())) {
                    appRepository.findBySlugAndOwnerIdAndDeletedAtIsNull(newSlug, ownerId)
                            .ifPresent(other -> {
                                if (other.getId() != null && !other.getId().equals(app.getId())) {
                                    throw new IllegalArgumentException(
                                            "App with slug '" + newSlug + "' already exists");
                                }
                            });
                    app.setSlug(newSlug);
                }
            }
        }

        return appRepository.save(app);
    }

    /**
     * Soft delete an app by slug
     * 
     * @param slug    App slug to delete
     * @param ownerId User ID (for ownership verification)
     * @throws IllegalArgumentException if app not found or not owned by user
     */
    @Transactional
    public void deleteAppBySlug(String slug, String ownerId) {
        App app = appRepository.findBySlugAndOwnerIdAndDeletedAtIsNull(slug, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("App not found"));

        // Soft delete
        app.setDeletedAt(LocalDateTime.now());
        appRepository.save(app);

        // Revoke all API keys associated with this app
        int revoked = apiKeyService.revokeAllKeysForApp(app.getId(), ownerId, "App deleted by owner");
        log.info("Deleted app {} (slug={}) and revoked {} keys", app.getId(), slug, revoked);
    }

    /**
     * Generate a URL-safe slug from app name
     * 
     * @param name App name
     * @return Generated slug
     */
    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "") // Remove special chars
                .replaceAll("\\s+", "-") // Replace spaces with hyphens
                .replaceAll("-+", "-") // Remove consecutive hyphens
                .replaceAll("^-|-$", ""); // Remove leading/trailing hyphens
    }
}
