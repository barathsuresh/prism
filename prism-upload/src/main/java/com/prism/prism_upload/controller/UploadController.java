package com.prism.prism_upload.controller;

import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.prism.prism_upload.dto.UploadProgressEvent;
import com.prism.prism_upload.dto.UploadResponse;
import com.prism.prism_upload.service.UploadService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Upload Controller
 * Handles video file uploads with progress tracking
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Slf4j
public class UploadController {

    private final UploadService uploadService;

    /**
     * Upload video file
     * POST /api/upload/videos/{videoId}/file
     * 
     * Requires X-App-Id header (injected by gateway from API key)
     */
    @PostMapping(value = "/videos/{videoId}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<UploadResponse> uploadVideo(
            @PathVariable String videoId,
            @RequestHeader("X-App-Id") String appId,
            @RequestPart("file") FilePart filePart) {

        log.info("Upload request received: videoId={}, appId={}, filename={}",
                videoId, appId, filePart.filename());

        return uploadService.uploadVideo(videoId, appId, filePart);
    }

    /**
     * Get upload progress via Server-Sent Events
     * GET /api/upload/videos/{videoId}/progress
     * 
     * Returns real-time progress updates as SSE stream
     * Note: X-App-Id is optional because EventSource doesn't support custom headers
     */
    @GetMapping(value = "/videos/{videoId}/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<UploadProgressEvent> getUploadProgress(@PathVariable String videoId) {

        log.info("Progress stream requested for videoId={}", videoId);

        return uploadService.getUploadProgress(videoId);
    }
}
