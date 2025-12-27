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

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Configuration
@Slf4j
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

        log.info("[API Key Validation] Incoming request: {} {}", exchange.getRequest().getMethod(), path);

        // Determine required scopes for this path and method (first match wins)
        String method = exchange.getRequest().getMethod().name();
        List<String> requiredScopes = props.getEnforcement() == null ? List.of()
                : props.getEnforcement().stream()
                        .filter(e -> matcher.match(e.getPath(), path))
                        .filter(e -> e.getMethod() == null || e.getMethod().equalsIgnoreCase(method))
                        .findFirst()
                        .map(ApiKeyEnforcementProperties.RouteScope::getScopes)
                        .orElse(List.of());

        // If no scopes required (internal/public routes), skip API key validation
        // entirely
        // This applies to paths like /api/**/internal/** and
        // /api/catalog/videos/public/**
        // No X-API-Key header is required and no auth validation call is made
        if (requiredScopes.isEmpty()) {
            log.debug(
                    "[API Key Validation] No scopes required for path: {} - Skipping validation (public/internal endpoint)",
                    path);
            return chain.filter(exchange);
        }

        log.debug("[API Key Validation] Required scopes: {} for path: {}", requiredScopes, path);

        // Read X-API-Key header
        String apiKeyHeader = exchange.getRequest().getHeaders().getFirst("X-API-Key");
        if (apiKeyHeader == null || apiKeyHeader.isBlank()) {
            log.warn("[API Key Validation] Missing or blank X-API-Key header for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        log.debug("[API Key Validation] X-API-Key header found, validating with auth service");

        // Call Auth internal validate endpoint with scopes
        String scopesCsv = requiredScopes.stream().collect(Collectors.joining(","));

        // Use service ID via Eureka: prism-auth
        String validateUrl = "http://prism-auth/api/auth/internal/validate?scopes=" + scopesCsv;
        log.debug("[API Key Validation] Calling auth service: {}", validateUrl);

        return webClient.post()
                .uri(validateUrl)
                .header("X-API-Key", apiKeyHeader)
                .exchangeToMono(clientResp -> {
                    if (clientResp.statusCode().value() == 401) {
                        log.warn("[API Key Validation] Auth service returned 401 - Invalid API key for path: {}", path);
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                    if (clientResp.statusCode().is5xxServerError()) {
                        log.error("[API Key Validation] Auth service returned 5xx error - status: {}",
                                clientResp.statusCode());
                        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                        return exchange.getResponse().setComplete();
                    }
                    return clientResp.bodyToMono(ValidationResponse.class)
                            .flatMap(res -> {
                                if (res == null || !res.isValid() || res.getApiKey() == null) {
                                    log.warn(
                                            "[API Key Validation] Invalid response from auth service - null or invalid: {}",
                                            res);
                                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                    return exchange.getResponse().setComplete();
                                }

                                // Forward appId and ownerUserName downstream
                                String appId = res.getApiKey().getAppId();
                                String ownerUserName = res.getApiKey().getOwnerUserName();

                                log.info(
                                        "[API Key Validation] API key validated successfully - appId: {}, owner: {} for path: {}",
                                        appId, ownerUserName, path);

                                ServerWebExchange mutated = exchange.mutate().request(
                                        exchange.getRequest().mutate()
                                                .header("X-App-Id", appId)
                                                .header("X-Owner-User",
                                                        ownerUserName != null ? ownerUserName : "unknown")
                                                .build())
                                        .build();

                                log.debug("[API Key Validation] Headers added - X-App-Id: {}, X-Owner-User: {}",
                                        appId, ownerUserName != null ? ownerUserName : "unknown");

                                return chain.filter(mutated);
                            }).onErrorResume(e -> {
                                log.error("[API Key Validation] Error while validating API key for path: {}", path, e);
                                exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                                return exchange.getResponse().setComplete();
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
        private String ownerUserName;

        public String getAppId() {
            return appId;
        }

        public void setAppId(String a) {
            this.appId = a;
        }

        public String getOwnerUserName() {
            return ownerUserName;
        }

        public void setOwnerUserName(String o) {
            this.ownerUserName = o;
        }
    }
}