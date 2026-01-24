package com.prism.prism_auth.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.prism.prism_auth.model.User;

/**
 * Repository for User entity.
 * 
 * Provides database access methods for User operations.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * Find a user by email.
     * 
     * @param email the email to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find a user by username.
     * 
     * @param username the username to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find an active (not deleted) user by email.
     * Excludes soft-deleted users.
     * 
     * @param email the email to search for
     * @return Optional containing the active user if found
     */
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    /**
     * Find an active (not deleted) user by username.
     * Excludes soft-deleted users.
     * 
     * @param username the username to search for
     * @return Optional containing the active user if found
     */
    Optional<User> findByUsernameAndDeletedAtIsNull(String username);

    /**
     * Check if a user with given email exists.
     * 
     * @param email the email to check
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Check if a user with given username exists.
     * 
     * @param username the username to check
     * @return true if user exists, false otherwise
     */
    boolean existsByUsername(String username);

    @Query("{ $or: [ { 'username': { $regex: ?0, $options: 'i' } }, { 'email': { $regex: ?1, $options: 'i' } } ] }")
    Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String email,
            Pageable pageable);
}
