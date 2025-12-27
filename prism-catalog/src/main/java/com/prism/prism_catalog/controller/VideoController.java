package com.prism.prism_catalog.controller;

import java.time.Instant;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prism.prism_catalog.dto.CreateVideoRequest;
import com.prism.prism_catalog.dto.PagedResponse;
import com.prism.prism_catalog.dto.PublicVideoResponse;
import com.prism.prism_catalog.dto.UpdateAfterUploadRequest;
import com.prism.prism_catalog.dto.UpdateStreamsRequest;
import com.prism.prism_catalog.dto.UpdateVideoRequest;
import com.prism.prism_catalog.dto.VideoResponse;
import com.prism.prism_catalog.model.enums.VideoStatus;
import com.prism.prism_catalog.model.enums.VideoVisibility;
import com.prism.prism_catalog.service.VideoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Video Catalog Controller
 * 
 * Handles CRUD operations for videos.
 * - App-owned endpoints require X-App-Id (set by gateway after API key
 * validation)
 * - Public endpoints are open (no API key)
 * - Internal endpoints are for service-to-service calls (no API key)
 */
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
@Slf4j
public class VideoController {

    private final VideoService videoService;

    // ==================== APP-OWNED ENDPOINTS ====================

    /**
     * Create a new video entry
     * POST /api/catalog/videos
     */
    @PostMapping("/videos")
    public ResponseEntity<VideoResponse> createVideo(
            @Valid @RequestBody CreateVideoRequest request,
            @RequestHeader("X-App-Id") String appId,
            @RequestHeader(value = "X-Owner-User", required = false, defaultValue = "unknown") String ownerUserName) {

        VideoResponse response = videoService.createVideo(request, appId, ownerUserName);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List videos for the app
     * GET /api/catalog/videos
     */
    @GetMapping("/videos")
    public ResponseEntity<PagedResponse<VideoResponse>> listVideos(
            @RequestHeader("X-App-Id") String appId,
            @RequestParam(required = false) VideoStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) VideoVisibility visibility,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        PagedResponse<VideoResponse> response = videoService.listVideosForApp(
                appId, status, category, visibility, tag, search, fromDate, toDate,
                page, size, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a single video by ID
     * GET /api/catalog/videos/{videoId}
     */
    @GetMapping("/videos/{videoId}")
    public ResponseEntity<VideoResponse> getVideo(
            @RequestHeader("X-App-Id") String appId,
            @PathVariable String videoId) {

        VideoResponse response = videoService.getVideoForApp(appId, videoId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a single video by slug
     * GET /api/catalog/videos/slug/{slug}
     */
    @GetMapping("/videos/slug/{slug}")
    public ResponseEntity<VideoResponse> getVideoBySlug(
            @RequestHeader("X-App-Id") String appId,
            @PathVariable String slug) {

        VideoResponse response = videoService.getVideoBySlugForApp(appId, slug);
        return ResponseEntity.ok(response);
    }

    /**
     * Update video metadata
     * PUT /api/catalog/videos/{videoId}
     */
    @PutMapping("/videos/{videoId}")
    public ResponseEntity<VideoResponse> updateVideo(
            @RequestHeader("X-App-Id") String appId,
            @PathVariable String videoId,
            @Valid @RequestBody UpdateVideoRequest request) {

        VideoResponse response = videoService.updateVideoForApp(appId, videoId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft delete a video
     * DELETE /api/catalog/videos/{videoId}
     */
    @DeleteMapping("/videos/{videoId}")
    public ResponseEntity<Void> deleteVideo(
            @RequestHeader("X-App-Id") String appId,
            @PathVariable String videoId) {

        videoService.deleteVideoForApp(appId, videoId);
        return ResponseEntity.noContent().build();
    }

    // ==================== PUBLIC ENDPOINTS ====================

    /**
     * List public videos
     * GET /api/catalog/videos/public
     */
    @GetMapping("/videos/public")
    public ResponseEntity<PagedResponse<PublicVideoResponse>> listPublicVideos(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        PagedResponse<PublicVideoResponse> response = videoService.listPublicVideos(
                category, tag, search, fromDate, toDate, page, size, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a single public video by ID
     * GET /api/catalog/videos/public/{videoId}
     */
    @GetMapping("/videos/public/{videoId}")
    public ResponseEntity<PublicVideoResponse> getPublicVideo(@PathVariable String videoId) {
        PublicVideoResponse response = videoService.getPublicVideo(videoId);
        return ResponseEntity.ok(response);
    }

    // ==================== INTERNAL ENDPOINTS ====================

    /**
     * Update video after upload (called by prism-upload)
     * PUT /api/catalog/internal/videos/{videoId}/upload
     */
    @PutMapping("/internal/videos/{videoId}/upload")
    public ResponseEntity<Void> updateAfterUpload(
            @PathVariable String videoId,
            @Valid @RequestBody UpdateAfterUploadRequest request) {

        videoService.updateAfterUpload(videoId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Update video streams after transcoding (called by prism-transcoder)
     * PUT /api/catalog/internal/videos/{videoId}/streams
     */
    @PutMapping("/internal/videos/{videoId}/streams")
    public ResponseEntity<Void> updateStreams(
            @PathVariable String videoId,
            @Valid @RequestBody UpdateStreamsRequest request) {

        videoService.updateStreams(videoId, request);
        return ResponseEntity.noContent().build();
    }
}
