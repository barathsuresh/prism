package com.prism.prism_gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
@EnableDiscoveryClient
@Slf4j
@RequiredArgsConstructor
public class PrismGatewayApplication {

        private final Environment environment;

        public static void main(String[] args) {
                log.info("[GATEWAY] Starting Prism Gateway");
                SpringApplication.run(PrismGatewayApplication.class, args);
        }

        @EventListener(ApplicationReadyEvent.class)
        public void onApplicationReady() {
                String port = environment.getProperty("server.port", "8080");
                String contextPath = environment.getProperty("server.servlet.context-path", "");

                log.info("[GATEWAY] Prism Gateway started successfully");
                log.info("[GATEWAY] Local access: http://localhost:{}{}", port, contextPath);

                try {
                        String hostAddress = java.net.InetAddress.getLocalHost().getHostAddress();
                        if (!hostAddress.equals("127.0.0.1")) {
                                log.info("[GATEWAY] Network access: http://{}:{}{}", hostAddress, port, contextPath);
                        }
                } catch (Exception e) {
                        log.debug("[GATEWAY] Could not determine network address", e);
                }
        }


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
                                                .path("/api/catalog/**")
                                                .uri("lb://prism-catalog"))

                                // 4. Upload Service Route
                                // Any URL starting with /upload goes to the File Ingest service
                                .route("upload-service", p -> p
                                                .path("/api/upload/**")
                                                .uri("lb://prism-upload"))

                                // 5. Streaming Service Route
                                // Any URL starting with /stream goes to the Video Player service
                                .route("stream-service-api", p -> p
                                                .path("/api/stream/**")
                                                .uri("lb://prism-stream"))

                                // 6. Smart Proxy Route (HLS manifest/segment streaming)
                                .route("stream-smart-proxy", p -> p
                                                .path("/stream/**")
                                                .uri("lb://prism-stream"))

                                .build();
        }

        @Bean
        @LoadBalanced
        public WebClient.Builder webClientBuilder() {
                return WebClient.builder();
        }

}
