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
    private Duration expiration;
    private Duration refreshExpiration;
    private Duration clockSkew;
    private String tokenPrefix;
    private String header;
}