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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Authentication Controller
 * 
 * Handles user authentication and registration endpoints
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    /**
     * SIGNIN ENDPOINT - Authenticates user and returns JWT token
     * 
     * WORKFLOW:
     * 1. Authenticate email or username/password via Spring Security
     * 2. Set authentication in SecurityContext
     * 3. Get authenticated user details
     * 4. Generate JWT token
     * 5. Return token with user info
     * 
     * Supports login with both email and username
     */
    @PostMapping("/public/signin")
    public ResponseEntity<?> authenticateUser(@Validated @RequestBody UserLoginRequest loginRequest) {
        Authentication authentication;
        try {
            // Step 1: Authenticate email or username/password
            // CustomUserDetailsService will try email first, then username
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(), // Can be email or username
                            loginRequest.getPassword()));
        } catch (AuthenticationException exception) {
            // Login failed - invalid credentials
            Map<String, Object> map = new HashMap<>();
            map.put("message", "Bad credentials");
            map.put("status", false);
            return new ResponseEntity<>(map, HttpStatus.UNAUTHORIZED);
        }

        // Step 2: Set authentication in SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Step 3: Get authenticated user details
        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();

        // Step 4: Generate JWT token
        String jwtToken = jwtProvider.generateTokenFromUsername(userDetails);

        // Step 5: Build response with token and user info
        UserLoginResponse response = UserLoginResponse.builder()
                .userId(userDetails.getId())
                .email(userDetails.getEmail())
                .username(userDetails.getUsername())
                .jwtToken(jwtToken)
                .expiresIn(jwtProvider.getJwtProperties().getExpiration().toSeconds())
                .tokenType("Bearer")
                .build();

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
    @PostMapping("/public/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegisterRequest signUpRequest) {
        try {
            // Create and save user (validation happens in service layer)
            User user = userService.register(signUpRequest);

            // Build response with user information (excluding sensitive data)
            UserResponse response = UserResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .firstName(signUpRequest.getFirstName())
                    .lastName(signUpRequest.getLastName())
                    .status(user.getStatus())
                    .role(user.getRoles() != null && !user.getRoles().isEmpty()
                            ? user.getRoles().get(0)
                            : null)
                    .emailVerified(user.getEmailVerified())
                    .createdAt(user.getCreatedAt())
                    .build();

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // Handle validation errors (email/username already exists)
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
        // Fetch full user details from database
        User user = userService.findByUsername(userDetails.getUsername());

        // Build response with user information (excluding sensitive data)
        UserResponse response = UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .status(user.getStatus())
                .role(user.getRoles() != null && !user.getRoles().isEmpty()
                        ? user.getRoles().get(0)
                        : null)
                .emailVerified(user.getEmailVerified())
                .emailVerifiedAt(user.getEmailVerifiedAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();

        return ResponseEntity.ok(response);
    }
}
