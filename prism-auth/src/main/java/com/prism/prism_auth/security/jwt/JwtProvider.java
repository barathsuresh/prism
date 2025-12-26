package com.prism.prism_auth.security.jwt;

import java.util.Date;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.prism.prism_auth.config.JwtProperties;
import com.prism.prism_auth.security.services.UserPrincipal;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtProvider {
    private final JwtProperties jwtProperties;

    /**
     * Get JWT properties (for accessing expiration, etc.)
     * 
     * @return JwtProperties instance
     */
    public JwtProperties getJwtProperties() {
        return jwtProperties;
    }

    /**
     * Retrieves the signing key from JWT properties.
     * 
     * @return SecretKey for signing/verifying JWTs
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret()));
    }

    /**
     * Extracts JWT from the Authorization header in the format "Bearer <token>".
     * 
     * @param request
     * @return
     */
    public String getJwtFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        log.debug("Authorization Header: {}", bearerToken);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Generates JWT token from UserPrincipal details.
     * 
     * @param userDetails
     * @return
     */
    public String generateTokenFromUsername(UserPrincipal userDetails) {
        String username = userDetails.getUsername();
        String roles = userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.joining(","));
        return Jwts.builder()
                .subject(username) // Store username in token
                .claim("roles", roles) // Add roles as claim
                .issuedAt(new Date()) // Current timestamp
                .expiration(new Date((new Date()).getTime() + jwtProperties.getExpiration().toMillis())) // Expiry time
                .signWith(getSigningKey()) // Sign with secret key
                .compact();
    }

    /**
     * Extracts username from JWT token.
     * 
     * @param token
     * @return
     */
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build().parseSignedClaims(token)
                .getPayload().getSubject(); // Returns username
    }

    /**
     * Validates the JWT token.
     * 
     * @param authToken
     * @return
     */
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken);
            return true; // Token is valid
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false; // Token validation failed
    }
}
