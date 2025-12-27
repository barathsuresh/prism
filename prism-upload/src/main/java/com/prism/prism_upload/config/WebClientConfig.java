package com.prism.prism_upload.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    @Qualifier("loadBalancedWebClientBuilder")
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean(name = "catalogWebClient")
    public WebClient catalogWebClient(@Qualifier("loadBalancedWebClientBuilder") WebClient.Builder builder) {
        // Use Eureka service name for load balancing
        return builder.baseUrl("http://prism-catalog").build();
    }
}
