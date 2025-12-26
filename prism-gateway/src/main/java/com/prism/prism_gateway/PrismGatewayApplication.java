package com.prism.prism_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableDiscoveryClient
public class PrismGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrismGatewayApplication.class, args);
    }

    // This is the "Java Way" to define routes (from the guide)
    @Bean
    public RouteLocator myRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("google-test", p -> p
                        .path("/google") // If user goes to localhost:8080/google
                        .filters(f -> f.rewritePath("/google", "/")) // Rewrite the path
                        .uri("https://google.com")) // Send them to Google
                // 2. Auth Service Route
                // Any URL starting with /auth goes to the Login/Register service
                .route("auth-service", p -> p
                        .path("/api/auth/**")
                        .uri("lb://prism-auth")) // lb:// means "Load Balance" using Eureka

                // 3. Catalog Service Route
                // Any URL starting with /videos goes to the Metadata service
                .route("catalog-service", p -> p
                        .path("/api/videos/**")
                        .uri("lb://prism-catalog"))

                // 4. Upload Service Route
                // Any URL starting with /uploads goes to the File Ingest service
                .route("upload-service", p -> p
                        .path("/api/uploads/**")
                        .uri("lb://prism-upload"))

                // 5. Streaming Service Route
                // Any URL starting with /stream goes to the Video Player service
                .route("stream-service", p -> p
                        .path("/api/stream/**")
                        .uri("lb://prism-stream"))

                .build();
    }

}
