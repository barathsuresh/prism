package com.prism.prism_upload.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.prism.prism_upload.dto.TranscodeMessage;
import com.prism.prism_upload.dto.UpdateAfterUploadRequest;
import com.prism.prism_upload.dto.UploadProgressEvent;
import com.prism.prism_upload.dto.UploadResponse;
import com.prism.prism_upload.dto.VideoInfo;
import com.prism.prism_upload.exception.DuplicateUploadException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadService {

        private final StorageService storageService;
        private final CatalogService catalogService;
        private final MessagePublisher messagePublisher;
        // Store progress sinks for each video upload
        private final Map<String, Sinks.Many<UploadProgressEvent>> progressSinks = new ConcurrentHashMap<>();

        // Allowed video file extensions
        private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
                        ".mp4", ".avi", ".mov", ".mkv", ".flv", ".wmv", ".webm", ".m4v");

        // Allowed MIME types
        private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
                        "video/mp4", "video/x-msvideo", "video/quicktime", "video/x-matroska",
                        "video/x-flv", "video/x-ms-wmv", "video/webm", "video/x-m4v");

        // Statuses allowed to start a new upload
        private static final List<String> ALLOWED_START_STATUSES = Arrays.asList("PENDING", "FAILED");

        /**
         * Upload video file
         */
        public Mono<UploadResponse> uploadVideo(String videoId, String appId, FilePart filePart) {
                log.info("[UPLOAD] Starting upload - videoId: {}, appId: {}, filename: {}",
                                videoId, appId, filePart.filename());

                // Check catalog status to prevent duplicate uploads
                return catalogService.getVideo(appId, videoId)
                                .onErrorResume(WebClientResponseException.NotFound.class, ex -> {
                                        return Mono.error(new IllegalArgumentException("Video not found: " + videoId));
                                })
                                .flatMap((VideoInfo info) -> {
                                        String status = info.getStatus() != null ? info.getStatus() : "";
                                        if (!ALLOWED_START_STATUSES.contains(status)) {
                                                String msg = String.format(
                                                                "Upload rejected: video status is %s. Upload already in progress or completed.",
                                                                status);
                                                log.warn("[UPLOAD] Duplicate upload rejected - videoId: {}, status: {}",
                                                                videoId, status);
                                                return Mono.error(new DuplicateUploadException(msg));
                                        }

                                        // Validate file and proceed
                                        return validateFile(filePart)
                                                        .flatMap(valid -> {
                                                                // Create progress sink for this upload
                                                                Sinks.Many<UploadProgressEvent> progressSink = Sinks
                                                                                .many().multicast()
                                                                                .onBackpressureBuffer();
                                                                progressSinks.put(videoId, progressSink);

                                                                // Upload to MinIO
                                                                return storageService
                                                                                .uploadFile(videoId, appId, filePart,
                                                                                                progressSink)
                                                                                .flatMap(objectKey -> {
                                                                                        String storageBasePath = storageService
                                                                                                        .getStorageBasePath(
                                                                                                                        appId,
                                                                                                                        videoId);

                                                                                        // Update catalog service
                                                                                        UpdateAfterUploadRequest catalogUpdate = UpdateAfterUploadRequest
                                                                                                        .builder()
                                                                                                        .storageBasePath(
                                                                                                                        storageBasePath)
                                                                                                        .sourceFileName(filePart
                                                                                                                        .filename())
                                                                                                        .sourceFileSizeBytes(
                                                                                                                        0L) // will
                                                                                                                            // be
                                                                                                                            // updated
                                                                                                                            // from
                                                                                                                            // actual
                                                                                                                            // size
                                                                                                        .sourceContentType(
                                                                                                                        filePart.headers()
                                                                                                                                        .getContentType() != null
                                                                                                                                                        ? filePart.headers()
                                                                                                                                                                        .getContentType()
                                                                                                                                                                        .toString()
                                                                                                                                                        : "application/octet-stream")
                                                                                                        .status(UpdateAfterUploadRequest.VideoStatus.PROCESSING)
                                                                                                        .build();

                                                                                        return catalogService
                                                                                                        .updateAfterUpload(
                                                                                                                        videoId,
                                                                                                                        catalogUpdate)
                                                                                                        .then(Mono.just(objectKey))
                                                                                                        .then(Mono.fromCallable(
                                                                                                                        () -> {
                                                                                                                                // Publish
                                                                                                                                // transcode
                                                                                                                                // message
                                                                                                                                TranscodeMessage transcodeMessage = TranscodeMessage
                                                                                                                                                .builder()
                                                                                                                                                .videoId(videoId)
                                                                                                                                                .appId(appId)
                                                                                                                                                .sourceFilePath(objectKey)
                                                                                                                                                .storageBasePath(
                                                                                                                                                                storageBasePath)
                                                                                                                                                .fileName(filePart
                                                                                                                                                                .filename())
                                                                                                                                                .contentType(catalogUpdate
                                                                                                                                                                .getSourceContentType())
                                                                                                                                                .build();
                                                                                                                                return transcodeMessage;
                                                                                                                        }));
                                                                                })
                                                                                .flatMap(transcodeMessage -> messagePublisher
                                                                                                .publishTranscodeMessage(
                                                                                                                transcodeMessage)
                                                                                                .then(Mono.just(transcodeMessage)))
                                                                                .map(transcodeMessage -> UploadResponse
                                                                                                .builder()
                                                                                                .videoId(videoId)
                                                                                                .message("File uploaded successfully and queued for transcoding")
                                                                                                .fileName(filePart
                                                                                                                .filename())
                                                                                                .fileSize(0L)
                                                                                                .storageBasePath(
                                                                                                                transcodeMessage.getStorageBasePath())
                                                                                                .status("PROCESSING")
                                                                                                .build())
                                                                                .doOnSuccess(response -> log.info(
                                                                                                "[UPLOAD] Upload completed - videoId: {}, status: PROCESSING, queued for transcoding",
                                                                                                videoId))
                                                                                .doOnError(error -> {
                                                                                        log.error("[UPLOAD] Upload failed - videoId: {}, error: {}",
                                                                                                        videoId,
                                                                                                        error.getMessage(),
                                                                                                        error);
                                                                                        // Emit failure event
                                                                                        Sinks.Many<UploadProgressEvent> sink = progressSinks
                                                                                                        .get(videoId);
                                                                                        if (sink != null) {
                                                                                                sink.tryEmitNext(
                                                                                                                UploadProgressEvent
                                                                                                                                .builder()
                                                                                                                                .videoId(videoId)
                                                                                                                                .status("FAILED")
                                                                                                                                .message("Upload failed: "
                                                                                                                                                + error.getMessage())
                                                                                                                                .build());
                                                                                                sink.tryEmitComplete();
                                                                                        }
                                                                                })
                                                                                .doFinally(signalType -> {
                                                                                        // Complete and remove progress
                                                                                        // sink
                                                                                        Sinks.Many<UploadProgressEvent> sink = progressSinks
                                                                                                        .remove(videoId);
                                                                                        if (sink != null) {
                                                                                                sink.tryEmitComplete();
                                                                                        }
                                                                                });
                                                        });
                                });
        }

        /**
         * Get upload progress stream
         */
        public Flux<UploadProgressEvent> getUploadProgress(String videoId) {
                Sinks.Many<UploadProgressEvent> sink = progressSinks.get(videoId);
                if (sink == null) {
                        // No active upload yet, create a temporary sink and wait for events
                        // This allows clients to connect before upload starts
                        Sinks.Many<UploadProgressEvent> tempSink = Sinks.many().multicast().onBackpressureBuffer();
                        progressSinks.put(videoId, tempSink);
                        return tempSink.asFlux().delaySubscription(java.time.Duration.ofMillis(100));
                }
                return sink.asFlux();
        }

        /**
         * Validate uploaded file
         */
        private Mono<Boolean> validateFile(FilePart filePart) {
                String filename = filePart.filename().toLowerCase();
                String contentType = filePart.headers().getContentType() != null
                                ? filePart.headers().getContentType().toString()
                                : "";

                // Check file extension
                boolean validExtension = ALLOWED_EXTENSIONS.stream()
                                .anyMatch(filename::endsWith);

                if (!validExtension) {
                        return Mono.error(new IllegalArgumentException(
                                        "Invalid file type. Allowed extensions: "
                                                        + String.join(", ", ALLOWED_EXTENSIONS)));
                }

                // Check MIME type
                boolean validMimeType = ALLOWED_MIME_TYPES.stream()
                                .anyMatch(contentType::contains);

                if (!validMimeType && !contentType.isEmpty()) {
                        log.warn("[UPLOAD] Invalid MIME type - type: {}, filename: {}", contentType, filename);
                        // Don't fail on MIME type, just warn
                }

                return Mono.just(true);
        }
}
