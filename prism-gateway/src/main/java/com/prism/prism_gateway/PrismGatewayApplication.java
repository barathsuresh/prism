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
            .build();
    }

}
