package com.prism.prism_auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.prism.prism_auth.model.App;

/**
 * Repository for App entity.
 * 
 * Provides database access methods for App operations.
 */
@Repository
public interface AppRepository extends MongoRepository<App, String> {

    /**
     * Find all active apps owned by a user.
     * Excludes soft-deleted apps.
     * 
     * @param ownerId the owner's user ID
     * @return list of active apps owned by the user
     */
    List<App> findByOwnerIdAndDeletedAtIsNull(String ownerId);

    /**
     * Find an active app by its slug.
     * Slug is unique, so at most one result.
     * Excludes soft-deleted apps.
     * 
     * @param slug the app slug (URL-safe identifier)
     * @return Optional containing the app if found and active
     */
    Optional<App> findBySlugAndDeletedAtIsNull(String slug);

    /**
     * Find an active app by its ID.
     * Excludes soft-deleted apps.
     * 
     * @param id the app ID (MongoDB ObjectId)
     * @return Optional containing the app if found and active
     */
    Optional<App> findByIdAndDeletedAtIsNull(String id);

    /**
     * Find an app by ID and owner ID.
     * Ensures the owner can only access their own apps.
     * 
     * @param id      the app ID
     * @param ownerId the owner's user ID
     * @return Optional containing the app if found and belongs to the owner
     */
    Optional<App> findByIdAndOwnerId(String id, String ownerId);

    /**
     * Find an active app by slug and owner ID.
     * Double-check that owner can only access their own apps.
     * Excludes soft-deleted apps.
     * 
     * @param slug    the app slug
     * @param ownerId the owner's user ID
     * @return Optional containing the app if found, active, and belongs to owner
     */
    Optional<App> findBySlugAndOwnerIdAndDeletedAtIsNull(String slug, String ownerId);

    /**
     * Check if a slug is already taken by any user.
     * Used during app creation to ensure uniqueness.
     * 
     * @param slug the slug to check
     * @return true if slug is already used, false otherwise
     */
    boolean existsBySlug(String slug);

    /**
     * Check if a slug is taken by a specific user.
     * Used during app update to allow same user to keep their slug.
     * 
     * @param slug    the slug to check
     * @param ownerId the owner's user ID
     * @return true if slug is used by this owner, false otherwise
     */
    boolean existsBySlugAndOwnerId(String slug, String ownerId);
}
