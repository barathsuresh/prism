package com.prism.prism_auth.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prism.prism_auth.dto.UserLoginRequest;
import com.prism.prism_auth.dto.UserLoginResponse;
import com.prism.prism_auth.dto.UserRegisterRequest;
import com.prism.prism_auth.dto.UserResponse;
import com.prism.prism_auth.model.User;
import com.prism.prism_auth.security.jwt.JwtProvider;
import com.prism.prism_auth.security.services.UserPrincipal;
import com.prism.prism_auth.service.UserService;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Authentication Controller
 * Handles user authentication and registration endpoints
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

        private final UserService userService;
        private final AuthenticationManager authenticationManager;
        private final JwtProvider jwtProvider;

        /**
         * SIGNIN ENDPOINT - Authenticates user and returns JWT token
         * WORKFLOW:
         * 1. Authenticate email or username/password via Spring Security
         * 2. Set authentication in SecurityContext
         * 3. Get authenticated user details
         * 4. Generate JWT token
         * 5. Return token with user info
         * Supports login with both email and username
         */
        @SecurityRequirements()
        @PostMapping("/public/signin")
        public ResponseEntity<?> authenticateUser(@Validated @RequestBody UserLoginRequest loginRequest) {
                log.info("[AUTH] Signin request initiated for identifier: {}", loginRequest.getEmail());

                Authentication authentication;
                try {
                        log.debug("[AUTH] Attempting authentication for: {}", loginRequest.getEmail());
                        // Step 1: Authenticate email or username/password
                        // CustomUserDetailsService will try email first, then username
                        authentication = authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(
                                                        loginRequest.getEmail(), // Can be email or username
                                                        loginRequest.getPassword()));
                        log.debug("[AUTH] Authentication successful for: {}", loginRequest.getEmail());
                } catch (AuthenticationException exception) {
                        // Login failed - invalid credentials
                        log.warn("[AUTH] Authentication failed for identifier: {} - Reason: {}",
                                        loginRequest.getEmail(), exception.getMessage());
                        Map<String, Object> map = new HashMap<>();
                        map.put("message", "Bad credentials");
                        map.put("status", false);
                        return new ResponseEntity<>(map, HttpStatus.UNAUTHORIZED);
                }

                // Step 2: Set authentication in SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("[AUTH] SecurityContext updated with authentication");

                // Step 3: Get authenticated user details
                UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
                log.debug("[AUTH] Retrieved user principal - userId: {}, username: {}",
                                userDetails.getId(), userDetails.getUsername());

                // Step 4: Generate JWT token
                String jwtToken = jwtProvider.generateTokenFromUsername(userDetails);
                log.debug("[AUTH] JWT token generated for userId: {}", userDetails.getId());

                // Step 5: Build response with token and user info
                UserLoginResponse response = UserLoginResponse.builder()
                                .userId(userDetails.getId())
                                .email(userDetails.getEmail())
                                .username(userDetails.getUsername())
                                .jwtToken(jwtToken)
                                .expiresIn(jwtProvider.getJwtProperties().getExpiration().toSeconds())
                                .tokenType("Bearer")
                                .build();

                log.info("[AUTH] Signin successful - userId: {}, username: {}, email: {}",
                                userDetails.getId(), userDetails.getUsername(), userDetails.getEmail());

                // Step 7: Return JWT token to client
                return ResponseEntity.ok(response);
        }

        /**
         * SIGNUP ENDPOINT - Registers new user account
         * 
         * WORKFLOW:
         * 1. Validate username/email uniqueness
         * 2. Create user with encrypted password
         * 3. Assign role (default: ROLE_DEVELOPER)
         * 4. Save to database
         * 5. Return user details
         */
        @SecurityRequirements()
        @PostMapping("/public/signup")
        public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegisterRequest signUpRequest) {
                log.info("[AUTH] Signup request initiated for email: {}, username: {}",
                                signUpRequest.getEmail(), signUpRequest.getUsername());

                try {
                        log.debug("[AUTH] Registering new user - email: {}, username: {}, firstName: {}, lastName: {}",
                                        signUpRequest.getEmail(), signUpRequest.getUsername(),
                                        signUpRequest.getFirstName(), signUpRequest.getLastName());

                        // Create and save user (validation happens in service layer)
                        User user = userService.register(signUpRequest);

                        log.debug("[AUTH] User created successfully - userId: {}, username: {}",
                                        user.getId(), user.getUsername());

                        // Build response with user information (excluding sensitive data)
                        UserResponse response = UserResponse.builder()
                                        .id(user.getId())
                                        .email(user.getEmail())
                                        .username(user.getUsername())
                                        .firstName(signUpRequest.getFirstName())
                                        .lastName(signUpRequest.getLastName())
                                        .status(user.getStatus())
                                        .roles(user.getRoles() != null && !user.getRoles().isEmpty()
                                                        ? user.getRoles()
                                                        : null)
                                        .emailVerified(user.getEmailVerified())
                                        .createdAt(user.getCreatedAt())
                                        .build();

                        log.info("[AUTH] Signup successful - userId: {}, username: {}, email: {}, role: {}",
                                        user.getId(), user.getUsername(), user.getEmail(),
                                        (user.getRoles() != null && !user.getRoles().isEmpty() ? user.getRoles().get(0)
                                                        : "NONE"));

                        return ResponseEntity.ok(response);
                } catch (IllegalArgumentException e) {
                        // Handle validation errors (email/username already exists)
                        log.warn("[AUTH] Signup failed for email: {}, username: {} - Reason: {}",
                                        signUpRequest.getEmail(), signUpRequest.getUsername(), e.getMessage());

                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("message", e.getMessage());
                        errorResponse.put("status", false);
                        return ResponseEntity.badRequest().body(errorResponse);
                }
        }

        /**
         * GET USER DETAILS - Returns current logged-in user information
         * 
         * @AuthenticationPrincipal automatically injects authenticated user details
         *                          Spring Security populates this from SecurityContext
         *                          after JWT validation
         */
        @GetMapping("/user")
        public ResponseEntity<?> getUserDetails(@AuthenticationPrincipal UserPrincipal userDetails) {
                log.info("[AUTH] Get user details request for userId: {}, username: {}",
                                userDetails.getId(), userDetails.getUsername());

                log.debug("[AUTH] Fetching user details from database for username: {}",
                                userDetails.getUsername());

                // Fetch full user details from database
                User user = userService.findByUsername(userDetails.getUsername());

                log.debug("[AUTH] User details retrieved - userId: {}, email: {}, status: {}",
                                user.getId(), user.getEmail(), user.getStatus());

                // Build response with user information (excluding sensitive data)
                UserResponse response = UserResponse.builder()
                                .id(user.getId())
                                .username(user.getUsername())
                                .email(user.getEmail())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .status(user.getStatus())
                                .roles(user.getRoles() != null && !user.getRoles().isEmpty()
                                                ? user.getRoles()
                                                : null)
                                .emailVerified(user.getEmailVerified())
                                .emailVerifiedAt(user.getEmailVerifiedAt())
                                .createdAt(user.getCreatedAt())
                                .updatedAt(user.getUpdatedAt())
                                .build();

                log.info("[AUTH] User details request successful - userId: {}, username: {}",
                                user.getId(), user.getUsername());

                return ResponseEntity.ok(response);
        }
}
