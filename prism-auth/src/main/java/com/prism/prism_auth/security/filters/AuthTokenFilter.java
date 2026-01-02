package com.prism.prism_auth.security.filters;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.prism.prism_auth.security.jwt.JwtProvider;
import com.prism.prism_auth.security.services.CustomUserDetailsService;
import com.prism.prism_auth.security.services.UserPrincipal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter that processes JWT authentication tokens from incoming requests.
 */
@Component
@Slf4j
public class AuthTokenFilter extends OncePerRequestFilter {
    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    /**
     * Filters each request to extract and validate JWT token.
     * If valid, sets the user authentication in the security context.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String requestMethod = request.getMethod();

        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtProvider.validateJwtToken(jwt)) {
                String username = jwtProvider.getUserNameFromJwtToken(jwt);

                log.debug("[JWT-FILTER] JWT token validated - username: {}, method: {}, path: {}",
                        username, requestMethod, requestPath);

                UserPrincipal userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());
                authentication.setDetails(userDetails);
                // Set user authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug(
                        "[JWT-FILTER] Authentication set in SecurityContext - userId: {}, username: {}, authorities: {}, method: {}, path: {}",
                        userDetails.getId(), username, userDetails.getAuthorities(), requestMethod, requestPath);
            } else if (jwt != null) {
                log.warn("[JWT-FILTER] Invalid JWT token - method: {}, path: {}",
                        requestMethod, requestPath);
            } else {
                log.trace("[JWT-FILTER] No JWT token found - method: {}, path: {}",
                        requestMethod, requestPath);
            }
        } catch (Exception e) {
            log.error("[JWT-FILTER] Authentication error - method: {}, path: {}, error: {}",
                    requestMethod, requestPath, e.getMessage(), e);
        }
        filterChain.doFilter(request, response);
    }

    /** Extracts JWT token from "Authorization: Bearer <token>" header */
    private String parseJwt(HttpServletRequest request) {
        String jwt = jwtProvider.getJwtFromHeader(request);
        if (jwt != null) {
            log.trace("[JWT-FILTER] JWT token extracted from header - path: {}", request.getRequestURI());
        }
        return jwt;
    }

}
