package com.prism.prism_gateway.filter;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import com.prism.prism_gateway.config.ApiKeyEnforcementProperties;

import reactor.core.publisher.Mono;

@Configuration
public class ApiKeyValidationFilter implements GlobalFilter {

    private final ApiKeyEnforcementProperties props;
    private final WebClient webClient;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public ApiKeyValidationFilter(ApiKeyEnforcementProperties props, WebClient.Builder webClientBuilder) {
        this.props = props;
        // Load-balanced WebClient for Eureka service calls
        this.webClient = webClientBuilder.build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI requestUri = exchange.getRequest().getURI();
        String path = requestUri.getPath();

        // Determine required scopes for this path (first match wins)
        List<String> requiredScopes = props.getEnforcement() == null ? List.of()
                : props.getEnforcement().stream()
                        .filter(e -> matcher.match(e.getPath(), path))
                        .findFirst()
                        .map(ApiKeyEnforcementProperties.RouteScope::getScopes)
                        .orElse(List.of());

        // If no scopes required, skip validation
        if (requiredScopes.isEmpty()) {
            return chain.filter(exchange);
        }

        // Read X-API-Key header
        String apiKeyHeader = exchange.getRequest().getHeaders().getFirst("X-API-Key");
        if (apiKeyHeader == null || apiKeyHeader.isBlank()) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Call Auth internal validate endpoint with scopes
        String scopesCsv = requiredScopes.stream().collect(Collectors.joining(","));

        // Use service ID via Eureka: prism-auth
        String validateUrl = "http://prism-auth/api/auth/internal/validate?scopes=" + scopesCsv;

        return webClient.post()
                .uri(validateUrl)
                .header("X-API-Key", apiKeyHeader)
                .exchangeToMono(clientResp -> {
                    if (clientResp.statusCode().value() == 401) {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                    if (clientResp.statusCode().is5xxServerError()) {
                        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                        return exchange.getResponse().setComplete();
                    }
                    return clientResp.bodyToMono(ValidationResponse.class)
                            .flatMap(res -> {
                                if (res == null || !res.isValid() || res.getApiKey() == null) {
                                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                    return exchange.getResponse().setComplete();
                                }
                                // Forward appId downstream
                                String appId = res.getApiKey().getAppId();
                                ServerWebExchange mutated = exchange.mutate().request(
                                        exchange.getRequest().mutate().header("X-App-Id", appId).build()).build();
                                return chain.filter(mutated);
                            });
                });
    }

    // Minimal DTOs to deserialize validation response
    public static class ValidationResponse {
        private boolean valid;
        private ApiKeyResponse apiKey;
        private String message;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean v) {
            this.valid = v;
        }

        public ApiKeyResponse getApiKey() {
            return apiKey;
        }

        public void setApiKey(ApiKeyResponse a) {
            this.apiKey = a;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String m) {
            this.message = m;
        }
    }

    public static class ApiKeyResponse {
        private String appId;

        public String getAppId() {
            return appId;
        }

        public void setAppId(String a) {
            this.appId = a;
        }
    }
}