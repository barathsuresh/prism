package com.prism.prism_stream.service;

import java.time.Duration;

import org.springframework.stereotype.Service;

import com.prism.prism_stream.config.MinIOConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinIOPresignService {

    private final MinIOConfig minIOConfig;

    /**
     * Generate a presigned URL for an object in MinIO
     * Presigned URLs expire after the specified duration
     * 
     * @param objectKey         the S3 object key (path in bucket)
     * @param expirationMinutes how long the URL is valid (default 15 min)
     * @return presigned URL string
     */
    public String generatePresignedUrl(String objectKey, int expirationMinutes) {
        try (S3Presigner presigner = S3Presigner.builder()
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build()) // avoid
                                                                                                      // bucket-as-subdomain
                                                                                                      // DNS
                .region(software.amazon.awssdk.regions.Region.US_EAST_1)
                .credentialsProvider(software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                        software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(
                                minIOConfig.getAccessKey(),
                                minIOConfig.getSecretKey())))
                .endpointOverride(java.net.URI.create(minIOConfig.getEndpoint()))
                .build()) {

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(minIOConfig.getBucket())
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
            String presignedUrl = presignedRequest.url().toString();

            log.debug("[STREAM-SERVICE] Generated presigned URL - key: {}, expiresIn: {}min", objectKey,
                    expirationMinutes);
            return presignedUrl;
        }
    }

    /**
     * Generate presigned URL with configured default expiration (12 hours)
     */
    public String generatePresignedUrl(String objectKey) {
        return generatePresignedUrl(objectKey, minIOConfig.getPresignedUrlExpirationMinutes());
    }
}
