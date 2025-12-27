package com.prism.prism_upload.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;

import com.prism.prism_upload.config.MinIOConfig;
import com.prism.prism_upload.dto.UploadProgressEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final S3AsyncClient s3AsyncClient;
    private final MinIOConfig.MinIOProperties minioProperties;

    /**
     * Upload file to MinIO with progress tracking
     */
    public Mono<String> uploadFile(String videoId, String appId, FilePart filePart,
            Sinks.Many<UploadProgressEvent> progressSink) {

        String objectKey = generateObjectKey(appId, videoId, filePart.filename());
        log.info("Uploading file to MinIO: bucket={}, key={}", minioProperties.getBucket(), objectKey);

        // Create temporary file to store upload
        return Mono.fromCallable(() -> Files.createTempFile("upload-", ".tmp"))
                .flatMap(tempFile -> {
                    // Write file parts to temp file with progress tracking
                    AtomicLong bytesWritten = new AtomicLong(0);

                    // Get total file size from headers if available
                    long contentLength = filePart.headers().getContentLength() > 0
                            ? filePart.headers().getContentLength()
                            : -1;
                    log.info("Starting upload stream: videoId={}, contentLength={}", videoId, contentLength);

                    return filePart.content()
                            .doOnNext(dataBuffer -> {
                            })
                            .as(flux -> DataBufferUtils.write(flux, tempFile, StandardOpenOption.CREATE,
                                    StandardOpenOption.WRITE))
                            .then(Mono.fromCallable(() -> Files.size(tempFile)))
                            .flatMap(fileSize -> {
                                log.info("File written to temp: {} bytes", fileSize);

                                // TODO: Implement chunked progress tracking for large files
                                // Currently emits 100% progress when temp file is written.
                                // For better UX with large files, implement progress tracking during MinIO
                                // upload
                                // by reading file in chunks and tracking bytes uploaded to cloud storage.
                                // Emit progress once we know the file size (100% written to disk)
                                emitProgress(progressSink, videoId, fileSize, fileSize, "UPLOADING");
                                PutObjectRequest putRequest = PutObjectRequest.builder()
                                        .bucket(minioProperties.getBucket())
                                        .key(objectKey)
                                        .contentType(filePart.headers().getContentType() != null
                                                ? filePart.headers().getContentType().toString()
                                                : "application/octet-stream")
                                        .contentLength(fileSize)
                                        .build();

                                return Mono.fromFuture(s3AsyncClient.putObject(putRequest,
                                        AsyncRequestBody.fromFile(tempFile)))
                                        .doOnSuccess(response -> {
                                            log.info("Successfully uploaded to MinIO: {}", objectKey);
                                            emitProgress(progressSink, videoId, fileSize, fileSize, "COMPLETED");
                                        })
                                        .doOnError(error -> {
                                            log.error("Failed to upload to MinIO", error);
                                            emitProgress(progressSink, videoId, bytesWritten.get(), fileSize, "FAILED");
                                        })
                                        .then(Mono.just(objectKey))
                                        .doFinally(signalType -> {
                                            // Clean up temp file
                                            try {
                                                Files.deleteIfExists(tempFile);
                                                log.debug("Deleted temp file: {}", tempFile);
                                            } catch (IOException e) {
                                                log.warn("Failed to delete temp file: {}", tempFile, e);
                                            }
                                        });
                            });
                });
    }

    /**
     * Generate object key for MinIO storage
     * Format: {appId}/{videoId}/source/{filename}
     */
    public String generateObjectKey(String appId, String videoId, String filename) {
        return String.format("%s/%s/source/%s", appId, videoId, filename);
    }

    /**
     * Get storage base path for video
     */
    public String getStorageBasePath(String appId, String videoId) {
        return String.format("%s/%s", appId, videoId);
    }

    /**
     * Emit progress event
     */
    private void emitProgress(Sinks.Many<UploadProgressEvent> sink, String videoId,
            long bytesUploaded, long totalBytes, String status) {
        int percentage = 0;
        if (totalBytes > 0) {
            percentage = (int) ((bytesUploaded * 100) / totalBytes);
        } else if (totalBytes == -1) {
            // If total size unknown, don't calculate percentage, just report bytes
            percentage = 0;
        }

        UploadProgressEvent event = UploadProgressEvent.builder()
                .videoId(videoId)
                .bytesUploaded(bytesUploaded)
                .totalBytes(totalBytes)
                .percentage(percentage)
                .status(status)
                .message(String.format("%s: uploaded %d bytes%s", status, bytesUploaded,
                        totalBytes > 0 ? String.format(" / %d (%d%%)", totalBytes, percentage) : ""))
                .build();

        log.debug("Emitting progress: videoId={}, bytes={}/{}, percentage={}, status={}",
                videoId, bytesUploaded, totalBytes, percentage, status);
        sink.tryEmitNext(event);
    }
}
