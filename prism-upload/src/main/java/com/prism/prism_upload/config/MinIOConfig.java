package com.prism.prism_upload.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * MinIO Configuration for Object Storage
 * 
 * MinIO is an object storage system (like Amazon S3, Google Cloud Storage).
 * Think of it as a file system in the cloud where you can store any type of
 * file.
 * 
 * Key Concepts:
 * 1. BUCKET: A container for objects (like a top-level folder)
 * Example: "prism-videos" bucket holds all your video files
 * 
 * 2. OBJECT KEY: The path/name of a file inside a bucket
 * Example: "app123/video456/source/movie.mp4"
 * 
 * 3. S3 PROTOCOL: Standard API for object storage (AWS S3 compatible)
 * MinIO uses the same API as Amazon S3, so we use AWS SDK
 * 
 * 4. ASYNC CLIENT: Non-blocking uploads/downloads for better performance
 * While uploading one file, your app can process other requests
 * 
 * Why MinIO instead of local file system?
 * - Scalable: Can store unlimited files across multiple servers
 * - Durable: Files are replicated, won't lose data if disk fails
 * - Accessible: Files can be accessed from any service (upload, transcoder,
 * stream)
 * - Cost-effective: Cheaper than databases for large files
 */
@Configuration
public class MinIOConfig {

    /**
     * Load MinIO configuration from application.yaml
     * 
     * Reads properties like:
     * minio:
     * endpoint: http://localhost:9000
     * accessKey: minioadmin
     * secretKey: minioadmin
     * bucket: prism-videos
     */
    @Bean
    @ConfigurationProperties(prefix = "minio")
    public MinIOProperties minioProperties() {
        return new MinIOProperties();
    }

    /**
     * S3 Async Client - Non-blocking file upload/download
     * 
     * This client allows reactive/async operations.
     * When uploading a 1GB file, your app doesn't freeze waiting for upload to
     * complete.
     * 
     * Configuration:
     * - endpointOverride: Points to MinIO server instead of AWS S3
     * - credentials: Username/password for MinIO (accessKey/secretKey)
     * - forcePathStyle: MinIO requires path-style URLs (bucket.host vs host/bucket)
     * 
     * Usage in code:
     * s3AsyncClient.putObject(request, body) - returns CompletableFuture<Response>
     */
    @Bean
    public S3AsyncClient s3AsyncClient(MinIOProperties properties) {
        // Create credentials (like username/password for MinIO)
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.getAccessKey(),
                properties.getSecretKey());

        return S3AsyncClient.builder()
                .endpointOverride(URI.create(properties.getEndpoint())) // MinIO URL (not AWS)
                .region(Region.of(properties.getRegion())) // Required by S3 API, doesn't matter for MinIO
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .forcePathStyle(true) // MinIO requires path-style URLs: http://localhost:9000/bucket/key
                .build();
    }

    /**
     * S3 Synchronous Client - Blocking file operations
     * 
     * This client blocks the thread until operation completes.
     * Use this for simple scripts or when you need to wait for result.
     * 
     * For web APIs with many concurrent requests, prefer S3AsyncClient.
     * 
     * Usage:
     * s3Client.putObject(request, body) - blocks until upload completes
     */
    @Bean
    public S3Client s3Client(MinIOProperties properties) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.getAccessKey(),
                properties.getSecretKey());

        return S3Client.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .forcePathStyle(true) // Required for MinIO
                .build();
    }

    /**
     * MinIO Configuration Properties
     * 
     * These values are loaded from application.yaml or environment variables
     */
    @Data
    public static class MinIOProperties {
        private String endpoint = "http://localhost:9000";
        private String accessKey;
        private String secretKey;
        private String bucket = "prism-videos";
        private String region = "us-east-1";
        /** Max file size allowed for uploads (5GB) */
        private long maxFileSize = 5368709120L; // 5GB default
    }
}
