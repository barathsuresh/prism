package com.prism.prism_auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret;
    private String issuer;
    private String audience;
    private Duration expiration;        // binds from expiration-seconds
    private Duration refreshExpiration; // binds from refresh-expiration-seconds
    private Duration clockSkew;         // binds from clock-skew-seconds
    private String tokenPrefix;         // binds from token-prefix
    private String header;
}