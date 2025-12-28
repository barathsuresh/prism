package com.prism.prism_transcoder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
public class StreamConfig {

    @Bean
    @ConfigurationProperties(prefix = "stream")
    public StreamProperties streamProperties() {
        return new StreamProperties();
    }

    @Data
    public static class StreamProperties {
        /**
         * Optional base URL to serve HLS via gateway/CDN (e.g.,
         * https://cdn.example.com)
         */
        private String baseUrl; // if empty, URLs will point to MinIO
    }
}
