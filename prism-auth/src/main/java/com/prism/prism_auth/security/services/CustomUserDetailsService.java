package com.prism.prism_auth.security.services;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.prism.prism_auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    /**
     * Load user by username (or email for this app)
     * Used by Spring Security during authentication
     */
    @Override
    public UserPrincipal loadUserByUsername(String username) throws UsernameNotFoundException {
        // Try to find by email first (primary login method)
        return userRepository.findByEmailAndDeletedAtIsNull(username)
                .or(() -> userRepository.findByUsernameAndDeletedAtIsNull(username))
                .map(UserPrincipal::build)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username/email: " + username));
    }
}
