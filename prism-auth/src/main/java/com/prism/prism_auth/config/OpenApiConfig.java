package com.prism.prism_auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Value("${app.dev-server-url:http://localhost:8080}")
    private String devServerUrl;

    final String securityScheme = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement()
                        .addList(securityScheme))
                .addServersItem(new Server()
                        .url(devServerUrl)
                        .description("API Gateway"))
                .info(new Info()
                        .title("Prism Auth API")
                        .version("1.0.0")
                        .description("API documentation for Prism Auth Service"))
                .components(new Components()
                        .addSecuritySchemes(securityScheme,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
