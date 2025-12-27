package com.prism.prism_auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.prism.prism_auth.model.ApiKey;
import com.prism.prism_auth.model.enums.ApiKeyStatus;

/**
 * Repository for ApiKey entity.
 * 
 * Provides database access methods for API key operations.
 * Critical for Gateway validation and key management.
 */
@Repository
public interface ApiKeyRepository extends MongoRepository<ApiKey, String> {

    /**
     * Find all active keys for an app.
     * Used for displaying available keys to the developer.
     * 
     * @param appId  the app ID
     * @param status the key status (ACTIVE, REVOKED, etc.)
     * @return list of keys matching the criteria
     */
    List<ApiKey> findByAppIdAndStatus(String appId, ApiKeyStatus status);

    /**
     * Find all keys for an app (regardless of status).
     * Used for admin operations and key management.
     * 
     * @param appId the app ID
     * @return list of all keys for the app
     */
    List<ApiKey> findByAppId(String appId);

    /**
     * Find a key by its hash and status.
     * **CRITICAL**: Used by Gateway to validate incoming API keys.
     * 
     * Workflow:
     * 1. Gateway receives X-API-KEY header with raw key value
     * 2. Gateway hashes the key
     * 3. Gateway calls this method to find matching key
     * 4. If found and status is ACTIVE, the request is authorized
     * 
     * @param keyHash the hash of the API key
     * @param status  the expected status (usually ACTIVE)
     * @return Optional containing the key if valid
     */
    Optional<ApiKey> findByKeyHashAndStatus(String keyHash, ApiKeyStatus status);

    /**
     * Find a key by its hash (any status).
     * Used during revocation and rotation workflows.
     * 
     * @param keyHash the hash of the API key
     * @return Optional containing the key if found
     */
    Optional<ApiKey> findByKeyHash(String keyHash);

    /**
     * Find a key by its public prefix.
     * Used during validation to retrieve the stored hash and compare secrets.
     *
     * @param keyPrefix the public key prefix (e.g., "pk_...")
     * @return Optional containing the key if found
     */
    Optional<ApiKey> findByKeyPrefix(String keyPrefix);

    /**
     * Find all active keys for a specific app.
     * Convenient method for getting only ACTIVE keys.
     * 
     * @param appId the app ID
     * @return list of active keys
     */
    List<ApiKey> findByAppIdAndStatusEquals(String appId, ApiKeyStatus status);

    /**
     * Find all keys created by a specific user.
     * Useful for auditing and user-specific key management.
     * 
     * @param ownerId the user ID (creator of the keys)
     * @return list of keys created by this user
     */
    List<ApiKey> findByOwnerId(String ownerId);

    /**
     * Find all keys created by a specific user by username.
     * Useful for listing all API keys for the current authenticated user.
     * 
     * @param ownerUserName the username of the key owner
     * @return list of keys created by this user
     */
    List<ApiKey> findByOwnerUserName(String ownerUserName);

    /**
     * Check if a key hash already exists.
     * Used during key generation to ensure no duplicates.
     * 
     * @param keyHash the hash to check
     * @return true if key exists, false otherwise
     */
    boolean existsByKeyHash(String keyHash);

    /**
     * Delete all keys for an app.
     * Used when an app is deleted (cascade delete).
     * Note: Consider soft delete instead of hard delete for audit trail.
     * 
     * @param appId the app ID
     */
    void deleteByAppId(String appId);

    /**
     * Count active keys for an app.
     * Used to enforce key limits per app (e.g., free tier max 5 keys).
     * 
     * @param appId  the app ID
     * @param status the status to count (usually ACTIVE)
     * @return number of keys matching criteria
     */
    long countByAppIdAndStatus(String appId, ApiKeyStatus status);
}
