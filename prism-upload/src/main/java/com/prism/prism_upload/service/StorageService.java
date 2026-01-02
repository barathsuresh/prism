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

/**
 * Storage Service - Handles file uploads to MinIO
 * 
 * Flow:
 * 1. Receive file from HTTP request (FilePart - reactive stream of bytes)
 * 2. Write file to temporary local disk (to get file size)
 * 3. Upload file from temp location to MinIO (object storage)
 * 4. Delete temporary file
 * 5. Return object key (path in MinIO)
 * 
 * Why temp file?
 * - Multipart uploads don't include Content-Length header
 * - We need to know file size before uploading to MinIO
 * - Also allows progress tracking
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    // MinIO client for async uploads (non-blocking)
    private final S3AsyncClient s3AsyncClient;

    // MinIO configuration (bucket name, endpoint, etc.)
    private final MinIOConfig.MinIOProperties minioProperties;

    /**
     * Upload file to MinIO with progress tracking
     * 
     * @param videoId      - unique video identifier
     * @param appId        - application/tenant identifier for multi-tenancy
     * @param filePart     - reactive stream of file bytes from HTTP upload
     * @param progressSink - reactive sink to emit progress events for SSE
     * @return Mono<String> - object key (path) in MinIO where file is stored
     */
    public Mono<String> uploadFile(String videoId, String appId, FilePart filePart,
            Sinks.Many<UploadProgressEvent> progressSink) {

        String objectKey = generateObjectKey(appId, videoId, filePart.filename());
        log.info("[UPLOAD] Starting file upload to MinIO - videoId: {}, appId: {}, filename: {}", videoId, appId,
                filePart.filename());

        // Create temporary file to store upload
        return Mono.fromCallable(() -> Files.createTempFile("upload-", ".tmp"))
                .flatMap(tempFile -> {
                    // Write file parts to temp file with progress tracking
                    AtomicLong bytesWritten = new AtomicLong(0);

                    // Get total file size from headers if available
                    long contentLength = filePart.headers().getContentLength() > 0
                            ? filePart.headers().getContentLength()
                            : -1;
                    log.debug("[UPLOAD] Stream started - videoId: {}, size: {}", videoId,
                            contentLength > 0 ? contentLength : "unknown");

                    return filePart.content()
                            .doOnNext(dataBuffer -> {
                            })
                            .as(flux -> DataBufferUtils.write(flux, tempFile, StandardOpenOption.CREATE,
                                    StandardOpenOption.WRITE))
                            .then(Mono.fromCallable(() -> Files.size(tempFile)))
                            .flatMap(fileSize -> {
                                log.debug("[UPLOAD] File written to temp - videoId: {}, size: {} bytes", videoId,
                                        fileSize);

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
                                            log.info(
                                                    "[UPLOAD] File uploaded successfully to MinIO - videoId: {}, key: {}",
                                                    videoId, objectKey);
                                            emitProgress(progressSink, videoId, fileSize, fileSize, "COMPLETED");
                                        })
                                        .doOnError(error -> {
                                            log.error("[UPLOAD] MinIO upload failed - videoId: {}", videoId, error);
                                            emitProgress(progressSink, videoId, bytesWritten.get(), fileSize, "FAILED");
                                        })
                                        .then(Mono.just(objectKey))
                                        .doFinally(signalType -> {
                                            // Clean up temp file
                                            try {
                                                Files.deleteIfExists(tempFile);
                                                log.debug("[UPLOAD] Temp file deleted - videoId: {}", videoId);
                                            } catch (IOException e) {
                                                log.warn("[UPLOAD] Failed to delete temp file - videoId: {}", videoId,
                                                        e);
                                            }
                                        });
                            });
                });
    }

    /**
     * Generate object key (file path) for MinIO storage
     * 
     * Object Key Structure:
     * {appId}/{videoId}/source/{filename}
     * 
     * Example: "app123/video456/source/movie.mp4"
     * 
     * Why this structure?
     * - appId: Multi-tenancy, each app has its own folder
     * - videoId: Each video has its own folder for all related files
     * - source/: Stores original uploaded file
     * - Later: hls/, thumbnails/ will be added by transcoder
     * 
     * Full path in MinIO: s3://prism-videos/app123/video456/source/movie.mp4
     */
    public String generateObjectKey(String appId, String videoId, String filename) {
        return String.format("%s/%s/source/%s", appId, videoId, filename);
    }

    /**
     * Get storage base path for video
     * 
     * Returns: "app123/video456"
     * Used by transcoder to know where to save HLS files and thumbnails
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

        log.debug("[UPLOAD] Emitting progress - videoId: {}, bytes: {}/{}, status: {}",
                videoId, bytesUploaded, totalBytes, status);
        sink.tryEmitNext(event);
    }
}
