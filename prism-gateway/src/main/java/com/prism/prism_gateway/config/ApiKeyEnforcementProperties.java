package com.prism.prism_gateway.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "gateway.apiKey")
@Data
public class ApiKeyEnforcementProperties {

    private List<RouteScope> enforcement;

    @Data
    public static class RouteScope {
        private String path; // Ant-style pattern
        private List<String> scopes; // e.g., ["videos:upload"]
    }
}