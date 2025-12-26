package com.prism.prism_auth.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prism.prism_auth.dto.UserRegisterRequest;
import com.prism.prism_auth.model.User;
import com.prism.prism_auth.model.enums.UserRole;
import com.prism.prism_auth.model.enums.UserStatus;
import com.prism.prism_auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Register a new user account
     * 
     * @param request Registration request with user details
     * @return Created user entity
     * @throws IllegalArgumentException if email or username already exists
     */
    @Transactional
    public User register(UserRegisterRequest request) {
        // Validation: Check if email already exists
        userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .ifPresent(u -> {
                    throw new IllegalArgumentException("Email already in use");
                });

        // Validation: Check if username already exists
        userRepository.findByUsername(request.getUsername())
                .ifPresent(u -> {
                    throw new IllegalArgumentException("Username already in use");
                });

        // Create new user with encrypted password
        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword())) // Never store plain text
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .roles(List.of(UserRole.ROLE_DEVELOPER)) // Default role
                .status(UserStatus.ACTIVE)
                .emailVerified(false) // Email not verified initially
                // Set account status flags (all enabled by default)
                .accountNonLocked(true)
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .enabled(true)
                // Set expiry dates (1 year from now)
                .credentialsExpiryDate(LocalDate.now().plusYears(1))
                .accountExpiryDate(LocalDate.now().plusYears(1))
                // Security settings
                .mfaEnabled(false) // 2FA disabled by default
                .failedLoginAttempts(0)
                .build();

        // Save new user to database
        return userRepository.save(user);
    }

    /**
     * Find user by username (excluding soft-deleted users)
     * 
     * @param username Username to search for
     * @return User entity
     * @throws RuntimeException if user not found
     */
    public User findByUsername(String username) {
        return userRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }

    /**
     * Find user by email (excluding soft-deleted users)
     * 
     * @param email Email to search for
     * @return User entity
     * @throws RuntimeException if user not found
     */
    public User findByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    /**
     * Find user by ID (excluding soft-deleted users)
     * 
     * @param id User ID
     * @return User entity
     * @throws RuntimeException if user not found
     */
    public User findById(String id) {
        return userRepository.findById(id)
                .filter(user -> user.getDeletedAt() == null)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }
}
