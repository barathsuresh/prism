package com.prism.prism_auth.security.services;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.prism.prism_auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    /**
     * Load user by username (or email for this app)
     * Used by Spring Security during authentication
     */
    @Override
    public UserPrincipal loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("[USER-DETAILS] Loading user details - identifier: {}", username);

        // Try to find by email first (primary login method)
        return userRepository.findByEmailAndDeletedAtIsNull(username)
                .map(user -> {
                    log.debug("[USER-DETAILS] User found by email - userId: {}, username: {}, email: {}",
                            user.getId(), user.getUsername(), user.getEmail());
                    return user;
                })
                .or(() -> {
                    log.debug("[USER-DETAILS] User not found by email, trying username lookup - identifier: {}",
                            username);
                    return userRepository.findByUsernameAndDeletedAtIsNull(username)
                            .map(user -> {
                                log.debug("[USER-DETAILS] User found by username - userId: {}, username: {}",
                                        user.getId(), user.getUsername());
                                return user;
                            });
                })
                .map(user -> {
                    log.info("[USER-DETAILS] User details loaded successfully - userId: {}, username: {}, email: {}",
                            user.getId(), user.getUsername(), user.getEmail());
                    return UserPrincipal.build(user);
                })
                .orElseThrow(() -> {
                    log.warn("[USER-DETAILS] User not found - identifier: {}", username);
                    return new UsernameNotFoundException("User not found with username/email: " + username);
                });
    }
}
