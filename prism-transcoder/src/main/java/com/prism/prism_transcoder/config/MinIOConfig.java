package com.prism.prism_transcoder.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class MinIOConfig {

    @Bean
    @ConfigurationProperties(prefix = "minio")
    public MinIOProperties minioProperties() {
        return new MinIOProperties();
    }

    @Bean
    public S3Client s3Client(MinIOProperties properties) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.getAccessKey(),
                properties.getSecretKey());

        return S3Client.builder()
                .endpointOverride(URI.create(properties.getUrl()))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .forcePathStyle(true)
                .build();
    }

    @Data
    public static class MinIOProperties {
        private String url = "http://localhost:9000"; // MinIO endpoint
        private String accessKey; // Access key (username)
        private String secretKey; // Secret key (password)
        private String bucketName = "prism-videos"; // Bucket to store outputs
        private String region = "us-east-1"; // S3 API requires a region
        private String tempDir; // Temp dir for transcoding workspace
    }
}
