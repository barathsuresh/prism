package com.prism.prism_transcoder.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.prism.prism_transcoder.config.MinIOConfig.MinIOProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinIOStorageService {

    private final S3Client s3Client;
    private final MinIOProperties minio;

    public Path downloadToTemp(String objectKey, Path tempDir) throws IOException {
        Files.createDirectories(tempDir);
        Path dest = tempDir.resolve(Path.of(objectKey).getFileName().toString());
        log.info("[TRANSCODER] Downloading from MinIO - bucket: {}, key: {}", minio.getBucketName(), objectKey);
        s3Client.getObject(GetObjectRequest.builder()
                .bucket(minio.getBucketName())
                .key(objectKey)
                .build(), dest);
        return dest;
    }

    public void uploadDirectory(Path dir, String baseKey) throws IOException {
        log.info("[TRANSCODER] Uploading directory to MinIO - bucket: {}, baseKey: {}", minio.getBucketName(), baseKey);
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(Files::isRegularFile).forEach(path -> {
                try {
                    String rel = dir.relativize(path).toString().replace('\\', '/');
                    String key = baseKey.endsWith("/") ? baseKey + rel : baseKey + "/" + rel;
                    String contentType = guessContentType(path);
                    PutObjectRequest req = PutObjectRequest.builder()
                            .bucket(minio.getBucketName())
                            .key(key)
                            .contentType(contentType)
                            .build();
                    s3Client.putObject(req, RequestBody.fromFile(path));
                    log.debug("[TRANSCODER] File uploaded - key: {}", key);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to upload file: " + path, e);
                }
            });
        }
    }

    private String guessContentType(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".m3u8"))
            return "application/vnd.apple.mpegurl";
        if (name.endsWith(".ts"))
            return "video/MP2T";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg"))
            return "image/jpeg";
        if (name.endsWith(".png"))
            return "image/png";
        return "application/octet-stream";
    }
}
