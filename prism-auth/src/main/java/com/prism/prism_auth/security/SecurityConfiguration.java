package com.prism.prism_auth.security;

import com.prism.prism_auth.security.filters.AuthTokenFilter;
import com.prism.prism_auth.security.jwt.AuthEntryPointJwt;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfiguration {
    private final Environment env;
    /**
     * Authentication token filter bean.
     *
     * @return a new instance of AuthTokenFilter
     */
    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    /**
     * Password encoder bean using BCrypt.
     *
     * @return a BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication manager bean.
     *
     * @param authenticationConfiguration the authentication configuration
     * @return the authentication manager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) {
        try {
            return authenticationConfiguration.getAuthenticationManager();
        } catch (Exception e) {
            // wrap checked exception in a runtime exception to avoid declaring throws
            throw new IllegalStateException("Failed to create AuthenticationManager", e);
        }
    }

    /**
     * Security filter chain configuration.
     *
     * @param http HttpSecurity instance
     * @return SecurityFilterChain configured for the application
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        boolean isDev = env.acceptsProfiles(Profiles.of("dev"));
        http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for APIs
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Stateless
                .authorizeHttpRequests(auth -> {
                    // Allow auth endpoints
                    auth.requestMatchers("/api/auth/public/**").permitAll();
                    // Allow internal service validation
                    auth.requestMatchers("/api/auth/internal/**").permitAll();
                    // Allow health check
                    auth.requestMatchers("/api/actuator/health").permitAll();
                    // Admin endpoints
                    auth.requestMatchers("/api/auth/admin/**").hasRole("ADMIN");
                    // Allow actuator endpoints
                    auth.requestMatchers("/actuator/**").permitAll();
                    // Only allow API docs when running with 'dev' profile
                    if (isDev) {
                        auth.requestMatchers("/api/auth/v3/api-docs").permitAll();
                    }
                    // Secure other endpoints
                    auth.anyRequest().authenticated();
                });

        http.exceptionHandling(exception -> exception
                .authenticationEntryPoint(new AuthEntryPointJwt())); // Handle auth errors
        // Add JWT token filter before username/password auth filter
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        http.httpBasic(Customizer.withDefaults());
        return http.build();
    }

}
